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

import softnet.asn.*;
import softnet.core.*;
import softnet.exceptions.*;

class StateController 
{
	public StateController(EndpointConnector endpointConnector, Object endpoint_mutex)
	{
		this.endpointConnector = endpointConnector;
		this.endpoint_mutex = endpoint_mutex;
	}

	private EndpointConnector endpointConnector;	
	private Object endpoint_mutex;

	public void onChannelConnected(Channel channel)
	{
		channel.registerComponent(Constants.Client.StateController.ModuleId, 
			new MsgAcceptor<Channel>()
			{
				public void accept(byte[] message, Channel _channel) throws AsnException, FormatException, SoftnetException
				{
					onMessageReceived(message, _channel);
				}
			});					
	}
		
	private void ProcessMessage_State(byte[] message) throws AsnException
	{	
		SequenceDecoder asnSequenceDecoder = ASNDecoder.Sequence(message, 2);
		if(asnSequenceDecoder.exists(1))
		{
			int pingPeriod = asnSequenceDecoder.Int32();
			endpointConnector.setRemotePingPeriod(pingPeriod);
		}
		asnSequenceDecoder.end();
	}
	
	private void ProcessMessage_SetPingPeriod(byte[] message) throws AsnException
	{	
		SequenceDecoder asnSequenceDecoder = ASNDecoder.Sequence(message, 2);
		int pingPeriod = asnSequenceDecoder.Int32();					
		asnSequenceDecoder.end();
		
		endpointConnector.setRemotePingPeriod(pingPeriod);
	}
	
	private void onMessageReceived(byte[] message, Channel channel) throws AsnException, FormatException, SoftnetException
	{
		synchronized(endpoint_mutex)
		{
			if(channel.closed())
				return;
			
			byte messageTag = message[1];
			if(messageTag == Constants.Client.StateController.STATE)
			{
				ProcessMessage_State(message);
			}
			else if(messageTag == Constants.Client.StateController.SET_PING_PERIOD)
			{
				ProcessMessage_SetPingPeriod(message);
			}
			else
				throw new FormatException();
		}
	}
}
