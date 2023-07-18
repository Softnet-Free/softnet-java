---
layout: default
title: 5. Softnet as a network of software endpoints
nav_order: 5
---

## 5. Softnet as a network of software endpoints

Softnet is all about client-service interaction. We use the terms **Service** and **Client** to refer to the software representation of a networked device and a client that consumes the device, respectively. The name Softnet itself comes from the words **soft**ware **net**work. Softnet defines **Service Endpoint** as an abstraction that enables service applications to employ networking functionality, while **Client Endpoint** is an abstraction defined for client applications for the same purpose. An application instantiates the <span class="datatype">ServiceEndpoint</span> class to create a [service endpoint]({{ site.baseurl }}{% link docs/service-endpoint.md %}). And to create a [client endpoint]({{ site.baseurl }}{% link docs/client-endpoint.md %}), an application instantiates one of the following two classes: <span class="datatype">ClientEndpoint</span> or <span class="datatype">ClientSEndpoint</span>. The first one implements a client endpoint to interact with multiple services of the same type. It is called a multi-service client endpoint. In turn, <span class="datatype">ClientSEndpoint</span> implements a client endpoint to interact with a single service. It is called a single-service client endpoint.
