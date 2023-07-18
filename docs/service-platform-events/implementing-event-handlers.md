---
layout: default_no_content
title: 11.2. Implementing event handlers
parent: 11. Platform events related to services
has_children: true
nav_order: 2
---

## 11.2. Implementing event handlers

Some handlers have a parameter 'e' of type <span class="datatype">ServiceEndpointEvent</span>. Its only member of our interest is the <span class="method">getEndpoint</span> method with the following signature:
```java
public ServiceEndpoint getEndpoint()
```
The method returns an endpoint, which is of type <span class="datatype">ServiceEndpoint</span>, that invoked the handler. In such case, your application gets the updated object, that caused an event to be raised, from the endpoint as the handler’s parameter e does not contain it.  

Some of the handlers associated with Users Membership have a parameter e of type <span class="datatype">MembershipUserEvent</span> derived from <span class="datatype">ServiceEndpointEvent</span>. Along with a derived method <span class="method">getEndpoint</span>, it has a field <span class="field">user</span> of type <span class="datatype">MembershipUser</span>. This is a membership user that caused the event to be raised by the platform.  

Let's look at how to work with each of the event handlers declared by the <span class="datatype">ServiceEventListener</span> interface.

---
#### TABLE OF CONTENTS
* [11.1. Interface ServiceEventListener]({{ site.baseurl }}{% link docs/service-platform-events/interface-service-event-listener.md %})
* 11.2. Implementing event handlers
	* [11.2.1. Connectivity status event handler]({{ site.baseurl }}{% link docs/service-platform-events/impl/connectivity-status-event-handler.md %})
	* [11.2.2. Service status event handler]({{ site.baseurl }}{% link docs/service-platform-events/impl/service-status-event-handler.md %})
	* [11.2.3. Hostname change event handler]({{ site.baseurl }}{% link docs/service-platform-events/impl/hostname-change-event-handler.md %})
	* [11.2.4. User Membership event handlers]({{ site.baseurl }}{% link docs/service-platform-events/impl/user-membership-event-handlers.md %})
	* [11.2.5. Persistence event handler]({{ site.baseurl }}{% link docs/service-platform-events/impl/persistence-event-handler.md %})
* [11.3. Adapter class ServiceEventAdapter]({{ site.baseurl }}{% link docs/service-platform-events/class-service-event-adapter.md %})