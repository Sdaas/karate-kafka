## Test Kafka Standalone with Schema Registry

### Description

The objective is to create an example using:

- Karate standalone JAR with Kafka libraries. This is mainly for non Java based installations.
See https://github.com/karatelabs/karate/blob/master/karate-netty/README.md#standalone-jar
- Create a REST request which will generate some Kafka topic output. This is simulated using a REST producer with the
Confluent REST proxy
- Avro Consumer using the confluent schema registry.

### Build of the standalone JAR file

From the source tree directory run:

```shell
mvn -Pfatjar clean install
```

this will generate the following fat jar:  `target/karate-kafka-0.1.2.jar`

For more information see: https://github.com/karatelabs/karate/blob/master/karate-netty/README.md#standalone-jar

### Start up of Kafka and Schema Registry

The following command will create:

- Zookeeper on port 2181
- Kafka on port 9092 (internally it listens on 19092)
- Confluent Schema Registry on http://127.0.0.1:8081
- Confluent Rest proxy on http://127.0.0.1:8082
- Landoop Schema registry UI on http://127.0.0.1:8083
- Landoop Topics UI browser on http://127.0.0.1:8000

```shell
cd examples/karate_standalone_with_kafka_schema_registry/
docker-compose up
```

To stop the environment run:


```shell
cd examples/karate_standalone_with_kafka_schema_registry/
docker-compose down
```

### Run the example test

The example test files are at `examples/karate_standalone_with_kafka_schema_registry/test`. And it includes the files:

- `kafa-rest-proxy-schema-registry.feature`: Example scenario to run 
- `message_key.avsc`: Key Avro Schema
- `message_value.avsc`: Value Avro Schema
- `message_key.json`: Example test key
- `message_value.json`: Example test value
                         

To run the test:

```shell
KARATE_JAR=$(pwd)/target/karate-kafka-0.1.2.jar
cd examples/karate_standalone_with_kafka_schema_registry/test
java -jar $KARATE_JAR --tags=@dev kafka_rest_proxy_schema_registry.feature
```

### Gherkin/Karate file

1. It creates a test entry from the `message_key.json` and `message_value.json` files. 

The basic idea is that there is some kind of REST request which will generate some Kafka messages.
In our example this is simulated using the Kafka Confluent Rest proxy and pushing directly a message
to the test topic. See https://docs.confluent.io/platform/current/kafka-rest/api.html

The key elements are the `Accept` and the `Content-Type` header which indicates that the content generated is of type
AVRO and sent to the proxy with JSON

2. It then reads with the Kafka Consumer using a Schema registry. 
 
The Kafka Consumer is using the well known class `karate.kafka.KarateKafkaConsumer`

3. The parameters which might vary between the environments have been setup in the variables:
  * restproxy: http://127.0.0.1:8082
  * kafkaprops: Here we indicate the bootstrap server, the group id, the key and value deserializer and the schema
registry url



```gherkin
Feature: Kafka Rest Proxy Avro Producer and Kafka Avro Consumer Demo

  Background:
    # Global info
    * def topic = 'test-topic'
    # Kafka consumer info
    * def KafkaConsumer = Java.type('karate.kafka.KarateKafkaConsumer')
    * def KafkaAvroDeserializer = Java.type("io.confluent.kafka.serializers.KafkaAvroDeserializer")
    # Rest proxy info
    * def rest_proxy_path = "/topics/" + topic
    * configure headers = { "Accept": "application/vnd.kafka.v2+json, application/vnd.kafka+json, application/json", "Content-Type": "application/vnd.kafka.avro.v2+json" }
    # Schemas
    # We need to escape the avsc quotes with backslash, and not send any newlines
    * def keySchema = karate.readAsString('this:message_key.avsc').replace(/[\""]/g, '\\"').replace(/[\n\r]/g, ' ')
    * def valueSchema = karate.readAsString('this:message_value.avsc').replace(/[\""]/g, '\\"').replace(/[\n\r]/g, ' ')
    # Example data
    * def recordKey = karate.readAsString('this:message_key.json').replace(/[\n\r]/g, ' ')
    * def recordValue = karate.readAsString('this:message_value.json').replace(/[\n\r]/g, ' ')
    * def requestString = '{ "key_schema": "' + keySchema +'", "value_schema": "' + valueSchema + '", "records": [ { "key": ' + recordKey + ', "value": ' + recordValue + ' } ] }'
    * print "requestString:", requestString


  Scenario Outline: Write messages to <restproxy> and read it back from a KafkaConsumer with props <kafkaprops>

    # Create a consumer with the properties per tag
    * def kc = new KafkaConsumer(topic,<kafkaprops>)
    Given url "<restproxy>"
    And path rest_proxy_path
    And request requestString
    When method post
    # Fetch message from kafka
    * json out = kc.take()
    # The values should match
    Then match out.key == { "campaign_id": "test_campaign", "destination_address": "you@example.com" }
    And match out.value contains { "status": "sent" }
    # Close the Consumer
    * kc.close()

    @dev
    Examples:
      | restproxy             | kafkaprops |
      | http://127.0.0.1:8082 | { "bootstrap.servers": "127.0.0.1:9092", "group.id": "my.test", "auto.offset.reset": "earliest", "schema.registry.url": "http://127.0.0.1:8081", "key.deserializer": "io.confluent.kafka.serializers.KafkaAvroDeserializer", "value.deserializer": "io.confluent.kafka.serializers.KafkaAvroDeserializer" } |
```


### Example output

```shell
KARATE_JAR=$(pwd)/target/karate-kafka-0.1.2.jar
cd examples/karate_standalone_with_kafka_schema_registry/test
java -jar $KARATE_JAR --tags=@dev kafka_rest_proxy_schema_registry.feature
14:51:46.827 [main]  INFO  com.intuit.karate - Karate version: 1.1.0
14:51:48.205 [main]  INFO  com.intuit.karate - [print] requestString: { "key_schema": "{   \"type\": \"record\",   \"namespace\": \"com.qindel.karate-kafka\",   \"name\": \"MessageStatusKey\",   \"doc\": \"Indicates the identifier of the message based on campaign_id and email.\",   \"fields\": [     {       \"name\": \"campaign_id\",       \"type\": \"string\",       \"doc\": \"Is the campaign id returned when the campaign is launched\"     },     {       \"name\": \"destination_address\",       \"type\": \"string\",       \"doc\": \"Is the destination address of the message, that is, the destination email for email\"     }   ] }", "value_schema": "{   \"type\": \"record\",   \"namespace\": \"com.qindel.karate-kafka\",   \"name\": \"MessageStatusValue\",   \"doc\": \"Indicates status of the message. Whether it is queued, sent, or has been delivered\",   \"fields\": [     {       \"name\": \"status\",       \"type\": {         \"type\": \"enum\",         \"name\": \"status\",         \"symbols\": [           \"other\",           \"queued\",           \"sent\",           \"delivered\",           \"error\"         ],         \"default\": \"other\"       },       \"doc\": \"The different status where the message can be: 'queued': the message is in the output queue. 'sent': the message has been sent, 'delivered': the message has reached the end user device. 'error': the message will not be sent, more information about the error will be in the description field, 'revoked': The message was not be possible to be delivered once sent for a period of time and has been removed of the system, in case of rcs, if sms fallback was configured, the next status might be 'queued' and the channel might change from 'rcs' to 'sms' and start again via 'queued', 'sent', 'delivered'... 'other' is mainly used for schema evolution for new statuses that might be defined in the future. If you get 'other' the most likely case is that there is a new version of the schema\"     },     {       \"name\": \"status_ts\",       \"type\": \"long\",       \"logicalType\": \"timestamp-millis\",       \"default\": 0,       \"doc\": \"Is the timestamp when the status was changed. Number of milliseconds from the unix epoch, 1 January 1970 00:00:00.000 UTC\"     },     {       \"name\": \"description\",       \"type\": \"string\",       \"default\": \"\",       \"doc\": \"Is extra information about the status. It might indicate the reason of an error, or the list that filtered the message\"     }   ] }", "records": [ { "key": {   "campaign_id": "test_campaign",   "destination_address": "you@example.com" } , "value": {   "status": "sent",   "status_ts": 0,   "description": "test" } } ] } 
14:51:48.519 [main]  DEBUG com.intuit.karate - request:
1 > POST http://127.0.0.1:8082/topics/test-topic
1 > Accept: application/vnd.kafka.v2+json, application/vnd.kafka+json, application/json
1 > Content-Type: application/vnd.kafka.avro.v2+json; charset=UTF-8
1 > Content-Length: 2620
1 > Host: 127.0.0.1:8082
1 > Connection: Keep-Alive
1 > User-Agent: Apache-HttpClient/4.5.13 (Java/11.0.13)
1 > Accept-Encoding: gzip,deflate
{ "key_schema": "{   \"type\": \"record\",   \"namespace\": \"com.qindel.karate-kafka\",   \"name\": \"MessageStatusKey\",   \"doc\": \"Indicates the identifier of the message based on campaign_id and email.\",   \"fields\": [     {       \"name\": \"campaign_id\",       \"type\": \"string\",       \"doc\": \"Is the campaign id returned when the campaign is launched\"     },     {       \"name\": \"destination_address\",       \"type\": \"string\",       \"doc\": \"Is the destination address of the message, that is, the destination email for email\"     }   ] }", "value_schema": "{   \"type\": \"record\",   \"namespace\": \"com.qindel.karate-kafka\",   \"name\": \"MessageStatusValue\",   \"doc\": \"Indicates status of the message. Whether it is queued, sent, or has been delivered\",   \"fields\": [     {       \"name\": \"status\",       \"type\": {         \"type\": \"enum\",         \"name\": \"status\",         \"symbols\": [           \"other\",           \"queued\",           \"sent\",           \"delivered\",           \"error\"         ],         \"default\": \"other\"       },       \"doc\": \"The different status where the message can be: 'queued': the message is in the output queue. 'sent': the message has been sent, 'delivered': the message has reached the end user device. 'error': the message will not be sent, more information about the error will be in the description field, 'revoked': The message was not be possible to be delivered once sent for a period of time and has been removed of the system, in case of rcs, if sms fallback was configured, the next status might be 'queued' and the channel might change from 'rcs' to 'sms' and start again via 'queued', 'sent', 'delivered'... 'other' is mainly used for schema evolution for new statuses that might be defined in the future. If you get 'other' the most likely case is that there is a new version of the schema\"     },     {       \"name\": \"status_ts\",       \"type\": \"long\",       \"logicalType\": \"timestamp-millis\",       \"default\": 0,       \"doc\": \"Is the timestamp when the status was changed. Number of milliseconds from the unix epoch, 1 January 1970 00:00:00.000 UTC\"     },     {       \"name\": \"description\",       \"type\": \"string\",       \"default\": \"\",       \"doc\": \"Is extra information about the status. It might indicate the reason of an error, or the list that filtered the message\"     }   ] }", "records": [ { "key": {   "campaign_id": "test_campaign",   "destination_address": "you@example.com" } , "value": {   "status": "sent",   "status_ts": 0,   "description": "test" } } ] }

14:51:50.716 [main]  DEBUG com.intuit.karate - response time in milliseconds: 2193
1 < 200
1 < Date: Sat, 08 Jan 2022 13:51:48 GMT
1 < Content-Type: application/vnd.kafka.v2+json
1 < Vary: Accept-Encoding, User-Agent
1 < Transfer-Encoding: chunked
{"offsets":[{"partition":0,"offset":0,"error_code":null,"error":null}],"key_schema_id":1,"value_schema_id":2}
14:51:55.062 [main]  INFO  com.intuit.karate - [print] kafka out: {
  "key": {
    "campaign_id": "test_campaign",
    "destination_address": "you@example.com"
  },
  "value": {
    "status": "sent",
    "status_ts": 0,
    "description": "test"
  }
}
 
---------------------------------------------------------
feature: kafa-rest-proxy-schema-registry.feature
scenarios:  1 | passed:  1 | failed:  0 | time: 7.2697
---------------------------------------------------------

14:51:55.863 [main]  INFO  com.intuit.karate.Suite - <<pass>> feature 1 of 1 (0 remaining) kafa-rest-proxy-schema-registry.feature
Karate version: 1.1.0
======================================================
elapsed:   8.91 | threads:    1 | thread time: 7.27 
features:     1 | skipped:    0 | efficiency: 0.82
scenarios:    1 | passed:     1 | failed: 0
======================================================

HTML report: (paste into browser to view) | Karate version: 1.1.0
```