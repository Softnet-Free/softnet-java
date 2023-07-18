---
layout: default_no_content
title: 11. Platform events related to services
nav_order: 11
has_children: true
---

## 11. Platform events related to services

Softnet is a very dynamic network. The internal state and connectivity of the network endpoints can change all the time. The structure of the network itself is changed by network users. There is a set of events that the platform raises to notify endpoints of these changes. This chapter discusses platform events related to services.

---
#### TABLE OF CONTENTS
* [11.1. Interface ServiceEventListener]({{ site.baseurl }}{% link docs/service-platform-events/interface-service-event-listener.md %})
* [11.2. Implementing event handlers]({{ site.baseurl }}{% link docs/service-platform-events/implementing-event-handlers.md %})
	* [11.2.1. Connectivity status event handler]({{ site.baseurl }}{% link docs/service-platform-events/impl/connectivity-status-event-handler.md %})
	* [11.2.2. Service status event handler]({{ site.baseurl }}{% link docs/service-platform-events/impl/service-status-event-handler.md %})
	* [11.2.3. Hostname change event handler]({{ site.baseurl }}{% link docs/service-platform-events/impl/hostname-change-event-handler.md %})
	* [11.2.4. User Membership event handlers]({{ site.baseurl }}{% link docs/service-platform-events/impl/user-membership-event-handlers.md %})
	* [11.2.5. Persistence event handler]({{ site.baseurl }}{% link docs/service-platform-events/impl/persistence-event-handler.md %})
* [11.3. Adapter class ServiceEventAdapter]({{ site.baseurl }}{% link docs/service-platform-events/class-service-event-adapter.md %})