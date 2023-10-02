---
layout: default
title: 12.2.3. User properties change event handler
parent: 12.2. Implementing event handlers
grand_parent: 12. Client-specific platform events
nav_order: 3
---

## 12.2.3. User properties change event handler

The handler signature: 
```java
void onUserUpdated(ClientEndpointEvent e)
```

In the handler’s body, call the <span class="method">getEndpoint</span> method on the 'e' parameter. It returns the endpoint that invoked the handler. Then call the endpoint’s <span class="method">getUser</span> method which has the following signature:
```java
public MembershipUser getUser()
```
Using the <span class="datatype">MembershipUser</span> API, you can check if the user is a named user with certain permissions, Guest or Stateless Guest. The example below demonstrates the handler implementation:
```java
public void onUserUpdated(ClientEndpointEvent e) {
    ClientEndpoint clientEndpoint = e.getEndpoint();
    MembershipUser user = clientEndpoint.getUser();
    System.out.println(String.format("Membership User: %s ", user.getName()));
    if(user.hasRoles()) {
        System.out.println("Roles:");
        for(String role: user.getRoles())
            System.out.println("  " + role);
    }			
}
```

---
#### TABLE OF CONTENTS
* [12.1. Interface ClientEventListener]({{ site.baseurl }}{% link docs/client-platform-events/interface-client-event-listener.md %})
* [12.2. Implementing event handlers]({{ site.baseurl }}{% link docs/client-platform-events/implementing-event-handlers.md %})
    * [12.2.1. Connectivity status event handler]({{ site.baseurl }}{% link docs/client-platform-events/impl/connectivity-status-event-handler.md %})
    * [12.2.2. Client status event handler]({{ site.baseurl }}{% link docs/client-platform-events/impl/client-status-event-handler.md %})
    * 12.2.3. User properties change event handler
    * [12.2.4. Service Group event handlers]({{ site.baseurl }}{% link docs/client-platform-events/impl/service-group-event-handlers.md %})
    * [12.2.5. Persistence event handler]({{ site.baseurl }}{% link docs/client-platform-events/impl/persistence-event-handler.md %})
* [12.3. Adapter class ClientEventAdapter]({{ site.baseurl }}{% link docs/client-platform-events/class-client-event-adapter.md %})