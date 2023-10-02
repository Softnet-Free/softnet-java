---
layout: default_no_content
title: 11.2.3. Hostname change event handler
parent: 11.2. Implementing event handlers
grand_parent: 11. Service-specific platform events
nav_order: 3
---

## 11.2.3. Hostname change event handler

The handler signature: 
```java
void onHostnameChanged(ServiceEndpointEvent e)
```

The procedure of getting the updated hostname is similar to the previous ones. In the handler’s body, call the <span class="method">getEndpoint</span> method on the 'e' parameter. It returns the endpoint that invoked the handler. Then call the endpoint’s <span class="method">getHostname</span> method. It has the following signature:
```java
public String getHostname()
```

The example below demonstrates these steps:
```java
public void onHostnameChanged(ServiceEndpointEvent e)
{
    ServiceEndpoint serviceEndpoint = e.getEndpoint();
    System.out.println(String.format("The updated service hostname: %s", serviceEndpoint.getHostname()));
}
```

---
#### TABLE OF CONTENTS
* [11.1. Interface ServiceEventListener]({{ site.baseurl }}{% link docs/service-platform-events/interface-service-event-listener.md %})
* [11.2. Implementing event handlers]({{ site.baseurl }}{% link docs/service-platform-events/implementing-event-handlers.md %})
	* [11.2.1. Connectivity status event handler]({{ site.baseurl }}{% link docs/service-platform-events/impl/connectivity-status-event-handler.md %})
	* [11.2.2. Service status event handler]({{ site.baseurl }}{% link docs/service-platform-events/impl/service-status-event-handler.md %})
	* 11.2.3. Hostname change event handler
	* [11.2.4. User Membership event handlers]({{ site.baseurl }}{% link docs/service-platform-events/impl/user-membership-event-handlers.md %})
	* [11.2.5. Persistence event handler]({{ site.baseurl }}{% link docs/service-platform-events/impl/persistence-event-handler.md %})
* [11.3. Adapter class ServiceEventAdapter]({{ site.baseurl }}{% link docs/service-platform-events/class-service-event-adapter.md %})