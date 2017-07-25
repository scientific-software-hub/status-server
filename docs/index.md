# Status Server User Manual
This document describes Status Server Tango User Interface (Commands and attributes available to the user and general use case guide line)

## Preface

Status Server collects data during the experiment.

Status Server is a Java application which supports Tango interface. So one can control it and obtain collected data through Tango using familiar tools. The main purpose of this server is to collect data in non-disturbing way. This means that this server will tolerate any exceptions occurred during the execution and simply writes “NA” (Not Available) if some value could not be read from the target server.

Operator configures which attributes of which target devices will be read in a simple xml file. Devices can be of both types: Tango and Tine. Attributes can be polled or updated by event. 

Figure 1. Status Server collected data and user requests.

Every value is linked with timestamp when is being read by Status Server. Therefore collected data forms a timeline representing the experiment. One can easily get a snapshot of values for every particular timestamp. See figure 1. This ability is used during the experiment when Control Server needs a snapshot before and after taking an image. Also Status Server can give away all collected data. This set of data is then stored in Nexus file. So the whole timeline of the experiment can be easily reproduced.

## Configuration

Status Server is configured via xml description in a configuration file and a properties file. The configuration file is located in {SS_HOME}/conf, properties file ­– in {SS_HOME}/bin. This section describes both files.

### Properties file

Currently the following properties are defined in the file:
 jacorb.poa.thread_pool_min=1 – defines minimum threads that will be used by CORBA for requests handling
 jacorb.poa.thread_pool_max=10 – defines maximum threads that will be used simultaneously by CORBA for requests handling
 persistent.threshold=1000000 – defines how many records StatusServer will keep in memory 
 persistent.delay=10 – defines how often (in seconds) StatusServer will flush in-mem values to the persistent storage (this won’t happen unless there are in-mem values at least as many as defined by persistent.threshold)
 persistent.root=/mnt/StatusServer/persistent – defines the root of the persistent storage. Under this root a data file will be created.

### Xml configuration

Root element is StatusServer. It has two attributes: server-name – defines server name specified in Tango DB for this Status Server and instance-name – defines instance specified in Tango DB for this Status Server. StatusServer contains a list of devices (devices element) and a list of embedded attributes (attributes element). 
Each embedded attribute (attribute element) has the following xml attributes:

  name – defines the name of the attribute. Should be valid Tango attribute name; 
  alias – defines alias for this attribute’s name;
  type – defines TangoDev data type.
  
StatusServer may have zero embedded attributes in this case xml configuration must have an empty attributes element.
Each device (device element) contains a name attribute which is fully qualified Tango or Tine device name, it also contains a list of attributes (attributes element). 

Each attribute (attribute element) contains the following attributes: 

  name – a name of the attribute;
  alias – an alias for the attribute’s name; 
  method – a method by which Status Server will obtain data from this attribute := poll|event;
  interpolation – defines method which will be used when user request data for a particular timestamp := last|nearest|linear; 
  delay – defines a rate at which Status Server will poll the attribute (> 20 for poll and 0 for event)
  precision – for numeric attributes defines the maximum delta between two values so as they are considered different, i.e. |x – y|>presicion, record y
  
On the figure 1 it is shown how these attributes affect Status Server behavior. Arc arrows show delay between polling. Assume user requests a data snapshot for time t1 and assume there are only two attributes are being polled (green and yellow). For green attribute last-interpolation is defined – it means that user receives the most actual value for the timestamp (a value which was collected just before the timestamp). Yellow demonstrates both nearest and linear-interpolations: if nearest is defined – user receives right value (it was collected at the time that is closer to the timestamp); if linear – Status Server will interpolate the value using the following formula: y=y0+((t-t0)(y1-y0))/t1-t0, where y0 – left value, y1 – right value, t – t1 on the figure, t0 – timestamp when Status Server received the left value, t1 – timestamp when Status Server received the right value.

#### Sample configuration:

## Status Server Tango Interface

> Important: Starting from version 1.0.1 it is mandatory to set up caching strategy on the client side, i.e. call method setSource with argument DEV on a StatusServer proxy object. See explanation below.

Each client is associated with a context on the server side. For users it is important to understand how this affects their calls to the server. Currently context holds the following references:

  useAliases (default:false)– defines whether output shows attribute aliases instead of names (see xml configuration section);
  encode (default:false)– defines whether output is encoded in Base64 and zipped;
  outputType (default:PLAIN)– defines form in which out is presented (JSON, PLAIN);
  lastTimestamp – holds last access timestamp. This one is used internally for updates attribute (see below);
  attributesGroup – holds current attribute group (see below) affects output so that it contains only attributes defined in the current group;

__ACHTUNG!__ The following limitation may not apply to version newer than 1.0.1

__Limitation__: as for version JTango-1.0.2-alpha which is used by StatusServer-1.0.1 there are three problems with this approach:

  1 – default Tango cache strategy on the client violates client identity on the server (one client may see the context of another), this can be workaround by setting appropriate strategy on the client side: 
In IDL, first make sure that you are using IDL2TangoJavaClient (>= 1.0.0):
joDeviceProxy->setSource, 0
(see http://www.esrf.eu/computing/cs/tango/tango_doc/kernel_doc/ds_prog/node7.html#SECTION00713300000000000000); 
  
  2 – client identificates itself only through the command call, therefore first call to the StatusServer should be a command. In IDL it is enough to execute command “Status” just after proxy object is created:
joDeviceProxy->executeCommand, “Status”

  3 – clients identify them self by the process id. This means that all proxies created within one process (IDL for instance) share same context.
These limitations might be removed in the upcoming JTango releases.

### Attributes

__NOTE__: java data types are used in this section. The most important is that long is LONG64 in IDL.

  crtTimestamp:=long
this attribute defines current time in millis for StatusServer

  clientId:=String
gives current client id

  encode:=boolean
when client sets this attribute to true the result from reading data operations will be encoded in Base64 and then zip compressed. This attribute is client specific.

  outputType:=String{PLAIN,JSON}
setting this attribute to JSON or PLAIN the client defines in which format the output from the reading commands will be presented. 
PLAIN:
```#!xml
<attribute-name>\n@<writeTimestamp>[<value>@<readTimestamp>]\n…
```
JSON:
```#!json
{
<attribute-name>:[{
‘value’: <value>,
‘read’: <readTimestamp>,
‘write’: <writeTimestamp>
},…]
}
```

This attribute is client specific.

  meta:=String[]
this attribute contains information about actual data types of each attribute that is being stored by StatusServer. Typical output:
sys/tg_test/1/float_scalar->DevFloat
sys/tg_test/1/double_spectrum->DevVarDoubleArr
, i.e. <attribute-name>-><TangoDevType>

  data:=String[]
returns to the user data set currently held in memory. Output depends on the current context. Specifically on the encode and outputType. Assuming that encode = false and oututType = PLAIN returning array has the following structure: each element contains a full qualified attribute name and its stored values accompanied by two timestamps – when the attribute was written by Status Server and when it was read on remote server: @<writeTimestamp>[<value>@<readTimestamp>]. Values separated by ‘\n’ (new line symbol). Typical output example:
```
sys/tg_test/1/double_scalar ---- attribute name
@1344523280334[253.508677@1344523281208]
^-----------------------   writeTimestamp                         
@1344523282319[252.848274@1344523283305]
               ^--------            value
@1344523284301[252.110850@1344523285287]
                         ^- readTimestamp
```

  updates:=String[]
is similar to data, but it will contain only values that are collected since the last request. This is client specific, i.e. each client has its own lastTimestamp reference.

  useAliases:=boolean
defines whether aliases will be used in the output instead of full attribute names. This attribute is client specific.
Embedded attributes
These attributes are defined in xml configuration file and are accessible for reading/writing as regular Tango attributes. Also reading data commands include them in the output as any other target attribute.

### Commands


  createAttributesGroup(String[]):=Void, group:=String and groups:String[]
creates a group of attributes. Input parameter – a String array where first element is a group name and the following elements are fully qualified attribute names (as they appear in the configuration file). Created alias is then stored internally in Status Server. Current group name can be observed by reading attribute group. Client may switch between groups by setting the group. Attribute groups contains all groups created by this client. Example Usage:
Java:
```#!java
String alias = “dbl”;
String input = new String[]{alias,”tango://hzgharwi3:10000/sys/tg-test/0”};
Proxy.executeCommand(“createAttributesGroup”, input);
eraseData(Void):=Void
```
clears all currently held in memory data, this data is being flushed into persistent storage. This does not remove the most recent values, i.e. subsequent call to getLatestSnapshot won’t return empty result. Typically used between starts of an experiment.

  getDataRange(long[]):=String[]
parameter is a long array specifies timestamp bounds, i.e. first element – ‘from’ and the second – ‘to’. Command returns all the values that were collected between these bounds.

  getLatestSnapshot(Void):=String[]
Returns single value for each attribute. Output format is similar to getData. Output will contain only latest value for the moment Status Server receives the request.

  getSnapshot(long):=String[]
Input parameter specifies timestamp for which a snapshot will be returned. Timestamp is a String representation of the time in millis (Unix time, since 1970), i.e. “1344523286286”. Returns single value for each attribute actual for the timestamp (may be interpolated according to configuration). Output format is similar to getData.

  startLightPolling(Void):=Void
Status Server starts to collected data storing only the latest values. Using this method, users are able to receive latest data even when an experiment is not running. 
startLightPollingAtFixedRate(long):=Void
similar to the previous command except that Status Server polls data with the delay specified by the long parameter. 

  startCollectData(Void):=Void
Status Server starts to collect the data. It starts to poll and subscribes to events from attributes according to configuration. All data is stored with the following constraint: a new value is added only if it is not equal to a previous one and if a precision check is passed, i.e. assume y – previous value, x – new value: x is added if x != y and |y – x| > E (where E is configured precision). Constraint to this method – Status Server should not be in RUNNING state otherwise – exception.

  stopCollectData(Void):=Void
Status Server stops to collect the data. It stops to poll the attributes and unsubscribes from all events. All data is preserved until eraseData method is called. Constraint to this method – Status Server should in RUNNING state otherwise – exception.

Typical use case

Suppose one wants StatusServer to protocol a tomography experiment with an automated sample changer. Suppose each sample takes 5 minutes. And finally suppose that after 5 minutes of running StatusServer will collect ~1M records (values from all target attributes). In this case persistent properties in the properties file should look like:
```
persistent.threshold=1000000
#we take a little bit longer delay to prevent values being flushed during one sample being processed 
persistent.delay=330
persistent.root=./persistent
```

During each sample processing the client is free to get any data, and this is fast, as only in memory values are being accessed. After each sample the client should eraseData (flush the values into persistent storage). The whole data stack is then available in the {SS_ROOT}/persistent/data file.
