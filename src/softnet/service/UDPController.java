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

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.UUID;

import softnet.*;
import softnet.asn.*;
import softnet.core.*;
import softnet.exceptions.*;
import softnet.utils.ByteConverter;

class UDPController 
{
	private Object mutex;	
	private ServiceEndpoint serviceEndpoint;
 	private Membership membership;
 	private ArrayList<UDPBinding> udpBindings;
	
	public UDPController(ServiceEndpoint serviceEndpoint, Membership membership)
	{
		this.serviceEndpoint = serviceEndpoint;
		this.membership = membership;
		mutex = new Object();
		udpBindings = new ArrayList<UDPBinding>();
	}
	
	public void listen(int virtualPort, int backlog)
	{
		if(backlog < 0)
			throw new IllegalArgumentException("The value of backlog must not be negative.");			

		synchronized(mutex)
		{		
			if(findBinding(virtualPort) != null)
				throw new IllegalArgumentException(String.format("The UDP virtual port %d is already in use.", virtualPort));					
			
			UDPBinding newBinding = new UDPBinding(serviceEndpoint, virtualPort, backlog);
			udpBindings.add(newBinding);
		}
	}
	
	public void listen(int virtualPort, int backlog, String roles)
	{
		if(backlog < 0)
			throw new IllegalArgumentException("The value of backlog must not be negative.");			

		if(membership.isRbacSupported() == false)
			throw new IllegalArgumentException("A role-based access control is not supported.");
		
		if(roles == null || roles.length() == 0)
			throw new IllegalArgumentException("The list of roles must not be null or empty");
				
		String[] roleNames = roles.split(";");
		for(int i = 0; i < roleNames.length; i++)
			roleNames[i] = roleNames[i].trim();
		
		for(String role: roleNames)
		{
			if(membership.containsRole(role) == false)
				throw new IllegalArgumentException(String.format("Illegal role '%s'.", role));
		}
		
		synchronized(mutex)
		{		
			if(findBinding(virtualPort) != null)
				throw new IllegalArgumentException(String.format("The UDP virtual port %d is already in use.", virtualPort));
			
			UDPBinding newBinding = new UDPBinding(serviceEndpoint, virtualPort, backlog, roleNames);
			udpBindings.add(newBinding);
		}
	}
	
	public void listen(int virtualPort, int backlog, GuestAccess guestAccess)
	{
		if(backlog < 0)
			throw new IllegalArgumentException("The value of backlog must not be negative.");			

		if(guestAccess == null)
			throw new IllegalArgumentException("'guestAccess' is null.");		

		synchronized(mutex)
		{		
			if(findBinding(virtualPort) != null)
				throw new IllegalArgumentException(String.format("The UDP virtual port %d is already in use.", virtualPort));
			
			UDPBinding newBinding = new UDPBinding(serviceEndpoint, virtualPort, backlog, guestAccess);
			udpBindings.add(newBinding);
		}
	}
	
	public void releasePort(int virtualPort)
	{
		synchronized(mutex)
		{		
			UDPBinding udpBinding = findBinding(virtualPort);
			if(udpBinding != null)
			{
				udpBindings.remove(udpBinding);
				udpBinding.close();
			}
		}
	}
	
	public void accept(int virtualPort, UDPAcceptHandler acceptHandler)
	{
		if(acceptHandler == null)
			throw new IllegalArgumentException("'acceptHandler' is null.");			

		UDPBinding udpBinding = null;
		synchronized(mutex)
		{		
			udpBinding = findBinding(virtualPort);
			if(udpBinding == null)
				throw new IllegalArgumentException(String.format("The specified port %d is not being listened for UDP connections.", virtualPort));			
		}
		udpBinding.accept(acceptHandler);
	}
	
	public void onEndpointConnected(Channel channel)
	{
		channel.registerComponent(Constants.Service.UdpController.ModuleId, 
			new MsgAcceptor<Channel>()
			{
				public void accept(byte[] message, Channel _channel) throws AsnException, FormatException, SoftnetException
				{
					onMessageReceived(message, _channel);
				}
			});		
	}
	
	public void onEndpointDisconnected()
	{
		synchronized(mutex)
		{
			for(UDPBinding udpBinding: udpBindings)
				udpBinding.onEndpointDisconnected();
		}
	}
	
	public void onEndpointClosed()
	{
		synchronized(mutex)
		{
			for(UDPBinding udpBinding: udpBindings)
				udpBinding.onEndpointClosed();
		}
	}
	
	private void processMessage_Request(byte[] message, Channel channel) throws AsnException
	{
		SequenceDecoder asnSequence = ASNDecoder.Sequence(message, 2);
		byte[] requestUid = asnSequence.OctetString(16);
		int virtualPort = asnSequence.Int32();
		int userKind = asnSequence.Int32(1, 4);
		long userId = asnSequence.Int64();
		long clientId = asnSequence.Int64();
		byte[] sessionTag = null;
		if(asnSequence.exists(1))
			sessionTag = asnSequence.OctetString();
		asnSequence.end();
		
		UDPBinding udpBinding = null; 
		synchronized(mutex)
		{
			if(channel.isClosed())
				return;			
			udpBinding = findBinding(virtualPort);
		}
		
		if(udpBinding == null)
		{			
			channel.send(EncodeMessage_RequestError(requestUid, ErrorCodes.PORT_UNREACHABLE, userKind, clientId));
			return;
		}
		
		MembershipUser user = membership.resolve(userKind, userId);
		if(user == null)
		{
			channel.send(EncodeMessage_RequestError(requestUid, ErrorCodes.ACCESS_DENIED, userKind, clientId));
			return;
		}
		
		if(udpBinding.roles != null)
		{
			if(user.isGuest())
			{
				channel.send(EncodeMessage_RequestError(requestUid, ErrorCodes.ACCESS_DENIED, userKind, clientId));
				return;				
			}
			
			boolean userAllowed = false;
			for(String role: udpBinding.roles)
			{
				if(user.isInRole(role))
				{
					userAllowed = true;
					break;
				}
			}
			
			if(userAllowed == false)
			{
				channel.send(EncodeMessage_RequestError(requestUid, ErrorCodes.ACCESS_DENIED, userKind, clientId));
				return;		
			}
		}
		else if(udpBinding.guestAccess != null)
		{
			if(udpBinding.guestAccess == GuestAccess.GuestDenied)
			{
				if(user.isGuest())
				{
					channel.send(EncodeMessage_RequestError(requestUid, ErrorCodes.ACCESS_DENIED, userKind, clientId));
					return;
				}
			}			
			else if(user.isStatelessGuest()) // udpBinding.guestAccess == GuestAccess.StatelessGuestDenied
			{
				channel.send(EncodeMessage_RequestError(requestUid, ErrorCodes.ACCESS_DENIED, userKind, clientId));
				return;
			}
		}

		if(udpBinding.isBusy())
		{
			channel.send(EncodeMessage_RequestError(requestUid, ErrorCodes.SERVICE_BUSY, userKind, clientId));		
			return;
		}
		
		channel.send(EncodeMessage_RequestOk(requestUid, virtualPort, userKind, clientId, sessionTag));
	}

	private void processMessage_RzvData(byte[] message, Channel channel) throws AsnException, FormatException
	{		
		SequenceDecoder asnSequence = ASNDecoder.Sequence(message, 2);
		byte[] requestUid = asnSequence.OctetString(16);
		UUID connectionUid = asnSequence.OctetStringToUUID();
		int serverId = asnSequence.Int32();
		byte[] serverIPBytes = asnSequence.OctetString();
		int virtualPort = asnSequence.Int32();
		int userKind = asnSequence.Int32(1, 4);
		long userId = asnSequence.Int64();
		long clientId = asnSequence.Int64();
		byte[] sessionTagEncoding = null;
		if(asnSequence.exists(1))
			sessionTagEncoding = asnSequence.OctetString();
		asnSequence.end();
		
		SequenceDecoder sessionTag = null;
		try {
			sessionTag = sessionTagEncoding == null ? null : ASNDecoder.Sequence(sessionTagEncoding);
		}
		catch(AsnException e) {
			return;
		}
		
		InetAddress serverIP = ByteConverter.toInetAddress(serverIPBytes);
		
		UDPBinding udpBinding = null; 
		synchronized(mutex)
		{
			if(channel.isClosed())
				return;			
			udpBinding = findBinding(virtualPort);
		}
		
		if(udpBinding == null)
		{			
			channel.send(EncodeMessage_RequestError(requestUid, ErrorCodes.PORT_UNREACHABLE, userKind, clientId));
			return;
		}
		
		MembershipUser user = membership.resolve(userKind, userId);
		if(user == null)
		{
			channel.send(EncodeMessage_RequestError(requestUid, ErrorCodes.ACCESS_DENIED, userKind, clientId));
			return;
		}		
		
		if(udpBinding.roles != null)
		{
			if(user.isGuest())
			{
				channel.send(EncodeMessage_RequestError(requestUid, ErrorCodes.ACCESS_DENIED, userKind, clientId));
				return;				
			}
			
			boolean userAllowed = false;
			for(String role: udpBinding.roles)
			{
				if(user.isInRole(role))
				{
					userAllowed = true;
					break;
				}
			}
			
			if(userAllowed == false)
			{
				channel.send(EncodeMessage_RequestError(requestUid, ErrorCodes.ACCESS_DENIED, userKind, clientId));
				return;		
			}			
		}
		else if(udpBinding.guestAccess != null)
		{
			if(udpBinding.guestAccess == GuestAccess.GuestDenied)
			{
				if(user.isGuest())
				{
					channel.send(EncodeMessage_RequestError(requestUid, ErrorCodes.ACCESS_DENIED, userKind, clientId));
					return;
				}
			}			
			else if(user.isStatelessGuest()) // udpBinding.guestAccess == GuestAccess.StatelessGuestDenied
			{
				channel.send(EncodeMessage_RequestError(requestUid, ErrorCodes.ACCESS_DENIED, userKind, clientId));
				return;
			}
		}
		
		udpBinding.createConnection(channel, requestUid, connectionUid, serverId, serverIP, userKind, user, clientId, sessionTag);
	}

	private void processMessage_AuthHash(byte[] message, Channel channel) throws AsnException
	{
		SequenceDecoder asnSequence = ASNDecoder.Sequence(message, 2);
		int virtualPort = asnSequence.Int32();
		UUID connectionUid = asnSequence.OctetStringToUUID();
		byte[] authHash = asnSequence.OctetString(20);
		byte[] authKey2 = asnSequence.OctetString(20);
		asnSequence.end();
		
		UDPBinding udpBinding = null; 
		synchronized(mutex)
		{
			if(channel.isClosed())
				return;			
			udpBinding = findBinding(virtualPort);
		}
		
		if(udpBinding != null)
		{
			udpBinding.onAuthenticationHash(connectionUid, authHash, authKey2);
		}
	}
	
	private void processMessage_AuthError(byte[] message, Channel channel) throws AsnException
	{
		SequenceDecoder asnSequence = ASNDecoder.Sequence(message, 2);
		int virtualPort = asnSequence.Int32();
		UUID connectionUid = asnSequence.OctetStringToUUID();
		asnSequence.end();
		
		UDPBinding udpBinding = null; 
		synchronized(mutex)
		{
			if(channel.isClosed())
				return;			
			udpBinding = findBinding(virtualPort);
		}
		
		if(udpBinding != null)
		{
			udpBinding.onAuthenticationError(connectionUid);
		}
	}
	
	private SoftnetMessage EncodeMessage_RequestOk(byte[] requestUid, int virtualPort, int userKind, long clientId, byte[] sessionTag)
	{
		ASNEncoder asnEncoder = new ASNEncoder();
        SequenceEncoder asnSequence = asnEncoder.Sequence();
        asnSequence.OctetString(requestUid);
        asnSequence.Int32(virtualPort);
        asnSequence.Int32(userKind);
        asnSequence.Int64(clientId);
        if(sessionTag != null)
        	asnSequence.OctetString(1, sessionTag);
        return MsgBuilder.Create(Constants.Service.UdpController.ModuleId, Constants.Service.UdpController.REQUEST_OK, asnEncoder);
	}
	
	private SoftnetMessage EncodeMessage_RequestError(byte[] requestUid, int errorCode, int userKind, long clientId)
	{
		ASNEncoder asnEncoder = new ASNEncoder();
        SequenceEncoder asnSequence = asnEncoder.Sequence();
        asnSequence.OctetString(requestUid);
        asnSequence.Int32(errorCode);
        asnSequence.Int32(userKind);
        asnSequence.Int64(clientId);
        return MsgBuilder.Create(Constants.Service.UdpController.ModuleId, Constants.Service.UdpController.REQUEST_ERROR, asnEncoder);
	}

	private void onMessageReceived(byte[] message, Channel channel) throws AsnException, FormatException
	{
		byte messageTag = message[1];
		if(messageTag == Constants.Service.UdpController.REQUEST)
		{
			processMessage_Request(message, channel);
		}
		else if(messageTag == Constants.Service.UdpController.RZV_DATA)
		{
			processMessage_RzvData(message, channel);
		}
		else if(messageTag == Constants.Service.UdpController.AUTH_HASH)
		{
			processMessage_AuthHash(message, channel);
		}
		else if(messageTag == Constants.Service.UdpController.AUTH_ERROR)
		{
			processMessage_AuthError(message, channel);
		}
		else
			throw new FormatException();
	}
	
	private UDPBinding findBinding(int virtualPort)
	{
		for(UDPBinding udpBinding:  udpBindings)
		{
			if(udpBinding.virtualPort == virtualPort)
				return udpBinding; 					
		}
		return null;
	}
}
