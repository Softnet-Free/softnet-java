---
layout: default
title: 12.2.4. Service Group event handlers
parent: 12.2. Implementing event handlers
grand_parent: 12. Client-specific platform events
nav_order: 4
---

## 12.2.4. Service Group event handlers

Interface <span class="datatype">ClientEventListener</span> declares 6 event handlers associated with Service Group. The first 5 of them have a parameter of type <span class="datatype">RemoteServiceEvent</span>:
```java
void onServiceOnline(RemoteServiceEvent e)
void onServiceOffline(RemoteServiceEvent e)
void onServiceIncluded(RemoteServiceEvent e)
void onServiceRemoved(RemoteServiceEvent e)
void onServiceUpdated(RemoteServiceEvent e)
```
They are invoked when only one remote service has its properties changed. Parameter 'e' contains the affected remote service in the 'service' field. The following example demonstrates this idea:
```java
public void onServiceOnline(RemoteServiceEvent e) {
	System.out.println("Remote service online!");					
	System.out.println("Hostname: " + e.service.getHostname());
	System.out.println("Service Version: " + e.service.getVersion());
}
```

If two or more remote services could have their properties changed the following handler is invoked:
```java
void onServicesUpdated(ClientEndpointEvent e)
```

The parameter e does not convey information about affected services. The application can only get an updated list of <span class="datatype">RemoteService</span> objects from the endpoint and look through the properties, as shown in the following example:
```java
public void onServicesUpdated(ClientEndpointEvent e) {
    ClientEndpoint clientEndpoint = e.getEndpoint();
    RemoteService[] remoteServices = clientEndpoint.getServices();
    System.out.println("Remote service list updated!");					
    for(RemoteService service: remoteServices) {
        System.out.println("Hostname: " + service.getHostname());
        System.out.println("Version: " + service.getVersion());
        System.out.println();
    }
}
```

---
#### TABLE OF CONTENTS
* [12.1. Interface ClientEventListener]({{ site.baseurl }}{% link docs/client-platform-events/interface-client-event-listener.md %})
* [12.2. Implementing event handlers]({{ site.baseurl }}{% link docs/client-platform-events/implementing-event-handlers.md %})
    * [12.2.1. Connectivity status event handler]({{ site.baseurl }}{% link docs/client-platform-events/impl/connectivity-status-event-handler.md %})
    * [12.2.2. Client status event handler]({{ site.baseurl }}{% link docs/client-platform-events/impl/client-status-event-handler.md %})
    * [12.2.3. User properties change event handler]({{ site.baseurl }}{% link docs/client-platform-events/impl/user-properties-change-event-handler.md %})
    * 12.2.4. Service Group event handlers
    * [12.2.5. Persistence event handler]({{ site.baseurl }}{% link docs/client-platform-events/impl/persistence-event-handler.md %})
* [12.3. Adapter class ClientEventAdapter]({{ site.baseurl }}{% link docs/client-platform-events/class-client-event-adapter.md %})