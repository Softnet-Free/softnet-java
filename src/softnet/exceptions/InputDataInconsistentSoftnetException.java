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

public class InputDataInconsistentSoftnetException extends SoftnetException 
{
	private static final long serialVersionUID = 7923360475610844911L;

	public InputDataInconsistentSoftnetException()
	{
		super(SoftnetError.InputDataInconsistent, "The data received from the softnet server is inconsistent.");
	}

	public InputDataInconsistentSoftnetException(String message)
	{
		super(SoftnetError.InputDataInconsistent, message);
	}
	
	public InputDataInconsistentSoftnetException(java.net.InetAddress serverAddress)
	{
		super(SoftnetError.InputDataInconsistent, String.format("The data received from the softnet server '%s' is inconsistent.", serverAddress.toString()));
	}	
}
