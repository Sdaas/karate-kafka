
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

  Scenario: Print the default properties for consumer

    * def props = KafkaConsumer.getDefaultProperties()
    * print props

  Scenario: Print the default properties for producer

    * def props = KafkaProducer.getDefaultProperties()
    * print props

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

    # Print it - the most advanced form of debugging
    * print output1
    * print output2

    # Doing the match
    # See https://intuit.github.io/karate/#karate-expressions
    * match output1 == { key : '#null', value : 'hello world' }
    * match output2 == { key : 'the_key', value : 'hello again' }

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

    # Print it - the most advanced form of debugging
    * print output

    # Doing the match
    * match output contains { key : #null, value : 'hello' }
    * match output contains { key : #notnull, value : 'world' }
