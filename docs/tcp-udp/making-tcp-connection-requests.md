---
layout: default
title: 15.2. Making TCP connection requests
parent: 15. TCP and UDP in dynamic IP environments
nav_order: 2
---

## 15.2. Making TCP connection requests

This section describes the technique of making TCP connection requests. The client sends a request to the virtual port on which the remote service is listening. Before making a request, it is desirable to check whether the remote service is online – for this, the <span class="datatype">RemoteService</span> has a method <span class="method">isOnline</span>. This is not required if your application sends a request at receiving the service online event for the target service (see the section "[Interface ClientEventListener]({{ site.baseurl }}{% link docs/client-platform-events/interface-client-event-listener.md %})").  

Let's see the method for making connection requests:
```java
public void tcpConnect(
    RemoteService remoteService,
    int virtualPort,
    TCPOptions tcpOptions,
    TCPResponseHandler responseHandler, 
    RequestParams requestParams)
```
*	<span class="param">remoteService</span> is the first parameter of any request method of the client endpoint. Before making a request, your app can check the online status and the version of the service's primary API against it. Note that a single-service endpoint has overloaded request methods without this parameter;
*	<span class="param">virtualPort</span> is a virtual port on which the remote service is listening;
*	<span class="param">tcpOptions</span> specifies the receive and send buffer sizes that will be assigned to the TCP connection at the client side;
*	<span class="param">responseHandler</span> is an implementation of the <span class="datatype">TCPResponseHandler</span> interface that client app provides to the method. It is discussed below;
*	<span class="param">requestParams</span> is an optional parameter whose structure is shown next.  

<span class="datatype">RequestParams</span> is a unified structure that clients use to specify additional parameters for any request method of the platform. It has three fields:
```java
public class RequestParams {
	public final Object attachment;
	public final int waitSeconds; 
	public final SequenceEncoder sessionTag;
}
```
*	<span class="field">attachment</span> contains any attached data you want to pass to the respone handler;
*	<span class="field">waitSeconds</span> is the wait timeout after which the request method completes with an exception of type <span class="exception">TimeoutExpiredSoftnetException</span>. Its default value is zero, which sets the default timeout value to 30 seconds;
*	<span class="field">sessionTag</span> is used to provide information about the session in the context of which the request will be made. The data size is limited to 128 bytes.  

Next to consider is the <span class="datatype">TCPResponseHandler</span> interface:
```java
public interface TCPResponseHandler {
	void onSuccess(ResponseContext context, SocketChannel socketChannel, ConnectionMode mode);
	void onError(ResponseContext context, SoftnetException exception);
}
```
The <span class="method">onSuccess</span> method is invoked if the request is successfully processed and the connection is established. Let’s see its parameters:
*	<span class="param">context</span> is of type <span class="datatype">ResponseContext</span>, which is the first parameter of any response handler of the platform. The type is described below;
*	<span class="param">socketChannel</span> is a Java NIO SocketChannel object that represents the established TCP connection;
*	<span class="param">mode</span> provides the mode of the connection – P2P or Proxy.  

The <span id="response-context" class="datatype">ResponseContext</span> class has the following fields:
```java
public class ResponseContext {
    public final ClientEndpoint clientEndpoint;
    public final RemoteService remoteService;
    public final Object attachment;	
}
```
*	<span class="field">clientEndpoint</span> is the client endpoint that made the connection request;
*	<span class="field">remoteService</span> represents a remote service to which the request has been made;
*	<span class="field">attachment</span> is the state data passed through the tcpConnect call.  

The <span id="on-error" class="method">onError</span> method of <span class="datatype">TCPResponseHandler</span> is invoked if the connection request fails. The second parameter of type <span class="exception">SoftnetException</span> specifies an error. 
Possible exceptions are listed below:
*	<span class="exception">ServiceOfflineSoftnetException</span> – the remote service is offline;
*	<span class="exception">ConnectionAttemptFailedSoftnetException</span> – the connection attempt failed. The details are provided in the exception message;
*	<span class="exception">AccessDeniedSoftnetException</span> – the client does not have enough permissions to establish this connection;
*	<span class="exception">PortUnreachableSoftnetException</span> – the remote service is not listening on the virtual port specified in the connection request; 
*	<span class="exception">ServiceBusySoftnetException</span> – the server's backlog of pending connections is full;
*	<span class="exception">TimeoutExpiredSoftnetException</span> – the connection request timeout expired.  

Below is an example of using the described method to make a TCP connection request. The client endpoint is assumed to be single-service. The <span class="datatype">RemoteService</span> object is retrieved by calling <span class="method">findService(0)</span> and used as the first argument to the <span class="method">tcpConnect</span> call. Before calling it, the code checks whether the service is online. The second argument is the virtual port, specified as 10. The remote service is expected to be listening on this port as it was demonstrated in the previous section. The example implements the <span class="datatype">TCPRequestHandler</span> interface and uses it as a fourth argument to the method call:
```java
public static void main(String[] args) {
    // the client endpoint creation code is omitted
    if(softnetClient.isSingleService()) {
        RemoteService remoteService = softnetClient.findService(0);
        if(remoteService.isOnline()) {
            softnetClient.tcpConnect(remoteService, 10, null, new MyTCPResponseHandler());
        }
    }
    // the rest of the code
}

// custom implementation of the TCPResponseHandler interface
class MyTCPResponseHandler implements TCPResponseHandler {
    public void onSuccess(ResponseContext context, SocketChannel socketChannel, ConnectionMode mode) {
        System.out.println(String.format(
            "The TCP connection has been established with
            the service hosted on '%s'. 
            The connection mode is '%s'.", context.remoteService.getHostname(),
            mode));
        // the rest of the code
    }
    public void onError(ResponseContext context, SoftnetException exception) {
        System.out.println(String.format(
            "The TCP connection attempt with the service
            hosted on '%s' has failed with an error '%s'.",
            context.remoteService.getHostname(),
            exception.getMessage()));	
    }
}
```

---
#### TABLE OF CONTENTS
* [15.1. Handling TCP connection requests]({{ site.baseurl }}{% link docs/tcp-udp/handling-tcp-connection-requests.md %})
* 15.2. Making TCP connection requests
* [15.3. Handling UDP connection requests]({{ site.baseurl }}{% link docs/tcp-udp/handling-udp-connection-requests.md %})
* [15.4. Making UDP connection requests]({{ site.baseurl }}{% link docs/tcp-udp/making-udp-connection-requests.md %})