Feature: Karate test for the Order domain

  # The order written to the order-input topic is "enriched" by the OrderTotalStream application and then
  # published to the order-output topic. In this example, we will publish an order and verify that it has
  # been enriched correctly.

  # We have a Integer key and JSON value. So we need to tell the producer how to deserialize the key
  # No configuration needed on the consumer side

  Background:

    * def KafkaProducer = Java.type('karate.kafka.KarateKafkaProducer')
    * def KafkaConsumer = Java.type('karate.kafka.KarateKafkaConsumer')
    * def consumerProps = KafkaConsumer.getDefaultProperties();
    * consumerProps["key.deserializer"] =  "org.apache.kafka.common.serialization.IntegerDeserializer"
    * print consumerProps
    * def input_topic = 'order-input'
    * def output_topic = 'order-output'
    * def randomInt =
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
    * def kc = new KafkaConsumer(output_topic, consumerProps)
    * def kp = new KafkaProducer()
    * def key = randomInt(1000)
    * def orderId = randomInt(1000)
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
    * print 'Send record for key = ' + key + ' orderId = ' + orderId
    # Read the topic
    * json output = kc.take()
      # Dont forget to close the consumer and producer
    * kp.close()
    * kc.close()
    * print output
    * def outkey = output.key
    * def outvalue = output.value
    * match outkey == key
    * match outvalue.id == orderId
    * match outvalue.customer.firstName == "John"
    * match outvalue.total == 44
    * match order.lineItems == '#[2]'
