---
layout: default
title: 17.2. Event delivery model
parent: 17. Pub/Sub Events
nav_order: 2
---

## 17.2. Event delivery model

Typically, a Softnet service raises an event in response to a change in the state of the electromechanical device hosting the service. This can happen regardless of whether or not the service is connected to the site. If the service is not connected, the event cannot be sent to the broker at that time. Depending on the implementation and current settings, the service endpoint can persist events until the connection is reestablished and then deliver them to the broker. On the other hand, a client subscribed to the event may also be disconnected from the site when a new event is received by the broker. However, the broker can deliver the event later when the client connects to the site. A client endpoint does not persist events, unlike a service endpoint, but only information about the last events received. On the broker, each event is persisted in a queue for the lifetime of the event or until it is pushed out by a new received event. An ability of the platform to store events while network failures or endpoint disconnections is called event persistence. The persistence mechanism of the service endpoint is called **Service Persistence**. It can have one or two levels depending on the underlying platform. Each level implements the <span class="datatype">ServicePersistence</span> interface. Accordingly, the persistence mechanism of the client endpoint is called **Client Persistence**. It can also have one or two levels depending on the underlying platform. Each level implements the <span class="datatype">ClientPersistence</span> interface.  

The level of persistence directly affects the quality of event delivery. Ideally, each event raised by the service must be delivered to the broker once and only once. And further, each event stored on the broker must be delivered to each subscribed client once and only once. However, there are variations depending on the persistence level of both communicating parts – the service and the client. In the following sections, we'll take a closer look at endpoint persistence mechanisms.

---
#### TABLE OF CONTENTS
* [17.1. Basic features]({{ site.baseurl }}{% link docs/pub-sub-events/basic-features.md %})
* 17.2. Event delivery model
* [17.3. Service Persistence]({{ site.baseurl }}{% link docs/pub-sub-events/service-persistence.md %})
* [17.4. Client Persistence]({{ site.baseurl }}{% link docs/pub-sub-events/client-persistence/index.md %})
* [17.5. Setting up the service persistence]({{ site.baseurl }}{% link docs/pub-sub-events/setting-service-persistence.md %})
* [17.6. Setting up the client persistence]({{ site.baseurl }}{% link docs/pub-sub-events/setting-client-persistence.md %})
* [17.7. Raising Replacing events]({{ site.baseurl }}{% link docs/pub-sub-events/raising-replacing-events.md %})
* [17.8. Handling Replacing events]({{ site.baseurl }}{% link docs/pub-sub-events/handling-replacing-events.md %})
* [17.9. Raising Queueing events]({{ site.baseurl }}{% link docs/pub-sub-events/raising-queueing-events.md %})
* [17.10. Handling Queueing events]({{ site.baseurl }}{% link docs/pub-sub-events/handling-queueing-events.md %})
* [17.11. Raising Private events]({{ site.baseurl }}{% link docs/pub-sub-events/raising-private-events.md %})
* [17.12. Handling Private events]({{ site.baseurl }}{% link docs/pub-sub-events/handling-private-events.md %})