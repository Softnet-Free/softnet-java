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

package softnet;

import softnet.exceptions.*;

public class EndpointConnectivity
{
	public final ConnectivityStatus status;
    public final SoftnetError error;
    public final String message;
    
    public EndpointConnectivity(ConnectivityStatus status)
    {
    	this.status = status;
        this.error = SoftnetError.NoError;
        this.message = null;
    }
    
    public EndpointConnectivity(ConnectivityStatus status, SoftnetError error, String message)
    {
    	this.status = status;
    	this.error = error;
    	this.message = message;
    }
    
    public EndpointConnectivity(ConnectivityStatus status, SoftnetException exception)
    {
    	this.status = status;
    	this.error = exception.Error;
    	this.message = exception.getMessage();
    }
}
