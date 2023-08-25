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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadPool
{
	private ExecutorService cachedThreadPool;
	private boolean shutdown = false;
	
	public void init()
	{
		cachedThreadPool = Executors.newCachedThreadPool();
	}
	
	public void execute(Runnable runnable)
	{
		try
		{
			cachedThreadPool.execute(runnable);
		}
		catch(java.util.concurrent.RejectedExecutionException e)
		{
			if(shutdown == true)
				return;
			throw e;
		}		
	}
	
	public void shutdown()
	{
		shutdown = true;
		cachedThreadPool.shutdownNow();
	}	
}
