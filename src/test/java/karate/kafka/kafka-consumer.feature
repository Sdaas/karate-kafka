Feature: Kafka Consumer Demo


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
    # ... create a producer ...
    # ... send some data to the topic ...
    # Read the output. The call to take() will block until some data is available.
    * def output = kc.take();
    # Please remember to close before doing the match. Otherwise if the test fails
    # you will not be able to close the consumer
    * kc.close()
    # ... do the match ....

