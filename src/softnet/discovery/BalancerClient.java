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

import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.IOException;
import java.net.InetAddress;

import softnet.core.*;
import softnet.exceptions.*;

public class BalancerClient implements STaskContext
{
	protected final String serverAddress;
	private QueryBuilder queryBuilder;
	private ThreadPool threadPool;
	private Scheduler scheduler;
	private ResponseHandler<InetAddress, SoftnetException> responseHandler;
	private Acceptor<SoftnetException> errorNotificationAcceptor;
	private boolean is_closed = false;
	
	public BalancerClient(String serverAddress, QueryBuilder queryBuilder, ThreadPool threadPool, Scheduler scheduler)
	{
		this.serverAddress = serverAddress;
		this.queryBuilder = queryBuilder;
		this.threadPool = threadPool;
		this.scheduler = scheduler;
	}
		
	public void request(ResponseHandler<InetAddress, SoftnetException> responseHandler, Acceptor<SoftnetException> notificationAcceptor)
	{
		this.responseHandler = responseHandler;
		this.errorNotificationAcceptor = notificationAcceptor;
		connect();
	}
	
	public boolean isClosed()
	{
		return is_closed;
	}

	public void close()
	{
		synchronized(mutex)
		{
			is_closed = true;

			if(m_bConnector != null)
				m_bConnector.close();
			
			if(m_queryExecutor != null)
				m_queryExecutor.close();
		}
	}

	private Object mutex = new Object();	
	private BConnector m_bConnector = null;
	private QueryExecutor m_queryExecutor = null;
	
	private void connect()
	{
		BConnector bConnector = null;			
		synchronized(mutex)
		{
			if (is_closed)
	             return;						
			bConnector = new BConnector(serverAddress, threadPool, scheduler);
			m_bConnector = bConnector;
		}
		
		bConnector.connect(
			new Acceptor<SocketChannel>()
			{
				AtomicInteger m_mutex = new AtomicInteger(0);
				@Override
				public void accept(SocketChannel channel)
				{
					if(m_mutex.compareAndSet(0, 1))
					{
						BConnector_onSuccess(channel);
					}
					else
					{
						closeChannel(channel);
					}
				}
			},
			new Acceptor<SoftnetException>()
			{
				@Override
				public void accept(SoftnetException ex)
				{
					BConnector_onErrorNotification(ex);
				}
			});
	}
	
	private void BConnector_onSuccess(SocketChannel socketChannel)
	{
		try
		{
			QueryExecutor queryExecutor = new QueryExecutor(socketChannel, serverAddress, queryBuilder, scheduler);		
			synchronized(mutex)
			{
				if (is_closed)
		            return;
				m_queryExecutor = queryExecutor;
			}			
			InetAddress trackerIP = queryExecutor.exec();
			responseHandler.onSuccess(trackerIP);
		}
		catch(CriticalSoftnetException ex)
		{
			responseHandler.onError(ex);
		}
		catch(SoftnetException ex)
		{
			QueryExecutor_onError(ex);
		}
		finally
		{
			closeChannel(socketChannel);
		}
	}

	private void BConnector_onErrorNotification(SoftnetException ex)
	{
		errorNotificationAcceptor.accept(ex);
	}
				
	protected void QueryExecutor_onError(SoftnetException ex)
    {
		errorNotificationAcceptor.accept(ex);
		
		Acceptor<Object> acceptor = new Acceptor<Object>()
		{
			public void accept(Object noData) { connect(); }
		};
		scheduler.add(new ScheduledContextTask(acceptor, this), 60);
    }
	
	private void closeChannel(SocketChannel socketChannel)
	{
		try { socketChannel.close(); } catch(IOException ex) { }
	}
}

















