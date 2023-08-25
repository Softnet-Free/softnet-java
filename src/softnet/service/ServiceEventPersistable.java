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

import java.util.UUID;

public class ServiceEventPersistable
{
	public final UUID instanceUid;
	public final String name;
	public final int kind;
	public final boolean isNull;
	public final long clientId;
	public final byte[] argumentsEncoding;
	
	private ServiceEventPersistable(String name, int kind, UUID instanceUid, boolean isNull, long clientId, byte[] argumentsEncoding)
	{
		this.instanceUid = instanceUid;
		this.name = name;
		this.kind = kind;
		this.isNull = isNull;
		this.clientId = clientId;
		this.argumentsEncoding = argumentsEncoding;
	}
	
	public static ServiceEventPersistable createReplacingEvent(String name, UUID instanceUid, byte[] argumentsEncoding)
	{
		return new ServiceEventPersistable(name, 1, instanceUid, false, 0, argumentsEncoding);
	}
	
	public static ServiceEventPersistable createReplacingNullEvent(String name, UUID instanceUid)
	{
		return new ServiceEventPersistable(name, 1, instanceUid, true, 0, null);
	}

	public static ServiceEventPersistable createQueueingEvent(String name, UUID instanceUid, byte[] argumentsEncoding)
	{
		return new ServiceEventPersistable(name, 2, instanceUid, false, 0, argumentsEncoding);
	}

	public static ServiceEventPersistable createPrivateEvent(String name, UUID instanceUid, long clientId, byte[] argumentsEncoding)
	{
		return new ServiceEventPersistable(name, 4, instanceUid, false, clientId, argumentsEncoding);
	}
}
