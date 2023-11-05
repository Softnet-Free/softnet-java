# Softnet Endpoint Library (Java)

This is the Java source code for Softnet Endpoint Library (Java). It runs on Linux, embedded Linux, Armbian, Raspbian, Android 5.0 and later versions. "[Softnet Programming Model in Java](https://github.com/Softnet-Free/softnet-java)" explains how to use the library in the embedded Java application development. The library has a dependency on [Softnet ASN.1 Codec (Java)](https://github.com/softnet-free/asn1codec-java).  

Softnet Endpoint Library (Java) utilizes data types and packages from Java SE 1.7. If your application is assumed to use the Java 1.7 or Java 1.8 runtime, you can also target this library to that runtime. In this case, to compile this library you need to comment out the code in 'src/module-info.java'. Otherwise, starting with Java 9, Java applications use Java Platform Module System (JPMS), and this code is required to declare module-related information and dependencies. You can also download executables compiled in Java 1.7 and java 9 from the [releases](https://github.com/Softnet-Free/softnet-java/releases) section of the repository.

Softnet Endpoint Library (Java) is free software. You can redistribute and/or modify it under the terms of the Apache License, Version 2.0.
