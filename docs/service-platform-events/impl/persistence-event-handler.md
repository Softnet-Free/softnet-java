---
layout: default_no_content
title: 11.2.5. Persistence event handler
parent: 11.2. Implementing event handlers
grand_parent: 11. Platform events related to services
nav_order: 5
---

## 11.2.5. Persistence event handler

This handler intercepts error notifications raised by the service persistence component. Apart from notifying the user or logging errors, there is no way for an application to handle this error. The handler signature:
```java
void onPersistenceFailed(ServicePersistenceFailedEvent e)
```
Parameter 'e' of type <span class="datatype">ServicePersistenceFailedEvent</span> contains a field <span class="field">exception</span>. It can be an instance of one of the following exception classes that derive from <span class="datatype">PersistenceSoftnetException</span>:
*	<span class="exception">PersistenceIOSoftnetException</span> – SPL-2 (service persistence level 2) encountered an I/O error and the endpoint switched to SPL-1 (service persistence level 1);
*	<span class="exception">PersistenceDataFormatSoftnetException</span> – SPL-2 encountered a data format error when reading events stored in storage. This error causes the storage to be cleared. The endpoint continues to use SPL-2;
*	<span class="exception">PersistenceStorageFullSoftnetException</span> – an attempt to serialize a new event into persistence storage caused it to overflow. As a result, the oldest serialized events are removed to make room.

---
#### TABLE OF CONTENTS
* [11.1. Interface ServiceEventListener]({{ site.baseurl }}{% link docs/service-platform-events/interface-service-event-listener.md %})
* [11.2. Implementing event handlers]({{ site.baseurl }}{% link docs/service-platform-events/implementing-event-handlers.md %})
	* [11.2.1. Connectivity status event handler]({{ site.baseurl }}{% link docs/service-platform-events/impl/connectivity-status-event-handler.md %})
	* [11.2.2. Service status event handler]({{ site.baseurl }}{% link docs/service-platform-events/impl/service-status-event-handler.md %})
	* [11.2.3. Hostname change event handler]({{ site.baseurl }}{% link docs/service-platform-events/impl/hostname-change-event-handler.md %})
	* [11.2.4. User Membership event handlers]({{ site.baseurl }}{% link docs/service-platform-events/impl/user-membership-event-handlers.md %})
	* 11.2.5. Persistence event handler
* [11.3. Adapter class ServiceEventAdapter]({{ site.baseurl }}{% link docs/service-platform-events/class-service-event-adapter.md %})