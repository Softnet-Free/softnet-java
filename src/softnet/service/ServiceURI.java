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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;
import java.util.UUID;

public class ServiceURI {
	public final String value;
	public final String scheme;
	public final UUID serviceUid;
	public final String server;
	
	public ServiceURI(String value)
	{
		try
		{
			this.value = value;
			
			URI uri = new URI(value);			
			String providedUriScheme = uri.getScheme();
			String providedServerAddress = uri.getHost();			
			String providedServiceUId = uri.getUserInfo();
			
			if(providedUriScheme == null)
				throw new IllegalArgumentException("Invalid service URI. The URI scheme is not provided.");
			
			if(providedServerAddress == null)
				throw new IllegalArgumentException("Invalid service URI. The server address is not provided.");

			if(providedServiceUId == null)
				throw new IllegalArgumentException("Invalid service URI. The service unique id is not provided.");
			
			if(providedUriScheme.equals("softnet-srv") == false)
				throw new IllegalArgumentException(String.format("Invalid service URI. The URI scheme '%s' is invalid", providedUriScheme));
						
			if (Pattern.matches("^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$", providedServiceUId) == false)
	            throw new IllegalArgumentException(String.format("Invalid service URI. The format of the service's unique id '%s' is illegal.", providedServiceUId));		
						
			if (Pattern.matches("^[a-zA-Z0-9_.\\-]+$", providedServerAddress) == false)
				throw new IllegalArgumentException(String.format("Invalid service URI. The server address '%s' contains illegal characters.", providedServerAddress));
			
			this.scheme = providedUriScheme;
			this.serviceUid = UUID.fromString(providedServiceUId);
			this.server = providedServerAddress;
		}
		catch(URISyntaxException e)
		{
			throw new IllegalArgumentException(e.getMessage());
		}
	}
}
