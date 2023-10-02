---
layout: default
title: 12.1. Interface ClientEventListener
parent: 12. Client-specific platform events
nav_order: 1
---

## 12.1. Interface ClientEventListener

Softnet provides the <span class="datatype">ClientEventListener</span> interface that listener class implements to intercept client related events raised by the platform. An application provides the listener to the following method of the client endpoint: 
```java
public void addEventListener(ClientEventListener listener)
```

As with services, an application can provide multiple event listeners with different implementations. If you do not want to implement all methods of the interface, you can derive your handler from the <span class="datatype">ClientEventAdapter</span> class which contains empty implementations of the handlers. This allows you to implement only those handlers that you need.  
Below is the listener interface:
```java
public interface ClientEventListener extends EventListener
{
	void onConnectivityChanged(ClientEndpointEvent e);
	void onStatusChanged(ClientEndpointEvent e);
	void onUserUpdated(ClientEndpointEvent e);
	void onServiceOnline(RemoteServiceEvent e);
	void onServiceOffline(RemoteServiceEvent e);
	void onServiceIncluded(RemoteServiceEvent e);
	void onServiceRemoved(RemoteServiceEvent e);
	void onServiceUpdated(RemoteServiceEvent e);
	void onServicesUpdated(ClientEndpointEvent e);
	void onPersistenceFailed(ClientPersistenceFailedEvent e);
}
```
Let’s consider each of the interface handlers. As with services, the model of Client Endpoint has two abstraction layers – Channel Layer and Functional Layer. The first two methods of <span class="datatype">ClientEventListener</span> intercept the status change events raised at these two abstraction layers.

*	<span class="method">onConnectivityChanged</span> is invoked whenever the connectivity status of the channel layer changes. After calling the endpoint’s connect method, the channel layer makes successive attempts to establish a control channel. In case of success, the connectivity status of the client is set to Connected;
*	<span id="onStatusChanged" class="method">onStatusChanged</span> interceptes the status change event of the functional layer. The functional layer status is the status of the client itself. The site assigns it to the client after the connectivity status is set to Connected. The possible client statuses are discussed in "[Client status event handler]({{ site.baseurl }}{% link docs/client-platform-events/impl/client-status-event-handler.md %})". If the client is assigned the Online status, it can start to interact with online services.  
<span class="text-important">Important note</span>! Typically, when a remote service goes online, the platform notifies clients about it by raising the <span class="datatype">ServiceOnline</span> event. However, in the case of a multi-service site, to avoid multiple <span class="datatype">ServiceOnline</span> events occurring at the same time, the platform does not raise these events for remote services that were already online at the time the client connected. If the client needs to know the online/offline status of services, it does so in the <span class="method">onStatusChanged</span> handler by checking the <span class="datatype">RemoteService</span> objects in the Service Group collection. However, if a remote service gets online when the client has already been online, the platform raises the <span class="datatype">ServiceOnline</span> event. The client intercepts it with the <span class="method">onServiceOnline</span> handler. How to work with Service Group is described in the chapter "[Service Status Detection]({{ site.baseurl }}{% link docs/service-group.md %})".

The next 6 handlers are designed to intercept events raised by Service Group as a result of changing the list of services on the site, changing the service parameters or updating the online/offline status of the services:

*	<span class="method">onServiceOnline</span> – if your client is waiting for the service to get online, it can start to communicate on receiving this event. As already said (see the "<span class="text-important">Important note</span>" to the <span class="method">onStatusChanged</span> handler), in the case of a multi-service site, if the service has already been online at the time the client connected, the platform does not raise the ServiceOnline event in order to avoid multiple events raising at the same time. In this case, the client needs to check the online status of the service in the onStatusChanged handler;
*	<span class="method">onServiceOffline</span> is invoked when an online service goes offline;
*	<span class="method">onServiceIncluded</span> is invoked when a new service is registered on the multi-service site. The event is raised only at a multi-service client endpoint;
*	<span class="method">onServiceRemoved</span> is invoked when the service is disabled, or removed from a multi-service site. The event is raised only at a multi-service client endpoint;
*	<span class="method">onServiceUpdated</span> is invoked when the service parameters has been updated;
*	<span class="method">onServicesUpdated</span> is invoked when the client becomes online and the Service Group detects that one o more services have been updated since the client’s last online session. The event is raised only at a multi-service client endpoint;  

The <span class="method">onUserUpdated</span> handler is called whenever the properties of a domain user that this client is associated with or its permissions to access the service change. You can use the information about the user’s permissions to make only those requests that are currently allowed to the user.  

The last handler, <span class="method">onPersistenceFailed</span>, is similar to that of the service’s one. It intercepts error notifications raised by the client persistence component.

---
#### TABLE OF CONTENTS
* 12.1. Interface ClientEventListener
* [12.2. Implementing event handlers]({{ site.baseurl }}{% link docs/client-platform-events/implementing-event-handlers.md %})
    * [12.2.1. Connectivity status event handler]({{ site.baseurl }}{% link docs/client-platform-events/impl/connectivity-status-event-handler.md %})
    * [12.2.2. Client status event handler]({{ site.baseurl }}{% link docs/client-platform-events/impl/client-status-event-handler.md %})
    * [12.2.3. User properties change event handler]({{ site.baseurl }}{% link docs/client-platform-events/impl/user-properties-change-event-handler.md %})
    * [12.2.4. Service Group event handlers]({{ site.baseurl }}{% link docs/client-platform-events/impl/service-group-event-handlers.md %})
    * [12.2.5. Persistence event handler]({{ site.baseurl }}{% link docs/client-platform-events/impl/persistence-event-handler.md %})
* [12.3. Adapter class ClientEventAdapter]({{ site.baseurl }}{% link docs/client-platform-events/class-client-event-adapter.md %})