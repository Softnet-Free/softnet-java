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

import java.util.regex.Pattern;

import softnet.asn.*;

public class RemoteProcedure
{	
	public final String name;
	public final SequenceEncoder arguments;

	public RemoteProcedure(String name)
	{
		validateProcedureName(name);
		this.name = name;
		asnEncoder = new ASNEncoder();
		arguments = asnEncoder.Sequence();
	}
	
	private ASNEncoder asnEncoder;
	protected byte[] getEncoding()
	{
		return asnEncoder.getEncoding();
	}
	
	private void validateProcedureName(String name)
	{
		if(name == null || name.length() == 0)
			throw new IllegalArgumentException("A procedure name is not allowed to be null or empty.");

		if(name.length() > 256)
	        throw new IllegalArgumentException(String.format("The length of the procedure name '%s' is greater then 256 characters.", name));

		if (Pattern.matches("^[\\u0020-\\u007F]+$", name) == false)			
	        throw new IllegalArgumentException(String.format("The procedure name '%s' contains illegal characters. Allowed characters: a-z A-Z 0-9 _.", name));
	
		if (Pattern.matches("^[a-zA-Z].*$", name) == false)
	        throw new IllegalArgumentException(String.format("The leading character in the name of procedure '%s' is illegal. Allowed characters: a-z A-Z.", name));		
	
		if (Pattern.matches("^[a-zA-Z0-9_]+$", name) == false)
	        throw new IllegalArgumentException(String.format("An illegal character in the name of procedure '%s'. Allowed characters: a-z A-Z 0-9 _.", name));			
	}
}

// the rest of the members are omitted 