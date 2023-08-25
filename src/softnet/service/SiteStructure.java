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

public interface SiteStructure
{
	void setGuestSupport();
	void setStatelessGuestSupport();
	
	void setRoles(String roles);
	void setOwnerRole(String role);
	
	void addReplacingEvent(String eventName);
	void addReplacingEvent(String eventName, GuestAccess guestAccess);
	void addReplacingEvent(String eventName, String roles);
	
	void addQueueingEvent(String eventName, int lifeTime, int maxQueueSize);
	void addQueueingEvent(String eventName, int lifeTime, int maxQueueSize, GuestAccess guestAccess);
	void addQueueingEvent(String eventName, int lifeTime, int maxQueueSize, String roles);	

	void addPrivateEvent(String eventName, int lifeTime);
}