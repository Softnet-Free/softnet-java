---
layout: default
title: 12.2.5. Persistence event handler
parent: 12.2. Implementing event handlers
grand_parent: 12. Platform events related to clients
nav_order: 5
---

## 12.2.5. Persistence event handler

This handler intercepts error notifications raised by the client persistence component. Apart from notifying the user or logging errors, there is no way for an application to handle this error. The handler signature:
```java
void onPersistenceFailed(ClientPersistenceFailedEvent e)
```

Parameter 'e' contains a field <span class="field">exception</span>. It can be an object of one of the following exception classes derived from <span class="exception">PersistenceSoftnetException</span>:
*	<span class="exception">PersistenceIOSoftnetException</span> – CPL-2 (client persistence level 2) encountered an I/O error and the endpoint switched to CPL-1 (client persistence level 1);
*	<span class="exception">PersistenceDataFormatSoftnetException</span> – CPL-2 encountered a data format error when reading stored information about events already received. This error causes the storage to be cleared. The endpoint continues to use CPL-2;

---
#### TABLE OF CONTENTS
* [12.1. Interface ClientEventListener]({{ site.baseurl }}{% link docs/client-platform-events/interface-client-event-listener.md %})
* [12.2. Implementing event handlers]({{ site.baseurl }}{% link docs/client-platform-events/implementing-event-handlers.md %})
    * [12.2.1. Connectivity status event handler]({{ site.baseurl }}{% link docs/client-platform-events/impl/connectivity-status-event-handler.md %})
    * [12.2.2. Client status event handler]({{ site.baseurl }}{% link docs/client-platform-events/impl/client-status-event-handler.md %})
    * [12.2.3. User properties change event handler]({{ site.baseurl }}{% link docs/client-platform-events/impl/user-properties-change-event-handler.md %})
    * [12.2.4. Service Group event handlers]({{ site.baseurl }}{% link docs/client-platform-events/impl/service-group-event-handlers.md %})
    * 12.2.5. Persistence event handler
* [12.3. Adapter class ClientEventAdapter]({{ site.baseurl }}{% link docs/client-platform-events/class-client-event-adapter.md %})