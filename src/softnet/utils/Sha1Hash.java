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

package softnet.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import softnet.exceptions.HostErrorSoftnetException;

public class Sha1Hash
{
	public static byte[] compute(byte[] buffer) throws HostErrorSoftnetException
	{
		try
		{
			MessageDigest msgDigest = MessageDigest.getInstance("SHA-1");			
			return msgDigest.digest(buffer);
		}
		catch (NoSuchAlgorithmException ex) 
		{
			throw new HostErrorSoftnetException(ex.getMessage());
		}		
	}
}
