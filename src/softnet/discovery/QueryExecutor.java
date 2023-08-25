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

package softnet.discovery;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.SocketChannel;

import softnet.core.*;
import softnet.exceptions.*;
import softnet.utils.ByteConverter;
import softnet.utils.Fnv1a;

class QueryExecutor implements STaskContext
{
	private SocketChannel socketChannel;
	private String serverAddress;
	private QueryBuilder queryBuilder;
	private Scheduler scheduler;
	
	public QueryExecutor(SocketChannel socketChannel, String serverAddress, QueryBuilder queryBuilder, Scheduler scheduler)
	{
		this.socketChannel = socketChannel;
		this.serverAddress = serverAddress;
		this.queryBuilder = queryBuilder;
		this.scheduler = scheduler;
	}
	
	private boolean is_closed = false;
	public boolean isClosed()
	{
		return is_closed;
	}
	
	public void close()
	{
		is_closed = true;
		closeChannel();		
	}
			
	public InetAddress exec() throws SoftnetException
	{
		try
		{
			java.net.Socket socket = socketChannel.socket();
			java.net.InetSocketAddress remoteAddress = (java.net.InetSocketAddress)socket.getRemoteSocketAddress();
			InetAddress serverIP = remoteAddress.getAddress();
			
			ByteBuffer requestBuffer = queryBuilder.GetQuery();			
			socketChannel.write(requestBuffer);
			socket.shutdownOutput();

			Acceptor<Object> acceptor = new Acceptor<Object>()
			{
				public void accept(Object noData) { closeChannel(); }
			};
			ScheduledTask task = new ScheduledContextTask(acceptor, this);
			scheduler.add(task, 30);
			
			ByteBuffer responseBuffer = ByteBuffer.allocate(24);
			responseBuffer.order(ByteOrder.BIG_ENDIAN);
			
			try
			{
				for(int i=0; i < 24; i++)
				{
					int bytesCount = socketChannel.read(responseBuffer);
					
					if(responseBuffer.remaining() == 0)
			        	throw new InvalidServerEndpointSoftnetException(serverIP, Constants.ServerPorts.Balancer);
					
					if(bytesCount == -1)
					{
						int dataSize = responseBuffer.position();
						if(dataSize < 6)
				        	throw new InvalidServerEndpointSoftnetException(serverIP, Constants.ServerPorts.Balancer);

						byte[] response = responseBuffer.array();
						int responseHash = ByteConverter.toInt32(response, 0);						
						
						byte[] requestHash = ByteConverter.getBytes(Fnv1a.get32BitHash(requestBuffer.array()));
						response[0] = requestHash[0];
						response[1] = requestHash[1];
						response[2] = requestHash[2];
						response[3] = requestHash[3];

		    			byte messageTag = response[4];
				        if(messageTag == Constants.Balancer.SUCCESS)
				        {
				        	int ipVersion = response[5];	        	
			    			if(ipVersion == Constants.Balancer.IP_V6 && dataSize == 22 && serverIP instanceof Inet6Address)
			    			{
			    				int computedResponseHash = Fnv1a.get32BitHash(response, 0, 22);
								if(responseHash != computedResponseHash)
				    				throw new InvalidServerEndpointSoftnetException(serverIP, Constants.ServerPorts.Balancer);
			    				
		    					byte[] addrBytes = new byte[16];
		    					System.arraycopy(response, 6, addrBytes, 0, 16);
		    					return InetAddress.getByAddress(addrBytes);
			    			}
			    			else if(ipVersion == Constants.Balancer.IP_V4 && dataSize == 10 && serverIP instanceof Inet4Address)
			    			{
			    				int computedResponseHash = Fnv1a.get32BitHash(response, 0, 10);
								if(responseHash != computedResponseHash)
				    				throw new InvalidServerEndpointSoftnetException(serverIP, Constants.ServerPorts.Balancer);

			    				byte[] addrBytes = new byte[4];
		    					System.arraycopy(response, 6, addrBytes, 0, 4);
		    					return InetAddress.getByAddress(addrBytes);
			    			}
				        }
				        else if(messageTag == Constants.Balancer.ERROR && dataSize == 7)
				        {
		    				int computedResponseHash = Fnv1a.get32BitHash(response, 0, 7);
							if(responseHash != computedResponseHash)
			    				throw new InvalidServerEndpointSoftnetException(serverIP, Constants.ServerPorts.Balancer);
				        	
				        	int errorCode = ByteConverter.toInt32FromInt16(response, 5);
				        	
				        	queryBuilder.ThrowException(errorCode);
				        	
				        	if(errorCode == ErrorCodes.SERVER_BUSY)
							{
								throw new ServerBusySoftnetException();
							}
							else if(errorCode == ErrorCodes.SERVER_DBMS_ERROR)
							{
								throw new ServerDbmsErrorSoftnetException();
							}
				    		else if (errorCode == ErrorCodes.SERVER_CONFIG_ERROR)
				            {
				    			throw new ServerConfigErrorSoftnetException();
				            }
							else if(errorCode == ErrorCodes.INVALID_SERVER_ENDPOINT)
							{
					        	throw new InvalidServerEndpointSoftnetException(String.format("The softnet registry at '%s' does not match the expected one.", serverAddress));
							}
							else if(errorCode == ErrorCodes.INCOMPATIBLE_PROTOCOL_VERSION)
							{
								throw new IncompatibleProtocolVersionSoftnetException();
							}
							else if (errorCode == ErrorCodes.ENDPOINT_DATA_FORMAT_ERROR)
				            {
				    			throw new EndpointDataFormatSoftnetException();
				            }
							else
							{
								throw new UnexpectedErrorSoftnetException(errorCode);
							}
				        }
				        
				    	throw new InvalidServerEndpointSoftnetException(serverIP, Constants.ServerPorts.Balancer);
					}
				}
				
		    	throw new NetworkErrorSoftnetException("The socket is not properly configured.");
			}
			catch(AsynchronousCloseException ex)
			{
				throw new TimeoutExpiredSoftnetException(String.format("The remote endpoint '%s:%d' did not properly respond after a period of time.", serverIP.toString(), Constants.ServerPorts.Balancer));
			}				
		}		
		catch(IOException ex)
		{			
			throw new NetworkErrorSoftnetException(ex.getMessage());			
		}				
	}
			
	private void closeChannel()
	{		
    	try { socketChannel.close(); } catch(IOException e) {}		
	}
}
