Feature: Kafka Producer

  # Write an event to Kafka

  Background:

    # Creating the Kafka Producer
    * def KafkaProducer = Java.type('karate.kafka.KarateKafkaProducer')
    * def producerProperties = { topic: 'test-topic' }
    * def kafkaProducer = new KafkaProducer(producerProperties)

  Scenario: Write some stuff to the test topic ...

    * def event = { key : 'theKey', value : 'theValue' }
    * call kafkaProducer.send(event);

