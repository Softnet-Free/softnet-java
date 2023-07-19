---
layout: default
title: 17.5. Setting up the service persistence
parent: 17. Application Events
nav_order: 5
---

## 17.5. Setting up the service persistence

The service persistence must be configured before making the initial call to any event-raising method or the connect method of the endpoint. All methods described in this section belong to the <span class="datatype">ServiceEndpoint</span> class. To set SPL-1 (service persistence level 1) with a default capacity of 16 kilobytes, call the following method:
```java
public void setPersistenceL1()
```

SPL-1 has a memory-based storage. If you want to set a different capacity of the storage, call the overload with a capacity parameter. Please note that the capacity of SPL-1 cannot be less than 4 kilobytes:
```java
public void setPersistenceL1(long memoryBasedStorageCapacity)
```

The next method sets SPL-2 (service persistence level 2) with a default capacity of 1 megabyte:
```java
public void setPersistenceL2()
```

The storage file will be created in the directory where the Softnet library file "softnet.jar" is located. The platform uses the following scheme for naming persistence storage files:
```
softnet.service.persistence_<serviceUuid>.ssp
```

The platform takes the value of &lt;serviceUuid&gt; from the service URI provided to the endpoint, which has the following format:
```
softnet-srv://<serviceUuid>@<serverAddress>
```

In order to set the persistence with a different capacity, or different location for the storage file, you have the following overload:
```java
public void setPersistenceL2(
    String fileBasedStorageDirectory,
    long fileBasedStorageCapacity,
    long memoryBasedStorageCapacity)
```

The first parameter takes a directory for the storage file. If you want to leave the default location, specify null. The second parameter, <span class="param">fileBasedStorageCapacity</span>, takes a capacity value. Again, if you want to leave the default value of 1 megabyte, specify 0. The minimum capacity of a file-based storage is 8 kilobytes. But the method also has a third parameter – <span class="param">memoryBasedStorageCapacity</span>, which specifies the SPL-1 capacity. It is included because the platform switches the endpoint to SPL-1 if SPL-2 encounters an I/O error during storage operations. To leave the default capacity, which is 16 kilobytes, specify 0. If you want, for example, to set SPL-2 with default storage directory, but specify the capacity of SPL-2 as 4 megabytes and capacity of SPL-1 as 64 kilobytes, you’d make the following call:
```java
serviceEndpoint.setPersistenceL2(null, 4194304, 65536);
```

Or, in a more elegant way:
```java
serviceEndpoint.setPersistenceL2(
    null, 
    MemorySize.fromM(4), 
    MemorySize.fromK(64));
```

At last, we have the third overload of the setPersistenceL2 method designed to set a custom implementation of SPL-2:
```java
public void setPersistenceL2(ServicePersistence servicePersistence, long memoryBasedStorageCapacity)
```

The first parameter is an implementation of the <span class="datatype">ServicePersistence</span> interface, and the second – capacity of SPL-1.

---
#### TABLE OF CONTENTS
* [17.1. Basic features]({{ site.baseurl }}{% link docs/application-events/basic-features.md %})
* [17.2. Event Persistence]({{ site.baseurl }}{% link docs/application-events/event-persistence.md %})
* [17.3. Service Persistence]({{ site.baseurl }}{% link docs/application-events/service-persistence.md %})
* [17.4. Client Persistence]({{ site.baseurl }}{% link docs/application-events/client-persistence/index.md %})
* 17.5. Setting up the service persistence
* [17.6. Setting up the client persistence]({{ site.baseurl }}{% link docs/application-events/setting-client-persistence.md %})
* [17.7. Raising Replacing events]({{ site.baseurl }}{% link docs/application-events/raising-replacing-events.md %})
* [17.8. Handling Replacing events]({{ site.baseurl }}{% link docs/application-events/handling-replacing-events.md %})
* [17.9. Raising Queueing events]({{ site.baseurl }}{% link docs/application-events/raising-queueing-events.md %})
* [17.10. Handling Queueing events]({{ site.baseurl }}{% link docs/application-events/handling-queueing-events.md %})
* [17.11. Raising Private events]({{ site.baseurl }}{% link docs/application-events/raising-private-events.md %})
* [17.12. Handling Private events]({{ site.baseurl }}{% link docs/application-events/handling-private-events.md %})