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

public class PortUnreachableSoftnetException extends SoftnetException 
{
	private static final long serialVersionUID = 1419665543633945665L;

	public PortUnreachableSoftnetException(int virtualPort) 
	{
		super(SoftnetError.PortUnreachable, String.format("The virtual port %d of a remote service is unreachable.", virtualPort));
	}

	public PortUnreachableSoftnetException(String message) 
	{
		super(SoftnetError.PortUnreachable, message);
	}
}
