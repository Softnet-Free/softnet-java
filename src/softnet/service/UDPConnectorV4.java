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

package softnet.service;

import java.net.*;
import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.UUID;

import softnet.ConnectionMode;
import softnet.asn.*;
import softnet.core.*;
import softnet.exceptions.*;
import softnet.utils.*;

class UDPConnectorV4 implements UDPConnector, STaskContext
{
	private UUID connectionUid;
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
	private InetSocketAddress remotePublicSocketAddress = null;
	private InetSocketAddress remotePrivateSocketAddress = null;
	private boolean isP2PInputHolePunched = false;
	private boolean isP2PInputLocalHolePunched = false;
	private boolean isP2PConnectionCreated = false;
	private boolean isProxyConnectionCreated = false;
	private boolean isTimeoutExpired = false;

	private enum ConnectorState {
        INITIAL, P2P_HANDSHAKE, PROXY_HANDSHAKE, COMPLETED
	}
	private ConnectorState connectorState = ConnectorState.INITIAL;
			
	public UDPConnectorV4(UUID connectionUid, InetAddress serverIP, Scheduler scheduler)
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
		
		responseHandler.onError(attachment);
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
				public void accept(NetworkErrorSoftnetException e) { completeOnError(); }
			};
			msgSocket.formatErrorHandler = new Runnable()
			{
				public void run() { completeOnError(); }
			};					
			msgSocket.minLength = 1;
			msgSocket.maxLength = 256;
			msgSocket.start();
			
			msgSocket.send(EncodeMessage_ServiceEndpoint());
		}
		catch(IOException ex)
		{			
			if(controlChannel != null)
				closeChannel(controlChannel);
			responseHandler.onError(attachment);
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
					packet.setData(data);
					dgmSocket.receive(packet);
					
					if(packet.getLength() == 17)
					{
						byte messageTag = data[0];
						if(messageTag == Constants.Proxy.UdpEndpoint.P2P_HOLE_PUNCH)
						{
							synchronized(mutex)
							{
								if(connectorState != ConnectorState.P2P_HANDSHAKE)
									continue;

								if(isP2PInputHolePunched || isP2PInputLocalHolePunched)
									continue;									
								
								if(ByteArrays.equals(data, 1, remoteEndpointUid, 0, 16) == false)
									continue;									

								isP2PInputHolePunched = true;
								remotePublicSocketAddress = (InetSocketAddress)packet.getSocketAddress();								
								msgSocket.send(MsgBuilder.Create(Constants.Proxy.UdpConnector.P2P_HOLE_PUNCHED));
							}
							continue;
						}
						
						if(messageTag == Constants.Proxy.UdpEndpoint.P2P_LOCAL_HOLE_PUNCH)
						{
							synchronized(mutex)
							{
								if(connectorState != ConnectorState.P2P_HANDSHAKE)
									continue;

								if(isP2PInputLocalHolePunched || isP2PInputHolePunched)
									continue;									

								if(ByteArrays.equals(data, 1, remoteEndpointUid, 0, 16) == false)
									continue;										

								isP2PInputLocalHolePunched = true;
								remotePrivateSocketAddress = (InetSocketAddress)packet.getSocketAddress();								
								msgSocket.send(MsgBuilder.Create(Constants.Proxy.UdpConnector.P2P_LOCAL_HOLE_PUNCHED));
							}
							continue;
						}
					}
				}
				catch (SocketTimeoutException e)
				{
					try
					{
						if(isP2PConnectionCreated)
						{
							synchronized(mutex)
							{
								if(connectorState == ConnectorState.COMPLETED)
									return;
								connectorState = ConnectorState.COMPLETED;			
								datagramSocket = null;
							}

							msgSocket.shutdownOutput();
							msgSocket.close();

							if(isP2PInputHolePunched)
							{
								dgmSocket.setSoTimeout(0);
								responseHandler.onSuccess(dgmSocket, remotePublicSocketAddress, ConnectionMode.P2P, attachment);
								return;							
							}

							if(isP2PInputLocalHolePunched)
							{
								dgmSocket.setSoTimeout(0);
								responseHandler.onSuccess(dgmSocket, remotePrivateSocketAddress, ConnectionMode.P2P, attachment);
								return;
							}
								
							dgmSocket.close();
							responseHandler.onError(attachment);
							return;
						}
						
						if(isProxyConnectionCreated)
						{
							synchronized(mutex)
							{
								if(connectorState == ConnectorState.COMPLETED)
									return;
								connectorState = ConnectorState.COMPLETED;
								datagramSocket = null;
							}
							
							msgSocket.shutdownOutput();
							msgSocket.close();

							dgmSocket.setSoTimeout(0);
							responseHandler.onSuccess(dgmSocket, new InetSocketAddress(serverIP, Constants.ServerPorts.UdpRzvPort), ConnectionMode.Proxy, attachment);
							return;							
						}
						
						if(isTimeoutExpired)
						{
							synchronized(mutex)
							{
								if(connectorState == ConnectorState.COMPLETED)
									return;
								connectorState = ConnectorState.COMPLETED;
							}
							
							msgSocket.shutdownOutput();
							msgSocket.close();
							dgmSocket.close();
							
							responseHandler.onError(attachment);
							return;							
						}
					}
					catch(IOException ex) {
						dgmSocket.close();
						responseHandler.onError(attachment);
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
			byte[] ipBytes = new byte[4];
			System.arraycopy(localIP.getAddress(), 0, ipBytes, 0, 4);
			for (int i = 0; i < 4; i++)
	            data[17+i] = (byte) ~ipBytes[i];
			ByteConverter.writeAsUInt16(dgmSocket.getLocalPort(), data, 21);			
			DatagramPacket packet = new DatagramPacket(data, 23, serverIP, Constants.ServerPorts.UdpRzvPort);
			dgmSocket.send(packet);

			int packetRepeatPeriod = (int)state;
			if(packetRepeatPeriod <= 8)
			{
				Acceptor<Object> acceptor = new Acceptor<Object>()
				{
					public void accept(Object state) { sendEndpointInfo(state); }
				};
				endpointEstablishmentTask = new ScheduledContextTask(acceptor, this, packetRepeatPeriod * 2);
				scheduler.add(endpointEstablishmentTask, packetRepeatPeriod);
			}
			else
			{
				Acceptor<Object> acceptor = new Acceptor<Object>()
				{
					public void accept(Object noData) { onEndpointEstablishmentFailed(); }
				};
				endpointEstablishmentTask = new ScheduledContextTask(acceptor, this, null);					
				scheduler.add(endpointEstablishmentTask, 4);
			}
		}
		catch(IOException ex)
		{
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
		
		responseHandler.onError(attachment);
	}
	
	private void sendP2PHolePunch(Object state)
	{
		DatagramSocket dgmSocket = null;
		InetSocketAddress remotePublicIEP = null;
		synchronized(mutex)
		{
			if(connectorState != ConnectorState.P2P_HANDSHAKE)
				return;
			dgmSocket = datagramSocket;
			remotePublicIEP = remotePublicSocketAddress;
		}
		
		try
		{
			byte[] data = new byte[17];
			data[0] = Constants.Proxy.UdpEndpoint.P2P_HOLE_PUNCH;
			System.arraycopy(thisEndpointUid, 0, data, 1, 16);

			DatagramPacket packet = new DatagramPacket(data, 17, remotePublicIEP);
			dgmSocket.send(packet);
						
			int packetCounter = (int)state;
			if(packetCounter <= 3)
			{
				Acceptor<Object> acceptor = new Acceptor<Object>()
				{
					public void accept(Object state) { sendP2PHolePunch(state); }
				};
				packetCounter++;
				ScheduledContextTask task = new ScheduledContextTask(acceptor, this, packetCounter);
				scheduler.add(task, 1);
			}
		}
		catch(IOException ex) {}
	}

	private void sendP2PLocalHolePunch(Object state)
	{
		DatagramSocket dgmSocket = null;
		InetSocketAddress remotePrivateIEP = null;
		synchronized(mutex)
		{
			if(connectorState != ConnectorState.P2P_HANDSHAKE)
				return;
			dgmSocket = datagramSocket;
			remotePrivateIEP = remotePrivateSocketAddress;
		}
		
		try
		{
			byte[] data = new byte[17];
			data[0] = Constants.Proxy.UdpEndpoint.P2P_LOCAL_HOLE_PUNCH;
			System.arraycopy(thisEndpointUid, 0, data, 1, 16);

			DatagramPacket packet = new DatagramPacket(data, 17, remotePrivateIEP);
			dgmSocket.send(packet);			
			
			int packetCounter = (int)state;
			if(packetCounter <= 3)
			{
				Acceptor<Object> acceptor = new Acceptor<Object>()
				{
					public void accept(Object state) { sendP2PLocalHolePunch(state); }
				};
				packetCounter++;
				ScheduledContextTask task = new ScheduledContextTask(acceptor, this, packetCounter);
				scheduler.add(task, 1);
			}
		}
		catch(IOException ex) {}
	}

	private void onP2PAttemptTimeoutExpired()
	{
		synchronized(mutex)
		{
			if(connectorState == ConnectorState.COMPLETED)
				return;
			if(!isP2PInputHolePunched && !isP2PInputLocalHolePunched) 
			{
				connectorState = ConnectorState.PROXY_HANDSHAKE;				
				msgSocket.send(MsgBuilder.Create(Constants.Proxy.UdpConnector.CREATE_PROXY_CONNECTION));
			}
		}
		
		Acceptor<Object> acceptor = new Acceptor<Object>() {
			public void accept(Object noData) { onTimeoutExpired(); }
		};
		ScheduledContextTask task = new ScheduledContextTask(acceptor, this, null);
		scheduler.add(task, 4);
	}
	
	private void onTimeoutExpired() {
		isTimeoutExpired = true;
	}
	
	private void ProcessMessage_CreateP2PConnection(byte[] message) throws AsnException, UnknownHostException
	{
		SequenceDecoder sequence = ASNDecoder.Sequence(message, 1);
		byte[] iepBytes = sequence.OctetString(6);
		byte[] endpointUid = sequence.OctetString(16);
		sequence.end();
		
		byte[] ipBytes = new byte[4];
		System.arraycopy(iepBytes, 0, ipBytes, 0, 4);		
		InetAddress ip = InetAddress.getByAddress(ipBytes);
		int port = ByteConverter.toInt32FromUInt16(iepBytes, 4);
		InetSocketAddress socketAddress = new InetSocketAddress(ip, port);
		
		synchronized(mutex)
		{
			if (connectorState != ConnectorState.INITIAL)
				return;
			connectorState = ConnectorState.P2P_HANDSHAKE;
			
			remoteEndpointUid = endpointUid;
			remotePublicSocketAddress = socketAddress;
			
			isEndpointEstablished = true;
			if(endpointEstablishmentTask != null)
				endpointEstablishmentTask.complete();
		}
		
		Acceptor<Object> acceptor = new Acceptor<Object>() {
			public void accept(Object noData) { onP2PAttemptTimeoutExpired(); }
		};
		ScheduledContextTask task = new ScheduledContextTask(acceptor, this, null);
		scheduler.add(task, Constants.UdpP2PConnectionAttemptTimeoutSeconds);
        
		sendP2PHolePunch(1);
	}
	
	private void ProcessMessage_CreateP2PConnectionInDualMode(byte[] message) throws AsnException, UnknownHostException
	{
		SequenceDecoder sequence = ASNDecoder.Sequence(message, 1);
		byte[] publicIepBytes = sequence.OctetString(6);
		byte[] privateIepBytes = sequence.OctetString(6);
		byte[] endpointUid = sequence.OctetString(16);
		sequence.end();
		
		byte[] ipBytes = new byte[4];
		System.arraycopy(publicIepBytes, 0, ipBytes, 0, 4);		
		InetAddress ip = InetAddress.getByAddress(ipBytes);
		int port = ByteConverter.toInt32FromUInt16(publicIepBytes, 4);
		InetSocketAddress publicSocketAddress = new InetSocketAddress(ip, port);

		for (int i = 0; i < 4; i++)
			ipBytes[i] = (byte) ~privateIepBytes[i];
		ip = InetAddress.getByAddress(ipBytes);
		port = ByteConverter.toInt32FromUInt16(privateIepBytes, 4);
		InetSocketAddress privateSocketAddress = new InetSocketAddress(ip, port);
		
		synchronized(mutex)
		{
			if (connectorState != ConnectorState.INITIAL)
				return;
			connectorState = ConnectorState.P2P_HANDSHAKE;
			
			remoteEndpointUid = endpointUid;
			remotePublicSocketAddress = publicSocketAddress;
			remotePrivateSocketAddress = privateSocketAddress;
			
			isEndpointEstablished = true;
			if(endpointEstablishmentTask != null)
				endpointEstablishmentTask.complete();
		}

		Acceptor<Object> acceptor = new Acceptor<Object>() {
			public void accept(Object noData) { onP2PAttemptTimeoutExpired(); }
		};
		ScheduledContextTask task = new ScheduledContextTask(acceptor, this, null);
		scheduler.add(task, Constants.UdpP2PConnectionAttemptTimeoutSeconds);
        
		sendP2PLocalHolePunch(1);
		sendP2PHolePunch(1);
	}

	private void ProcessMessage_P2PConnectionCreated()
	{
		synchronized(mutex)
		{
			if(isP2PInputHolePunched || isP2PInputLocalHolePunched)
				isP2PConnectionCreated = true;
		}
	}

	private void ProcessMessage_ProxyConnectionCreated()
	{
		synchronized(mutex)
		{
			if(connectorState == ConnectorState.INITIAL || connectorState == ConnectorState.PROXY_HANDSHAKE)
				isProxyConnectionCreated = true;
		}
	}
	
	private SoftnetMessage EncodeMessage_ServiceEndpoint()
	{
		ASNEncoder asnEncoder = new ASNEncoder();
		SequenceEncoder sequence = asnEncoder.Sequence();
		sequence.OctetString(connectionUid);
		return MsgBuilder.Create(Constants.Proxy.UdpConnector.SERVICE_ENDPOINT, asnEncoder);
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
			else if(messageTag == Constants.Proxy.UdpConnector.CREATE_P2P_CONNECTION_IN_DUAL_MODE)
			{
				ProcessMessage_CreateP2PConnectionInDualMode(message);
			}		
			else if(messageTag == Constants.Proxy.UdpConnector.P2P_CONNECTION_CREATED)
			{
				ProcessMessage_P2PConnectionCreated();
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
