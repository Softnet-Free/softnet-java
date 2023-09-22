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

import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.LinkedList;
import java.util.UUID;

import softnet.*;
import softnet.asn.*;
import softnet.core.*;
import softnet.exceptions.*;
import softnet.utils.ByteConverter;

class UDPController
{
	private ClientEndpoint clientEndpoint;
	private ThreadPool threadPool;
	private Scheduler scheduler;
	private Object mutex;
	private Channel channel;
	private LinkedList<UdpRequest> requestList;

	private enum StatusEnum
	{ 
		Disconnected, Connected, Online
	}
	private StatusEnum clientStatus = StatusEnum.Disconnected;
	
	public UDPController(ClientEndpoint clientEndpoint)
	{
		this.clientEndpoint = clientEndpoint;
		this.threadPool = clientEndpoint.threadPool;
		this.scheduler = clientEndpoint.scheduler;
		requestList = new LinkedList<UdpRequest>();
		mutex = new Object();
	}
	
	public void onEndpointConnected(Channel channel)
	{
		channel.registerComponent(Constants.Client.UdpController.ModuleId, 
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
			
			for(UdpRequest request: requestList)
			{
				request.timeoutControlTask.cancel();
				
				if(request.udpConnector != null)
					request.udpConnector.abort();
				
				if(request.datagramSocket != null)
					request.datagramSocket.close();
				
				final UdpRequest f_request = request;				
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

			for(UdpRequest request: requestList)
			{
				request.timeoutControlTask.cancel();		
				
				if(request.udpConnector != null)
					request.udpConnector.abort();
				
				if(request.datagramSocket != null)
					request.datagramSocket.close();
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
				UdpRequest request = requestList.get(i);
				if(request.remoteService.getId() == serviceId)
				{
					requestList.remove(i);
					
					request.timeoutControlTask.cancel();

					if(request.udpConnector != null)
						request.udpConnector.abort();

					if(request.datagramSocket != null)
						request.datagramSocket.close();

					final UdpRequest f_request = request;
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

	public void connect(RemoteService remoteService, int virtualPort, UDPResponseHandler responseHandler)
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
				
				UdpRequest request = new UdpRequest(requestUid);
				request.remoteService = remoteService;
				request.virtualPort = virtualPort;
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
				SoftnetMessage message = MsgBuilder.Create(Constants.Client.UdpController.ModuleId, Constants.Client.UdpController.REQUEST, asnEncoder);
				
				channel.send(message);
				scheduler.add(request.timeoutControlTask, Constants.UdpConnectingWaitSeconds);
			}
		}
		catch(SoftnetException ex)
		{
			responseHandler.onError(new ResponseContext(clientEndpoint, remoteService, null), ex);
		}
	}

	public void connect(RemoteService remoteService, int virtualPort, UDPResponseHandler responseHandler, RequestParams requestParams)
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
				
				UdpRequest request = new UdpRequest(requestUid);
				request.remoteService = remoteService;
				request.virtualPort = virtualPort;
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
				SoftnetMessage message = MsgBuilder.Create(Constants.Client.UdpController.ModuleId, Constants.Client.UdpController.REQUEST, asnEncoder);
				
				channel.send(message);
				scheduler.add(request.timeoutControlTask, requestParams.waitSeconds > 0 ? requestParams.waitSeconds : Constants.UdpConnectingWaitSeconds);
			}
		}
		catch(SoftnetException ex)
		{
			responseHandler.onError(new ResponseContext(clientEndpoint, remoteService, requestParams.attachment), ex);
		}
	}
	
	private void onConnectionAttemptTimedOut(Object state)
	{
		UdpRequest request = (UdpRequest)state;
		synchronized(mutex)	{
			if(requestList.remove(request) == false)
				return;
		}
		
		if(request.udpConnector != null)
			request.udpConnector.abort();
		
		if(request.datagramSocket != null)
			request.datagramSocket.close();

		request.responseHandler.onError(new ResponseContext(clientEndpoint, request.remoteService, request.attachment), new TimeoutExpiredSoftnetException("The connection attempt timed out."));		
	}
	
	private void processMessage_RzvData(byte[] message, Channel channel) throws AsnException, FormatException
	{
		SequenceDecoder asnRootSequence = ASNDecoder.Sequence(message, 2);
		UUID requestUid = asnRootSequence.OctetStringToUUID();
		byte[] connectionUid = asnRootSequence.OctetString(16);
		int serverId = asnRootSequence.Int32();
		byte[] serverIPBytes = asnRootSequence.OctetString();
		asnRootSequence.end();
		
		InetAddress serverIP = ByteConverter.toInetAddress(serverIPBytes);
		
		UdpRequest request = null;
		synchronized(mutex)
		{
			if(channel.closed())
				return;
			
			request = findRequest(requestUid);		
			if(request == null)
				return;
			
			request.serverId = serverId;
			if(serverIP instanceof Inet6Address)
				request.udpConnector = new UDPConnectorV6(connectionUid, serverIP, scheduler);
			else
				request.udpConnector = new UDPConnectorV4(connectionUid, serverIP, scheduler);
		}
		
		request.udpConnector.connect(new UDPResponseHandler()
		{
			@Override
			public void onSuccess(ResponseContext context, java.net.DatagramSocket datagramSocket, java.net.InetSocketAddress remoteSocketAddress, ConnectionMode mode)
			{
				onUdpConnectorSuccess(datagramSocket, remoteSocketAddress, mode, (UdpRequest)context.attachment);
			}

			@Override
			public void onError(ResponseContext context, SoftnetException exception)
			{
				onUdpConnectorError((UdpRequest)context.attachment);
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
		UdpRequest request = (UdpRequest)attachment;
		synchronized(mutex)
		{
			if(clientStatus != StatusEnum.Online)
				return;
			channel.send(encodeMessage_AuthKey(request.requestUid, request.serverId, authKey));
		}
	}
	
	private void onUdpConnectorSuccess(java.net.DatagramSocket datagramSocket, java.net.InetSocketAddress remoteSocketAddress, ConnectionMode mode, final UdpRequest request)
	{
		synchronized(mutex)
		{
			if(requestList.contains(request) == false) {
				datagramSocket.close();
				return;
			}
			
			if(request.is_connection_accepted == false)
			{
				request.datagramSocket = datagramSocket;
				request.remoteSocketAddress = remoteSocketAddress;
				request.mode = mode;
				request.is_connection_established = true;
				return;
			}
			
			requestList.remove(request);
		}				
		
		request.timeoutControlTask.cancel();
		request.responseHandler.onSuccess(new ResponseContext(clientEndpoint, request.remoteService, request.attachment), datagramSocket, remoteSocketAddress, mode);		
	}
	
	private void onUdpConnectorError(final UdpRequest request)
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
		
		UdpRequest request = null;
		synchronized(mutex)
		{
			if(channel.closed())
				return;
			request = removeRequest(requestUid);			
			if(request == null)
				return;
		}				
		
		request.timeoutControlTask.cancel();
		
		final SoftnetException f_exception = resolveError(request, errorCode);
		final UdpRequest f_request = request;
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

		UdpRequest request = null;
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
		
		final UdpRequest f_request = request;
		Runnable runnable = new Runnable()
		{
			@Override
			public void run()
			{
				f_request.responseHandler.onSuccess(
					new ResponseContext(clientEndpoint, f_request.remoteService, f_request.attachment), 
					f_request.datagramSocket, 
					f_request.remoteSocketAddress, 
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
		
		UdpRequest request = null;
		synchronized(mutex)
		{
			if(channel.closed())
				return;
			request = findRequest(requestUid);		
			if(request == null)
				return;
		}		
		request.udpConnector.onAuthenticationHash(authHash, authKey2);
	}
	
	private void processMessage_AuthError(byte[] message, Channel channel) throws AsnException
	{
		SequenceDecoder asnSequence = ASNDecoder.Sequence(message, 2);
		UUID requestUid = asnSequence.OctetStringToUUID();
		asnSequence.end();
		
		UdpRequest request = null;
		synchronized(mutex)
		{
			if(channel.closed())
				return;
			request = removeRequest(requestUid);			
			if(request == null)
				return;
		}
		
		request.timeoutControlTask.cancel();
		
		if(request.udpConnector != null)
			request.udpConnector.abort();

		final UdpRequest f_request = request;
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
        return MsgBuilder.Create(Constants.Client.UdpController.ModuleId, Constants.Client.UdpController.AUTH_KEY, asnEncoder);
	}
	
	private void onMessageReceived(byte[] message, Channel channel) throws AsnException, FormatException
	{
		byte messageTag = message[1];
		if(messageTag == Constants.Client.UdpController.RZV_DATA)
		{
			processMessage_RzvData(message, channel);
		}
		else if(messageTag == Constants.Client.UdpController.CONNECTION_ACCEPTED)
		{
			processMessage_ConnectionAccepted(message, channel);
		}
		else if(messageTag == Constants.Client.UdpController.REQUEST_ERROR)
		{
			processMessage_RequestError(message, channel);
		}
		else if(messageTag == Constants.Client.UdpController.AUTH_HASH)
		{
			processMessage_AuthHash(message, channel);
		}
		else if(messageTag == Constants.Client.UdpController.AUTH_ERROR)
		{
			processMessage_AuthError(message, channel);
		}
		else
			throw new FormatException();
	}
	
	private SoftnetException resolveError(UdpRequest udpRequest, int errorCode)
	{
		if(errorCode == ErrorCodes.CONNECTION_ATTEMPT_FAILED)
			return new ConnectionAttemptFailedSoftnetException();	
		if(errorCode == ErrorCodes.SERVICE_OFFLINE)
			return new ServiceOfflineSoftnetException();			
		if(errorCode == ErrorCodes.PORT_UNREACHABLE)
			return new PortUnreachableSoftnetException(udpRequest.virtualPort);
		if(errorCode == ErrorCodes.ACCESS_DENIED)
			return new AccessDeniedSoftnetException();	
		if(errorCode == ErrorCodes.SERVICE_BUSY)
			return new ServiceBusySoftnetException();	
		return new UnexpectedErrorSoftnetException(errorCode);
	}
	
	private UdpRequest findRequest(UUID requestUid)
	{
		for(UdpRequest request: requestList)
		{
			if(request.requestUid.equals(requestUid))
				return request;
		}
		return null;
	}
	
	private UdpRequest removeRequest(UUID requestUid)
	{
		for(UdpRequest request: requestList)
		{
			if(request.requestUid.equals(requestUid))
			{
				requestList.remove(request);
				return request;
			}
		}
		return null;
	}
	
	private class UdpRequest
	{
		public final UUID requestUid;
		public RemoteService remoteService;
		public int virtualPort;
		public UDPResponseHandler responseHandler;
		public Object attachment;
		public int serverId;
		public UDPConnector udpConnector = null;
		public ScheduledTask timeoutControlTask;
		
		public boolean is_connection_established = false;
		public boolean is_connection_accepted = false;
		
		public java.net.DatagramSocket datagramSocket = null;
		public java.net.InetSocketAddress remoteSocketAddress;
		public ConnectionMode mode;
		
		public UdpRequest(UUID requestUid) {
			this.requestUid = requestUid;
		}
	}
}
