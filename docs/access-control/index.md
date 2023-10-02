---
layout: default
title: 9. Access Control
nav_order: 9
has_children: true
---

## 9. Access Control

As mentioned in the very beginning of this guide, Softnet offers two mechanisms, Access Control and Service Status Detection, which make the platform convenient for IoT development. To ensure this functionality, Softnet implements two dynamic structures: **User Membership** and **Service Group**, respectively. The former is used by services and the latter by clients. This section describes the Access Control mechanism and the User Membership API.  

The Softnet site implements a structure that contains permissions for domain users to access the services on the site. This structure is called User Membership and is managed by the administrator through the site management panel. When the service application connects to the site, Softnet loads the User Membership data into the service endpoint and keeps it synchronized with the site. Any change to the User Membership made on the site is immediately propagated to the application’s copy. Whenever the service application receives a request from one of the clients, Softnet provides the application with a MembershipUser object associated with that client. This object contains the user's authority information. The application can check if the user is allowed to access the requested resource. You can define access rules declaratively for the Softnet native request handling methods (see the section "[Declarative definition of access rules]({{ site.baseurl }}{% link docs/access-control/sections/declarative-access-rules.md %})") or pass the membership user to application layer protocols for more granular access control (see the section "[Fine-grained access control]({{ site.baseurl }}{% link docs/access-control/sections/fine-grained-access-control.md %})"). The former is applicable in definitions of TCP and UDP listeners and RPC procedures. It is also the only way to specify access rules for the service events when you define them in the site structure. In this case, access control will be applied to client subscriptions.
