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

import java.util.GregorianCalendar;
import softnet.asn.*;

public class RemoteEvent
{
	public final long instanceId;
	public final String name;
	public final EventCategory category;
	public final boolean isNull;
	public final long serviceId;
	public final long age;
	public final GregorianCalendar createdDate;
	public final SequenceDecoder arguments;
	
	protected RemoteEvent(EventIData eventIData) throws AsnException
	{
		if(eventIData.category == EventCategory.Replacing)
		{
			this.category = EventCategory.Replacing;
			this.instanceId = eventIData.instanceId;
			this.name = eventIData.name;
			this.serviceId = eventIData.serviceId;
			this.age = eventIData.age;
			this.createdDate = eventIData.createdDate;
			if(eventIData.isNull)
			{
				this.isNull = true;
				this.arguments = null;
			}
			else
			{
				this.isNull = false;
				if(eventIData.argumentsEncoding != null)
					this.arguments = ASNDecoder.Sequence(eventIData.argumentsEncoding);
				else
					this.arguments = null;
			}
		}
		else if(eventIData.category == EventCategory.Queuing)
		{
			this.category = EventCategory.Queuing;
			this.instanceId = eventIData.instanceId;
			this.name = eventIData.name;
			this.serviceId = eventIData.serviceId;
			this.age = eventIData.age;
			this.createdDate = eventIData.createdDate;
			if(eventIData.argumentsEncoding != null)
				this.arguments = ASNDecoder.Sequence(eventIData.argumentsEncoding);
			else
				this.arguments = null;
			this.isNull = false;
		}		
		else			
		{
			this.category = EventCategory.Private;
			this.instanceId = eventIData.instanceId;
			this.name = eventIData.name;
			this.serviceId = eventIData.serviceId;
			this.age = eventIData.age;
			this.createdDate = eventIData.createdDate;
			if(eventIData.argumentsEncoding != null)
				this.arguments = ASNDecoder.Sequence(eventIData.argumentsEncoding);
			else
				this.arguments = null;
			this.isNull = false;
		}
	}
}
