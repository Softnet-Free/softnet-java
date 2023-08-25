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

import softnet.asn.*;
import softnet.core.*;
import softnet.exceptions.ErrorCodes;
import softnet.exceptions.ServiceNotRegisteredSoftnetException;
import softnet.exceptions.SoftnetException;
import softnet.service.ServiceURI;
import softnet.utils.IPUtility;
import java.nio.ByteBuffer;

public class QueryOnServiceUid extends QueryBuilder
{
	private ServiceURI serviceURI;
	
	public QueryOnServiceUid(ServiceURI serviceURI)
	{
		this.serviceURI = serviceURI;
	}
	
	public ByteBuffer GetQuery()
	{
		ASNEncoder asnEncoder = new ASNEncoder();
        SequenceEncoder sequence = asnEncoder.Sequence();
        
        if(IPUtility.isLiteralIP(serviceURI.server) == false)
        	sequence.UTF8String(1, serviceURI.server.toLowerCase());
    	sequence.OctetString(serviceURI.serviceUid);
    	
    	byte[] buffer = asnEncoder.getEncoding(2);
    	buffer[0] = Constants.ProtocolVersion;
    	buffer[1] = Constants.Balancer.SERVICE_UID;
    	
    	return ByteBuffer.wrap(buffer);
	}
	
	public void ThrowException(int errorCode) throws SoftnetException
	{
		if(errorCode == ErrorCodes.SERVICE_NOT_REGISTERED)
			throw new ServiceNotRegisteredSoftnetException(serviceURI.serviceUid);		
	}
}