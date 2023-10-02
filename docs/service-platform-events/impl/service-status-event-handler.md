---
layout: default_no_content
title: 11.2.2. Service status event handler
parent: 11.2. Implementing event handlers
grand_parent: 11. Service-specific platform events
nav_order: 2
---

## 11.2.2. Service status event handler

After the control channel is established, the site checks the connected service against the set of parameters provided by the endpoint as well as those set by the service owner on the site. If the service is eligible, it installs on the site. Then, the site assigns it an online status, and it is ready to serve clients. Otherwise, based on the reason for ineligibility, the site assigns it a status from the <span class="datatype">ServiceStatus</span> enumeration. The whole process ends with a call to the following handler:
```java
void onStatusChanged(ServiceEndpointEvent e)
```

To get the updated service status, follow these steps. In the handler’s body, call the <span class="method">getEndpoint</span> method on the e parameter. It returns the endpoint, which is of type <span class="datatype">ServiceEndpoint</span>, that invoked the handler. Then call the endpoint’s <span class="method">getStatus</span> method.  It has the following signature:
```java
public ServiceStatus getStatus()
```

The example below demonstrates these steps:
```java
public void onStatusChanged(ServiceEndpointEvent e)
{
    ServiceEndpoint serviceEndpoint = e.getEndpoint();
    ServiceStatus serviceStatus = serviceEndpoint.getStatus();
    System.out.println(String.format("The service status: %s", serviceStatus)); 
}
```

Let's consider the status names from the <span class="datatype">ServiceStatus</span> enumeration and their interpretation.
```java
public enum ServiceStatus
{
    Offline,
    Online,
    ServiceTypeConflict,
    SiteStructureMismatch,
    OwnerDisabled,
    SiteDisabled,
    Disabled
}
```
* <span class="text-monospace">Offline</span> – while the endpoint is not connected to the site, the service has this status;
* <span class="text-monospace">Online</span> – the service is ready to serve clients;
* <span class="text-monospace">ServiceTypeConflict</span> – the Service Type and Contract Author provided by the service are different from those declared on the site;
* <span class="text-monospace">SiteStructureMismatch</span> – there is no conflict with Service Type and Contract Author but the site structure provided by the service is different from the structure that has been used to build the site; 
* <span class="text-monospace">OwnerDisabled</span> – the service owner is disabled by the Softnet MS administrator;
* <span class="text-monospace">SiteDisabled</span> – the site on which the service is registered is disabled by the owner.
* <span class="text-monospace">Disabled</span> – the service is disabled by the owner;

---
#### TABLE OF CONTENTS
* [11.1. Interface ServiceEventListener]({{ site.baseurl }}{% link docs/service-platform-events/interface-service-event-listener.md %})
* [11.2. Implementing event handlers]({{ site.baseurl }}{% link docs/service-platform-events/implementing-event-handlers.md %})
	* [11.2.1. Connectivity status event handler]({{ site.baseurl }}{% link docs/service-platform-events/impl/connectivity-status-event-handler.md %})
	* 11.2.2. Service status event handler
	* [11.2.3. Hostname change event handler]({{ site.baseurl }}{% link docs/service-platform-events/impl/hostname-change-event-handler.md %})
	* [11.2.4. User Membership event handlers]({{ site.baseurl }}{% link docs/service-platform-events/impl/user-membership-event-handlers.md %})
	* [11.2.5. Persistence event handler]({{ site.baseurl }}{% link docs/service-platform-events/impl/persistence-event-handler.md %})
* [11.3. Adapter class ServiceEventAdapter]({{ site.baseurl }}{% link docs/service-platform-events/class-service-event-adapter.md %})