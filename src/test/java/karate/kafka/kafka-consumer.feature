Feature: Kafka Producer Consumer Demo

  # Write an event to Kafka and read it back

  Background:

    # Remember that all the code in the Background section gets executed for every scenario
    * def KafkaProducer = Java.type('karate.kafka.KarateKafkaProducer')
    * def KafkaConsumer = Java.type('karate.kafka.KarateKafkaConsumer')
    * def topic = 'test-topic'

  Scenario: Print the default properties

    * def props = KafkaConsumer.getDefaultProperties()
    * print props

  Scenario: Write strings to test-topic and read it back

    # Create a consumer. It starts listening to the topic as soon as it is created
    * def kc = new KafkaConsumer(topic)
    # Create a producer and send the event
    * def kp = new KafkaProducer()
    * def key = 'theKey'
    * def value = 'hello consumer'
    * kp.send(topic, key, value);
    # Read the output. The call to take() will block until some data is available.
    * def output = kc.take();
    # Please remember to close before doing the match. Otherwise if the test fails
    # you will not be able to close the producer and consumer
    * kp.close()
    * kc.close()
    # Check the output
    * match output.key == key
    * match output.value == value

  Scenario: Write JSON to test-topic and read it back

    # Create a consumer. It starts listening to the topic as soon as it is created
    * def kc = new KafkaConsumer(topic)
    * def kp = new KafkaProducer()
    * def key = { id: 10 }
    * def value = { message : 'Hello Consumer' }
    * kp.send(topic, key, value);
    * def output = kc.take();
    * kp.close()
    * kc.close()
    * match output.key == key
    * match output.value == value


  Scenario: Writing values (without keys) and reading them back

    # Create a consumer. It starts listening to the topic as soon as it is created
    * def kc = new KafkaConsumer(topic)
    * def kp = new KafkaProducer()
    * def value = 'look no keys'
    * kp.send(topic, value);
    * def output = kc.take();
    * print output
    * kp.close()
    * kc.close()
    # THe output should not contain any key
    * match output !contains { key : '#notnull' }
    * match output.value == value


  # TODO
  # take(N) <-- not implemented
  # take(timeout) <- not implemented