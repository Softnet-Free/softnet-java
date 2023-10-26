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

public class PrivateEvent {
	public final String name;
	public final long clientId;
	public final SequenceEncoder arguments;

	public PrivateEvent(String name, long clientId) {
		this.name = name;
		this.clientId = clientId;
		uid = UUID.randomUUID();
		asnEncoder = new ASNEncoder();
		arguments = asnEncoder.Sequence();
	}	
	
	public final UUID uid;

	private ASNEncoder asnEncoder;
	public byte[] getEncoding() {
		if(arguments.count() > 0)
			return asnEncoder.getEncoding();
		return null;		
	}	
}
