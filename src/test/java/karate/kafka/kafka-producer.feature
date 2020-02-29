Feature: Kafka Producer with String Serializer

  # By default the Kafka Producer uses a String Serializer both for keys and values. Which makes it super easy
  # to produce String or JSON (as String) into Kafka

  Background:

    * def KafkaProducer = Java.type('karate.kafka.KarateKafkaProducer')
    * def topic = 'test-topic'


  Scenario: Print the default properties

    * def props = KafkaProducer.getDefaultProperties()
    * print props

  Scenario: Writing simple strings to the topic ...

    * def kp = new KafkaProducer()
    # Write a message with a null key
    * kp.send(topic, "hello world")
    # Write a message with a key
    * kp.send(topic, "the key", "hello again")
    # Remember to close the producer ....
    * kp.close()

  Scenario: Writing JSON to the topic ...

    # Both the key and value can be JSON

    * def kp = new KafkaProducer()
    * def key = { id : "121" }
    * def data =
    """
    {
      id: 10,
      person: {
        firstName: 'John',
        lastName: 'Doe'
      }
    }
    """
    * kp.send(topic, key, data)
    * kp.close()

  Scenario: Write to a topic and inspect the result
    # Check the result of the produce operation
    # In order for this to work, you must specify a key.

    * def kp = new KafkaProducer()
    # Create the callback handler to inspect the result of the send
    * def handler = function(msg){ karate.signal(msg) }
    * kp.send(topic, "the key", "hello with handler", handler)
    # Wait 2 seconds for the call back handler
    * def result = karate.listen(2000)
    * print result
    * kp.close()

  Scenario: A producer with non-default properties

    # The properties for a Kafka producer can be expressed in JSON has shown below
    * def properties =
    """
     {
        "bootstrap.servers": "127.0.0.1:9092",
        "value.serializer": "org.apache.kafka.common.serialization.StringSerializer",
        "key.serializer": "org.apache.kafka.common.serialization.StringSerializer",
        "linger.ms": "20",
        "enable.idempotence": "true",
        "acks": "all"
     }
    """
    * def kp = new KafkaProducer(properties)
    * kp.send(topic, "the key", "hello from non-default values")
    * kp.close()

