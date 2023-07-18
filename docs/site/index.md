---
layout: default
title: 6. Site and Site Structure
nav_order: 6
has_children: true
---

## 6. Site and Site Structure

**Site** is a key abstraction in Softnet. The service owner creates a site to install one or multiple identical services and their clients. The site runs on the Softnet server and ensures interaction of connected endpoints. The site synchronizes states of the endpoints, communicates commands between them, receives events from the services and delivers them to the clients, transmits RPC requests and responses, controls the authority of users and associated clients, manages the endpoint accounts, etc. Each site has properties specific for a given service type. The service application defines these properties in a structure called **Site Structure**. When the service connects to a blank site for the first time, the service provides the structure to the Softnet server, and the server constructs the site according to these properties.
Softnet provides an implementation of the SiteStructure interface – an object that developers use to define the site structure.