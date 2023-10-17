---
layout: default
title: 16. Remote Procedure Calls
has_children: true
nav_order: 16
---

## 16. Remote Procedure Calls

Most applications use application layer protocols based on TCP and UDP for client/server communication scenarios. However, some IoT tasks can be solved simply using a request-response pattern. In such cases, you can employ Softnet RPC instead of messing around with resource-intensive application layer protocols.  

In Softnet, all messages are in ASN.1 DER format. Applications can also take advantages of using Softnet ASN.1 Codec. Clients can pass data organized in complex hierarchical structures up to 64 kilobytes in size to the remote procedure. The service, in turn, can return the result also in ASN.1 DER format and up to 64 kilobytes in size. There is always a chance that an RPC request may result in an error. The handler can return structured information about the error in ASN.1 format. You can learn the specification for the ASN.1 codec in «[The Developer Guide to Softnet ASN.1 Codec (Java)](https://softnet-free.github.io/asn1codec-java){:target="_blank"}{:rel="noopener noreferrer"}».  

Note that apart from ASN.1, you can use any other format such as JSON to represent application data. In this case, the sending application puts the data serialized as a UTF-8 character string or byte array into the procedure's argument which is an ASN.1 sequence encoder. On the other side, the application fetchs data from the ASN.1 sequence decoder and deserialize it.