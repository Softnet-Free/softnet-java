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

package softnet.discovery;

import softnet.asn.ASNEncoder;
import softnet.asn.SequenceEncoder;
import softnet.client.ClientCategory;
import softnet.client.ClientURI;
import softnet.core.Constants;
import softnet.exceptions.*;
import softnet.utils.IPUtility;
import java.nio.ByteBuffer;

public class QueryOnClientKey extends QueryBuilder
{
	private ClientURI clientURI;
	
	public QueryOnClientKey(ClientURI clientURI)
	{		
		this.clientURI = clientURI;
	}
	
	public ByteBuffer GetQuery()
	{
		ASNEncoder asnEncoder = new ASNEncoder();
        SequenceEncoder sequence = asnEncoder.Sequence();
        
        if(IPUtility.isLiteralIP(clientURI.server) == false)
        	sequence.UTF8String(1, clientURI.server.toLowerCase());
		sequence.PrintableString(clientURI.clientKey);
        
		byte[] buffer = asnEncoder.getEncoding(2);
		buffer[0] = Constants.ProtocolVersion;
    	
		if(clientURI.category == ClientCategory.SingleService)
			buffer[1] = Constants.Balancer.CLIENT_S_KEY;
        else if(clientURI.category == ClientCategory.SingleServiceStateless)
        	buffer[1] = Constants.Balancer.CLIENT_SS_KEY;
        else if(clientURI.category == ClientCategory.MultiService)
        	buffer[1] = Constants.Balancer.CLIENT_M_KEY;
        else // clientURI.category == ClientKind.MultiServiceStateless
        	buffer[1] = Constants.Balancer.CLIENT_MS_KEY;

		return ByteBuffer.wrap(buffer);
	}
	
	public void ThrowException(int errorCode) throws SoftnetException
	{
		if(errorCode == ErrorCodes.CLIENT_NOT_REGISTERED)
			throw new ClientNotRegisteredSoftnetException(clientURI);
		if(errorCode == ErrorCodes.INVALID_CLIENT_CATEGORY)
			throw new InvalidClientCategorySoftnetException(clientURI);
	}	
}
