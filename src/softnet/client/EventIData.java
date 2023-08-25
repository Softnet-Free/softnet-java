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

class EventIData
{
	public long eventId;
	public byte[] transactionUid;
	public String name;
	public EventCategory category;
	public boolean isNull;
	public long instanceId;
	public long serviceId;
	public long age;
	public GregorianCalendar createdDate;
	public byte[] argumentsEncoding = null;
}
