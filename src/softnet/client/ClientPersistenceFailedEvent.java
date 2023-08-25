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

import softnet.exceptions.PersistenceSoftnetException;

public class ClientPersistenceFailedEvent extends ClientEndpointEvent 
{
	private static final long serialVersionUID = -1602993696370585911L;
	public final PersistenceSoftnetException exception;
	
	public ClientPersistenceFailedEvent(PersistenceSoftnetException ex, ClientEndpoint source) {
		super(source);
		exception = ex; 
	}
}
