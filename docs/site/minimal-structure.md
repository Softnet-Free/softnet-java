---
layout: default
title: 6.1. Defining the minimal site structure
parent: 6. Site and Site Structure
nav_order: 1
---

## 6.1. Defining the minimal site structure

The development of a service application begins with defining the site structure. You define it programmatically in the <span class="datatype">SiteStructure</span> object, then bind it to the service endpoint. The site structure requires at least two parameters – the name of **Service Type** and the name of **Contract Author**. These parameters compose a named representation of the service interface contract, and serve as an identifier to refer to the specific functionality and behavior of the service. In addition, you can specify a version for the service's primary API, that is, an API which version information you want to deliver to clients via the Service Status Detection mechanism. Whenever the service comes online, this mechanism provides the version information to clients through the Service Online event. This allows each client to check whether it is compatible with this specific version of the service's primary API. Clients can perform the compatibility check before making any requests to the service. See the "[Service Status Detection]({{ site.baseurl }}{% link docs/service-group.md %})" chapter for details.  

In order to avoid naming collisions, the service type (it can sometimes be quite short string) is complemented with the contract author, the person or organization who designed the interface contract. Thus, two services have the same service type only if they have identical contract authors. In the site management panel these names are displayed as follows: &lt;service type&gt; (&lt;contract author&gt;), i.e., the name of the service type followed by the contract author's name in parentheses. For example, the service type can be the device’s model name plus generation number, and the contract author can be the manufacturer's name as follows: T-800 (Cyberdyne Systems). If you are a DIY developer, you can specify your project name and your own name as the service type and contract author respectively as follows: Home Thermostat (John Doe).  
 
But what does Softnet need these names for? Softnet assigns these names to the appropriate properties of the site when the site is constructed. It is supposed that together they compose a meaningful, concise and at the same time unique name for the service interface. This name serves three purposes. The first one – makes it impossible to connect to the site for a client that is not designed to consume this service. This is achieved by the fact that when connecting to the site, the client also provides the service type and the contract author of the service for which it is designed. And if they do not match with the site’s ones, the client fails. The second purpose – makes it impossible to connect to the site for a service that has these two properties different from the site’s ones. And the third purpose – on the management panel in Softnet MS, the owner can see what type of service is installed on a given site. This is why the name of the service type (complemented with the contract author) should be meaningful, albeit concise.  

The <span class="datatype">ServiceEndpoint</span> class has a static method to create the <span class="datatype">SiteStructure</span> object:
```java
public static SiteStructure createStructure(
    String serviceType,
    String contractAuthor)
```
The name length must be in the range [1, 256]. The code snippet below demonstrates the use of this method:
```java
SiteStructure siteStructure = ServiceEndpoint.createStructure(
    "Home Thermostat",	// service type
    "John Doe");	// contract author
```

---
#### TABLE OF CONTENTS
* 6.1. Defining the minimal site structure
* [6.2. Defining the guest support]({{ site.baseurl }}{% link docs/site/guest-support.md %})
* [6.3. Defining user roles]({{ site.baseurl }}{% link docs/site/user-roles.md %})
* [6.4. Defining service events]({{ site.baseurl }}{% link docs/site/service-events.md %})
* [6.5. How it works]({{ site.baseurl }}{% link docs/site/how-works.md %})