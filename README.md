(work in progress)

## Introduction

TBD

## Managing the local Kafka broker

The configuration for Kafka and Zookeeper is specified in `kafka-single-broker.yml`. See
[Wurstmeister's Github wiki](https://github.com/wurstmeister/kafka-docker) on how to configure this.

### Setting up the Kafka Cluster

From the command line, run 

```
$ ./bin/setup.sh
```

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

In another terminal start off a producer, and enter some data for the producer. Ideally the consumer
will print them out on the console ... 
```
$ kafka-console-producer.sh --broker-list 127.0.0.1:9092 --topic test-input
>
> (ctrl C to quit)

```

Type something into the producer. If all goes well, you should see the consumer echo it back.

### Tearing down the setup

From the command-line, run

```
$ ./teardown.sh
```

## Interop between Karate and Java

- hello-feature
- parameter passing to constructor and methods

## Producing Data to Kafka

We will write something to the `test-topic` in Kafka and consume it through the console consumer

Start the Kafka cluster and a consumer
```
$ ./setup.sh
Starting kafka-karate_zookeeper_1 ... done
Starting kafka-karate_kafka_1     ... done
CONTAINER ID        IMAGE                    NAMES
ce9b01556d15        wurstmeister/zookeeper   kafka-karate_zookeeper_1
33685067cb82        wurstmeister/kafka       kafka-karate_kafka_1
*** sleeping for 10 seconds (give time for containers to spin up)
*** the following topic were created ....
test-input
test-output
test-topic
$ kafka-console-consumer.sh --bootstrap-server localhost:9092 \
      --topic test-topic \
      --formatter kafka.tools.DefaultMessageFormatter \
      --property print.key=true \
      --property print.value=true \
      --property print.timestamp=true \
      --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer \
      --property value.deserializer=org.apache.kafka.common.serialization.StringDeserializer
```

From the IDE, run `kafka-producer.feature` in the folder `src/test/java/karate/kafka/`. If all goes well, the 
Kafka consumer should output all the data written by the producer.

### Kafka Producer

```cucumber
# Create Kafka Producer with the default properties
* def kp = new KafkaProducer()

# Create Kafka Producer with the specified properties
* def prop = { ... } 
* def kp = new KafkaProducer(prop)

# Get the default Properties
* def props = KafkaProducer.getDefaultProperties()

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

# Close the Kafka producer
* kp.close()
```

### Kafka Consumer

```cucumber
# Create a Kafka consumer with the default properties. A consumer starts listening to
# the input topic as soon as it is created
* def kc = new KafkaConsumer(topic)

# Create a Kafka consumer with the specified properties
* def props = { ... }
* def kc = new KafkaConsumer(topic, props)

# Get the default Properties
* def props = KafkaConsumer.getDefaultProperties()

# Read a record from the topic. This call will block until data is availale    
* def output = kc.take();

# Close the consumer
# This is very important. Kafka allows only one consumer per consumer group to listen to a partition
* kc.close()
```

### References

* [Running Kafka inside Docker](https://github.com/wurstmeister/kafka-docker)
* [Markdown syntax](https://github.com/adam-p/markdown-here/wiki/Markdown-Cheatsheet)
* [Word count usin Kafka Streams](https://github.com/gwenshap/kafka-streams-wordcount) by Gwen Shapiro



