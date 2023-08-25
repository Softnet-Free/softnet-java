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

import java.util.EventListener;

public interface ClientEventListener extends EventListener
{
	void onConnectivityChanged(ClientEndpointEvent e);
	void onStatusChanged(ClientEndpointEvent e);
	void onUserUpdated(ClientEndpointEvent e);
	void onServiceOnline(RemoteServiceEvent e);
	void onServiceOffline(RemoteServiceEvent e);
	void onServiceIncluded(RemoteServiceEvent e);
	void onServiceRemoved(RemoteServiceEvent e);
	void onServiceUpdated(RemoteServiceEvent e);
	void onServicesUpdated(ClientEndpointEvent e);
	void onPersistenceFailed(ClientPersistenceFailedEvent e);
}