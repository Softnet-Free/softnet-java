---
layout: default
title: 12.2. Implementing event handlers
parent: 12. Platform events related to clients
has_children: true
nav_order: 2
---

## 12.2. Implementing event handlers

Here you follow the same approach as when implementing service event handlers. If the handler has a parameter of type <span class="datatype">ClientEndpointEvent</span>, the updated object that caused an event to be raised can only be retrieved from the endpoint. To get the endpoint, call <span class="method">getEndpoint</span> on the handler’s parameter 'e'. The method has the following signature:
```java
public ClientEndpoint getEndpoint()
```
The return value is of type <span class="datatype">ClientEndpoint</span>, which represents a multi-service endpoint. If the endpoint is supposed to be single-service, you can cast it to <span class="datatype">ClientSEndpoint</span> after checking if it is single-service (see the chapter "[Service Group]({{ site.baseurl }}{% link docs/service-group.md %})"). The type is convenient to interact with a single remote service.  

Some of the handlers associated with Service Group have a parameter 'e' of type <span class="datatype">RemoteServiceEvent</span> derived from <span class="datatype">ClientEndpointEvent</span>. Along with a derived method <span class="method">getEndpoint</span>, it has a field service of type <span class="datatype">RemoteService</span>. This is a remote service that caused the event to be raised by the platform.  

Let's look at how to implement each of the event handlers declared by the <span class="datatype">ClientEventListener</span> interface.

---
#### TABLE OF CONTENTS
* [12.1. Interface ClientEventListener]({{ site.baseurl }}{% link docs/client-platform-events/interface-client-event-listener.md %})
* 12.2. Implementing event handlers
    * [12.2.1. Connectivity status event handler]({{ site.baseurl }}{% link docs/client-platform-events/impl/connectivity-status-event-handler.md %})
    * [12.2.2. Client status event handler]({{ site.baseurl }}{% link docs/client-platform-events/impl/client-status-event-handler.md %})
    * [12.2.3. User properties change event handler]({{ site.baseurl }}{% link docs/client-platform-events/impl/user-properties-change-event-handler.md %})
    * [12.2.4. Service Group event handlers]({{ site.baseurl }}{% link docs/client-platform-events/impl/service-group-event-handlers.md %})
    * [12.2.5. Persistence event handler]({{ site.baseurl }}{% link docs/client-platform-events/impl/persistence-event-handler.md %})
* [12.3. Adapter class ClientEventAdapter]({{ site.baseurl }}{% link docs/client-platform-events/class-client-event-adapter.md %})