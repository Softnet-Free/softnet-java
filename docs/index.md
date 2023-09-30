---
layout: default
title: Softnet Programming Model in Java
nav_order: 0
permalink: /
---

# Softnet Programming Model in Java
{: .fs-7 }

by Robert Koifman  

## Abstract

This document guides you through the process of developing IoT applications using Softnet Endpoint Library (Java). It explains how to use Softnet in developing client/server communication scenarios as well as event-driven M2M scenarios. The platform makes the TCP and UDP protocols available for use in dynamic IP environments. Softnet offers Remote Procedure Calls and Pub/Sub Events as additional remote IPC mechanisms. The Softnet mechanism of Access Control can be used to implement different levels of access to devices, and a mechanism called Service Status Detection provides clients with information about the connectivity status of remote devices. Using Softnet, you can concentrate on the application’s logic without concerning about networking issues. 

Softnet Endpoint Library (Java) has a dependancy from [Softnet ASN.1 Codec (Java)](https://github.com/softnet-free/asn1codec-java){:target="_blank"}{:rel="noopener noreferrer"}. The platform uses it to encode/decode messages in the ASN.1 DER format for transmission over a network. You can also use this tool to compactly represent complex hierarchical data for transmission over a network, although you can use any other format, such as JSON. [The Developer Guide to Softnet ASN.1 Codec (Java)](https://softnet-free.github.io/asn1codec-java){:target="_blank"}{:rel="noopener noreferrer"} explains how to use this codec in the application development.  

Softnet Free offers a comprehensive solution for managing IoT projects, services, clients, users, permissions, and contacts. The platform supports shared access to devices with other persons/organizations, allowing owners to establish user-to-user or business-to-consumer relationships. Designed for both users and developers, [The User Guide to Softnet Management System](https://softnet-free.github.io/softnet-ms){:target="_blank"}{:rel="noopener noreferrer"} contains a detailed explanation of this solution.  

The executable jar files of the "Softnet Endpoint Library (Java)" and "Softnet ASN.1 Codec (Java)" libraries can be dowloaded from the [releases](https://github.com/Softnet-Free/softnet-java/releases) page. However, it is recommended to use executables generated on the target platform instead, because, as practice shows, a jar file created on one platform may not work on another one.