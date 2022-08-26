
[![Java CI with Maven](https://github.com/Sdaas/karate-kafka/actions/workflows/maven.yml/badge.svg)](https://github.com/Sdaas/karate-kafka/actions/workflows/maven.yml)

## Introduction

Work In Progress

This project provides a library to test Kafka applications using [KarateDSL](https://github.com/karatelabs/karate). 
It provides a `KafkaProducer` and `KafkaConsumer` that can be called from a Karate feature. An example :

```cucumber
Feature: Kafka Producer and Consumer Demo

  Background:
    * def KafkaProducer = Java.type('karate.kafka.KarateKafkaProducer')
    * def KafkaConsumer = Java.type('karate.kafka.KarateKafkaConsumer')
    * def topic = 'test-topic'

  Scenario: Write messages to test-topic and read it back

    * def kp = new KafkaProducer()
    * def props = KafkaConsumer.getDefaultProperties()
    * def kc = new KafkaConsumer(topic,props)
    * def key = "message_key"
    * def value =
    """
    {
      person : {
          firstName : "Santa",
          lastName : "Claus"
          },
      location : "North Pole"
    }
    """
    * def headers = { x-header-one : "header-one-value", x-header-two : "header-two-value" }
    * kp.send(topic, key, value,headers);

    # Read from the consumer
    * json out = kc.take()

    * kp.close()
    * kc.close()

    # Match
    * match out.key == "message_key"
    * match out.value.person.firstName == 'Santa'
    * match out.headers contains { "x-header-one": "header-one-value" }
    * match out.headers contains { "x-header-two": "header-two-value" }
```
## Quick Demo

Start up a single-node Kafka cluster locally. Running `karate.KarateTests` will invoke
`src/test/java/karate/kafka/example.feature` which will attempt to
write a few messages to `test-topic` and read it back. Finally, shut down
the Kafka cluster.

```
$ docker-compose up -d 
$ mvn test
$ docker-compose down  
```

## Documentation
### Using this in your project

Add the following to your `pom.xml` :
```xml
<dependency>
    <groupId>com.daasworld</groupId>
    <artifactId>karate-kafka</artifactId>
    <version>0.3.2</version>
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
* def kp = new KafkaProducer()
```
Get the default KafkaProducer properties
```cucumber
* def props = KafkaProducer.getDefaultProperties()
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
The `karate.kafka.MyGenericSerializer` tries  to automatically guess the key/value type and attempts 
to serialize it as `Integer`, `Long`, `String`, or `JSON` 
based on the input. These default properties should work most of the time for testing, but you can always
override them ...

Overriding the default Properties
```cucumber
* def prop = { ... } 
* def kp = new KafkaProducer(prop)


```
Producing a message with or without a key
```cucumber
* kp.send(topic, "hello world")
* kp.send(topic, "the key", "hello again")
```
Producing a Message with headers. Header key and values must be strings. 
```cucumber
* def headers = { x-header-one : "header-one-value", x-header-two : "header-two-value" }
* kp.send(topic, "message_key", "message_payload", headers)
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

Read a record from the topic. This call will block until data is available.
```cucumber  
* json output = kc.take();
```
After reading, the `output` will contain
* `key`: Is always present. If producer did not specify a key, then this will be `null`.
* `value`: The message value.
* `headers`: Kafka message headers. Present only if the message had headers.
 
        
Read a record from the topic waiting upto the specified amount of time (in milliseconds). If the data is not available
by that time, it will return null. This can be used (for example) to check that no records were written to a topic
```cucumber  
* def raw = kc.poll(3000);
* match raw == null
```

Read multiple records from the topic. This call will block until data is available.  
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

* Add `@parallel=false` at the top of each feature file. This will ensure that the scenarios are run serially. BTW, Karate does NOT 
guarantee that the scenarios will be executed in the same order that they appear in the feature file

* Set the number of threads to 1 the `Runner`. E.g., `Runner.path(...).parallel(1);

Currently `karate.KarateTests.java` has a single method to execute all the test cases
```java
@Test
void testall() {
    Results results = Runner.path("classpath:karate").parallel(1);
    assertEquals(0, results.getFailCount(), results.getErrorMessages());
}
```

This is ok for automated testing, but does not allow us to run one test at a time. To do that, you should replace that
with the following which will allow us to run one feature at a time
```java
@Karate.Test
Karate jsonExampleTest() throws InterruptedException {
    return Karate.run("classpath:karate/json-example.feature");
}
```

### Cheat Sheet for configuring Serializers and Deserializers

On the consumer side, you need to specify a deserializer for the key / value the data type is an integer

| Data Type | Serializer                                                |
|-----------|-----------------------------------------------------------|
| Integer   | org.apache.kafka.common.serialization.IntegerDeserializer |
| Longer    | org.apache.kafka.common.serialization.LongDeserializer    |
| String    | auto-configured                                           |
| JSON      | auto-configured                                           |

On the Producer Side, you should never have to configure a serializer either for the key or data

### Karate standalone

If you want to use a standalone Kafka Jar please use this:

```shell
mvn -Pfatjar clean install
```

There is an example on how to use this in the [example karate standalone with kafka using schema_registry](examples/karate_standalone_with_kafka_schema_registry)

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
[Karate Documentation](https://github.com/karatelabs/karate#large-numbers)   

```cucumber
* def param = new java.math.BigDecimal(123456789012345567890)
* def out = hk.echoBigDecimal(param)
* match out == param
```

## Developer Instructions

This repo uses github actions to automatically build the library whenever code is pushed to master branch. 
See [maven.yml](.github/workflows/maven.yml)

The primary challenge is where to publish the maven artifacts. Onboarding to 
[mavenrepository.com](https://mvnrepository.com/repos/central) is a pain. One would have thought
that [Github Package Registry](https://docs.github.com/en/packages/learn-github-packages/introduction-to-github-packages) but 
you would be quite wrong. It turns out that authentication is needed even to download artifacts from the repository (read all 
about it [here](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry)). So
we have chosen to deploy all to the `mvn-repo` branch of this repo. See [this article](https://dev.to/iamthecarisma/hosting-a-maven-repository-on-github-site-maven-plugin-9ch)
on how to host a maven repository inside a github repo. 

### Prerequisites

Create a github token with the following permissions. See github.com -> Settings -> Developer Settings -> Personal Access Token

* Repo : full access
* Workflow (used by Github actions for CI)
* Write and read packages 
* Delete packages
* Read:user
* User:email

Make sure that you have a `~/.m2/settings.xml` file configured with a github token. For example

```xml
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>sdaas</username>
      <password>THE GITHUB ACCESS TOKEN </password>
    </server>
  </servers>
</settings>
```

### Release process

* `mvn test` 
* `mvn release:prepare -DdryRun=true`
* `mvn release:clean`
* `mvn release:prepare`
* `mvn release:perform`
* Assuming that we are releasing `A.B.C`
  * At this point the pom.xml should have been updated to the `next-version-SNAPSHOT` version
  * There should be an entry for `A.B.C` version of the artifact in the `mvn-repo` branch
  * There should a github tag called `vA.B.C`
* We still need to create a Release (and release notes) manually from this tag.

### References

* [Karate](https://github.com/karatelabs/karate)
* [Kafkacat](https://github.com/edenhill/kafkacat)
* [Markdown syntax](https://github.com/adam-p/markdown-here/wiki/Markdown-Cheatsheet)
* [Java-Javascript Interop Issues in Nashorn](https://github.com/EclairJS/eclairjs-nashorn/wiki/Nashorn-Java-to-JavaScript-interoperability-issues)
* [Maven Release Plugin](https://maven.apache.org/maven-release/maven-release-plugin/index.html)


