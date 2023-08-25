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

public class InputDataFormatSoftnetException extends SoftnetException 
{
	private static final long serialVersionUID = 3799687744268163541L;

	public InputDataFormatSoftnetException()
	{
		super(SoftnetError.InputDataFormatError, "Data received from the softnet server has an invalid format.");
	}

	public InputDataFormatSoftnetException(String message)
	{
		super(SoftnetError.InputDataFormatError, message);
	}
	
	public InputDataFormatSoftnetException(java.net.InetAddress serverAddress)
	{
		super(SoftnetError.InputDataFormatError, String.format("Data received from the softnet server '%s' has an invalid format.", serverAddress.toString()));
	}
	
	public InputDataFormatSoftnetException(java.net.InetAddress serverAddress, int port)
	{
		super(SoftnetError.InputDataFormatError, String.format("Data received from the softnet server '%s:%d' has an invalid format.", serverAddress.toString(), port));
	}
}