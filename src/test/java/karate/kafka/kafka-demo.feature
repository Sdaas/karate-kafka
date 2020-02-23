Feature: Kafka Producer

  # Write an event to Kafka

  Background:

    # Creating the Kafka Producer
    * def KafkaProducer = Java.type('karate.kafka.KarateKafkaProducer')
    * def producerProperties = { topic: 'test-topic' }
    * def kafkaProducer = new KafkaProducer(producerProperties)

    # Creating the Kafka Consumer
    # Creating the Kafka consumer also makes it start listening to the topic
    *  def KafkaConsumer = Java.type('karate.kafka.KarateKafkaConsumer')
    * def consumerProperties = { topic: 'test-topic' }
    * def kafkaConsumer = new KafkaConsumer(consumerProperties)


  Scenario: Write some stuff to the test topic ...

    * def event = { key : '123', value : 'Hello Consumer' }
    * call kafkaProducer.send(event);
    * def output = kafkaConsumer.read();
    * print output

