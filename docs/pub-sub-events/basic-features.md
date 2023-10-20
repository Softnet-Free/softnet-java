---
layout: default
title: 17.1. Basic features
parent: 17. Pub/Sub Events
nav_order: 1
---

## 17.1. Basic features

Softnet supports three categories of events: Replacing, Queueing, and Private. Events must be defined in the Site Structure, where, along with the category, you specify parameters such as name, lifetime, max queue size and access rule. This technique is described in the section "[Defining service events]({{ site.baseurl }}{% link docs/site/service-events.md %})". An event broker hosted on the site implements a queue for each event defined in the Site Structure. The queue of Replacing events can contain only one last event. Each new event replaces the old one. In contrast, each new Queueing event joins the queue of previously received instances. If the queue is full when a new event is received, the oldest one is removed from the tail to make room. The maximum size of a Queueing event queue is specified in the event definition. The queue of Private events works in a similar way. Its maximum size is fixed at 1000. Note that if the site is multi-service, then each queue is created separately for each service.  

An event can have arguments. For this purpose, event classes have an <span class="field">arguments</span> field of the <span class="datatype">SequenceEncoder</span> type, where the type is defined in Softnet ASN.1 Codec, through which services can pass data organized in complex hierarchical structures to subscribers. Accordingly, on the receiving side, events have an <span class="field">arguments</span> field of type <span class="datatype">SequenceDecoder</span>. The data size in <span class="field">arguments</span> is limited to 2 kilobytes. Before raising an event, an application can check the size by calling the <span class="method">getSize</span> method on <span class="field">arguments</span>.  

On the client side, each event has two additional properties that can be useful in event handling. These are <span class="field">age</span> and <span class="field">createdDate</span>. In the <span class="field">age</span> property, Softnet specifies the time elapsed since the event has been received by the broker. This value is zero if the event is sent to the client without delay as soon as it is received by the broker. You can use <span class="field">age</span>, for example, to ignore events that are older than a certain age. In the <span class="field">createdDate</span> property, Softnet specifies the date and time when the event has been received by the broker. Note that Softnet uses the timeline of the Softnet server, not the service application that generated the event, to assign values to both properties.  

Pub/Sub Events is a quite complex mechanism. To use it correctly, it is desirable that you understand what event persistence is and how it affects the quality of event delivery. The next sections describe this issue in detail.

---
#### TABLE OF CONTENTS
* 17.1. Basic features
* [17.2. Event delivery model]({{ site.baseurl }}{% link docs/pub-sub-events/delivery-model.md %})
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