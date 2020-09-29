
[![Build Status](https://api.travis-ci.com/Sdaas/karate-kafka.svg?branch=master)](https://travis-ci.com/Sdaas/karate-kafka)
 
## Introduction

Work In Progress

This project provides a library to test Kafka applications using KarateDSL. It provides a `KafkaProducer` and
a `KafkaConsumer` that can be called from a Karate feature. An example :

```cucumber
Feature: Karate-Kafka Demo

  Background:

    * def KafkaConsumer = Java.type('karate.kafka.KarateKafkaConsumer')
    * def KafkaProducer = Java.type('karate.kafka.KarateKafkaProducer')
    * def topic = 'test-topic'

  Scenario: Write strings to test-topic and read it back

    * def kc = new KafkaConsumer(topic)
    * def kp = new KafkaProducer()
    * kp.send(topic, "hello world")
    * kp.send(topic, "the_key", "hello again")

    * json output1 = kc.take()
    * json output2 = kc.take()

    * kp.close()
    * kc.close()

    # Doing the match
    * match output1 == { key : '#null', value : 'hello world' }
    * match output2 == { key : 'the_key', value : 'hello again' }
```
## Quick Demo

Start up a single-node Kafka cluster locally with a topic called `test-topic`. Running
`KarateTests` will invoke `src/test/java/karate/kafka/example.feature` which will attempt to
write a few messages to this topic and read it back. Finally, shut down the Kafka cluster.

```
$ ./startup.sh   
$ mvn test -Dtest=KafkaRunner  
$ ./teardown.sh  
```

## Documentation
### Using this in your project

Add the following to your `pom.xml` :
```xml
<dependency>
    <groupId>com.daasworld</groupId>
    <artifactId>karate-kafka</artifactId>
    <version>0.1.0</version>
</dependency>
```
and
```xml
<repositories>
    <repository>
        <id>karate-kafka</id>
        <url>https://raw.github.com/sdaas/karate-kafka/mvn-repo/</url>
    </repository>
</repositories>

```
### Kafka Producer

Creating a Kafka producer with the default properties ...

```cucumber
# Create Kafka Producer with the default properties
* def kp = new KafkaProducer()
```

The following default properties are used to create the producer. 

```
{
  "bootstrap.servers": "127.0.0.1:9092",
  "value.serializer": "karate.kafka.MyGenericSerializer",
  "key.serializer": "karate.kafka.MyGenericSerializer",
  "linger.ms": "20",
  "max.in.flight.requests.per.connection": "5",
  "batch.size": "32768",
  "enable.idempotence": "true",
  "compression.type": "snappy",
  "acks": "all"
}
```

The `karate.kafka.MyGenericSerializer` tries
to automatically guess the key/value type and attempts to serialize it as `Integer`, `Long`, `String`, or `JSON` 
based on the input. These default properties should work most of the time for testing, but you can always
override them ...

```cucumber
# Create Kafka Producer with the specified properties
* def prop = { ... } 
* def kp = new KafkaProducer(prop)

# Get the default Properties
* def props = KafkaProducer.getDefaultProperties()
```

Producing a message with or without a key
```cucumber
* kp.send(topic, "hello world")
* kp.send(topic, "the key", "hello again")
```

Producing a JSON message 
```cucumber
* def key = { ... }
* def value= { ... }
* kp.send(topic, key, value)
```
Inspecting the results of a produce operation. Note that you must specify a key, and that the result is NOT JSON
```cucumber
* def handler = function(msg){ karate.signal(msg) }
* kp.send(topic, "the key", "hello with handler", handler)
* def result = karate.listen(2000)
```

Terminating the Kafka producer ...
```
* kp.close()
```

### Kafka Consumer

Creating a Kafka consumer with the default properties. A consumer starts listening to the topic as soon as it is created
```cucumber
* def kc = new KafkaConsumer(topic)
```

The following default properties are used to create the consumer
```
{
  "bootstrap.servers": "127.0.0.1:9092",
  "key.deserializer": "org.apache.kafka.common.serialization.StringDeserializer",
  "value.deserializer": "org.apache.kafka.common.serialization.StringDeserializer",
  "group.id": "karate-kafka-default-consumer-group"
}
```

Creating a consumer with specified properties ...
```cucumber
* def props = { ... }
* def kc = new KafkaConsumer(topic, props)

# Get the default Properties
* def props = KafkaConsumer.getDefaultProperties()
```
Create a customer that filters the key and the value. The key filter is a regular
expression, and the value filter is a [jsonPath](https://github.com/json-path/JsonPath) predicate expression. 
```cucumber
* def kc = new KafkaConsumer(topic, consumerProps, "test.*", "[?(@.message =~ /hi.*/)]")
```

Read a record from the topic. This call will block until data is available  
```cucumber  
* json output = kc.take();
```

Read multiple records from the topic. This call will block until data is available  
```cucumber  
* json output = kc.take(5);
```

Terminating the Kafka consumer ...
```cucumber
* kc.close()
```

### Running Features and Scenarios

By default, Karate runs all the features ( and the scenarios in each feature) in parallel. Having multiple threads reading and
and writing from Kafka can lead to interleaving of results from different test cases. For this, it is best to run all the features
and scenario in a single thread. To do this, the following changes are needed:

* Add `@parallel=false` at the top of each feature file. This will ensure that the scenarios are run serially. BTW, Kafka does NOT 
guarantee that the scenarios will be executed in the same order that they appear in the feature file

* Set the number of threads to 1 in the `xxxRunner.java` file. e.g., `Runner.path(...).parallel(1);

### Cheat Sheet for configuring Serializers and Deserializers

On the consumer side, you need to specify a deserializer for the key / value the data type is an integer

| Data Type  | Serializer |
| ---| ---|
| Integer | org.apache.kafka.common.serialization.IntegerDeserializer  |
| Longer | org.apache.kafka.common.serialization.LongDeserializer  |
| String | auto-configured  |
| JSON | auto-configured  |

On the Producer Side, you should never have to configure a serializer either for the key or data

## Managing the local Kafka broker

The configuration for Kafka and Zookeeper is specified in `kafka-single-broker.yml`. See
[Wurstmeister's Github wiki](https://github.com/wurstmeister/kafka-docker) on how to configure this.

### Setting up the Kafka Cluster

From the command line, run 

```
$ ./startup.sh
Starting karate-kafka_zookeeper_1 ... done
Starting karate-kafka_kafka_1     ... done
CONTAINER ID        IMAGE                    NAMES
ce9b01556d15        wurstmeister/zookeeper   karate-kafka_zookeeper_1
33685067cb82        wurstmeister/kafka       karate-kafka_kafka_1
*** sleeping for 10 seconds (give time for containers to spin up)
*** the following topic were created ....
test-topic
```

To smoke test this, we will setup a consumer that will echo whatever the producer writes. In
one terminal start off a consumer by running `./consumer.sh`. In another terminal, start off
a producer by running `./producer.sh`.  Type something into the producer. If all goes well, 
you should see the consumer echo it back.

From the command-line, run `./teardown.sh` to tear down the cluster. This stops zookeeper
and all the kafka brokers, and also deletes the containers. This means all the data written
to the kafka cluster will be lost. During testing, this is good because it
allows us to start each test from the same known state.

## Interop between Karate and Java

This section briefly talks about how Karate interoperates with Java ....

### Numbers
Karate internally uses Nashorn. Due to the way Nashnorn works, the number conversion
between Karate DSL and Java can sometimes have issues. The exact conversion rules for 
Nashorn vary by the JDK version and even the OS See [this](https://github.com/EclairJS/eclairjs-nashorn/wiki/Nashorn-Java-to-JavaScript-interoperability-issues)
and [this](https://stackoverflow.com/questions/38140399/jdk-1-8-0-92-nashorn-js-engine-indexof-behaviour/38148917#38148917)
articles for some samples.

The largest number that can be safely converted to Java's integer or long is 
`2147483647`. For example, referring to the `hello-java.feature` and the accompanying
`HelloKarate.java` example ...

```cucumber
 * def param = 2147483647
 # This works !
 * def out1 = hk.echoInt(param)
 * match out1 == param
 # This also works !!
 * def out2 = hk.echoLong(param)
 * match out2 == param
 # This does NOT work :(
 * def out3 = hk.echoInt(2147483648)
 * match out3 == param
```

To pass in big numbers, first convert them in `java.math.BigDecimal` as described in the
[Karate Documentation](https://github.com/intuit/karate#large-numbers)   

```cucumber
* def param = new java.math.BigDecimal(123456789012345567890)
* def out = hk.echoBigDecimal(param)
* match out == param
```

### Developer Instructions

( work in progress ) for those developing this code

To deploy to github
* create an oauth2 token ( Personal Access Token)  
    * Settings -> Developer Settings -> Personal Access Token
    * give repo and read:user access
* mvn deploy

### References

* [Running Kafka inside Docker](https://github.com/wurstmeister/kafka-docker)
* [Markdown syntax](https://github.com/adam-p/markdown-here/wiki/Markdown-Cheatsheet)
* [Java-Javascript Interop Issues in Nashorn](https://github.com/EclairJS/eclairjs-nashorn/wiki/Nashorn-Java-to-JavaScript-interoperability-issues)
* [Hosting a maven repository on github](https://dev.to/iamthecarisma/hosting-a-maven-repository-on-github-site-maven-plugin-9ch)

