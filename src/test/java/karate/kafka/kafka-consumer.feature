Feature: Kafka Producer Consumer Demo

  # Write an event to Kafka and read it back

  Background:

    # Remember that all the code in the Background section gets executed for every scenario
    # That does not matter in this case, but we will need to take care of it in future

    # Creating the Kafka Producer
    * def KafkaProducer = Java.type('karate.kafka.KarateKafkaProducer')
    * def producerProperties = { topic: 'test-topic' }
    * def kafkaProducer = new KafkaProducer(producerProperties)

    # Creating the Kafka Consumer
    # Creating the Kafka consumer also makes it start listening to the topic
    *  def KafkaConsumer = Java.type('karate.kafka.KarateKafkaConsumer')
    * def consumerProperties = { topic: 'test-topic' }
    * def kafkaConsumer = new KafkaConsumer(consumerProperties)


  Scenario: Write to test-topic and read it back

    * def event = { key : '123', value : 'Hello Consumer' }
    * call kafkaProducer.send(event);
    * def output = kafkaConsumer.read();
    * print output
    * call kafkaProducer.close()
    * call kafkaConsumer.close()

