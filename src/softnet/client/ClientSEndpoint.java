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

import softnet.TCPOptions;
import softnet.core.BiAcceptor;

public class ClientSEndpoint extends ClientEndpoint
{
	private RemoteService remoteService;

	public boolean isServiceOnline()
	{
		return remoteService.isOnline();
	}
	
	public RemoteService getService()
	{
		return remoteService;
	}

	protected void initialize(String serviceType, String contractAuthor, ClientURI clientURI, String password, String clientDescription)
	{
		final SingleServiceGroup singleServiceGroup = new SingleServiceGroup(this, endpoint_mutex);
		remoteService = singleServiceGroup.getService();
		
		singleServiceGroup.RemoteServiceOfflineCallback = new BiAcceptor<Long, Channel>()
		{
			public void accept(Long serviceId, Channel channel)
			{
				ServiceGroup_onRemoteServiceOffline(serviceId, channel);
			}			
		};
		
		super.initialize(serviceType, contractAuthor, clientURI, password, clientDescription, singleServiceGroup);
	}

	public static ClientSEndpoint create(String serviceType, String contractAuthor, ClientURI clientURI)
	{
		validateServiceType(serviceType);
		validateContractAuthor(contractAuthor);

		if(clientURI == null)
			throw new IllegalArgumentException("'clientURI' must not be null.");

		if(clientURI.category == ClientCategory.MultiService || clientURI.category == ClientCategory.MultiServiceStateless)
			throw new IllegalArgumentException(String.format("The URI '%s' identifies a multi-service client that is not allowed in this context.", clientURI.value));				

		if(clientURI.category == ClientCategory.SingleService)
			throw new IllegalArgumentException(String.format("The URI '%s' identifies a stateful client that is not allowed in this context.", clientURI.value));				
								
		ClientSEndpoint clientSEndpoint = new ClientSEndpoint();		
		clientSEndpoint.initialize(serviceType, contractAuthor, clientURI, null, null);			
		return clientSEndpoint;
	}
	
	public static ClientSEndpoint create(String serviceType, String contractAuthor, ClientURI clientURI, String password)
	{
		validateServiceType(serviceType);
		validateContractAuthor(contractAuthor);

		if(clientURI == null)
			throw new IllegalArgumentException("'clientURI' must not be null.");

		if(clientURI.category == ClientCategory.MultiService || clientURI.category == ClientCategory.MultiServiceStateless)
			throw new IllegalArgumentException(String.format("The URI '%s' identifies a multi-service client that is not allowed in this context.", clientURI.value));				

		if(clientURI.category == ClientCategory.SingleService)
		{
			if(password == null || password.length() == 0)
				throw new IllegalArgumentException("'password' must not be null or empty for a statefull client.");
			
			if (password.length() > 256)
				throw new IllegalArgumentException("The length of 'password' must not be greater than 256.");
		}
								
		ClientSEndpoint clientSEndpoint = new ClientSEndpoint();		
		clientSEndpoint.initialize(serviceType, contractAuthor, clientURI, password, null);			
		return clientSEndpoint;
	}
	
	public static ClientSEndpoint create(String serviceType, String contractAuthor, ClientURI clientURI, String password, String clientDescription)
	{
		if(clientURI == null)
			throw new IllegalArgumentException("'clientURI' must not be null.");
		
		if(clientURI.category == ClientCategory.MultiService || clientURI.category == ClientCategory.MultiServiceStateless)
			throw new IllegalArgumentException(String.format("The URI '%s' identifies a multi-service client that is not allowed in this context.", clientURI.value));		
		
		if((password == null || password.length() == 0) && clientURI.category == ClientCategory.SingleService)
			throw new IllegalArgumentException("'password' must not be null or empty for a statefull client.");		
	
		validateServiceType(serviceType);
		validateContractAuthor(contractAuthor);

		if(clientURI.category == ClientCategory.SingleServiceStateless)
		{
			clientDescription = null;
		}
		else if(clientDescription != null)
		{
			if(clientDescription.length() == 0)
				clientDescription = null;
			else
				validateClientDescription(clientDescription);
		}
		
		ClientSEndpoint clientSEndpoint = new ClientSEndpoint();		
		clientSEndpoint.initialize(serviceType, contractAuthor, clientURI, password, clientDescription);			
		return clientSEndpoint;
	}

	public void call(RemoteProcedure remoteProcedure, RPCResponseHandler responseHandler)
	{
		super.call(remoteService, remoteProcedure, responseHandler);
	}
		
	public void call(RemoteProcedure remoteProcedure, RPCResponseHandler responseHandler, RequestParams requestParams)
	{
		super.call(remoteService, remoteProcedure, responseHandler, requestParams);
	}
	
	public void tcpConnect(int virtualPort, TCPResponseHandler responseHandler, TCPOptions tcpOptions)
	{
		super.tcpConnect(remoteService, virtualPort, tcpOptions, responseHandler);
	}
	
	public void tcpConnect(int virtualPort, TCPResponseHandler responseHandler, TCPOptions tcpOptions, RequestParams requestParams)
	{		
		super.tcpConnect(remoteService, virtualPort, tcpOptions, responseHandler, requestParams);
	}

	public void udpConnect(int virtualPort, UDPResponseHandler responseHandler)
	{
		super.udpConnect(remoteService, virtualPort, responseHandler);
	}

	public void udpConnect(int virtualPort, UDPResponseHandler responseHandler, RequestParams requestParams)
	{
		super.udpConnect(remoteService, virtualPort, responseHandler, requestParams);
	}
}
