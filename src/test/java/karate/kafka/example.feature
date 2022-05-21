
@parallel=false
Feature: Karate-Kafka Demo

  # Demonstration of writing a string to a Kafka topic and reading it back
  # If there are multiple scenarios consider adding @parallel=false at the top
  # of the file to force all the scenarios to run sequentially
  # https://intuit.github.io/karate/#junit-5-parallel-execution

  Background:

    # Remember that all the code in the Background section gets executed for every scenario
    * def KafkaConsumer = Java.type('karate.kafka.KarateKafkaConsumer')
    * def KafkaProducer = Java.type('karate.kafka.KarateKafkaProducer')
    * def topic = 'test-topic'

  Scenario: Get the default properties for Consumer

    * def props = KafkaConsumer.getDefaultProperties()
    * match props contains { "bootstrap.servers": "127.0.0.1:9092" }

  Scenario: Print the default properties for producer

    * def props = KafkaProducer.getDefaultProperties()
    * match props contains { "bootstrap.servers": "127.0.0.1:9092" }

  Scenario: Write strings to test-topic and read it back

    # Create a consumer. It starts listening to the topic as soon as it is created
    * def kc = new KafkaConsumer(topic)
    # Create a producer
    * def kp = new KafkaProducer()
    # Sending a message without key ...
    * kp.send(topic, "hello world")
    # Sending a message with a key
    * kp.send(topic, "the_key", "hello again")
    # Remember to close the producer ....
    * kp.close()

    # Read the output. The call to take() will block until some data is available.
    # The data read from the topic is always a JSON and consists of a key and value
    * json output1 = kc.take()
    * json output2 = kc.take()
    # Remember to close before doing the match. Otherwise if the test fails
    # you will not be able to close the consumer
    * kc.close()

    # Doing the match
    # See https://intuit.github.io/karate/#karate-expressions
    * match output1 == { key : '#null', value : 'hello world' }
    * match output2 == { key : 'the_key', value : 'hello again' }


  Scenario: Using Integer data type in the Value

    # Create producer with integer serializer
    * def producerProps = KafkaProducer.getDefaultProperties()
    * producerProps["value.serializer"] = "org.apache.kafka.common.serialization.IntegerSerializer"
    * def kp = new KafkaProducer(producerProps)
    # Create consumer with long deserializer
    * def consumerProps = KafkaConsumer.getDefaultProperties()
    * consumerProps["value.deserializer"] = "org.apache.kafka.common.serialization.IntegerDeserializer"
    * def kc = new KafkaConsumer(topic,consumerProps)
    # * def value = new java.lang.Integer(12345) OR simply
    * def value = 12345
    * kp.send(topic, "message_key", value)
    * json out = kc.take()
    * kc.close()
    * kp.close()
    # Do the matching
    * match out == { key : '#notnull', value : 12345 }
    * match out.value == 12345

  Scenario: Read a list of messages from the test-topic

    * def kc = new KafkaConsumer(topic)
    * def kp = new KafkaProducer()
    # Sending two messages ...
    * kp.send(topic, "hello")
    * kp.send(topic, "the_key", "world")
    # Remember to close the producer ....
    * kp.close()

    # Read the output. The call to take() will block until some data is available.
    # The data read from the topic is always a JSON and consists of a key and value
    * json output = kc.take(2)
    # Remember to close before doing the match. Otherwise if the test fails
    # you will not be able to close the consumer
    * kc.close()

    # Doing the match
    * match output contains { key : #null, value : 'hello' }
    * match output contains { key : #notnull, value : 'world' }

  Scenario: Reading from test-topic with timeout

    * def kc = new KafkaConsumer(topic)
    * def raw = kc.poll(1000)
    * kc.close()
    * match raw == null

  Scenario: Kafka producer and consumer with properties

    # Create a consumer.
    * def consumerDefaults = KafkaConsumer.getDefaultProperties()
    * def kc = new KafkaConsumer(topic, consumerDefaults)
    # Create a producer
    * def producerDefaults = KafkaProducer.getDefaultProperties()
    * def kp = new KafkaProducer(producerDefaults)
    * kp.send(topic, "the_message_key", "the_message_value")
    * json output = kc.take()

    # Remember to close the producer and consumer
    * kp.close()
    * kc.close()
    * match output == { key : 'the_message_key', value : 'the_message_value' }

  Scenario: Kafka producer and consumer with headers

    * def kc = new KafkaConsumer(topic)
    * def kp = new KafkaProducer()
    * def headers = { x-header-one : "header-one-value", x-header-two : "header-two-value" }
    * kp.send(topic, "message_key", "message_payload", headers)
    * json out = kc.take()
    * print out
    * kc.close()
    * kp.close()
    * match out.headers contains { "x-header-one": "header-one-value" }
    * match out.headers contains { "x-header-two": "header-two-value" }
    * match out.key == "message_key"
    * match out.value == "message_payload"


