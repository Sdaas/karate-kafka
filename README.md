(work in progress)

## Introduction

TBD

### Managing the local Kafka broker

The configuration for Kafka and Zookeeper is specified in `kafka-single-broker.yml`. See
[Wurstmeister's Github wiki](https://github.com/wurstmeister/kafka-docker) on how to configure this.

#### Setting up the Kafka Cluster

From the command line, run 

```
$ ./bin/setup.sh
```

Topics Created

* `test-topic` : scratchpad topic. not used for anything
* `test-input ` : for ad-hoc testing 
* `test-output` : for ad-doc testing
* `daily-event-count` : 
* `daily-event-history` :

Start off a consumer ...

```
kafka-console-consumer.sh --bootstrap-server localhost:9092 \
    --topic test-output \
    --from-beginning \
    --formatter kafka.tools.DefaultMessageFormatter \
    --property print.key=true \
    --property print.value=true \
    --property print.timestamp=true \
    --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer \
    --property value.deserializer=org.apache.kafka.common.serialization.LongDeserializer
```

In another terminal start off a producer, and enter some data for the producer. Ideally the consumer
will print them out on the console ... 
```
$ kafka-console-producer.sh --broker-list 127.0.0.1:9092 --topic test-input
>
> (ctrl C to quit)

```

#### Tearing down the setup

From the command-line, run

```
$ ./teardown.sh
```

### Interop between Karate and Java

- hello-feature
- parameter passing to constructor and methods

### Simple Kafka Producer

We will write something to the `test-topic` in Kafka and consume it through the console consumer

Start the Kafka cluster and a consumer
```
$ ./setup.sh
Starting kafka-karate_zookeeper_1 ... done
Starting kafka-karate_kafka_1     ... done
CONTAINER ID        IMAGE                    NAMES
ce9b01556d15        wurstmeister/zookeeper   kafka-karate_zookeeper_1
33685067cb82        wurstmeister/kafka       kafka-karate_kafka_1
*** sleeping for 5 seconds (give time for containers to spin up)
*** sleeping for 5 seconds (give time for all topics to be created)
*** the following topic were created ....
test-input
test-output
test-topic
$ kafka-console-consumer.sh --bootstrap-server localhost:9092 \
      --topic test-topic \
      --from-beginning \
      --formatter kafka.tools.DefaultMessageFormatter \
      --property print.key=true \
      --property print.value=true \
      --property print.timestamp=true \
      --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer \
      --property value.deserializer=org.apache.kafka.common.serialization.StringDeserializer
```
From the IDE, run `kafka-producer.feature` in the folder `src/test/java/karate/kafka/`

### References

* [Running Kafka inside Docker](https://github.com/wurstmeister/kafka-docker)
* [Markdown syntax](https://github.com/adam-p/markdown-here/wiki/Markdown-Cheatsheet)



