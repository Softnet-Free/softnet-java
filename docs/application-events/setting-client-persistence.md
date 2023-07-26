---
layout: default
title: 17.6. Setting up the client persistence
parent: 17. Application Events
nav_order: 6
---

## 17.6. Setting up the client persistence

Unlike in the case with services, the client persistence is set to level 1 by default. If you want to set it to the layer 2 (CPL-2), you must do so before the initial call to the endpoint's <span class="method">connect</span> method. The <span class="datatype">ClientEndpoint</span> class has a method <span class="method">setPersistenceL2</span> to set CPL-2:
```java
public void setPersistenceL2()
```

The storage file will be created in the directory where the Softnet library file "softnet.jar" is located. The platform uses the following scheme for naming client persistence storage files:
```
softnet.client.persistence_<serverAddress>_<clientKey>.scp
```

The platform takes the values of &lt;serverAddress&gt; and &lt;clientKey&gt; from the client URI provided to the endpoint, which has the following form:
```
<clientScheme>://<clientKey>@<serverAddress>
```

The second overload of the <span class="method">setPersistenceL2</span> method sets the persistence with a different location of the storage file:
```java
public void setPersistenceL2(String fileBasedStorageDirectory)
```

There is a third overload of the <span class="method">setPersistenceL2</span> method designed to set a custom implementation of CPL-2:
```java
public void setPersistenceL2(ClientPersistence clientPersistence)
```

This was the last of the sections on event persistence and quality of event delivery. The following sections of the chapter describe the technique for raising and handling application events.

---
#### TABLE OF CONTENTS
* [17.1. Basic features]({{ site.baseurl }}{% link docs/application-events/basic-features.md %})
* [17.2. Event delivery model]({{ site.baseurl }}{% link docs/application-events/delivery-model.md %})
* [17.3. Service Persistence]({{ site.baseurl }}{% link docs/application-events/service-persistence.md %})
* [17.4. Client Persistence]({{ site.baseurl }}{% link docs/application-events/client-persistence/index.md %})
* [17.5. Setting up the service persistence]({{ site.baseurl }}{% link docs/application-events/setting-service-persistence.md %})
* 17.6. Setting up the client persistence
* [17.7. Raising Replacing events]({{ site.baseurl }}{% link docs/application-events/raising-replacing-events.md %})
* [17.8. Handling Replacing events]({{ site.baseurl }}{% link docs/application-events/handling-replacing-events.md %})
* [17.9. Raising Queueing events]({{ site.baseurl }}{% link docs/application-events/raising-queueing-events.md %})
* [17.10. Handling Queueing events]({{ site.baseurl }}{% link docs/application-events/handling-queueing-events.md %})
* [17.11. Raising Private events]({{ site.baseurl }}{% link docs/application-events/raising-private-events.md %})
* [17.12. Handling Private events]({{ site.baseurl }}{% link docs/application-events/handling-private-events.md %})