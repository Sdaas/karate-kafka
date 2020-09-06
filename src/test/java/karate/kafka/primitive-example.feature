Feature: Where the key and values are primitive data types

  # Scenarios to demonstrate where the key or value are Integer, Long, or String. There is also
  # a scenario where there is no key

  Background:

    * def KafkaProducer = Java.type('karate.kafka.KarateKafkaProducer')
    * def KafkaConsumer = Java.type('karate.kafka.KarateKafkaConsumer')
    * def props = KafkaConsumer.getDefaultProperties()
    * def topic = 'test-topic'
    * def randomInt =
      """
      function() {
        var Random = Java.type('java.util.Random')
        var r = new Random()
        return r.nextInt()
      }
      """
    * def randomLong =
      """
      function() {
        var Random = Java.type('java.util.Random')
        var r = new Random()
        return r.nextLong()
      }
      """



  Scenario: Record with String key and value types

    * def kp = new KafkaProducer()
    * def kc = new KafkaConsumer(topic)
    * def key = "the key"
    * def value = "the value"
    * kp.send(topic, key, value)
    * json output = kc.take()
    * kp.close()
    * kc.close();
    * match output.key == key
    * match output.value == value


  Scenario: Record with Integer key and value types

    * def kp = new KafkaProducer()
    * props["key.deserializer"] = "org.apache.kafka.common.serialization.IntegerDeserializer"
    * props["value.deserializer"] = "org.apache.kafka.common.serialization.IntegerDeserializer"
    * def kc = new KafkaConsumer(topic, props)
    * def key = randomInt()
    * def value = randomInt()
    * kp.send(topic, key, value)
    * json output = kc.take()
    * kp.close()
    * kc.close();
    * match output.key == key
    * match output.value == value

  Scenario: Javascript Numbers are generally treated as Integers ...

    # Karate uses Nashorn to convert Javascript numbers to Java integer. Due to the way
    # Nashorn works, the largest number that can be safely converted to Integer or Long is
    # 2147483647

    * def kp = new KafkaProducer()
    * props["key.deserializer"] = "org.apache.kafka.common.serialization.IntegerDeserializer"
    * props["value.deserializer"] = "org.apache.kafka.common.serialization.IntegerDeserializer"
    * def kc = new KafkaConsumer(topic, props)
    * def key = 1234
    * def value = 12345
    * kp.send(topic, key, value)
    * json output = kc.take()
    * kp.close()
    * kc.close();
    * match output.key == key
    * match output.value == value

  Scenario: Record with null key

    # The KafkaProducer requires a key serializer, even if the message does not contain a key
    # In this case, it really does not make sense to read back the key from the consumer

    * def kp = new KafkaProducer()
    # No need to specify the deserializer for the key.
    * props["value.deserializer"] = "org.apache.kafka.common.serialization.IntegerDeserializer"
    * def kc = new KafkaConsumer(topic,props)
    * def value = randomInt()
    * kp.send(topic, value)
    * json output = kc.take()
    * kp.close()
    * kc.close();
    # Dont read the output.key - it is meaningless
    * match output.value == value

  Scenario: Handling of Long at the Producer

     # If the key or value is a Long, then the Producer will automatically serialize it as a Long.
     # On the consumer side, you must specify a LongDeserializer to handle this.

    * def kp = new KafkaProducer()
    * props["key.deserializer"] = "org.apache.kafka.common.serialization.LongDeserializer"
    * props["value.deserializer"] = "org.apache.kafka.common.serialization.LongDeserializer"
    * def kc = new KafkaConsumer(topic,props)
    * def key = randomLong()
    * def value = randomLong()
    * kp.send(topic, key, value)
    * json output = kc.take()
    * kp.close()
    * kc.close();
    * match output.key == key
    * match output.value == value