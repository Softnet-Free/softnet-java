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

package softnet.exceptions;

import java.util.UUID;

public class ServiceNotRegisteredSoftnetException extends CriticalSoftnetException 
{
	private static final long serialVersionUID = 6556483597545987332L;

	public ServiceNotRegisteredSoftnetException(String message)
	{
		super(SoftnetError.ServiceNotRegistered, message);
	}
	
	public ServiceNotRegisteredSoftnetException(UUID serviceUid)
	{
		super(SoftnetError.ServiceNotRegistered, String.format("The service uid '%s' has not been found in the softnet registry.", serviceUid.toString()));
	}
}