# Requirements

* Java 1.7 server environment
* Tango 8+

# User guide

Download [manual](https://bitbucket.org/hzgwpn/statusserver/downloads/StatusServerUserManual.pdf)

# How To ... 

## ... resolve dependencies (ver > 1.0.0)
goto {SS_ROOT}/devkit and execute install.bat(.sh)

for older versions manually download and install tine and TangoAPI jars. 

## ... build from source

There are still some tests that are strictly environment depended (e.g. TangoClientTest) to build the project simply turn off tests in maven: ```mvn package -Dmaven.test.skip=true```

## ... run StatusServer without Tango environment (ver < 1.0.0)

As the for version 1.8 there is no way to run StatusServer without Tango installed. At least programmatically. But it is possible to adjust code so it will start:

1. change the code of the Launcher.java (see below)
2. change the code of the StatusServerClass.java (see below)
3. execute {{{mvn clean package}}}
4. deploy
5. run with additional JVM parameter:  -DOAPort=56234 (see below full command)

In Launcher.java change main method:

```
#!java
Launcher.java
public static void main(String[] args) {
...
log.info("Initializing Tango framework...");
Util util = Util.init(new String[]{configuration.getInstanceName(),"-nodb","-dlist","development/local/0"}, configuration.getServerName());
util.add_class(configuration.getServerName());
Util.set_serial_model(TangoConst.NO_SYNC);
log.info("Done.");
...
}
```

In StatusServerClass.java comment ```write_class_property``` and ```get_class_property``` methods call in the constructor (```StatusServerClass(String s)```)

```
#!java
StatusServerClass.java
...
protected StatusServerClass(String s) throws DevFailed {
    super(s);

//        write_class_property();
//        get_class_property();
}
...
```

To run StatusServer use the following command:
```
java -DXmx1G -Dss.home={SS_HOME} -DOAPort=56234 wpn.hdri.ss.Launcher --config {PATH_TO_CONFIG}
```

# Benchmark tests 

## Server and client are running on the same machine.

Machine configuration:
* Intel(R) Core(TM) i5-2500 CPU @ 3.30GHz
* RAM 8Gb
* RAM&CPU clock //TODO
* java version "1.6.0_29"
* Java(TM) SE Runtime Environment (build 1.6.0_29-b11)
* Java HotSpot(TM) 64-Bit Server VM (build 20.4-b02, mixed mode)

The application started in -server mode

100K getLatestSnapshot were performed to warm up the JVM 

Results measured on 10K invocations of getLatestSnapshot:

```
Delta time in getLatestValues (nano) = 11746809173
Delta time in getLatestValues (millis) = 11746
Average time in getLatestValues (nano) = 1174680
Average time in getLatestValues (millis) = 1
Average time in getLatestValues (seconds) = 0
```

## Server and client are running on different machines connected with 1Gbit network.

Server machine configuration:

* Intel(R) Xeon(R) CPU E5620  @ 2.40GHz
* RAM 48Gb
* RAM&CPU clock //TODO
* java version "1.6.0_33"
* Java(TM) SE Runtime Environment (build 1.6.0_33-b03)
* Java HotSpot(TM) 64-Bit Server VM (build 20.8-b03, mixed mode)

The application started in -server mode.

100K getLatestSnapshot were performed to warm up the JVM 

Results measured on 10K invocations of getLatestSnapshot:

```
Delta time in getLatestValues (nano) = 71063988508
Delta time in getLatestValues (millis) = 71063
Average time in getLatestValues (nano) = 7106398
Average time in getLatestValues (millis) = 7
Average time in getLatestValues (seconds) = 0
```

Ping:
```
Pinging 131.169.65.240 with 32 bytes of data:
Reply from 131.169.65.240: bytes=32 time<1ms TTL=64
Reply from 131.169.65.240: bytes=32 time<1ms TTL=64
Reply from 131.169.65.240: bytes=32 time<1ms TTL=64
Reply from 131.169.65.240: bytes=32 time<1ms TTL=64

Ping statistics for 131.169.65.240:
    Packets: Sent = 4, Received = 4, Lost = 0 (0% loss),
Approximate round trip times in milli-seconds:
    Minimum = 0ms, Maximum = 0ms, Average = 0ms
```


## 1.0.10 Benchmark test:

The test was performed on a single host.

```
Result "test_getLatestSnapshot":
  1042.600 ±(99.9%) 2.616 ops/s [Average]
  (min, avg, max) = (575.350, 1042.600, 1724.124), stdev = 74.460
  CI (99.9%): [1039.984, 1045.216] (assumes normal distribution)


# Run complete. Total time: 02:36:29

Benchmark                                                   Mode   Cnt     Score    Error  Units
tango.ss.benchmark.SimpleBenchmark.test_getLatestSnapshot  thrpt  8780  1042.600 ±  2.616  ops/s
```