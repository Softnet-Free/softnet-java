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

public class InvalidServerEndpointSoftnetException extends SoftnetException
{
	private static final long serialVersionUID = 754574313424304194L;

	public InvalidServerEndpointSoftnetException(String message)
	{
		super(SoftnetError.InvalidServerEndpoint, message);
	}

	public InvalidServerEndpointSoftnetException(java.net.InetAddress peerAddress)
	{
		super(SoftnetError.InvalidServerEndpoint, String.format("The server endpoint '%s' is not valid.", peerAddress.toString()));
	}

	public InvalidServerEndpointSoftnetException(java.net.InetAddress peerAddress, int port)
	{
		super(SoftnetError.InvalidServerEndpoint, String.format("The server endpoint '%s:%d' is not valid.", peerAddress.toString(), port));
	}
}
