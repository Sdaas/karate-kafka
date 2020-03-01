Feature: Karate test for the Order domain

  Background:

    * def KafkaProducer = Java.type('karate.kafka.KarateKafkaProducer')
    * def KafkaConsumer = Java.type('karate.kafka.KarateKafkaConsumer')
    * def input_topic = 'order-input'
    * def output_topic = 'order-output'
    * def random =
      """
      function(limit) {
        var Random = Java.type('java.util.Random')
        var r = new Random()
        return r.nextInt(limit)
      }
      """

  Scenario: Produce an Order

    # Remember to create a consumer before sending anything to the topic. That way
    # the consumer is already listening to the topic.
    * def kc = new KafkaConsumer(output_topic)
    * def kp = new KafkaProducer()
    * def key = random(10000)
    * def orderId = random(10000)
    * def order =
    """
    {
      id : #(orderId),
      customer : {
        firstName : "John",
        lastName  : "Smith",
        contact   : {
            email : "john@gmail.com",
            phone : "619-123-4567"
         }
      },
      lineItems :
        [
          { id: 12345, quantity: 3, price: 10 },
          { id: 67890, quantity: 2, price: 7 }
        ]
    }
    """
    * kp.send(input_topic, key, order)
    * print 'Send record for key = ' + key
    # Read the topic
    * def output = kc.take()
    * print output
    * print output.customer
    # Dont forget to close the consumer and producer
    * kp.close()
    * kc.close()
