---
layout: default
title: 12.2.2. Client status event handler
parent: 12.2. Implementing event handlers
grand_parent: 12. Platform events related to clients
nav_order: 2
---

## 12.2.2. Client status event handler

After the control channel is established, the site checks the connected client against the set of parameters provided by the endpoint as well as those set by the administrator on the site. If the client is eligible, it installs on the site. Then, the site assigns it an online status, and it is ready to interact with services. Otherwise, based on the reason for ineligibility, the site assigns it a status from the <span class="datatype">ClientStatus</span> enumeration. The whole process ends with a call to the following handler:
```java
void onStatusChanged(ClientEndpointEvent e)
```

To get the updated client status, follow these steps. In the handler’s body, call the <span class="method">getEndpoint</span> method on the 'e' parameter. It returns the endpoint, which is of type <span class="datatype">ClientEndpoint</span>, that invoked the handler. Then call the endpoint’s <span class="method">getStatus</span> method. It has the following signature:
```java
public ClientStatus getStatus()
```

The example below demonstrates these steps:
```java
public void onStatusChanged(ClientEndpointEvent e)
{
    ClientEndpoint clientEndpoint = e.getEndpoint();
    ClientStatus clientStatus = clientEndpoint.getStatus();
    System.out.println(String.format("The client status: %s", clientStatus)); 
}
```

Let's consider the status names from the <span class="datatype">ClientStatus</span> enumeration and their interpretation.
```java
public enum ClientStatus
{
    Offline,
    Online,
    ServiceTypeConflict,
    AccessDenied,    
    ServiceOwnerDisabled,
    SiteDisabled,
    CreatorDisabled
}
```
*	<span class="text-monospace">Offline</span> – the client is offline while the endpoint is not connected to the site;
*	<span class="text-monospace">Online</span> – the client is ready to communicate with services;
*	<span class="text-monospace">ServiceTypeConflict</span> – the Service Type and Contract Author provided by the client are different from those declared on the site;
*	<span class="text-monospace">AccessDenied</span> – the client does not have any access rights to the service, while the Guest is not allowed or is not supported by the service;
*	<span class="text-monospace">ServiceOwnerDisabled</span> – the service owner is disabled by the Softnet MS administrator;
*	<span class="text-monospace">ServiceDisabled</span> – the service is disabled by the owner;
*	<span class="text-monospace">CreatorDisabled</span> – the client’s creator is disabled by the Softnet MS administrator. The creator can be the Owner or one of their contacts.

---
#### TABLE OF CONTENTS
* [12.1. Interface ClientEventListener]({{ site.baseurl }}{% link docs/client-platform-events/interface-client-event-listener.md %})
* [12.2. Implementing event handlers]({{ site.baseurl }}{% link docs/client-platform-events/implementing-event-handlers.md %})
    * [12.2.1. Connectivity status event handler]({{ site.baseurl }}{% link docs/client-platform-events/impl/connectivity-status-event-handler.md %})
    * 12.2.2. Client status event handler
    * [12.2.3. User properties change event handler]({{ site.baseurl }}{% link docs/client-platform-events/impl/user-properties-change-event-handler.md %})
    * [12.2.4. Service Group event handlers]({{ site.baseurl }}{% link docs/client-platform-events/impl/service-group-event-handlers.md %})
    * [12.2.5. Persistence event handler]({{ site.baseurl }}{% link docs/client-platform-events/impl/persistence-event-handler.md %})
* [12.3. Adapter class ClientEventAdapter]({{ site.baseurl }}{% link docs/client-platform-events/class-client-event-adapter.md %})