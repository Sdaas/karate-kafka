
[![Build Status](https://api.travis-ci.com/Sdaas/karate-kafka.svg?branch=master)](https://travis-ci.com/Sdaas/karate-kafka)
 

## Introduction

( Work in Progress to Split this into the main code and demo apps )

## Documentation

- Must have the following on your machine
- Java 1.8.0
- Maven 3.6.2
- Docker 19.03.5
- Docker compose 1.25
- Kafka command-line shell 2.4.0

### Kafka Producer

Creating a Kafka producer with the default properties ...

```cucumber
# Create Kafka Producer with the default properties
* def kp = new KafkaProducer()
```

The following default properties are used to create the producer. The `karate.kafka.MyGenericSerializer` tries
to automatically guess the key/value type and attempts to serialize it as `Integer`, `Long`, `String`, or `JSON` 
based on the input. 

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

These default properties should work most of the time for testing, but you can always
override them ...

```cucumber
# Create Kafka Producer with the specified properties
* def prop = { ... } 
* def kp = new KafkaProducer(prop)

# Get the default Properties
* def props = KafkaProducer.getDefaultProperties()
```

Using a Kafka producer ....
```cucumber
# Write a message with or without a key
* kp.send(topic, "hello world")
* kp.send(topic, "the key", "hello again")

# Using JSON key and/or values
* def key = { ... }
* def value= { ... }
* kp.send(topic, key, value)

# Inspecting the results of a produce operation
# Note that 
*    - you must specify a key
*    - the result is NOT JSON
* def handler = function(msg){ karate.signal(msg) }
* kp.send(topic, "the key", "hello with handler", handler)
* def result = karate.listen(2000)
```

Terminating the Kafka producer ...
```
# Close the Kafka producer
* kp.close()
```

### Kafka Consumer

Creating a Kafka consumer with the default properties ...
```cucumber
# Create a Kafka consumer with the default properties. A consumer starts listening to
# the input topic as soon as it is created
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
# Create a Kafka consumer with the specified properties
* def props = { ... }
* def kc = new KafkaConsumer(topic, props)

# Get the default Properties
* def props = KafkaConsumer.getDefaultProperties()
```

Using a Kafka producer ...
```cucumber
# Read a record from the topic. This call will block until data is availale    
* json output = kc.take();
```

Terminating the Kafka consumer ...
```cucumber
# Close the consumer
# This is very important. Kafka allows only one consumer per consumer group to listen to a partition
* kc.close()
```

### Cheat Sheet for configuring Serializers and Deserializers

On the consumer side, you need to specify a deserializer for the key / value the data type is an integer

| Data Type  | Serializer |
| ---| ---|
| Integer | org.apache.kafka.common.serialization.IntegerDeserializer  |
| Longer | org.apache.kafka.common.serialization.LongDeserializer  |
| String | auto-configured  |
| JSON | auto-configured  |

On the Producer Side, you should never have to configure a serializer either for the key or data


## Examples

Make sure to run `mvn clean install` from the root directory before running any of the examples
below. This will ensure that that the `karate-kafka` jar is installed correctly in the local
repository before it is used.

### Order

In this demo, the `OrderProduer` creates an order with multiple line items and publishes it to the `order-input` topic.
The `OrderTotalStream` enriches this by calculating the total price of the order and publishes it
to the `order-output` topic, where is it picked up by the `OrderConsumer`.

#### Unit Tests 

The unit test for this stream application is done using the `TopologyTestDriver`
as described in the Kafka Streams Developer Guide for
[Testing a Streams Application]
(https://kafka.apache.org/11/documentation/streams/developer-guide/testing.html)

```
$ cd order
$ mvn test
```
#### Demo

To see how it works, start up the Kafka cluster, and then start the `OrderTotalStream`, `OrderConsumer`, 
and `OrderProducer` ..

```
$ ./setup.sh
$ cd order
$ mvn clean package
$ mvn exec:java@total
$ mvn exec:java@consumer
$ mvn exec:java@producer
```

#### Test using karate-kafka

To test this using karate-kafka ...

* Ensure that the Kafka cluster is running ( or run `./setup.sh` to start it up)
* Ensure that the `OrderProducer` is NOT running.
* Ensure that the `OrderConsumer` is NOT running.
* Ensure that the `OrderTotalStream` is running.
* From the IDE, run `order-demo.feature`

( Right now you cam run this only form IDE - need to add support to do this from command line)

( Also right now the match is failing because I am using the wrong deserializer)

mvn test -Dtest=CatsRunner


## Managing the local Kafka broker

The configuration for Kafka and Zookeeper is specified in `kafka-single-broker.yml`. See
[Wurstmeister's Github wiki](https://github.com/wurstmeister/kafka-docker) on how to configure this.

### Setting up the Kafka Cluster

From the command line, run 

```
$ ./setup.sh
Starting karate-kafka_zookeeper_1 ... done
Starting karate-kafka_kafka_1     ... done
CONTAINER ID        IMAGE                    NAMES
ce9b01556d15        wurstmeister/zookeeper   karate-kafka_zookeeper_1
33685067cb82        wurstmeister/kafka       karate-kafka_kafka_1
*** sleeping for 10 seconds (give time for containers to spin up)
*** the following topic were created ....
test-input
test-output
test-topic
```

To smoke test this, we will setup a consumer that will echo whatever the producer writes. 

Start off a consumer ...

```
kafka-console-consumer.sh --bootstrap-server localhost:9092 \
    --topic test-topic \
    --from-beginning \
    --formatter kafka.tools.DefaultMessageFormatter \
    --property print.key=true \
    --property print.value=true \
    --property print.timestamp=true \
    --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer \
    --property value.deserializer=org.apache.kafka.common.serialization.StringDeserializer
```

In another terminal start off a producer, and enter some data for the producer. 
```
$ kafka-console-producer.sh --broker-list 127.0.0.1:9092 --topic test-input
>
> (ctrl C to quit)
```

Type something into the producer. If all goes well, you should see the consumer echo it back.

### Tearing down the kafka cluster

From the command-line, run
```
$ ./teardown.sh
```

This stops all the zookeeper and kafka broker containers, and also delete the containers. So
all the data written to the kafka cluster will be lost. During testing, this is good because it
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
    
## Producing Data to Kafka

We will write something to the `test-topic` in Kafka and consume it through the console consumer

Start the Kafka cluster and a consumer
```
$ ./setup.sh
$ kafka-console-consumer.sh --bootstrap-server localhost:9092 \
      --topic test-topic \
      --formatter kafka.tools.DefaultMessageFormatter \
      --property print.key=true \
      --property print.value=true \
      --property print.timestamp=true \
      --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer \
      --property value.deserializer=org.apache.kafka.common.serialization.StringDeserializer
```

From the IDE, run `kafka-producer.feature` in the folder `karate-kafka/src/test/java/karate/kafka/`. If all goes well, the 
Kafka consumer should output all the data written by the producer.

### References

* [Running Kafka inside Docker](https://github.com/wurstmeister/kafka-docker)
* [Markdown syntax](https://github.com/adam-p/markdown-here/wiki/Markdown-Cheatsheet)
* [Word count usin Kafka Streams](https://github.com/gwenshap/kafka-streams-wordcount) by Gwen Shapiro
* [Java-Javascript Interop Issues in Nashorn](https://github.com/EclairJS/eclairjs-nashorn/wiki/Nashorn-Java-to-JavaScript-interoperability-issues)


