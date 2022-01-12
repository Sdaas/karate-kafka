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