Feature: Where the key and values are primitive data types

  # Scenarios to demonstrate where the key or value are Integer, Long, or String. There is also
  # a scenario where the there is no key

  Background:

    * def KafkaProducer = Java.type('karate.kafka.KarateKafkaProducer')
    * def KafkaConsumer = Java.type('karate.kafka.KarateKafkaConsumer')
    * def props = KafkaConsumer.getDefaultProperties()
    * def topic = 'test-topic'

  Scenario: Record with null key

    # The KafkaProducer requires a key serializer, even if the message does not contain a key
    # In this case, it really does not make sense to read back the key from the consumer
    * def kp = new KafkaProducer()
    # No need to specify the deserializer for the key.
    * props["value.deserializer"] = "org.apache.kafka.common.serialization.IntegerDeserializer"
    * def kc = new KafkaConsumer(topic,props)
    * def value = 12345
    * kp.send(topic, value)
    * def strOutput = kc.take();
    * json output = strOutput
    * print output
    * kp.close()
    * kc.close();
    # Dont read the output.key - it is meaningless
    * match output.value == value


  Scenario: Record with Integer key and value types

     * def kp = new KafkaProducer()
     * props["key.deserializer"] = "org.apache.kafka.common.serialization.IntegerDeserializer"
     * props["value.deserializer"] = "org.apache.kafka.common.serialization.IntegerDeserializer"
     * def kc = new KafkaConsumer(topic,props)
     * def key = 123
     * def value = 12345
     * kp.send(topic, key, value)
     * def strOutput = kc.take();
     * json output = strOutput
     * kp.close()
     * kc.close();
     * match output.key == key
     * match output.value == value


