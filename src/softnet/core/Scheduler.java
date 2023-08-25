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

package softnet.core;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Scheduler
{
	private ThreadPool threadPool;
	private ScheduledExecutorService scheduledThreadPool;	
	private boolean isShutdown = false;
	
	public Scheduler(ThreadPool threadPool)
	{
		this.threadPool = threadPool;
	}
	
	public void init()
	{
		scheduledThreadPool = Executors.newScheduledThreadPool(1);
	}

	public void add(ScheduledTask task, long delaySeconds)
	{
		try
		{
			scheduledThreadPool.schedule(new STaskWrapper(task, threadPool), delaySeconds, TimeUnit.SECONDS);
		}
		catch(java.util.concurrent.RejectedExecutionException e)
		{
			if(isShutdown == true)
				return;
			throw e;
		}		
	}
	
	public void shutdown()
	{
		isShutdown = true;
		scheduledThreadPool.shutdownNow();
	}
}

class STaskWrapper implements Runnable
{
	private ThreadPool threadPool;
	private ScheduledTask task;
	
	public STaskWrapper(ScheduledTask task, ThreadPool threadPool)
	{
		this.task = task;
		this.threadPool = threadPool;
	}
	
	public void run()
	{
		if(task.complete())
		{			
			threadPool.execute(new Runnable()
			{
				public void run()
				{
					task.acceptor.accept(task.state);
				}
			});
		}
	}
}
