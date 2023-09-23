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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.nio.ByteBuffer;
import java.nio.channels.*;

import softnet.*;
import softnet.asn.*;
import softnet.core.*;
import softnet.exceptions.*;
import softnet.utils.*;

class TCPConnectorV4 implements TCPConnector
{
	private byte[] connectionUid;
	private InetAddress serverIP;
	private TCPOptions tcpOptions;
	private TCPResponseHandler responseHandler;
	private BiAcceptor<byte[], Object> authenticationHandler;
	private Object attachment;
	
	private Object mutex = new Object();
	private MsgSocket msgSocket = null;
	private InetSocketAddress localIEP = null;
	private InetSocketAddress remotePublicIEP = null;
	private InetSocketAddress remotePrivateIEP = null;
	private byte[] secretKey = null;
	private byte[] remoteSecretKey = null;

	private SocketChannel controlSocketChannel = null;
	private ServerSocketChannel listenerSocketChannel = null;
	private SocketChannel p2pSocketChannel = null;
	private SocketChannel p2pLocalSocketChannel = null;
	private SocketChannel proxySocketChannel = null;
	private ArrayList<SocketChannel> acceptedChannels;
	
	private enum ConnectorState
    {
        INITIAL, P2P_MODE, P2P_HANDSHAKE, PROXY_MODE, PROXY_HANDSHAKE, COMPLETED
    }
	private ConnectorState connectorState = ConnectorState.INITIAL;
	
	public TCPConnectorV4(byte[] connectionUid, InetAddress serverIP, TCPOptions tcpOptions)
	{
		this.connectionUid = connectionUid;
		this.serverIP = serverIP;
		this.tcpOptions = tcpOptions;
		acceptedChannels = new ArrayList<SocketChannel>(2);
	}
	
	public void connect(TCPResponseHandler responseHandler, BiAcceptor<byte[], Object> authenticationHandler, Object attachment)
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
        msgSocket.send(MsgBuilder.Create(Constants.Proxy.TcpConnector.AUTH_HASH, asnEncoder));
	}
	
	public void abort()
	{
		synchronized(mutex)
		{
			if(connectorState == ConnectorState.COMPLETED)
				return;			
			connectorState = ConnectorState.COMPLETED;
			freeResources();
		}
	}
			
	private void completeOnError()
	{		
		synchronized(mutex)
		{
			if (connectorState == ConnectorState.COMPLETED)
                return;
			connectorState = ConnectorState.COMPLETED;
			freeResources();
		}		
		responseHandler.onError(new ResponseContext(null, null, attachment), null);
	}

	private void freeResources()
	{
		closeWithNullCheck(controlSocketChannel);
		closeWithNullCheck(p2pSocketChannel);
		closeWithNullCheck(p2pLocalSocketChannel);
		closeWithNullCheck(proxySocketChannel);

		if(listenerSocketChannel != null)
			closeServerChannel(listenerSocketChannel);
		
		if(acceptedChannels.size() > 0)
		{
			for(SocketChannel acceptedChannel: acceptedChannels)
				closeChannel(acceptedChannel);
			acceptedChannels.clear();
		}		
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
				controlSocketChannel = SocketChannel.open();
				controlChannel = controlSocketChannel;
			}
			
			if (tcpOptions != null)
			{
				if(8196 <= tcpOptions.receiveBufferSize && tcpOptions.receiveBufferSize <= 1073741824)
					controlChannel.setOption(StandardSocketOptions.SO_RCVBUF, tcpOptions.receiveBufferSize);
			}
						
			controlChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);			
			controlChannel.configureBlocking(true);
			controlChannel.connect(new InetSocketAddress(serverIP, Constants.ServerPorts.TcpRzvPort));	
						
			localIEP = (InetSocketAddress)controlChannel.getLocalAddress();
			
			msgSocket = new MsgSocket(controlChannel);
			msgSocket.messageReceivedHandler = new Acceptor<byte[]>()
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

			try
			{	
				SocketChannel localP2PChannel = null;			
				synchronized(mutex)
				{
					if(connectorState != ConnectorState.INITIAL)
						return;
					p2pLocalSocketChannel = SocketChannel.open();
					localP2PChannel = p2pLocalSocketChannel;
				}
	
				if (tcpOptions != null)
				{
					if(8196 <= tcpOptions.receiveBufferSize && tcpOptions.receiveBufferSize <= 1073741824)
						localP2PChannel.setOption(StandardSocketOptions.SO_RCVBUF, tcpOptions.receiveBufferSize);
	
					if(8196 <= tcpOptions.sendBufferSize && tcpOptions.sendBufferSize <= 1073741824)
						localP2PChannel.setOption(StandardSocketOptions.SO_SNDBUF, tcpOptions.sendBufferSize);
				}
				
				localP2PChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
				localP2PChannel.configureBlocking(true);
				localP2PChannel.bind(localIEP);
				
				try
				{	
					SocketChannel p2pChannel = null;
					synchronized(mutex)
					{
						if(connectorState != ConnectorState.INITIAL)
							return;
						p2pSocketChannel = SocketChannel.open();
						p2pChannel = p2pSocketChannel;						
					}
	
					if (tcpOptions != null)
					{
						if(8196 <= tcpOptions.receiveBufferSize && tcpOptions.receiveBufferSize <= 1073741824)
							p2pChannel.setOption(StandardSocketOptions.SO_RCVBUF, tcpOptions.receiveBufferSize);
	
						if(8196 <= tcpOptions.sendBufferSize && tcpOptions.sendBufferSize <= 1073741824)
							p2pChannel.setOption(StandardSocketOptions.SO_SNDBUF, tcpOptions.sendBufferSize);
					}
					
					p2pChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
					p2pChannel.configureBlocking(true);
					p2pChannel.bind(localIEP);
				}
				catch(IOException | UnsupportedOperationException ex) {}
				
				try
				{									
					ServerSocketChannel listenerChannel = null;
					synchronized(mutex)
					{
						if(connectorState != ConnectorState.INITIAL)
							return;
						listenerSocketChannel = ServerSocketChannel.open();
						listenerChannel = listenerSocketChannel;
					}
					
					if (tcpOptions != null)
					{
						if(8196 <= tcpOptions.receiveBufferSize && tcpOptions.receiveBufferSize <= 1073741824)
							listenerChannel.setOption(StandardSocketOptions.SO_RCVBUF, tcpOptions.receiveBufferSize);
					}
				
					listenerChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
					listenerChannel.configureBlocking(true);
					listenerChannel.bind(localIEP);
				
					final ServerSocketChannel f_listenerChannel = listenerChannel;
					Thread thread = new Thread()
					{
					    public void run(){
					    	executeListener(f_listenerChannel);
					    }
					};
					thread.start();
				}
				catch(IOException | UnsupportedOperationException ex) {}
								
				synchronized(mutex)
				{
					if(connectorState != ConnectorState.INITIAL)
						return;
					connectorState = ConnectorState.P2P_MODE;
				}
				
				msgSocket.send(EncodeMessage_ClientP2P());
			}
			catch(IOException | UnsupportedOperationException ex)
			{	
				onP2PSetup1Failed();
			}
		}
		catch(UnsupportedOperationException ex)
		{
			closeWithNullCheck(controlChannel);
			onP2PSetup2Failed();
		}
		catch(IOException ex)
		{			
			closeWithNullCheck(controlChannel);
            responseHandler.onError(new ResponseContext(null, null, attachment), null);
		}
	}
	
	private void onP2PSetup1Failed()
	{
		synchronized(mutex)
		{
			if(connectorState != ConnectorState.INITIAL)
				return;
			connectorState = ConnectorState.PROXY_MODE;
		}		
		msgSocket.send(EncodeMessage_ClientProxy());
	}
	
	private void onP2PSetup2Failed()
	{
		SocketChannel controlChannel = null;
		try
		{
			synchronized(mutex)
			{
				if(connectorState != ConnectorState.INITIAL)
					return;				
				connectorState = ConnectorState.PROXY_MODE;

				controlSocketChannel = SocketChannel.open();
				controlChannel = controlSocketChannel;
			}
			
			if (tcpOptions != null)
			{
				if(8196 <= tcpOptions.receiveBufferSize && tcpOptions.receiveBufferSize <= 1073741824)
					controlChannel.setOption(StandardSocketOptions.SO_RCVBUF, tcpOptions.receiveBufferSize);
			}
						
			controlChannel.configureBlocking(true);
			controlChannel.connect(new InetSocketAddress(serverIP, Constants.ServerPorts.TcpRzvPort));	
			
			msgSocket = new MsgSocket(controlChannel);
			msgSocket.messageReceivedHandler = new Acceptor<byte[]>()
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
						
			msgSocket.send(EncodeMessage_ClientProxy());
		}
		catch(IOException ex)
		{			
			closeWithNullCheck(controlChannel);
            responseHandler.onError(new ResponseContext(null, null, attachment), null);
		}
	}

	private void executeListener(ServerSocketChannel listenerChannel)
	{
		int counter = 0;
		while(counter < 10)
		{		
			counter++;
			try
			{
				SocketChannel acceptedChannel = listenerChannel.accept();				
				synchronized(mutex)
				{
					if(connectorState == ConnectorState.P2P_HANDSHAKE)
					{
						acceptedChannels.add(acceptedChannel);						
						ClientAuthenticationThread thread = new ClientAuthenticationThread(acceptedChannel);
						thread.start();						
					}
					else if (connectorState == ConnectorState.P2P_MODE)
					{
						acceptedChannels.add(acceptedChannel);
					}
					else
					{
						closeChannel(acceptedChannel);
						return;
					}
				}
			}
			catch(ClosedChannelException e)	{
				return;
			}
			catch(IOException e){}
		}		
	}
	
	private void authenticateClient(SocketChannel acceptedChannel)
	{
		try
		{
			acceptedChannel.configureBlocking(true);

			ByteBuffer bbSecretKey = ByteBuffer.wrap(secretKey);
			while(bbSecretKey.hasRemaining())
				acceptedChannel.write(bbSecretKey);
			
			ByteBuffer bbReceivedRemoteSecretKey = ByteBuffer.allocate(4);
			while(bbReceivedRemoteSecretKey.hasRemaining())
			{
				int bytesRead = acceptedChannel.read(bbReceivedRemoteSecretKey);
				if(bytesRead == -1)
					return;
			}
			
			byte[] receivedRemoteSecretKey = bbReceivedRemoteSecretKey.array();
			if(java.util.Arrays.equals(receivedRemoteSecretKey, remoteSecretKey) == false)
				return;
			
			synchronized(mutex)
	        {
	            if (connectorState != ConnectorState.P2P_HANDSHAKE)
	                return;
	            connectorState = ConnectorState.COMPLETED;                
	            acceptedChannels.remove(acceptedChannel);            
	            freeResources();
	        }

	        responseHandler.onSuccess(new ResponseContext(null, null, attachment), acceptedChannel, ConnectionMode.P2P);	
		}
		catch(IOException ex) {}
	}

	private void tryP2PConnection()
	{
		try
		{			
			SocketChannel p2pChannel = null;
			synchronized(mutex)
			{
				if(connectorState != ConnectorState.P2P_HANDSHAKE)
					return;
				p2pChannel = p2pSocketChannel;				
			}
						
			p2pChannel.connect(remotePublicIEP);
						
			ByteBuffer bbSecretKey = ByteBuffer.wrap(secretKey);
			while(bbSecretKey.hasRemaining())
				p2pChannel.write(bbSecretKey);
			
			ByteBuffer bbReceivedRemoteSecretKey = ByteBuffer.allocate(4);
			while(bbReceivedRemoteSecretKey.hasRemaining())
			{
				int bytesRead = p2pChannel.read(bbReceivedRemoteSecretKey);
				if(bytesRead == -1)
					return;
			}
			
			byte[] receivedRemoteSecretKey = bbReceivedRemoteSecretKey.array();
			if(java.util.Arrays.equals(receivedRemoteSecretKey, remoteSecretKey) == false)
				return;
			
			synchronized(mutex)
	        {
	            if (connectorState != ConnectorState.P2P_HANDSHAKE)
	                return;
	            connectorState = ConnectorState.COMPLETED;
	            p2pSocketChannel = null;
	            freeResources();
	        }

	        responseHandler.onSuccess(new ResponseContext(null, null, attachment), p2pChannel, ConnectionMode.P2P);	
		}
		catch(IOException e) {}	
	}

	private void tryLocalP2PConnection()
	{
		try
		{			
			SocketChannel p2pLocalChannel = null;
			synchronized(mutex)
			{
				if(connectorState != ConnectorState.P2P_HANDSHAKE)
					return;
				p2pLocalChannel = p2pLocalSocketChannel;				
			}
			
			p2pLocalChannel.connect(remotePrivateIEP);
			
			ByteBuffer bbSecretKey = ByteBuffer.wrap(secretKey);
			while(bbSecretKey.hasRemaining())
				p2pLocalChannel.write(bbSecretKey);
			
			ByteBuffer bbReceivedRemoteSecretKey = ByteBuffer.allocate(4);
			while(bbReceivedRemoteSecretKey.hasRemaining())
			{
				int bytesRead = p2pLocalChannel.read(bbReceivedRemoteSecretKey);
				if(bytesRead == -1)
					return;
			}
			
			byte[] receivedRemoteSecretKey = bbReceivedRemoteSecretKey.array();
			if(java.util.Arrays.equals(receivedRemoteSecretKey, remoteSecretKey) == false)
				return;
			
			synchronized(mutex)
	        {
	            if (connectorState != ConnectorState.P2P_HANDSHAKE)
	                return;
	            connectorState = ConnectorState.COMPLETED;
	            p2pLocalSocketChannel = null;
	            freeResources();
	        }

	        responseHandler.onSuccess(new ResponseContext(null, null, attachment), p2pLocalChannel, ConnectionMode.P2P);	
		}
		catch(IOException e) {}
	}
	
	private void tryProxyConnection(int serverPort)
	{
		try
		{			
			SocketChannel proxyChannel = null;
			synchronized(mutex)
			{
				if(!(connectorState == ConnectorState.P2P_HANDSHAKE || connectorState == ConnectorState.P2P_MODE || connectorState == ConnectorState.PROXY_MODE))
					return;
				connectorState = ConnectorState.PROXY_HANDSHAKE;				
				
				proxySocketChannel = SocketChannel.open();
				proxyChannel = proxySocketChannel;
			}

			if (tcpOptions != null)
			{
				if(8196 <= tcpOptions.receiveBufferSize && tcpOptions.receiveBufferSize <= 1073741824)
					proxyChannel.setOption(StandardSocketOptions.SO_RCVBUF, tcpOptions.receiveBufferSize);
				
				if(8196 <= tcpOptions.sendBufferSize && tcpOptions.sendBufferSize <= 1073741824)
					proxyChannel.setOption(StandardSocketOptions.SO_SNDBUF, tcpOptions.sendBufferSize);
			}
			
			proxyChannel.configureBlocking(true);
			proxyChannel.connect(new InetSocketAddress(serverIP, serverPort));
			
			byte[] header = new byte[17];
			header[0] = Constants.Proxy.TcpProxy.CLIENT_PROXY_ENDPOINT;
			System.arraycopy(connectionUid, 0, header, 1, 16);
			
			ByteBuffer bbHeader = ByteBuffer.wrap(header);
			while(bbHeader.hasRemaining())
				proxyChannel.write(bbHeader);
			
			bbHeader.clear();
			while(bbHeader.hasRemaining())
			{
				int bytesRead = proxyChannel.read(bbHeader);
				if(bytesRead == -1) {
					completeOnError();
					return;
				}
			}
			
			synchronized(mutex)
	        {
	            if (connectorState != ConnectorState.PROXY_HANDSHAKE)
	                return;
	            connectorState = ConnectorState.COMPLETED;                
	            proxySocketChannel = null;
	            freeResources();
	        }

	        responseHandler.onSuccess(new ResponseContext(null, null, attachment), proxyChannel, ConnectionMode.Proxy);
		}
		catch(IOException e) {
			completeOnError();
		}		
	}
	
	private void ProcessMessage_AuthKey(byte[] message) throws AsnException
	{
		SequenceDecoder sequence = ASNDecoder.Sequence(message, 1);            
		byte[] authKey = sequence.OctetString(20);
        sequence.end();        
        authenticationHandler.accept(authKey, attachment);
	}
				
	private void ProcessMessage_CreateP2PConnection(byte[] message) throws AsnException, UnknownHostException
	{
		SequenceDecoder sequence = ASNDecoder.Sequence(message, 1);
		byte[] publicIepBytes = sequence.OctetString(6);
		secretKey = sequence.OctetString(4);
		remoteSecretKey = sequence.OctetString(4);
		sequence.end();
		
		byte[] ipBytes = new byte[4];
		System.arraycopy(publicIepBytes, 0, ipBytes, 0, 4);		
		InetAddress ip = InetAddress.getByAddress(ipBytes);
		int port = ByteConverter.toInt32FromUInt16(publicIepBytes, 4);
		remotePublicIEP = new InetSocketAddress(ip, port);

		synchronized(mutex)
		{
			if (connectorState != ConnectorState.P2P_MODE)
                return;
			connectorState = ConnectorState.P2P_HANDSHAKE;
			
			if(acceptedChannels.size() > 0)
			{				
				for(SocketChannel acceptedChannel: acceptedChannels)
				{
					ClientAuthenticationThread thread = new ClientAuthenticationThread(acceptedChannel);
					thread.start();
				}
			}
		}
		
		Thread thread = new Thread()
		{
		    public void run(){
		    	tryP2PConnection();		    	
		    }
		};
		thread.start();
	}

	private void ProcessMessage_CreateP2PConnectionInDualMode(byte[] message) throws AsnException, UnknownHostException
	{
		SequenceDecoder sequence = ASNDecoder.Sequence(message, 1);
		byte[] publicIepBytes = sequence.OctetString(6);
		secretKey = sequence.OctetString(4);
		remoteSecretKey = sequence.OctetString(4);
		byte[] privateIepBytes = sequence.OctetString(6);
		sequence.end();
		
		byte[] ipBytes = new byte[4];
		System.arraycopy(publicIepBytes, 0, ipBytes, 0, 4);		
		InetAddress ip = InetAddress.getByAddress(ipBytes);
		int port = ByteConverter.toInt32FromUInt16(publicIepBytes, 4);
		remotePublicIEP = new InetSocketAddress(ip, port);

		for (int i = 0; i < 4; i++)
			ipBytes[i] = (byte) ~privateIepBytes[i];
		ip = InetAddress.getByAddress(ipBytes);
		port = ByteConverter.toInt32FromUInt16(privateIepBytes, 4);
		remotePrivateIEP = new InetSocketAddress(ip, port);
				
		synchronized(mutex)
		{
			if (connectorState != ConnectorState.P2P_MODE)
                return;
			connectorState = ConnectorState.P2P_HANDSHAKE;
			
			if(acceptedChannels.size() > 0)
			{				
				for(SocketChannel acceptedChannel: acceptedChannels)
				{
					ClientAuthenticationThread thread = new ClientAuthenticationThread(acceptedChannel);
					thread.start();
				}
			}
		}
		
		Thread thread = new Thread()
		{
		    public void run(){
		    	tryLocalP2PConnection();
		    }
		};
		thread.start();
		
		Thread thread2 = new Thread()
		{
		    public void run(){
		    	tryP2PConnection();		    	
		    }
		};
		thread2.start();
	}

	private void ProcessMessage_CreateProxyConnection(byte[] message) throws AsnException
	{
		SequenceDecoder sequence = ASNDecoder.Sequence(message, 1);
		final int serverPort = sequence.Int32();
		sequence.end();
		
		Thread thread = new Thread()
		{
		    public void run(){
				tryProxyConnection(serverPort);
		    }
		};
		thread.start();		
	}

	private SoftnetMessage EncodeMessage_ClientP2P()
	{
		byte[] iepBytes = new byte[6];
		System.arraycopy(localIEP.getAddress().getAddress(), 0, iepBytes, 0, 4);
		for (int i = 0; i < 4; i++)
			iepBytes[i] = (byte) ~iepBytes[i];
		ByteConverter.writeAsUInt16(localIEP.getPort(), iepBytes, 4);
				
		ASNEncoder asnEncoder = new ASNEncoder();
        SequenceEncoder sequence = asnEncoder.Sequence();
        sequence.OctetString(connectionUid);        
        sequence.OctetString(iepBytes);            
        return MsgBuilder.Create(Constants.Proxy.TcpConnector.CLIENT_P2P, asnEncoder);
	}
	
	private SoftnetMessage EncodeMessage_ClientProxy()
	{
		ASNEncoder asnEncoder = new ASNEncoder();
        SequenceEncoder sequence = asnEncoder.Sequence();
        sequence.OctetString(connectionUid);        
        return MsgBuilder.Create(Constants.Proxy.TcpConnector.CLIENT_PROXY, asnEncoder);
	}
	
	private void onMessageReceived(byte[] message)
	{
		try		
		{
			byte messageTag = message[0];
			if(messageTag == Constants.Proxy.TcpConnector.AUTH_KEY)
			{
				ProcessMessage_AuthKey(message);
			}
			else if(messageTag == Constants.Proxy.TcpConnector.CREATE_P2P_CONNECTION)
			{
				ProcessMessage_CreateP2PConnection(message);
			}
			else if(messageTag == Constants.Proxy.TcpConnector.CREATE_P2P_CONNECTION_IN_DUAL_MODE)
			{
				ProcessMessage_CreateP2PConnectionInDualMode(message);
			}
			else if(messageTag == Constants.Proxy.TcpConnector.CREATE_PROXY_CONNECTION)
			{
				ProcessMessage_CreateProxyConnection(message);
			}
			else if(messageTag == Constants.Proxy.TcpConnector.ERROR)
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

	private void closeServerChannel(ServerSocketChannel channel)
	{
		try	{
			channel.close();
		}
		catch(IOException e) {}
	}

	private void closeChannel(SocketChannel channel)
	{
		try	{
			channel.close();
		}
		catch(IOException e) {}
	}
	
	private void closeWithNullCheck(SocketChannel channel)
	{
		if(channel != null) {
			try	{
				channel.close();
			}
			catch(IOException e) {}
		}
	}

	class ClientAuthenticationThread extends Thread
	{
		private SocketChannel acceptedChannel;
		
		public ClientAuthenticationThread(SocketChannel acceptedChannel){
			this.acceptedChannel = acceptedChannel;
		}
		
		@Override
		public void run() {
			authenticateClient(acceptedChannel);	
		}		
	}
}
