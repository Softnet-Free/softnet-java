---
layout: default
title: 1. Requirements for the network infrastructure
nav_order: 1
---

## 1. Requirements for the network infrastructure

Softnet exploits standard TCP/IP socket functionality without resorting to raw socket operations prohibited in most end-user operating systems. The platform does not impose special requirements on the network infrastructure. The only requirement is that outbound connections to TCP ports 7737, 7740, 7778, 7779, 37780-38259 and UDP port 7779 must not be blocked by firewalls. Inter-stack TCP and UDP connections over IPv4/IPv6 are supported without the need for special IPv4/IPv6 transition equipment.