Feature: Kafka Producer

  # Write an event to Kafka

  Background:

    * def KafkaProducer = Java.type('karate.kafka.KarateKafkaProducer')
    * def topic = 'test-topic'


  Scenario: Print the default properties

    * def props = KafkaProducer.getDefaultProperties()
    * print props

  Scenario: Create a Producer with default properties and write to a topic
    # Creates a Kafka producer with the default properties. This means that it is using a StringSerializer/Deserializer
    #

    * def kp = new KafkaProducer()
    # Write a message with a null key
    * kp.send(topic, "hello world")
    # Write a message with a key
    * kp.send(topic, "the key", "hello again")
    # Remember to close the producer ....
    * kp.close()

  Scenario: Write to a topic and inspect the result
    # Creates a Kafka producer with the default properties. This means that it is using a StringSerializer/Deserializer
    # Send a message and inspect the result of the callback to determine whether the write was successful or not

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

  Scenario: Sending a JSON

    # Since this topic uses a StringSerializer, it expects the input to be a String.  However, if send()
    # is called with a JSON object, Karate converts it first to a HashMap which send() does not understand.
    # So we must first manually convert this to a string.

    * def kp = new KafkaProducer()
    * def topic = 'test-topic'
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
    # WRONG !
    # * kp.send(topic, data)
    # RIGHT !
    * string str = data
    * kp.send(topic, str)
    * kp.close()