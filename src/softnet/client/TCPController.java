/*
*	Copyright 2023 Robert Koifman
*
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*/

package softnet.client;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.UUID;

import softnet.*;
import softnet.asn.*;
import softnet.core.*;
import softnet.exceptions.*;
import softnet.utils.ByteConverter;

class TCPController 
{
	private ClientEndpoint clientEndpoint;
	private ThreadPool threadPool;
	private Scheduler scheduler;
	private Object mutex;
	private Channel channel;
	private LinkedList<TcpRequest> requestList;

	private enum StatusEnum
	{ 
		Disconnected, Connected, Online
	}
	private StatusEnum clientStatus = StatusEnum.Disconnected;

	public TCPController(ClientEndpoint clientEndpoint)
	{
		this.clientEndpoint = clientEndpoint;
		this.threadPool = clientEndpoint.threadPool;
		this.scheduler = clientEndpoint.scheduler;
		requestList = new LinkedList<TcpRequest>();
		mutex = new Object();
	}

	public void onEndpointConnected(Channel channel)
	{
		channel.registerComponent(Constants.Client.TcpController.ModuleId, 
			new MsgAcceptor<Channel>()
			{
				public void accept(byte[] message, Channel _channel) throws AsnException, FormatException, SoftnetException
				{
					onMessageReceived(message, _channel);
				}
			});		
		
		synchronized(mutex)
		{
			clientStatus = StatusEnum.Connected;
			this.channel = channel;
		}
	}
	
	public void onClientOnline()
	{
		synchronized(mutex)
		{
			clientStatus = StatusEnum.Online;
		}
	}
	
	public void onEndpointDisconnected()
	{
		synchronized(mutex)
		{
			clientStatus = StatusEnum.Disconnected;
			channel = null;
			
			for(TcpRequest request: requestList)
			{
				request.timeoutControlTask.cancel();
				
				if(request.tcpConnector != null)
					request.tcpConnector.abort();

				if(request.socketChannel != null)
					closeChannel(request.socketChannel);

				final TcpRequest f_request = request;				
				Runnable runnable = new Runnable()
				{
					@Override
					public void run()
					{
						f_request.responseHandler.onError(new ResponseContext(clientEndpoint, f_request.remoteService, f_request.attachment), new ClientOfflineSoftnetException());
					}
				};
				threadPool.execute(runnable);				
			}
			requestList.clear();
		}	
	}
	
	public void onEndpointClosed()
	{
		synchronized(mutex)
		{
			clientStatus = StatusEnum.Disconnected;

			for(TcpRequest request: requestList)
			{
				request.timeoutControlTask.cancel();	
				
				if(request.tcpConnector != null)
					request.tcpConnector.abort();
				
				if(request.socketChannel != null)
					closeChannel(request.socketChannel);
			}
			requestList.clear();
		}	
	}
	
	public void onRemoteServiceOffline(long serviceId, Channel channel)
	{
		synchronized(mutex)
		{
			if(channel.closed())
				return;

			for(int i = requestList.size()-1; i >= 0; i--)
			{
				TcpRequest request = requestList.get(i);
				if(request.remoteService.getId() == serviceId)
				{
					requestList.remove(i);
					
					request.timeoutControlTask.cancel();
					
					if(request.tcpConnector != null)
						request.tcpConnector.abort();
						
					if(request.socketChannel != null)
						closeChannel(request.socketChannel);

					final TcpRequest f_request = request;
					Runnable runnable = new Runnable()
					{
						@Override
						public void run()
						{
							f_request.responseHandler.onError(new ResponseContext(clientEndpoint, f_request.remoteService, f_request.attachment), new ServiceOfflineSoftnetException());
						}
					};
					threadPool.execute(runnable);
				}
			}			
		}
	}
	
	public void connect(RemoteService remoteService, int virtualPort, TCPOptions tcpOptions, TCPResponseHandler responseHandler)
	{
		if(remoteService == null)
			throw new IllegalArgumentException("The argument 'remoteService' is null."); 

		if(responseHandler == null)
			throw new IllegalArgumentException("The argument 'responseHandler' is null."); 

		try
		{
			if(remoteService.isOnline() == false)
				throw new ServiceOfflineSoftnetException();
			
			synchronized(mutex)
			{
				if(clientStatus != StatusEnum.Online)
					throw new ClientOfflineSoftnetException();
				
				UUID requestUid = UUID.randomUUID();
				
				TcpRequest request = new TcpRequest(requestUid);
				request.remoteService = remoteService;
				request.virtualPort = virtualPort;
				request.tcpOptions = tcpOptions;
				request.responseHandler = responseHandler;
				Acceptor<Object> acceptor = new Acceptor<Object>()
				{
					public void accept(Object state) { onConnectionAttemptTimedOut(state); }
				};
				request.timeoutControlTask = new ScheduledTask(acceptor, request);
				requestList.add(request);
				
				ASNEncoder asnEncoder = new ASNEncoder();
				SequenceEncoder rootSequence = asnEncoder.Sequence();
				rootSequence.OctetString(requestUid);
				rootSequence.Int64(remoteService.getId());
				rootSequence.Int32(virtualPort);
				SoftnetMessage message = MsgBuilder.Create(Constants.Client.TcpController.ModuleId, Constants.Client.TcpController.REQUEST, asnEncoder);
				
				channel.send(message);
				scheduler.add(request.timeoutControlTask, Constants.TcpConnectingWaitSeconds);
			}
		}
		catch(SoftnetException ex)
		{
			responseHandler.onError(new ResponseContext(clientEndpoint, remoteService, null), ex);
		}
	}

	public void connect(RemoteService remoteService, int virtualPort, TCPOptions tcpOptions, TCPResponseHandler responseHandler, RequestParams requestParams)
	{
		if(remoteService == null)
			throw new IllegalArgumentException("The argument 'remoteService' is null."); 

		if(responseHandler == null)
			throw new IllegalArgumentException("The argument 'responseHandler' is null."); 

		if(requestParams == null)
			throw new IllegalArgumentException("The argument 'requestParams' is null."); 

		try
		{
			if(remoteService.isOnline() == false)
				throw new ServiceOfflineSoftnetException();
			
			synchronized(mutex)
			{
				if(clientStatus != StatusEnum.Online)
					throw new ClientOfflineSoftnetException();
				
				UUID requestUid = UUID.randomUUID();
				
				TcpRequest request = new TcpRequest(requestUid);
				request.remoteService = remoteService;
				request.virtualPort = virtualPort;
				request.tcpOptions = tcpOptions;
				request.responseHandler = responseHandler;
				request.attachment = requestParams.attachment;								
				Acceptor<Object> acceptor = new Acceptor<Object>()
				{
					public void accept(Object state) { onConnectionAttemptTimedOut(state); }
				};
				request.timeoutControlTask = new ScheduledTask(acceptor, request);
				requestList.add(request);
				
				ASNEncoder asnEncoder = new ASNEncoder();
				SequenceEncoder rootSequence = asnEncoder.Sequence();
				rootSequence.OctetString(requestUid);
				rootSequence.Int64(remoteService.getId());
				rootSequence.Int32(virtualPort);
				if(requestParams.sessionTag != null)
					rootSequence.OctetString(1, requestParams.getSessionTagEncoding());
				SoftnetMessage message = MsgBuilder.Create(Constants.Client.TcpController.ModuleId, Constants.Client.TcpController.REQUEST, asnEncoder);
				
				channel.send(message);
				scheduler.add(request.timeoutControlTask, requestParams.waitSeconds > 0 ? requestParams.waitSeconds : Constants.TcpConnectingWaitSeconds);
			}
		}
		catch(SoftnetException ex)
		{
			responseHandler.onError(new ResponseContext(clientEndpoint, remoteService, requestParams.attachment), ex);
		}
	}
		
	private void onConnectionAttemptTimedOut(Object state)
	{
		TcpRequest request = (TcpRequest)state;
		synchronized(mutex) {
			if(requestList.remove(request) == false)
				return;
		}

		if(request.tcpConnector != null)
			request.tcpConnector.abort();

		if(request.socketChannel != null)
			closeChannel(request.socketChannel);

		request.responseHandler.onError(new ResponseContext(clientEndpoint, request.remoteService, request.attachment), new ConnectionAttemptFailedSoftnetException("The connection attempt timed out."));		
	}
	
	private void processMessage_RzvData(byte[] message, Channel channel) throws AsnException, FormatException
	{
		SequenceDecoder asnRootSequence = ASNDecoder.Sequence(message, 2);
		UUID requestUid = asnRootSequence.OctetStringToUUID();
		byte[] connectionUid = asnRootSequence.OctetString(16);
		int serverId = asnRootSequence.Int32();
		byte[] serverIpBytes = asnRootSequence.OctetString();
		asnRootSequence.end();
		
		InetAddress serverIp = ByteConverter.toInetAddress(serverIpBytes);
		
		TcpRequest request = null;
		synchronized(mutex)
		{
			if(channel.closed())
				return;
			
			request = findRequest(requestUid);		
			if(request == null)
				return;
			
			request.serverId = serverId;
			if(serverIp instanceof Inet6Address)
				request.tcpConnector = new TCPConnectorV6(connectionUid, serverIp, request.tcpOptions);
			else
				request.tcpConnector = new TCPConnectorV4(connectionUid, serverIp, request.tcpOptions);
		}
		
		request.tcpConnector.connect(new TCPResponseHandler()
		{
			@Override
			public void onSuccess(ResponseContext context, SocketChannel socketChannel, ConnectionMode mode)
			{
				onTcpConnectorSuccess(socketChannel, mode, (TcpRequest)context.attachment);
			}

			@Override
			public void onError(ResponseContext context, SoftnetException exception)
			{
				onTcpConnectorError((TcpRequest)context.attachment);
			}
		},
		new BiAcceptor<byte[], Object>()
		{
			@Override
			public void accept(byte[] authKey, Object attachment)
			{
				sendAuthenticationKey(authKey, attachment);
			}
		}, request);
	}	
	
	private void sendAuthenticationKey(byte[] authKey, Object attachment)
	{
		TcpRequest request = (TcpRequest)attachment;
		synchronized(mutex)
		{
			if(clientStatus != StatusEnum.Online)
				return;
			channel.send(encodeMessage_AuthKey(request.requestUid, request.serverId, authKey));
		}
	}
	
	private void onTcpConnectorSuccess(SocketChannel socketChannel, ConnectionMode mode, final TcpRequest request)
	{
		synchronized(mutex)
		{
			if(requestList.contains(request) == false) {
				closeChannel(request.socketChannel);
				return;
			}

			if(request.is_connection_accepted == false)
			{
				request.socketChannel = socketChannel;
				request.mode = mode;
				request.is_connection_established = true;
				return;
			}
			
			requestList.remove(request);			
		}				
		
		request.timeoutControlTask.cancel();
		request.responseHandler.onSuccess(new ResponseContext(clientEndpoint, request.remoteService, request.attachment), socketChannel, mode);
	}
	
	private void onTcpConnectorError(final TcpRequest request)
	{
		synchronized(mutex)
		{
			if(requestList.remove(request) == false)
				return;
		}						
		request.timeoutControlTask.cancel();
		request.responseHandler.onError(new ResponseContext(clientEndpoint, request.remoteService, request.attachment), new ConnectionAttemptFailedSoftnetException());
	}
	
	private void processMessage_RequestError(byte[] message, Channel channel) throws AsnException
	{
		SequenceDecoder asnRootSequence = ASNDecoder.Sequence(message, 2);
		UUID requestUid = asnRootSequence.OctetStringToUUID();
		int errorCode = asnRootSequence.Int32();
		asnRootSequence.end();
		
		TcpRequest request = null;
		synchronized(mutex)
		{
			if(channel.closed())
				return;
			request = removeRequest(requestUid);			
			if(request == null)
				return;
		}				
		
		request.timeoutControlTask.cancel();		
		
		if(request.tcpConnector != null)
			request.tcpConnector.abort();
		
		final SoftnetException f_exception = resolveError(request, errorCode);
		final TcpRequest f_request = request;
		Runnable runnable = new Runnable()
		{
			@Override
			public void run()
			{
				f_request.responseHandler.onError(new ResponseContext(clientEndpoint, f_request.remoteService, f_request.attachment), f_exception);
			}
		};
		threadPool.execute(runnable);			
	}

	private void processMessage_ConnectionAccepted(byte[] message, Channel channel) throws AsnException
	{
		SequenceDecoder asnRootSequence = ASNDecoder.Sequence(message, 2);
		UUID requestUid = asnRootSequence.OctetStringToUUID();
		asnRootSequence.end();

		TcpRequest request = null;
		synchronized(mutex)
		{
			if(channel.closed())
				return;
			request = findRequest(requestUid);			
			if(request == null)
				return;
			
			if(request.is_connection_established == false) {
				request.is_connection_accepted = true;
				return;
			}
			
			requestList.remove(request);
		}				

		request.timeoutControlTask.cancel();
		
		final TcpRequest f_request = request;
		Runnable runnable = new Runnable()
		{
			@Override
			public void run()
			{
				f_request.responseHandler.onSuccess(
					new ResponseContext(clientEndpoint, f_request.remoteService, f_request.attachment), 
					f_request.socketChannel, 
					f_request.mode);		
			}
		};
		threadPool.execute(runnable);			
	}
	
	private void processMessage_AuthHash(byte[] message, Channel channel) throws AsnException
	{
		SequenceDecoder asnSequence = ASNDecoder.Sequence(message, 2);
		UUID requestUid = asnSequence.OctetStringToUUID();
		byte[] authHash = asnSequence.OctetString(20);
		byte[] authKey2 = asnSequence.OctetString(20);
		asnSequence.end();
		
		TcpRequest request = null;
		synchronized(mutex)
		{
			if(channel.closed())
				return;
			request = findRequest(requestUid);		
			if(request == null)
				return;
		}		
		request.tcpConnector.onAuthenticationHash(authHash, authKey2);
	}
	
	private void processMessage_AuthError(byte[] message, Channel channel) throws AsnException
	{
		SequenceDecoder asnSequence = ASNDecoder.Sequence(message, 2);
		UUID requestUid = asnSequence.OctetStringToUUID();
		asnSequence.end();
		
		TcpRequest request = null;
		synchronized(mutex)
		{
			if(channel.closed())
				return;
			request = removeRequest(requestUid);			
			if(request == null)
				return;
		}
		
		request.timeoutControlTask.cancel();
		
		if(request.tcpConnector != null)
			request.tcpConnector.abort();

		final TcpRequest f_request = request;
		Runnable runnable = new Runnable()
		{
			@Override
			public void run()
			{
				f_request.responseHandler.onError(new ResponseContext(clientEndpoint, f_request.remoteService, f_request.attachment), new ConnectionAttemptFailedSoftnetException());
			}
		};
		threadPool.execute(runnable);			
	}

	private SoftnetMessage encodeMessage_AuthKey(UUID requestUid, int serverId, byte[] authKey)
	{
		ASNEncoder asnEncoder = new ASNEncoder();
        SequenceEncoder asnSequence = asnEncoder.Sequence();
        asnSequence.OctetString(requestUid);
        asnSequence.Int32(serverId);
        asnSequence.OctetString(authKey);
        return MsgBuilder.Create(Constants.Client.TcpController.ModuleId, Constants.Client.TcpController.AUTH_KEY, asnEncoder);
	}

	private void onMessageReceived(byte[] message, Channel channel) throws AsnException, FormatException
	{
		byte messageTag = message[1];
		if(messageTag == Constants.Client.TcpController.RZV_DATA)
		{
			processMessage_RzvData(message, channel);
		}
		else if(messageTag == Constants.Client.TcpController.CONNECTION_ACCEPTED)
		{
			processMessage_ConnectionAccepted(message, channel);
		}
		else if(messageTag == Constants.Client.TcpController.REQUEST_ERROR)
		{
			processMessage_RequestError(message, channel);
		}
		else if(messageTag == Constants.Client.TcpController.AUTH_HASH)
		{
			processMessage_AuthHash(message, channel);
		}
		else if(messageTag == Constants.Client.TcpController.AUTH_ERROR)
		{
			processMessage_AuthError(message, channel);
		}
		else		
			throw new FormatException();
	}
	
	private SoftnetException resolveError(TcpRequest tcpRequest, int errorCode)
	{
		if(errorCode == ErrorCodes.CONNECTION_ATTEMPT_FAILED)
			return new ConnectionAttemptFailedSoftnetException();	
		if(errorCode == ErrorCodes.SERVICE_OFFLINE)
			return new ServiceOfflineSoftnetException();			
		if(errorCode == ErrorCodes.PORT_UNREACHABLE)
			return new PortUnreachableSoftnetException(tcpRequest.virtualPort);
		if(errorCode == ErrorCodes.ACCESS_DENIED)
			return new AccessDeniedSoftnetException();	
		if(errorCode == ErrorCodes.SERVICE_BUSY)
			return new ServiceBusySoftnetException();	
		return new UnexpectedErrorSoftnetException(errorCode);
	}
	
	private TcpRequest findRequest(UUID requestUid)
	{
		for(TcpRequest request: requestList)
		{
			if(request.requestUid.equals(requestUid))
				return request;
		}
		return null;
	}
	
	private TcpRequest removeRequest(UUID requestUid)
	{
		for(TcpRequest request: requestList)
		{
			if(request.requestUid.equals(requestUid))
			{
				requestList.remove(request);
				return request;
			}
		}
		return null;
	}
	
	private void closeChannel(SocketChannel channel)
	{
		try	{
			channel.close();
		}
		catch(IOException e) {}
	}
	
	private class TcpRequest
	{
		public final UUID requestUid;
		public RemoteService remoteService;
		public int virtualPort;
		public TCPOptions tcpOptions;
		public TCPResponseHandler responseHandler;
		public Object attachment = null;
		public int serverId;
		public TCPConnector tcpConnector = null;
		public ScheduledTask timeoutControlTask;
		
		public boolean is_connection_established = false;
		public boolean is_connection_accepted = false;
		
		public SocketChannel socketChannel = null;
		public ConnectionMode mode;
		
		public TcpRequest(UUID requestUid) {
			this.requestUid = requestUid;
		}
	}
}
