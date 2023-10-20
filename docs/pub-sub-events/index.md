---
layout: default
title: 17. Pub/Sub Events
nav_order: 17
has_children: true
---

## 17. Pub/Sub Events

Interaction with a remote device is often triggered by an event on that device. This is called event-driven interaction. The popular eventing model in IoT is Pub/Sub. For example, it is employed by MQTT. Pub/Sub implements loose coupling between publishers and subscribers. That is, subscribers may have no idea who the publishers are. Softnet also implements the Pub/Sub model. Services publish events to the site, while subscribed clients can receive these events if they have sufficient permissions. But unlike the traditional model, clients know which service raised each event they received. This is because the set of events that a given service can raise is part of its interface contract. Use the Softnet eventing pattern to react to the state changes on the remote devices.