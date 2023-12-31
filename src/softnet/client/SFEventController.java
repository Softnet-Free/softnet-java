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

import java.util.*;
import java.util.regex.Pattern;

import softnet.asn.*;
import softnet.core.*;
import softnet.exceptions.*;
import softnet.utils.*;

class SFEventController implements EventController
{	
	public SFEventController(ClientEndpoint clientEndpoint, ClientURI clientURI)
	{
		this.clientEndpoint = clientEndpoint;
		this.threadPool = clientEndpoint.threadPool;
		this.clientURI = clientURI;
		clientPersistence = new ClientMemoryPersistence();
		subscriptions = new ArrayList<Subscription>();	
		eventListeners = new HashSet<ClientEventListener>();
		isSynchronized = false;
	}
	
	private Object mutex = new Object();
	private ClientEndpoint clientEndpoint;
	private ThreadPool threadPool;
	private boolean isClosed = false;
	private boolean isInitialized = false;
	private Channel channel;
	private ClientURI clientURI;
	private ClientPersistence clientPersistence;
	private ArrayList<Subscription> subscriptions;	
	private HashSet<ClientEventListener> eventListeners;
	private boolean isSynchronized;

	public void setPersistenceL2()
	{
		synchronized(mutex)
		{
			if(isInitialized)
				throw new IllegalStateException("The persistence has already been set.");
			isInitialized = true;
			
			try
			{
				try
				{
					clientPersistence = ClientFilePersistence.create(clientURI);
				}
				catch(PersistenceDataFormatSoftnetException ex)
				{
					clientPersistence.reset();
					raisePersistenceFailedEvent(ex);
				}
			}
			catch(PersistenceIOSoftnetException ex)
			{
				clientPersistence.close();
				clientPersistence = new ClientMemoryPersistence();
				raisePersistenceFailedEvent(ex);
			}
		}
	}
	
	public void setPersistenceL2(String fileBasedPersistenceDirectory)
	{
		synchronized(mutex)
		{
			if(isInitialized)
				throw new IllegalStateException("The persistence has already been set.");
			isInitialized = true;
			
			try
			{
				try
				{
					clientPersistence = ClientFilePersistence.create(clientURI, fileBasedPersistenceDirectory);
				}
				catch(PersistenceDataFormatSoftnetException ex)
				{
					clientPersistence.reset();
					raisePersistenceFailedEvent(ex);
				}
			}
			catch(PersistenceIOSoftnetException ex)
			{
				clientPersistence.close();
				clientPersistence = new ClientMemoryPersistence();
				raisePersistenceFailedEvent(ex);
			}
		}		
	}
	
	public void onConnectCalled()
	{
		synchronized(mutex) {		
			isInitialized = true;
		}
	}
	
	public void onEndpointClosed()
	{
		synchronized(mutex)
		{
			isClosed = true;
			clientPersistence.close();
		}
	}
		
	public void onEndpointConnected(Channel channel)
	{
		channel.registerComponent(Constants.Client.EventController.ModuleId, 
			new MsgAcceptor<Channel>()
			{
				public void accept(byte[] message, Channel _channel) throws AsnException, FormatException, SoftnetException
				{
					onMessageReceived(message, _channel);
				}
			});		
		
		synchronized(mutex)
		{
			this.channel = channel;
		}
	}
	
	public void onEndpointDisconnected()
	{
		synchronized(mutex)
		{
			isSynchronized = false;
			channel = null;
		}
	}
	
	public void addEventListener(ClientEventListener listener)
	{
		synchronized(eventListeners)
		{
			eventListeners.add(listener);
		}
	}

	public void removeEventListener(ClientEventListener listener)
	{
		synchronized(eventListeners)
		{
			eventListeners.remove(listener);
		}		
	}

	public void subscribeToREvent(String eventName, RemoteEventListener listener)
	{
		validateEventName(eventName);		

		if(listener == null)
			throw new IllegalArgumentException("The argument 'listener' must not be null.");

		synchronized(mutex)
		{
			if(isClosed) return;			

			for(Subscription subscription: subscriptions)
			{
				if(subscription.eventName.equals(eventName))
					throw new IllegalArgumentException(String.format("The event '%s' has already been subscribed.", eventName));	
			}
						
			Subscription newSubscription = new Subscription(EventCategory.Replacing, eventName, listener);
			subscriptions.add(newSubscription);
			
			if(isSynchronized)
			{
				ASNEncoder asnEncoder = new ASNEncoder();
		        SequenceEncoder asnSequence = asnEncoder.Sequence();
		        asnSequence.Int32(newSubscription.eventCategory);
		        asnSequence.IA5String(newSubscription.eventName);
		        channel.send(MsgBuilder.Create(Constants.Client.EventController.ModuleId, Constants.Client.EventController.ADD_SUBSCRIPTION, asnEncoder));
			}
		}		
	}

	public void subscribeToQEvent(String eventName, RemoteEventListener listener)
	{
		validateEventName(eventName);		
		
		if(listener == null)
			throw new IllegalArgumentException("The argument 'listener' must not be null.");

		synchronized(mutex)
		{
			if(isClosed) return;			

			for(Subscription subscription: subscriptions)
			{
				if(subscription.eventName.equals(eventName))
					throw new IllegalArgumentException(String.format("The event '%s' has already been subscribed.", eventName));
			}
						
			Subscription newSubscription = new Subscription(EventCategory.Queuing, eventName, listener);
			subscriptions.add(newSubscription);
			
			if(isSynchronized)
			{
				ASNEncoder asnEncoder = new ASNEncoder();
		        SequenceEncoder asnSequence = asnEncoder.Sequence();
		        asnSequence.Int32(newSubscription.eventCategory);
		        asnSequence.IA5String(newSubscription.eventName);
		        channel.send(MsgBuilder.Create(Constants.Client.EventController.ModuleId, Constants.Client.EventController.ADD_SUBSCRIPTION, asnEncoder));
			}
		}		
	}

	public void subscribeToPEvent(String eventName, RemoteEventListener listener)
	{
		validateEventName(eventName);		
		
		if(listener == null)
			throw new IllegalArgumentException("The argument 'listener' must not be null.");

		synchronized(mutex)
		{
			if(isClosed) return;			

			for(Subscription subscription: subscriptions)
			{
				if(subscription.eventName.equals(eventName))
					throw new IllegalArgumentException(String.format("The event '%s' has already been subscribed.", eventName));
			}
						
			Subscription newSubscription = new Subscription(EventCategory.Private, eventName, listener);
			subscriptions.add(newSubscription);
			
			if(isSynchronized)
			{
				ASNEncoder asnEncoder = new ASNEncoder();
		        SequenceEncoder asnSequence = asnEncoder.Sequence();
		        asnSequence.Int32(newSubscription.eventCategory);
		        asnSequence.IA5String(newSubscription.eventName);
		        channel.send(MsgBuilder.Create(Constants.Client.EventController.ModuleId, Constants.Client.EventController.ADD_SUBSCRIPTION, asnEncoder));
			}
		}		
	}
	
	public boolean removeSubscription(String eventName)
	{
		if(eventName == null)
			return false;

		synchronized(mutex)
		{
			if(isClosed) 
				return false;

			for(Subscription subscription: subscriptions)
			{
				if(subscription.eventName.equals(eventName))
				{
					subscriptions.remove(subscription);
	
					if(isSynchronized)
					{
						ASNEncoder asnEncoder = new ASNEncoder();
				        SequenceEncoder asnSequence = asnEncoder.Sequence();
				        asnSequence.Int32(subscription.eventCategory);
				        asnSequence.IA5String(subscription.eventName);
				        channel.send(MsgBuilder.Create(Constants.Client.EventController.ModuleId, Constants.Client.EventController.REMOVE_SUBSCRIPTION, asnEncoder));	
					}					
					return true;
				}
			}
		}
		return false;
	}
		
	private void handleReplacingEvent(EventIData eventIData, RemoteEventListener eventListener, Channel channel)
	{				
		try
		{
			eventListener.accept(clientEndpoint, new RemoteEvent(eventIData));
		}
		catch(AsnException e) { }
		
		synchronized(mutex)
		{
			if(isClosed) return;
			
			try
			{
				clientPersistence.putItem(eventIData.name, eventIData.instanceId);
			}
			catch(PersistenceIOSoftnetException ex)
			{
				clientPersistence.close();
				clientPersistence = new ClientMemoryPersistence();
				((ClientMemoryPersistence)clientPersistence).putItem(eventIData.name, eventIData.instanceId);
				raisePersistenceFailedEvent(ex);
			}			
			
			channel.send(EncodeMessage_ReplacingEventAck(eventIData));	
		}
	}
	
	private void handleQueueingEvent(EventIData eventIData, RemoteEventListener eventListener, Channel channel)
	{
		try
		{
			eventListener.accept(clientEndpoint, new RemoteEvent(eventIData));
		}
		catch(AsnException e) { }
		
		synchronized(mutex)
		{
			if(isClosed) return;
			
			try
			{
				clientPersistence.putItem(eventIData.name, eventIData.instanceId);
			}
			catch(PersistenceIOSoftnetException ex)
			{
				clientPersistence.close();
				clientPersistence = new ClientMemoryPersistence();
				((ClientMemoryPersistence)clientPersistence).putItem(eventIData.name, eventIData.instanceId);
				raisePersistenceFailedEvent(ex);
			}			
			channel.send(EncodeMessage_QueueingEventAck(eventIData));
		}
	}

	private void handlePrivateEvent(EventIData eventIData, RemoteEventListener eventListener, Channel channel)
	{
		try
		{
			eventListener.accept(clientEndpoint, new RemoteEvent(eventIData));
		}
		catch(AsnException e) { }
		
		synchronized(mutex)
		{
			if(isClosed) return;
			
			try
			{
				clientPersistence.putItem(eventIData.name, eventIData.instanceId);
			}
			catch(PersistenceIOSoftnetException ex)
			{
				clientPersistence.close();
				clientPersistence = new ClientMemoryPersistence();
				((ClientMemoryPersistence)clientPersistence).putItem(eventIData.name, eventIData.instanceId);
				raisePersistenceFailedEvent(ex);
			}		
			
			channel.send(EncodeMessage_PrivateEventAck(eventIData));
		}
	}

	private void ProcessMessage_Sync(byte[] message, Channel channel) throws AsnException, softnet.exceptions.HostErrorSoftnetException
	{
		byte[] receivedhash = null;
		SequenceDecoder sequenceDecoder = ASNDecoder.Sequence(message, 2);
		if(sequenceDecoder.exists(1))
			receivedhash = sequenceDecoder.OctetString(20); 
		sequenceDecoder.end();
		
		byte[] hash = null;
		if(subscriptions.size() > 0)
		{
			Subscription[] subscriptionArray = new Subscription[subscriptions.size()];
			subscriptionArray = subscriptions.toArray(subscriptionArray);
			Arrays.sort(subscriptionArray); 
			
			ASNEncoder asnEncoder = new ASNEncoder();
	        SequenceEncoder asnRoot = asnEncoder.Sequence();
	        for (Subscription subscription: subscriptionArray)
	        {
	        	SequenceEncoder asnSubscription = asnRoot.Sequence();
	        	asnSubscription.Int32(subscription.eventCategory);
	        	asnSubscription.IA5String(subscription.eventName);
	        }
	        hash = Sha1Hash.compute(asnEncoder.getEncoding());
		}
	
		if(java.util.Arrays.equals(receivedhash, hash))
			channel.send(MsgBuilder.Create(Constants.Client.EventController.ModuleId, Constants.Client.EventController.SYNC_OK));
		else
			channel.send(EncodeMessage_Subscriptions());
	
		isSynchronized = true;
	}
	
	private void ProcessMessage_ReplacingEvent(byte[] message, final Channel channel) throws AsnException
	{
		final EventIData eventIData = new EventIData();
		SequenceDecoder sequenceDecoder = ASNDecoder.Sequence(message, 2);
		eventIData.name = sequenceDecoder.IA5String(1, 256);
		eventIData.category = EventCategory.Replacing;
		eventIData.isNull = false;
		eventIData.eventId = sequenceDecoder.Int64();
		eventIData.transactionUid = sequenceDecoder.OctetString(16);
		eventIData.instanceId = sequenceDecoder.Int64();
		eventIData.serviceId = sequenceDecoder.Int64();
		eventIData.age = sequenceDecoder.Int64();
		eventIData.createdDate = sequenceDecoder.GndTimeToDate();
		if(sequenceDecoder.exists(1))
			eventIData.argumentsEncoding = sequenceDecoder.OctetString(2, 4096);
				
		for(Subscription subscription: subscriptions)
		{
			if(subscription.eventCategory == 1 && subscription.eventName.equals(eventIData.name))
			{
				ClientEventPersistable cep = clientPersistence.getItem(eventIData.name);
				if(cep == null || cep.instanceId != eventIData.instanceId)
				{				
					final RemoteEventListener eventListener = subscription.listener; 
					Runnable runnable = new Runnable()
					{
						@Override
						public void run()
						{
							handleReplacingEvent(eventIData, eventListener, channel);
						}
					};
					threadPool.execute(runnable);
				}
				else
				{
					channel.send(EncodeMessage_ReplacingEventAck(eventIData));	
				}
				return;
			}
		}
		
		channel.send(EncodeMessage_EventRejected(eventIData, Constants.EventCategory.Replacing));
	}

	private void ProcessMessage_ReplacingNullEvent(byte[] message, final Channel channel) throws AsnException
	{
		final EventIData eventIData = new EventIData();
		SequenceDecoder sequenceDecoder = ASNDecoder.Sequence(message, 2);
		eventIData.name = sequenceDecoder.IA5String(1, 256);
		eventIData.category = EventCategory.Replacing;
		eventIData.isNull = true;
		eventIData.eventId = sequenceDecoder.Int64();
		eventIData.transactionUid = sequenceDecoder.OctetString(16);
		eventIData.instanceId = sequenceDecoder.Int64();
		eventIData.serviceId = sequenceDecoder.Int64();
		eventIData.age = sequenceDecoder.Int64();
		eventIData.createdDate = sequenceDecoder.GndTimeToDate();		

		for(Subscription subscription: subscriptions)
		{
			if(subscription.eventCategory == 1 && subscription.eventName.equals(eventIData.name))
			{
				ClientEventPersistable cep = clientPersistence.getItem(eventIData.name);
				if(cep == null || cep.instanceId != eventIData.instanceId)
				{
					final RemoteEventListener eventListener = subscription.listener; 
					Runnable runnable = new Runnable()
					{
						@Override
						public void run()
						{
							handleReplacingEvent(eventIData, eventListener, channel);
						}
					};
					threadPool.execute(runnable);
				}
				else
				{
					channel.send(EncodeMessage_ReplacingEventAck(eventIData));	
				}
				return;
			}
		}
		
		channel.send(EncodeMessage_EventRejected(eventIData, Constants.EventCategory.Replacing));
	}
	
	private void ProcessMessage_QueueingEvent(byte[] message, final Channel channel) throws AsnException
	{
		final EventIData eventIData = new EventIData();
		SequenceDecoder sequenceDecoder = ASNDecoder.Sequence(message, 2);
		eventIData.name = sequenceDecoder.IA5String(1, 256);
		eventIData.category = EventCategory.Queuing;
		eventIData.eventId = sequenceDecoder.Int64();
		eventIData.transactionUid = sequenceDecoder.OctetString(16);
		eventIData.instanceId = sequenceDecoder.Int64();
		eventIData.serviceId = sequenceDecoder.Int64();
		eventIData.age = sequenceDecoder.Int64();
		eventIData.createdDate = sequenceDecoder.GndTimeToDate();
		if(sequenceDecoder.exists(1))
			eventIData.argumentsEncoding = sequenceDecoder.OctetString(2, 4096);
		
		for(Subscription subscription: subscriptions)
		{
			if(subscription.eventCategory == 2 && subscription.eventName.equals(eventIData.name))
			{
				ClientEventPersistable cep = clientPersistence.getItem(eventIData.name);
				if(cep == null || cep.instanceId != eventIData.instanceId)
				{
					final RemoteEventListener eventListener = subscription.listener; 
					Runnable runnable = new Runnable()
					{
						@Override
						public void run()
						{
							handleQueueingEvent(eventIData, eventListener, channel);
						}
					};
					threadPool.execute(runnable);
				}
				else
				{
					channel.send(EncodeMessage_QueueingEventAck(eventIData));
				}
				return;
			}
		}
		
		channel.send(EncodeMessage_EventRejected(eventIData, Constants.EventCategory.Queueing));
	}

	private void ProcessMessage_PrivateEvent(byte[] message, final Channel channel) throws AsnException
	{
		final EventIData eventIData = new EventIData();
		SequenceDecoder sequenceDecoder = ASNDecoder.Sequence(message, 2);
		eventIData.name = sequenceDecoder.IA5String(1, 256);
		eventIData.category = EventCategory.Private;
		eventIData.eventId = sequenceDecoder.Int64();
		eventIData.transactionUid = sequenceDecoder.OctetString(16);
		eventIData.instanceId = sequenceDecoder.Int64();
		eventIData.serviceId = sequenceDecoder.Int64();
		eventIData.age = sequenceDecoder.Int64();
		eventIData.createdDate = sequenceDecoder.GndTimeToDate();
		if(sequenceDecoder.exists(1))
			eventIData.argumentsEncoding = sequenceDecoder.OctetString(2, 4096);				
		
		for(Subscription subscription: subscriptions)
		{
			if(subscription.eventCategory == 4 && subscription.eventName.equals(eventIData.name))
			{
				ClientEventPersistable cep = clientPersistence.getItem(eventIData.name);
				if(cep == null || cep.instanceId != eventIData.instanceId)
				{
					final RemoteEventListener eventListener = subscription.listener; 
					Runnable runnable = new Runnable()
					{
						@Override
						public void run()
						{
							handlePrivateEvent(eventIData, eventListener, channel);
						}
					};
					threadPool.execute(runnable);
				}
				else
				{
					channel.send(EncodeMessage_PrivateEventAck(eventIData));
				}				
				return;
			}
		}
		
		channel.send(EncodeMessage_EventRejected(eventIData, Constants.EventCategory.Private));
	}

	private void ProcessMessage_IllegalSubscription(byte[] message) throws AsnException
	{
		SequenceDecoder sequenceDecoder = ASNDecoder.Sequence(message, 2);
		String eventName = sequenceDecoder.IA5String(1, 256);
		sequenceDecoder.end();
		
		for(Subscription subscription: subscriptions)
		{
			if(subscription.eventName.equals(eventName))
			{
				final SoftnetException softnetException = new IllegalNameSoftnetException(String.format("The name '%s' of the event you have subscribed to is illegal.", eventName));				
				final RemoteEventListener eventListener = subscription.listener; 
				
				Runnable runnable = new Runnable()
				{
					@Override
					public void run()
					{
						eventListener.acceptError(clientEndpoint, softnetException);
					}
				};				
				threadPool.execute(runnable);
				return;
			}
		}
	}

	private SoftnetMessage EncodeMessage_Subscriptions()
	{
		ASNEncoder asnEncoder = new ASNEncoder();
        SequenceEncoder asnRoot = asnEncoder.Sequence();
        for(Subscription subscription: subscriptions)
        {
        	SequenceEncoder asnSubscription = asnRoot.Sequence();
        	asnSubscription.Int32(subscription.eventCategory);
        	asnSubscription.IA5String(subscription.eventName);       	
        }
        return MsgBuilder.Create(Constants.Client.EventController.ModuleId, Constants.Client.EventController.SUBSCRIPTIONS, asnEncoder);		
	}

	private SoftnetMessage EncodeMessage_ReplacingEventAck(EventIData eventIData)
	{
		ASNEncoder asnEncoder = new ASNEncoder();
        SequenceEncoder asnSequence = asnEncoder.Sequence();
        asnSequence.Int64(eventIData.eventId);
        asnSequence.OctetString(eventIData.transactionUid);
        return MsgBuilder.Create(Constants.Client.EventController.ModuleId, Constants.Client.EventController.REPLACING_EVENT_ACK, asnEncoder);	
	}

	private SoftnetMessage EncodeMessage_QueueingEventAck(EventIData eventIData)
	{
		ASNEncoder asnEncoder = new ASNEncoder();
        SequenceEncoder asnSequence = asnEncoder.Sequence();
        asnSequence.Int64(eventIData.eventId);
        asnSequence.OctetString(eventIData.transactionUid);
        return MsgBuilder.Create(Constants.Client.EventController.ModuleId, Constants.Client.EventController.QUEUEING_EVENT_ACK, asnEncoder);	
	}

	private SoftnetMessage EncodeMessage_PrivateEventAck(EventIData eventIData)
	{
		ASNEncoder asnEncoder = new ASNEncoder();
        SequenceEncoder asnSequence = asnEncoder.Sequence();
        asnSequence.Int64(eventIData.eventId);
        asnSequence.OctetString(eventIData.transactionUid);
        return MsgBuilder.Create(Constants.Client.EventController.ModuleId, Constants.Client.EventController.PRIVATE_EVENT_ACK, asnEncoder);	
	}

	private SoftnetMessage EncodeMessage_EventRejected(EventIData eventIData, int kind)
	{
		ASNEncoder asnEncoder = new ASNEncoder();
        SequenceEncoder asnSequence = asnEncoder.Sequence();
        asnSequence.Int32(kind);
        asnSequence.Int64(eventIData.eventId);
        asnSequence.OctetString(eventIData.transactionUid);
        return MsgBuilder.Create(Constants.Client.EventController.ModuleId, Constants.Client.EventController.EVENT_REJECTED, asnEncoder);	
	}

	private void onMessageReceived(byte[] message, Channel channel) throws AsnException, FormatException, SoftnetException
	{
		synchronized(mutex)
		{
			if(channel.closed())
				return;
			
			byte messageTag = message[1];
			if(messageTag == Constants.Client.EventController.REPLACING_EVENT)
			{
				ProcessMessage_ReplacingEvent(message, channel);
			}
			else if(messageTag == Constants.Client.EventController.REPLACING_NULL_EVENT)
			{
				ProcessMessage_ReplacingNullEvent(message, channel);
			}
			else if(messageTag == Constants.Client.EventController.QUEUEING_EVENT)
			{
				ProcessMessage_QueueingEvent(message, channel);
			}			
			else if(messageTag == Constants.Client.EventController.PRIVATE_EVENT)
			{
				ProcessMessage_PrivateEvent(message, channel);
			}
			else if(messageTag == Constants.Client.EventController.ILLEGAL_SUBSCRIPTION)
			{
				ProcessMessage_IllegalSubscription(message);
			}		
			else if(messageTag == Constants.Client.EventController.SYNC)
			{
				ProcessMessage_Sync(message, channel);
			}		
			else
				throw new FormatException();
		}
	}
	
	private void raisePersistenceFailedEvent(PersistenceSoftnetException ex)
	{
		final ClientPersistenceFailedEvent event = new ClientPersistenceFailedEvent(ex, clientEndpoint);		
		synchronized(eventListeners)
		{
			for (final ClientEventListener listener: eventListeners)
			{
				final Runnable runnable = new Runnable()
				{
					@Override
					public void run()
					{
						listener.onPersistenceFailed(event);
					}
				};
				threadPool.execute(runnable);
		    }
		}
	}
	
	private class Subscription implements Comparable<Subscription>
	{
		public final int eventCategory;
		public final String eventName;
		public final RemoteEventListener listener;
		
		public Subscription(EventCategory kind, String eventName, RemoteEventListener listener)
		{
			if(kind == EventCategory.Replacing)
				this.eventCategory = Constants.EventCategory.Replacing;
			else if(kind == EventCategory.Queuing)
				this.eventCategory = Constants.EventCategory.Queueing;
			else if(kind == EventCategory.Private)
				this.eventCategory = Constants.EventCategory.Private;
			else
				throw new IllegalArgumentException();
			this.eventName = eventName;
			this.listener = listener;
		}
		
		public int compareTo(Subscription other)
        {
			if(this.eventCategory > other.eventCategory)
				return 1;
			else if(this.eventCategory < other.eventCategory)
				return -1;
			else
				return this.eventName.compareTo(other.eventName);
        }
        
        @Override
        public boolean equals(Object other)
        {
        	if (other == null) return false;
        	Subscription subscription = (Subscription)other;
            return this.eventCategory == subscription.eventCategory && this.eventName.equals(subscription.eventName);
        }
        
        @Override
        public int hashCode()
        {
            return (String.valueOf(this.eventCategory) + this.eventName).hashCode();
        }
	}

    private static void validateEventName(String eventName)
    {
    	if(eventName == null || eventName.length() == 0)
			throw new IllegalArgumentException("An event name must not be null or empty.");		
		
		if(eventName.length() > 256)
			throw new IllegalArgumentException(String.format("The event name '%s' contains more than 256 characters.", eventName));
		
		if (Pattern.matches("^[\\u0020-\\u007F]+$", eventName) == false)
            throw new IllegalArgumentException(String.format("The event name '%s' contains illegal characters. Allowed characters: a-z A-Z 0-9 space _ - + . @ # $ %% & * ' : ^ / ! ( ) [ ].", eventName));
		
		if (Pattern.matches("^[a-zA-Z].*$", eventName) == false)
            throw new IllegalArgumentException(String.format("The leading character in the event name '%s' is illegal. Allowed characters: a-z A-Z.", eventName));
		
		if (Pattern.matches("^.*\\s$", eventName))
            throw new IllegalArgumentException(String.format("The trailing space in the event name '%s' is illegal.", eventName));
		
		if (Pattern.matches("^.*\\s\\s.*$", eventName))
            throw new IllegalArgumentException(String.format("The format of the event name '%s' is illegal. Two or more consecutive spaces are not allowed.", eventName));	

		if (Pattern.matches("^[\\w\\s.$*+()#@%&=':\\^\\[\\]\\-/!]+$", eventName) == false)
            throw new IllegalArgumentException(String.format("The event name '%s' contains illegal characters. Allowed characters: a-z A-Z 0-9 space _ - + . @ # $ %% & * ' : ^ / ! ( ) [ ].", eventName));		    
    }
}
