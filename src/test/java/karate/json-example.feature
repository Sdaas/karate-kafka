@parallel=false
Feature: Kafka Producer and Consumer using JSON

  # Demonstration of writing JSON to a Kafka topic and reading it back

  # No special configuration is required either on the consumer or producer side to handle JSON

  Background:

    # Remember that all the code in the Background section gets executed for every scenario
    * def KafkaProducer = Java.type('karate.kafka.KarateKafkaProducer')
    * def KafkaConsumer = Java.type('karate.kafka.KarateKafkaConsumer')
    * def topic = 'test-topic'

  Scenario: Print the default properties

    * def props = KafkaConsumer.getDefaultProperties()
    * print props

  Scenario: Write JSON to test-topic and read it back

    * def kp = new KafkaProducer()
    * def props = KafkaConsumer.getDefaultProperties()
    * def kc = new KafkaConsumer(topic,props)
    * def key = { id: 10 }
    * def value =
    """
    {
      person : {
          firstName : "Santa",
          lastName : "Claus"
          },
      location : "North Pole"
    }
    """
    * kp.send(topic, key, value);
    # Read the data from the topic. At this point, this is a String, so we need to
    # convert it back to Json in order to do matches
    * json output = kc.take()
    # make sure to close the consumer and producer before doing a match. That way
    # the producer and consumer will be closed even if the assert fails
    * kp.close()
    * kc.close()
    * match output.key == key
    * match output.value == value
    # Since this is json, we can do all the fancy matches ...
    * def keyout = output.key
    * def valueout = output.value
    * match keyout.id == 10
    * match valueout.person.firstName == 'Santa'
    * print "*********"

