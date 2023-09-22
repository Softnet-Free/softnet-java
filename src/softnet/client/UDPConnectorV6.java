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

import java.net.*;
import java.io.IOException;
import java.nio.channels.SocketChannel;

import softnet.*;
import softnet.asn.*;
import softnet.core.*;
import softnet.exceptions.*;
import softnet.utils.*;

class UDPConnectorV6 implements UDPConnector, STaskContext
{
	private byte[] connectionUid;
	private InetAddress serverIP;
	private Scheduler scheduler;
	private UDPResponseHandler responseHandler;
	private BiAcceptor<byte[], Object> authenticationHandler;
	private Object attachment;

	private Object mutex = new Object();	
	private MsgSocket msgSocket = null;
	private InetAddress localIP = null;
	private DatagramSocket datagramSocket = null;
	private boolean isEndpointEstablished = false;
	private ScheduledContextTask endpointEstablishmentTask = null;
	private byte[] thisEndpointUid = null;
	private byte[] remoteEndpointUid = null;
	private InetSocketAddress remoteSocketAddress = null;
	private boolean isP2PInputHolePunched = false;
	private boolean isP2POutputHolePunched = false;
	private boolean isProxyConnectionCreated = false;
	private boolean isTimeoutExpired = false;
	
	private enum ConnectorState {
        INITIAL, P2P_HANDSHAKE, COMPLETED
	}
	private ConnectorState connectorState = ConnectorState.INITIAL;
	
	public UDPConnectorV6(byte[] connectionUid, InetAddress serverIP, Scheduler scheduler)
	{
		this.connectionUid = connectionUid;
		this.serverIP = serverIP;
		this.scheduler = scheduler;
	}
		
	public void connect(UDPResponseHandler responseHandler, BiAcceptor<byte[], Object> authenticationHandler, Object attachment)
	{
		this.responseHandler = responseHandler;
		this.authenticationHandler = authenticationHandler;
		this.attachment = attachment;
		
		Thread thread = new Thread()
		{
		    public void run(){
		    	execute();
		    }
		};
		thread.start();
	}
	
	public void onAuthenticationHash(byte[] authHash, byte[] authKey2)
	{
		ASNEncoder asnEncoder = new ASNEncoder();
        SequenceEncoder sequence = asnEncoder.Sequence();
        sequence.OctetString(authHash);        
        sequence.OctetString(authKey2);   
        
        msgSocket.send(MsgBuilder.Create(Constants.Proxy.UdpConnector.AUTH_HASH, asnEncoder));
	}
	
	public boolean isClosed()
	{
		return connectorState == ConnectorState.COMPLETED;
	}
	
	public void abort()
	{
		synchronized(mutex)
		{
			if (connectorState == ConnectorState.COMPLETED)
				return;
			connectorState = ConnectorState.COMPLETED;	
			
			if(msgSocket != null)
				msgSocket.close();
			
			if(datagramSocket != null)
				datagramSocket.close();			
		}
	}
		
	private void completeOnError()
	{		
		synchronized(mutex)
		{			
			if (connectorState == ConnectorState.COMPLETED)
				return;
			connectorState = ConnectorState.COMPLETED;
			
			if(msgSocket != null)
				msgSocket.close();
			
			if(datagramSocket != null)
				datagramSocket.close();
		}		
		
		responseHandler.onError(new ResponseContext(null, null, attachment), null);
	}
	
	private void execute()
	{
		SocketChannel controlChannel = null;
		try
		{
			synchronized(mutex)
			{
				if(connectorState != ConnectorState.INITIAL)
					return;
				controlChannel = SocketChannel.open();
				msgSocket = new MsgSocket(controlChannel);
			}
			
			controlChannel.configureBlocking(true);
			controlChannel.connect(new InetSocketAddress(serverIP, Constants.ServerPorts.UdpRzvPort));	
			
			localIP = ((InetSocketAddress)controlChannel.getLocalAddress()).getAddress();
			
			msgSocket.messageReceivedHandler	= new Acceptor<byte[]>()
			{
				public void accept(byte[] message) { onMessageReceived(message); }
			};
			msgSocket.networkErrorHandler = new Acceptor<NetworkErrorSoftnetException>()
			{
				public void accept(NetworkErrorSoftnetException ex) { completeOnError(); }
			};
			msgSocket.formatErrorHandler = new Runnable()
			{
				public void run() { completeOnError(); }
			};					
			msgSocket.minLength = 1;
			msgSocket.maxLength = 256;
			msgSocket.start();
			
			msgSocket.send(EncodeMessage_ClientEndpoint());
		}
		catch(IOException ex)
		{			
			if(controlChannel != null)
				closeChannel(controlChannel);
            responseHandler.onError(new ResponseContext(null, null, attachment), null);
		}
	}
	
	private void ProcessMessage_AuthKey(byte[] message) throws AsnException
	{
		SequenceDecoder sequence = ASNDecoder.Sequence(message, 1);            
		byte[] authKey = sequence.OctetString(20);
		byte[] endpointUid = sequence.OctetString(16);
		sequence.end();

		synchronized(mutex)
		{
			if (connectorState != ConnectorState.INITIAL)
				return;
			thisEndpointUid = endpointUid;			
		}
        
		Thread thread = new Thread()
		{
		    public void run(){
		    	udpExecute();
		    }
		};
		thread.start();
        
		authenticationHandler.accept(authKey, attachment);
	}
	
	private void udpExecute()
	{
		DatagramSocket dgmSocket = null;		
		try
		{
			synchronized(mutex)
			{
				if(connectorState == ConnectorState.COMPLETED)
					return;
				datagramSocket = new DatagramSocket(new InetSocketAddress(localIP, 0));
				datagramSocket.setSoTimeout(100);
				dgmSocket = datagramSocket;				
			}
		}
		catch(IOException ex)
		{
			completeOnError();
			return;
		}
		
		sendEndpointInfo(1);
		
		byte[] data = new byte[18];
		DatagramPacket packet = new DatagramPacket(data, 18);

		try {
			while(connectorState !=  ConnectorState.COMPLETED)
			{			
				try {
					packet.setLength(18);
					dgmSocket.receive(packet);
					
					if(packet.getLength() == 17)
					{
						byte messageTag = data[0];
						if(messageTag == Constants.Proxy.UdpEndpoint.P2P_HOLE_PUNCH)
						{
							synchronized(mutex)
							{
								if(isP2PInputHolePunched)
									continue;									
								
								if(ByteArrays.equals(data, 1, remoteEndpointUid, 0, 16) == false)
									continue;									

								isP2PInputHolePunched = true;
								remoteSocketAddress = (InetSocketAddress)packet.getSocketAddress();	

								if(!isP2POutputHolePunched)
									continue;
								
								connectorState =  ConnectorState.COMPLETED;
								datagramSocket = null;
							}
							
							msgSocket.send(MsgBuilder.Create(Constants.Proxy.UdpConnector.P2P_CONNECTION_CREATED));
							msgSocket.shutdownOutput();
							msgSocket.close();

							dgmSocket.setSoTimeout(0);
							responseHandler.onSuccess(new ResponseContext(null, null, attachment), dgmSocket, remoteSocketAddress, ConnectionMode.P2P);
							return;							
						}						
					}
				}
				catch (SocketTimeoutException e)
				{
					if(isP2POutputHolePunched && isP2PInputHolePunched)
					{
						synchronized(mutex)
						{
							if(connectorState == ConnectorState.COMPLETED)
								continue;							
							connectorState =  ConnectorState.COMPLETED;
							datagramSocket = null;
						}
						
						msgSocket.send(MsgBuilder.Create(Constants.Proxy.UdpConnector.P2P_CONNECTION_CREATED));
						msgSocket.shutdownOutput();
						msgSocket.close();

						dgmSocket.setSoTimeout(0);
						responseHandler.onSuccess(new ResponseContext(null, null, attachment), dgmSocket, remoteSocketAddress, ConnectionMode.P2P);
						return;							
					}
					
					if(isProxyConnectionCreated)
					{
						synchronized(mutex)
						{
							if(connectorState == ConnectorState.COMPLETED)
								continue;							
							connectorState =  ConnectorState.COMPLETED;
							datagramSocket = null;
						}
						
						msgSocket.shutdownOutput();
						msgSocket.close();
						
						dgmSocket.setSoTimeout(0);
						responseHandler.onSuccess(new ResponseContext(null, null, attachment), dgmSocket, new InetSocketAddress(serverIP, Constants.ServerPorts.UdpRzvPort), ConnectionMode.Proxy);
						return;
					}
					
					if(isTimeoutExpired)
					{
						synchronized(mutex)
						{
							if(connectorState == ConnectorState.COMPLETED)
								continue;
							connectorState =  ConnectorState.COMPLETED;
						}
						
						dgmSocket.close();
						msgSocket.shutdownOutput();
						msgSocket.close();
						
						responseHandler.onError(new ResponseContext(null, null, attachment), null);
						return;
					}
				}
			}
		}
		catch(IOException e) {			
			completeOnError();
		}	
	}
		
	private void sendEndpointInfo(Object state)
	{
		if(isEndpointEstablished)
			return;
		
		DatagramSocket dgmSocket = null;
		synchronized(mutex)
		{
			if(connectorState == ConnectorState.COMPLETED)
				return;
			dgmSocket = datagramSocket;
		}

		try
		{
			byte[] data = new byte[23];
			data[0] = Constants.Proxy.UdpEndpoint.ENDPOINT_INFO;
			System.arraycopy(thisEndpointUid, 0, data, 1, 16);
			System.arraycopy(localIP.getAddress(), 0, data, 17, 4);
			ByteConverter.writeAsUInt16(dgmSocket.getLocalPort(), data, 21);

			DatagramPacket packet = new DatagramPacket(data, 23, serverIP, Constants.ServerPorts.UdpRzvPort);
			dgmSocket.send(packet);

			int packetRepeatPeriod = (int)state;
			if(packetRepeatPeriod <= 8)
			{
				Acceptor<Object> acceptor = new Acceptor<Object>() {
					public void accept(Object state) { sendEndpointInfo(state); }
				};
				endpointEstablishmentTask = new ScheduledContextTask(acceptor, this, packetRepeatPeriod * 2);
				scheduler.add(endpointEstablishmentTask, packetRepeatPeriod);
			}
			else
			{
				Acceptor<Object> acceptor = new Acceptor<Object>() {
					public void accept(Object noData) { onEndpointEstablishmentFailed(); }
				};
				endpointEstablishmentTask = new ScheduledContextTask(acceptor, this, null);					
				scheduler.add(endpointEstablishmentTask, 4);
			}
		}
		catch(IOException ex) {
			completeOnError();			
		}
	}
	
	private void onEndpointEstablishmentFailed()
	{
		synchronized(mutex)
		{
			if (isEndpointEstablished || connectorState == ConnectorState.COMPLETED)
				return;
			connectorState = ConnectorState.COMPLETED;
			msgSocket.close();
			datagramSocket.close();
		}

		responseHandler.onError(new ResponseContext(null, null, attachment), null);
	}
		
	private void sendP2PHolePunch(Object state)
	{
		DatagramSocket dgmSocket = null;
		InetSocketAddress remoteIEP = null;
		synchronized(mutex)
		{
			if(connectorState != ConnectorState.P2P_HANDSHAKE)
				return;
			dgmSocket = datagramSocket;
			remoteIEP = remoteSocketAddress;
		}
				
		try
		{
			byte[] data = new byte[17];
			data[0] = Constants.Proxy.UdpEndpoint.P2P_HOLE_PUNCH;
			System.arraycopy(thisEndpointUid, 0, data, 1, 16);

			DatagramPacket packet = new DatagramPacket(data, 17, remoteIEP);
			dgmSocket.send(packet);
			
			int packetCounter = (int)state;
			if(packetCounter <= 3)
			{
				Acceptor<Object> acceptor = new Acceptor<Object>() {
					public void accept(Object state) { sendP2PHolePunch(state); }
				};
				packetCounter++;
				ScheduledContextTask task = new ScheduledContextTask(acceptor, this, packetCounter);
				scheduler.add(task, 1);
			}			
		}
		catch(IOException ex) {}
	}

	private void onTimeoutExpired()
	{
		isTimeoutExpired = true;
	}
	
	private void ProcessMessage_CreateP2PConnection(byte[] message) throws AsnException, UnknownHostException
	{
		SequenceDecoder sequence = ASNDecoder.Sequence(message, 1);
		byte[] iepBytes = sequence.OctetString(18);
		byte[] endpointUid = sequence.OctetString(16);
		sequence.end();
		
		byte[] ipBytes = new byte[16];
		System.arraycopy(iepBytes, 0, ipBytes, 0, 16);		
		InetAddress ip = InetAddress.getByAddress(ipBytes);
		int port = ByteConverter.toInt32FromUInt16(iepBytes, 16);
		InetSocketAddress socketAddress = new InetSocketAddress(ip, port);
		
		synchronized(mutex)
		{
			if (connectorState != ConnectorState.INITIAL)
				return;
			connectorState = ConnectorState.P2P_HANDSHAKE;
			
			remoteEndpointUid = endpointUid;
			remoteSocketAddress = socketAddress;
						
			isEndpointEstablished = true;
			if(endpointEstablishmentTask != null)
				endpointEstablishmentTask.complete();
		}
		
		Acceptor<Object> acceptor = new Acceptor<Object>() {
			public void accept(Object noData) { onTimeoutExpired(); }
		};
		ScheduledContextTask task = new ScheduledContextTask(acceptor, this, null);
		scheduler.add(task, Constants.UdpP2PConnectionAttemptTimeoutSeconds + 2);
        
		sendP2PHolePunch(1);
	}
	
	private void ProcessMessage_P2PHolePunched()
	{
		isP2POutputHolePunched = true;
	}
	
	private void ProcessMessage_ProxyConnectionCreated()
	{
		isProxyConnectionCreated = true;
	}
	
	private SoftnetMessage EncodeMessage_ClientEndpoint()
	{
		ASNEncoder asnEncoder = new ASNEncoder();
        SequenceEncoder sequence = asnEncoder.Sequence();
        sequence.OctetString(connectionUid);
        return MsgBuilder.Create(Constants.Proxy.UdpConnector.CLIENT_ENDPOINT, asnEncoder);
	}
	
	private void onMessageReceived(byte[] message)
	{
		try		
		{
			byte messageTag = message[0];
			if(messageTag == Constants.Proxy.UdpConnector.AUTH_KEY)
			{
				ProcessMessage_AuthKey(message);
			}
			else if(messageTag == Constants.Proxy.UdpConnector.CREATE_P2P_CONNECTION)
			{
				ProcessMessage_CreateP2PConnection(message);
			}
			else if(messageTag == Constants.Proxy.UdpConnector.P2P_HOLE_PUNCHED)
			{
				ProcessMessage_P2PHolePunched();
			}
			else if(messageTag == Constants.Proxy.UdpConnector.PROXY_CONNECTION_CREATED)
			{
				ProcessMessage_ProxyConnectionCreated();
			}
			else if(messageTag == Constants.Proxy.UdpConnector.ERROR)
			{
				completeOnError();
			}
			else
			{
				completeOnError();
			}
		}
		catch(AsnException | UnknownHostException e)
		{
			completeOnError();
		}
	}
		
	private void closeChannel(SocketChannel channel)
	{
		try	{
			channel.close();
		}
		catch(IOException e) {}
	}
}
