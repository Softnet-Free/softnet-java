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

import java.util.ArrayList;

public class ClientMemoryPersistence implements ClientPersistence
{
	public ClientMemoryPersistence()
	{
		records = new ArrayList<Record>();
	}
	
	public void reset()
	{
		synchronized(mutex)
		{
			records.clear();
		}	
	}
		
	public void close()
	{
		synchronized(mutex)
		{
			isClosed = true;
			records.clear();
		}		
	}
	
	private Object mutex = new Object();
	private ArrayList<Record> records;
	private boolean isClosed = false;
	
	public ClientEventPersistable getItem(String name)
	{
		synchronized(mutex)
		{
			if(isClosed)
				throw new IllegalStateException("The storage has been closed.");
			
			for(Record record: records)
			{
				if(record.name.equals(name))
					return new ClientEventPersistable(record.name, record.instanceId);
			}
			return null;
		}
	}

	public void putItem(String name, long instanceId)
	{
		synchronized(mutex)
		{
			if(isClosed)
				throw new IllegalStateException("The storage has been closed.");
			
			Record record = null;
			for(Record rec: records)
			{
				if(rec.name.equals(name))
				{
					record = rec;
					break;
				}
			}
					
			if(record != null)
			{
				record.instanceId = instanceId;
			}
			else
			{
				record = new Record();
				record.name = name;
				record.instanceId = instanceId;
				records.add(record);
			}
		}
	}
	
	private class Record
	{
		public String name;
		public long instanceId;
	}
}
