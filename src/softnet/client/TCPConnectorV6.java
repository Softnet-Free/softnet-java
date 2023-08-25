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
import softnet.utils.ByteConverter;

public class TCPConnectorV6 implements TCPConnector
{
	private byte[] connectionUid;
	private InetAddress serverIp;
	private TCPOptions tcpOptions;
	private Scheduler scheduler;
	private TCPResponseHandler responseHandler;
	private BiAcceptor<byte[], Object> authenticationHandler;
	private Object attachment;
	
	private Object mutex = new Object();
	private boolean isDisposed = false;
	
	private MsgSocket msgSocket = null;
	private InetSocketAddress localIEP = null;
	private InetSocketAddress remoteIEP = null;
	private byte[] secretKey = null;
	private byte[] remoteSecretKey = null;
	private ScheduledTask p2pConnectionAttemptTimeoutControlTask = null;

	private SocketChannel m_controlChannel = null;
	private ServerSocketChannel m_listenerChannel = null;
	private SocketChannel m_p2pChannel = null;	
	private SocketChannel m_proxyChannel = null;
	private ArrayList<SocketChannel> acceptedChannels;
	
	private enum ConnectorState
    {
        P2P_MODE, P2P_HANDSHAKE, PROXY_MODE, PROXY_HANDSHAKE, COMPLETED
    }
	private ConnectorState connectorState = ConnectorState.P2P_MODE;
	
	public TCPConnectorV6( byte[] connectionUid, InetAddress serverIp, TCPOptions tcpOptions, Scheduler scheduler)
	{
		this.connectionUid = connectionUid;
		this.serverIp = serverIp;
		this.tcpOptions = tcpOptions;
		this.scheduler = scheduler;
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
		dispose();
	}
	
	private void dispose()
	{
		synchronized(mutex)
		{
			if(isDisposed)
				return;
			
			isDisposed = true;
			connectorState = ConnectorState.COMPLETED;			

			if(m_controlChannel != null)
				closeChannel(m_controlChannel);
			
			if(m_listenerChannel != null)
				closeServerChannel(m_listenerChannel);
			
			if(m_p2pChannel != null)
				closeChannel(m_p2pChannel);
			
			if(acceptedChannels.size() > 0)
			{
				for(SocketChannel acceptedChannel: acceptedChannels)
					closeChannel(acceptedChannel);
			}
			
			if(m_proxyChannel != null)
				closeChannel(m_proxyChannel);
		}
	}
	
	private void completeOnError()
	{		
		synchronized(mutex)
		{
			if (connectorState == ConnectorState.COMPLETED)
                return;
			connectorState = ConnectorState.COMPLETED;
		}
		
		dispose();
		responseHandler.onError(new ResponseContext(null, null, attachment), new ConnectionAttemptFailedSoftnetException());
	}
	
	private void execute()
	{
		SocketChannel controlChannel = null;
		try
		{
			synchronized(mutex)
			{
				if(isDisposed == false)
				{
					m_controlChannel = SocketChannel.open();
					controlChannel = m_controlChannel;
				}
				else return;
			}
			
			if (tcpOptions != null)
			{
				if(8196 <= tcpOptions.receiveBufferSize && tcpOptions.receiveBufferSize <= 1073741824)
				{
					controlChannel.setOption(StandardSocketOptions.SO_RCVBUF, tcpOptions.receiveBufferSize);
				}
			}
						
			controlChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);			
			controlChannel.configureBlocking(true);
			controlChannel.connect(new InetSocketAddress(serverIp, Constants.ServerPorts.TcpRzvPort));	
						
			localIEP = (InetSocketAddress)controlChannel.getLocalAddress();
			
			msgSocket = new MsgSocket(controlChannel);
			msgSocket.messageReceivedHandler	= new Acceptor<byte[]>()
			{
				public void accept(byte[] message) { onMessageReceived(message); }
			};
			msgSocket.networkErrorHandler = new Acceptor<NetworkErrorSoftnetException>()
			{
				public void accept(NetworkErrorSoftnetException ex) { onNetworkError(ex); }
			};
			msgSocket.formatErrorHandler = new Runnable()
			{
				public void run() { onFormatError(); }
			};					
			msgSocket.minLength = 1;
			msgSocket.maxLength = 256;
			msgSocket.start();			

			try
			{
				SocketChannel p2pChannel = null;
				synchronized(mutex)
				{
					if(isDisposed == false)
					{
						m_p2pChannel = SocketChannel.open();
						p2pChannel = m_p2pChannel;						
					}
					else return;
				}
	
				if (tcpOptions != null)
				{
					if(8196 <= tcpOptions.receiveBufferSize && tcpOptions.receiveBufferSize <= 1073741824)
					{
						p2pChannel.setOption(StandardSocketOptions.SO_RCVBUF, tcpOptions.receiveBufferSize);
					}

					if(8196 <= tcpOptions.sendBufferSize && tcpOptions.sendBufferSize <= 1073741824)
					{
						p2pChannel.setOption(StandardSocketOptions.SO_SNDBUF, tcpOptions.sendBufferSize);
					}
				}
				
				p2pChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
				p2pChannel.configureBlocking(true);
				p2pChannel.bind(localIEP);
				
				try
				{									
					ServerSocketChannel listenerChannel = null;
					synchronized(mutex)
					{
						if(isDisposed == false)
						{						
							m_listenerChannel = ServerSocketChannel.open();
							listenerChannel = m_listenerChannel;
						}
						else return;
					}
					
					if (tcpOptions != null)
					{
						if(8196 <= tcpOptions.receiveBufferSize && tcpOptions.receiveBufferSize <= 1073741824)
						{
							listenerChannel.setOption(StandardSocketOptions.SO_RCVBUF, tcpOptions.receiveBufferSize);
						}
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
								
				msgSocket.send(EncodeMessage_ClientP2P());
			}
			catch(IOException | UnsupportedOperationException ex)
			{	
				onP2PSetup1Failed();
			}
		}
		catch(UnsupportedOperationException ex)
		{
			if(controlChannel != null)
				closeChannel(controlChannel);
			onP2PSetup2Failed();
		}
		catch(IOException ex)
		{			
			if(controlChannel != null)
				closeChannel(controlChannel);
            responseHandler.onError(new ResponseContext(null, null, attachment), new ConnectionAttemptFailedSoftnetException());
		}
	}
	
	private void onP2PSetup1Failed()
	{
		synchronized(mutex)
		{
			if(connectorState != ConnectorState.P2P_MODE)
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
				if(connectorState != ConnectorState.P2P_MODE)
					return;				
				connectorState = ConnectorState.PROXY_MODE;
				
				m_controlChannel = SocketChannel.open();
				controlChannel = m_controlChannel;
			}
			
			if (tcpOptions != null)
			{
				if(8196 <= tcpOptions.receiveBufferSize && tcpOptions.receiveBufferSize <= 1073741824)
				{
					controlChannel.setOption(StandardSocketOptions.SO_RCVBUF, tcpOptions.receiveBufferSize);
				}
			}
						
			controlChannel.configureBlocking(true);
			controlChannel.connect(new InetSocketAddress(serverIp, Constants.ServerPorts.TcpRzvPort));	
			
			msgSocket = new MsgSocket(controlChannel);
			msgSocket.messageReceivedHandler	= new Acceptor<byte[]>()
			{
				public void accept(byte[] message) { onMessageReceived(message); }
			};
			msgSocket.networkErrorHandler = new Acceptor<NetworkErrorSoftnetException>()
			{
				public void accept(NetworkErrorSoftnetException ex) { onNetworkError(ex); }
			};
			msgSocket.formatErrorHandler = new Runnable()
			{
				public void run() { onFormatError(); }
			};					
			msgSocket.minLength = 1;
			msgSocket.maxLength = 256;
			msgSocket.start();
			
			msgSocket.send(EncodeMessage_ClientProxy());
		}
		catch(IOException ex)
		{			
			if(controlChannel != null)
				closeChannel(controlChannel);
            responseHandler.onError(new ResponseContext(null, null, attachment), new ConnectionAttemptFailedSoftnetException());
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
			catch(ClosedChannelException e)
			{
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
		}
		catch(IOException ex)
		{
			return;
		}

		synchronized(mutex)
        {
            if (connectorState != ConnectorState.P2P_HANDSHAKE)
                return;
            connectorState = ConnectorState.COMPLETED;                
            p2pConnectionAttemptTimeoutControlTask.cancel();
            acceptedChannels.remove(acceptedChannel);
        }

		dispose();
        responseHandler.onSuccess(new ResponseContext(null, null, attachment), acceptedChannel, ConnectionMode.P2P);
	}
	
	private void tryP2PConnection()
	{
		SocketChannel p2pChannel = null;
		try
		{
			synchronized(mutex)
			{
				if(connectorState != ConnectorState.P2P_HANDSHAKE)
					return;
				p2pChannel = m_p2pChannel;				
			}
			
			p2pChannel.connect(remoteIEP);
			
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
		}
		catch(IOException e)
		{
			return;
		}

		synchronized(mutex)
        {
            if (connectorState != ConnectorState.P2P_HANDSHAKE)
                return;
            connectorState = ConnectorState.COMPLETED;
            p2pConnectionAttemptTimeoutControlTask.cancel();
            m_p2pChannel = null;
        }

		dispose();
        responseHandler.onSuccess(new ResponseContext(null, null, attachment), p2pChannel, ConnectionMode.P2P);
	}
	
	private void onP2PConnectionAttemptFailed()
	{
		synchronized(mutex)
        {
            if (connectorState != ConnectorState.P2P_HANDSHAKE)
                return;
            connectorState = ConnectorState.PROXY_MODE;

            if(m_listenerChannel != null)
			{
				closeServerChannel(m_listenerChannel);
				m_listenerChannel = null;
			}
			
			if(m_p2pChannel != null)
			{
				closeChannel(m_p2pChannel);
				m_p2pChannel = null;
			}
			
            if(acceptedChannels.size() > 0)
            {
				for(SocketChannel acceptedChannel: acceptedChannels)
					closeChannel(acceptedChannel);
				acceptedChannels.clear();
            }
        }
		
		msgSocket.send(MsgBuilder.Create(Constants.Proxy.TcpConnector.P2P_FAILED));
	}
	
	private void tryProxyConnection(int serverPort)
	{
		SocketChannel proxyChannel = null;
		try
		{
			synchronized(mutex)
			{
				if(connectorState == ConnectorState.COMPLETED || connectorState == ConnectorState.PROXY_HANDSHAKE)
					return;
				connectorState = ConnectorState.PROXY_HANDSHAKE;				
				
				m_proxyChannel = SocketChannel.open();
				proxyChannel = m_proxyChannel;			
			}

			if (tcpOptions != null)
			{
				if(8196 <= tcpOptions.receiveBufferSize && tcpOptions.receiveBufferSize <= 1073741824)
				{
					proxyChannel.setOption(StandardSocketOptions.SO_RCVBUF, tcpOptions.receiveBufferSize);
				}
				
				if(8196 <= tcpOptions.sendBufferSize && tcpOptions.sendBufferSize <= 1073741824)
				{
					proxyChannel.setOption(StandardSocketOptions.SO_SNDBUF, tcpOptions.sendBufferSize);
				}
			}
			
			proxyChannel.configureBlocking(true);
			proxyChannel.connect(new InetSocketAddress(serverIp, serverPort));
			
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
				if(bytesRead == -1)
					throw new IOException();
			}
		}
		catch(Exception ex)
		{
			completeOnError();
			return;
		}
		
		synchronized(mutex)
        {
            if (connectorState != ConnectorState.PROXY_HANDSHAKE)
                return;
            connectorState = ConnectorState.COMPLETED;                
            m_proxyChannel = null;
        }

		dispose();
        responseHandler.onSuccess(new ResponseContext(null, null, attachment), proxyChannel, ConnectionMode.Proxy);
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
		byte[] iepBytes = sequence.OctetString(18);
		secretKey = sequence.OctetString(4);
		remoteSecretKey = sequence.OctetString(4);
		sequence.end();
		
		byte[] ipBytes = new byte[16];
		System.arraycopy(iepBytes, 0, ipBytes, 0, 16);		
		InetAddress ip = InetAddress.getByAddress(ipBytes);
		int port = ByteConverter.toInt32FromUInt16(iepBytes, 16);
		remoteIEP = new InetSocketAddress(ip, port);

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
			
			Acceptor<Object> acceptor = new Acceptor<Object>()
			{
				public void accept(Object noData) { onP2PConnectionAttemptFailed(); }
			};
			p2pConnectionAttemptTimeoutControlTask = new ScheduledTask(acceptor, this);
			scheduler.add(p2pConnectionAttemptTimeoutControlTask, Constants.TcpP2PConnectionAttemptTimeoutSeconds);
		}
		
		Thread thread = new Thread()
		{
		    public void run(){
		    	tryP2PConnection();		    	
		    }
		};
		thread.start();
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
	
	private void ProcessMessage_Error(byte[] message) throws AsnException
	{
		completeOnError();
	}
	
	private SoftnetMessage EncodeMessage_ClientP2P()
	{
		ASNEncoder asnEncoder = new ASNEncoder();
        SequenceEncoder sequence = asnEncoder.Sequence();
        sequence.OctetString(connectionUid);        
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
			else if(messageTag == Constants.Proxy.TcpConnector.CREATE_PROXY_CONNECTION)
			{
				ProcessMessage_CreateProxyConnection(message);
			}
			else if(messageTag == Constants.Proxy.TcpConnector.ERROR)
			{
				ProcessMessage_Error(message);
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
	
	private void onNetworkError(NetworkErrorSoftnetException ex)
	{
		completeOnError();
	}

	private void onFormatError()
	{
		completeOnError();
	}	
	
	private void closeChannel(SocketChannel channel)
	{
		try
		{
			channel.close();
		}
		catch(IOException e) {}
	}

	private void closeServerChannel(ServerSocketChannel channel)
	{
		try
		{
			channel.close();
		}
		catch(IOException e) {}
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
