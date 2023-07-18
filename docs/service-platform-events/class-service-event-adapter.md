---
layout: default_no_content
title: 11.3. Adapter class ServiceEventAdapter
parent: 11. Platform events related to services
nav_order: 3
---

## 11.3. Adapter class ServiceEventAdapter

Interface <span class="datatype">ServiceEventListener</span> contains declarations for various categories of handlers. Each specific listener class may not require all of them. The <span class="datatype">ServiceEventAdapter</span> class has been introduced to relieve listener classes from the burden of implementing handlers they don’t need. The adapter class contains empty implementations of the interface methods. You can derive your listener class from the adapter and implement only those handlers that you need. Here is the adapter class definition:
```java
public class ServiceEventAdapter implements ServiceEventListener
{
    public void onConnectivityChanged(ServiceEndpointEvent e) {}
    public void onStatusChanged(ServiceEndpointEvent e) {}
    public void onHostnameChanged(ServiceEndpointEvent e) {}
    public void onUserIncluded(MembershipUserEvent e) {}
    public void onUserUpdated(MembershipUserEvent e) {}
    public void onUserRemoved(MembershipUserEvent e) {}
    public void onUsersUpdated(ServiceEndpointEvent e) {}
    public void onGuestAccessChanged(ServiceEndpointEvent e) {}
    public void onPersistenceFailed(ServicePersistenceFailedEvent e) {}
}
```

The following is an example of implementing a few of the handlers using an anonymous class derived from <span class="datatype">ServiceEventAdapter</span>:
```java
serviceEndpoint.addEventListener(new ServiceEventAdapter()
{
    @Override
    public void onConnectivityChanged(ServiceEndpointEvent e) {
        ServiceEndpoint serviceEndpoint = e.getEndpoint();
        EndpointConnectivity connectivity = serviceEndpoint.getConnectivity();
        System.out.println(
            String.format(
            "Connectivity Status: %s, Error: %s, Message: %s", 
            connectivity.status,
            connectivity.error,
            connectivity.message));
    }

    @Override
    public void onStatusChanged(ServiceEndpointEvent e) {
        ServiceEndpoint serviceEndpoint = e.getEndpoint();
        System.out.println(String.format("Service Status: %s", serviceEndpoint.getStatus()));		
    }

    @Override
    public void onUsersUpdated(ServiceEndpointEvent e) {
        System.out.println(String.format("Membership Users ------------"));
        ServiceEndpoint serviceEndpoint = e.getEndpoint();
        MembershipUser[] users = serviceEndpoint.getUsers();        
        for(MembershipUser user: users)
        {
            System.out.println(user.getName());
            for(String role: user.getRoles())
                System.out.println("  " + role);
        }
    }
}
```

---
#### TABLE OF CONTENTS
* [11.1. Interface ServiceEventListener]({{ site.baseurl }}{% link docs/service-platform-events/interface-service-event-listener.md %})
* [11.2. Implementing event handlers]({{ site.baseurl }}{% link docs/service-platform-events/implementing-event-handlers.md %})
	* [11.2.1. Connectivity status event handler]({{ site.baseurl }}{% link docs/service-platform-events/impl/connectivity-status-event-handler.md %})
	* [11.2.2. Service status event handler]({{ site.baseurl }}{% link docs/service-platform-events/impl/service-status-event-handler.md %})
	* [11.2.3. Hostname change event handler]({{ site.baseurl }}{% link docs/service-platform-events/impl/hostname-change-event-handler.md %})
	* [11.2.4. User Membership event handlers]({{ site.baseurl }}{% link docs/service-platform-events/impl/user-membership-event-handlers.md %})
	* [11.2.5. Persistence event handler]({{ site.baseurl }}{% link docs/service-platform-events/impl/persistence-event-handler.md %})
* 11.3. Adapter class ServiceEventAdapter