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

import softnet.asn.*;
import softnet.core.*;
import softnet.exceptions.*;
import java.util.LinkedList;
import java.util.UUID;

class RPCController 
{
	private Object endpoint_mutex;	
	private ThreadPool threadPool;
	private Scheduler scheduler;
	private ClientEndpoint clientEndpoint;
	private StatusEnum clientStatus;	
	private Channel channel;
	private LinkedList<RpcRequest> requestList;

	private enum StatusEnum
	{ 
		Disconnected, Connected, Online
	}

	public RPCController(ClientEndpoint clientEndpoint)
	{
		this.clientEndpoint = clientEndpoint;
		this.threadPool = clientEndpoint.threadPool;
		this.scheduler = clientEndpoint.scheduler;
		this.endpoint_mutex = clientEndpoint.endpoint_mutex;
		clientStatus = StatusEnum.Disconnected;
		requestList = new LinkedList<RpcRequest>();
	}
	
	public void onEndpointConnected(Channel channel)
	{
		channel.registerComponent(Constants.Client.RpcController.ModuleId, 
			new MsgAcceptor<Channel>()
			{
				public void accept(byte[] message, Channel _channel) throws AsnException, FormatException, SoftnetException
				{
					onMessageReceived(message, _channel);
				}
			});		
		
		this.channel = channel;
		clientStatus = StatusEnum.Connected;
	}

	public void onClientOnline()
	{
		clientStatus = StatusEnum.Online;
	}

	public void onEndpointDisconnected()
	{
		clientStatus = StatusEnum.Disconnected;
		channel = null;
		
		for(RpcRequest request: requestList)
		{
			request.timeoutControlTask.cancel();
			
			final RpcRequest f_request = request;				
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

	public void onRemoteServiceOffline(long serviceId, Channel channel)
	{
		for(int i = requestList.size()-1; i >= 0; i--)
		{
			RpcRequest request = requestList.get(i);
			if(request.remoteService.getId() == serviceId)
			{
				if(request.timeoutControlTask.cancel())
				{				
					requestList.remove(i);
					final RpcRequest f_request = request; 
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
	
	public void call(RemoteService remoteService, RemoteProcedure remoteProcedure, RPCResponseHandler responseHandler)
	{
		if(remoteService == null)
			throw new IllegalArgumentException("The argument 'remoteService' is null."); 

		if(remoteProcedure == null)
			throw new IllegalArgumentException("The argument 'remoteProcedure' is null."); 
		
		if(responseHandler == null)
			throw new IllegalArgumentException("The argument 'responseHandler' is null."); 

		try
		{			
			if(remoteService.isOnline() == false)
				throw new ServiceOfflineSoftnetException();

			UUID transactionUid = UUID.randomUUID();

			ASNEncoder asnEncoder = new ASNEncoder();
			SequenceEncoder rootSequence = asnEncoder.Sequence();
			rootSequence.OctetString(transactionUid);
			rootSequence.Int64(remoteService.getId());
			rootSequence.IA5String(remoteProcedure.name);			
			rootSequence.OctetString(remoteProcedure.getEncoding());
			SoftnetMessage message = MsgBuilder.Create(Constants.Client.RpcController.ModuleId, Constants.Client.RpcController.REQUEST, asnEncoder);
			
			RpcRequest request = new RpcRequest(transactionUid);
			request.remoteService = remoteService;
			request.procedureName = remoteProcedure.name;
			request.responseHandler = responseHandler;			
			Acceptor<Object> acceptor = new Acceptor<Object>()
			{
				public void accept(Object state) { onRequestTimeoutExpired(state); }
			};
			request.timeoutControlTask = new ScheduledTask(acceptor, request);
			
			synchronized(endpoint_mutex)
			{
				if(clientStatus != StatusEnum.Online)
					throw new ClientOfflineSoftnetException();			
				requestList.add(request);
		        channel.send(message);
			}
			
			scheduler.add(request.timeoutControlTask, Constants.RpcWaitSeconds);
		}
		catch(SoftnetException ex)
		{
			responseHandler.onError(new ResponseContext(clientEndpoint, remoteService, null), ex);
		}
	}

	public void call(RemoteService remoteService, RemoteProcedure remoteProcedure, RPCResponseHandler responseHandler, RequestParams requestParams)
	{
		if(remoteService == null)
			throw new IllegalArgumentException("The argument 'remoteService' is null."); 

		if(remoteProcedure == null)
			throw new IllegalArgumentException("The argument 'remoteProcedure' is null."); 
		
		if(responseHandler == null)
			throw new IllegalArgumentException("The argument 'responseHandler' is null."); 

		if(requestParams == null)
			throw new IllegalArgumentException("The argument 'requestParams' is null."); 
		
		try
		{			
			if(remoteService.isOnline() == false)
				throw new ServiceOfflineSoftnetException();

			UUID transactionUid = UUID.randomUUID();

			ASNEncoder asnEncoder = new ASNEncoder();
			SequenceEncoder rootSequence = asnEncoder.Sequence();
			rootSequence.OctetString(transactionUid);
			rootSequence.Int64(remoteService.getId());
			rootSequence.IA5String(remoteProcedure.name);			
			rootSequence.OctetString(remoteProcedure.getEncoding());
			if(requestParams.sessionTag != null)
				rootSequence.OctetString(1, requestParams.getSessionTagEncoding()); 			
			SoftnetMessage message = MsgBuilder.Create(Constants.Client.RpcController.ModuleId, Constants.Client.RpcController.REQUEST, asnEncoder);
			
			RpcRequest request = new RpcRequest(transactionUid);
			request.remoteService = remoteService;
			request.procedureName = remoteProcedure.name;
			request.responseHandler = responseHandler;
			request.attachment = requestParams.attachment;
			Acceptor<Object> acceptor = new Acceptor<Object>()
			{
				public void accept(Object state) { onRequestTimeoutExpired(state); }
			};
			request.timeoutControlTask = new ScheduledTask(acceptor, request);
			
			synchronized(endpoint_mutex)
			{
				if(clientStatus != StatusEnum.Online)
					throw new ClientOfflineSoftnetException();			
				requestList.add(request);
		        channel.send(message);
			}
			
			scheduler.add(request.timeoutControlTask, requestParams.waitSeconds > 0 ? requestParams.waitSeconds : Constants.RpcWaitSeconds);
		}
		catch(SoftnetException ex)
		{
			responseHandler.onError(new ResponseContext(clientEndpoint, remoteService, requestParams.attachment), ex);
		}
	}

	private void onRequestTimeoutExpired(Object state)
	{
		RpcRequest request = (RpcRequest)state;
		synchronized(endpoint_mutex)
		{
			if(requestList.remove(request) == false)
				return;
		}
		request.responseHandler.onError(new ResponseContext(clientEndpoint, request.remoteService, request.attachment), new TimeoutExpiredSoftnetException("The RPC call timeout expired."));		
	}
	
	private void processMessage_Result(byte[] message, Channel channel) throws AsnException, SoftnetException
	{
		SequenceDecoder asnRootSequence = ASNDecoder.Sequence(message, 2);
		UUID transactionUid = asnRootSequence.OctetStringToUUID();
		byte[] resultEncoding = asnRootSequence.OctetString(2, 65536);
		asnRootSequence.end();
		
		RpcRequest request = null;
		synchronized(endpoint_mutex)
		{
			if(channel.closed())
				return;
			request = removeRequest(transactionUid);
		}
		
		if(request != null)
		{
			if(request.timeoutControlTask.cancel() == false)
				return;
			
			final RpcRequest f_request = request;
			final SequenceDecoder asnResult = ASNDecoder.Sequence(resultEncoding);
			Runnable runnable = new Runnable()
			{
				@Override
				public void run()
				{
					f_request.responseHandler.onSuccess(new ResponseContext(clientEndpoint, f_request.remoteService, f_request.attachment), asnResult);
				}
			};
			threadPool.execute(runnable);
		}
	}
	
	private void processMessage_SoftnetError(byte[] message, Channel channel) throws AsnException, SoftnetException
	{
		SequenceDecoder asnRootSequence = ASNDecoder.Sequence(message, 2);
		UUID transactionUid = asnRootSequence.OctetStringToUUID();
		int errorCode = asnRootSequence.Int32();
		asnRootSequence.end();
				
		RpcRequest request = null;
		synchronized(endpoint_mutex)
		{
			if(channel.closed())
				return;
			request = removeRequest(transactionUid);			
		}
		
		if(request != null)
		{
			if(request.timeoutControlTask.cancel() == false)
				return;
			
			final SoftnetException exception = resolveError(request, errorCode);
			final RpcRequest f_request = request;
			Runnable runnable = new Runnable()
			{
				@Override
				public void run()
				{
					f_request.responseHandler.onError(new ResponseContext(clientEndpoint, f_request.remoteService, f_request.attachment), exception);
				}
			};
			threadPool.execute(runnable);			
		}
	}
	
	private void processMessage_AppError(byte[] message, Channel channel) throws AsnException, SoftnetException
	{
		SequenceDecoder asnRootSequence = ASNDecoder.Sequence(message, 2);
		UUID transactionUid = asnRootSequence.OctetStringToUUID();
		final int errorCode = asnRootSequence.Int32();
		byte[] errorEncoding = asnRootSequence.OctetString(2, 65536);
		asnRootSequence.end();
		
		RpcRequest request = null;
		synchronized(endpoint_mutex)
		{
			if(channel.closed())
				return;
			request = removeRequest(transactionUid);		
		}
		
		if(request != null)
		{
			if(request.timeoutControlTask.cancel() == false)
				return;
			
			final RpcRequest f_request = request;
			final SequenceDecoder asnError = ASNDecoder.Sequence(errorEncoding);
			Runnable runnable = new Runnable()
			{
				@Override
				public void run()
				{
					f_request.responseHandler.onError(new ResponseContext(clientEndpoint, f_request.remoteService, f_request.attachment), errorCode, asnError);
				}
			};
			threadPool.execute(runnable);
		}
	}
	
	private void onMessageReceived(byte[] message, Channel channel) throws AsnException, FormatException, SoftnetException
	{
		byte messageTag = message[1];
		if(messageTag == Constants.Client.RpcController.RESULT)
		{
			processMessage_Result(message, channel);
		}
		else if(messageTag == Constants.Client.RpcController.SOFTNET_ERROR)
		{
			processMessage_SoftnetError(message, channel);
		}
		else if(messageTag == Constants.Client.RpcController.APP_ERROR)
		{
			processMessage_AppError(message, channel);
		}
		else
			throw new FormatException();
	}
			
	private SoftnetException resolveError(RpcRequest rpcRequest, int errorCode)
	{
		if(errorCode == ErrorCodes.SERVICE_OFFLINE)
			return new ServiceOfflineSoftnetException();			
		if(errorCode == ErrorCodes.MISSING_PROCEDURE)
			return new MissingProcedureSoftnetException(rpcRequest.procedureName);
		if(errorCode == ErrorCodes.ACCESS_DENIED)
			return new AccessDeniedSoftnetException();	
		if(errorCode == ErrorCodes.SERVICE_BUSY)
			return new ServiceBusySoftnetException();	
		return new UnexpectedErrorSoftnetException(errorCode);
	}
	
	private RpcRequest removeRequest(UUID transactionUid)
	{
		for(RpcRequest request: requestList)
		{
			if(request.transactionUid.equals(transactionUid))
			{
				requestList.remove(request);
				return request;
			}
		}
		return null;
	}

	private class RpcRequest
	{
		public final UUID transactionUid;
		public RemoteService remoteService;
		public String procedureName;	
		public RPCResponseHandler responseHandler;
		public Object attachment;
		public ScheduledTask timeoutControlTask;
		
		public RpcRequest(UUID transactionUid)
		{
			this.transactionUid = transactionUid;
			this.attachment = null;
		}		
	}
}
