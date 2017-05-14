# TUIO Mouse Driver
This is small code examples of how to drive a mouse using a [TUIO](http://tuio.org/) device.
 1. One finger is for mouse move.
 2. Two figers is for left-click.  Mouse down is maintained while the second finger is pressed.
 3. Three fingers is right-click.  Mouse up is immediately called, no need to keep finger down.

### Execution
To use the mouse driver:
 1. git clone https://github.com/artistech-inc/tuio-mouse-driver.git
 2. cd tuio-mouse-driver
 3. git checkout v1.1.3
 4. mvn package
 5. java -jar target/tuio-mouse-driver-1.1.3.jar

## ZeroMQ Support
Also available in this module is the ability to subscribe to TUIO broadcasts via [ZeroMQ](http://zeromq.org/).

### Dependencies
ZeroMQ support is dependent on available native libraries.  When compiling, maven will search for these files and provide any jar dependencies suitable.
 1. Linux:
   1. Searches for /usr/lib/libjzmq.so
   2. If this file exists, the dependency jar [jzmq.jar](https://github.com/zeromq/jzmq) is imported.
   3. If this file is missing, the dependency jar [jeromq.jar](https://github.com/zeromq/jeromq) is imported.
 2. Mac OS X:
   1. Searches for /usr/lib/libjzmq.dynlib
   2. If this file exists, the dependency jar [jzmq.jar](https://github.com/zeromq/jzmq) is imported.
   3. If this file is missing, the dependency jar [jeromq.jar](https://github.com/zeromq/jeromq) is imported.

The two jar files provide identical support.  However, the [jzmq.jar](https://github.com/zeromq/jzmq) file uses JNI to provide faster support where [jeromq.jar](https://github.com/zeromq/jeromq) is a pure java implementation.  The jzmq.jar requires libjzmq.so which in turn requires libzmq.so to be available.

### ZeroMQ Transmission/Serialization
Transmission of the TUIO objects via ZeroMQ is provided by 3 different mechanisms.
 1. Java Object Serialization
 2. JSON Serialization (using [Jackson](https://github.com/FasterXML/jackson))
 3. [Google Protocol Buffer](https://developers.google.com/protocol-buffers/)

Since this is the subscribing client, it is unknown how the object has been serialized by the publisher, and so all 3 mechanisms will be attempted for deserialization.

### Execution
To use the ZeroMqMouse driver:
 1. git clone https://github.com/artistech-inc/tuio-mouse-driver.git
 2. cd tuio-mouse-driver
 3. git checkout v1.1.3
 4. mvn package
 5. java -cp target/tuio-mouse-driver-1.1.3.jar com.artistech.tuio.mouse.ZeroMqMouse -z &lt;ZMQ_PUB_HOST:PORT&gt;

If using the [companion tuio-zeromq-publish application](https://github.com/artistech-inc/tuio-zeromq-publish), the default port used is 5565, so invokation would look similar to:
>java -cp target/tuio-mouse-driver-1.1.3.jar com.artistech.tuio.mouse.ZeroMqMouse -z localhost:5565
