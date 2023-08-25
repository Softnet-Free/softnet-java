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

import java.util.UUID;
import softnet.asn.*;

public class ReplacingEvent
{
	public final UUID uid;
	public final String name;
	public final SequenceEncoder arguments;		
	public final boolean isNull;

	private ASNEncoder asnEncoder;	
	public byte[] getEncoding()
	{
		if(arguments != null && arguments.count() > 0)
			return asnEncoder.getEncoding(); 
		return null;
	}

	public ReplacingEvent(String name)
	{
		this.name = name;
		uid = UUID.randomUUID();
		asnEncoder = new ASNEncoder();
		arguments = asnEncoder.Sequence();
		isNull = false;
	}

	protected ReplacingEvent(String name, boolean isNull)
	{
		this.name = name;
		uid = UUID.randomUUID();
		if(isNull) {
			arguments = null;
			this.isNull = true;
		}
		else {
			asnEncoder = new ASNEncoder();
			arguments = asnEncoder.Sequence();
			this.isNull = false;
		}
	}
}
