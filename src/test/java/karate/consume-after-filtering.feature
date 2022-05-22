Feature: Karate-Kafka Demo-2

  # Demonstration of writing a Json to a Kafka topic and reading it back based on the key AND/OR value patterns

  Background:

    # Remember that all the code in the Background section gets executed for every scenario
    * def KafkaConsumer = Java.type('karate.kafka.KarateKafkaConsumer')
    * def KafkaProducer = Java.type('karate.kafka.KarateKafkaProducer')
    * def topic = 'test-topic-2'


  Scenario: Write Jsons to topic and read only the records matching given key and value filters

    * def consumerProps = KafkaConsumer.getDefaultProperties()
    * consumerProps.group_id = "test-consumer-1"
    * print consumerProps
    # Create a consumer. It starts listening to the topic as soon as it is created.
    # It consumes only the records that meet the key filter and value filter criteria
    # keyFilter: test.*  -- This is a java regular expression
    # valueFilter: [?(@.message =~ /hi.*/)]  -- This is a jsonPath predicate expression
    # See https://github.com/json-path/JsonPath for JsonPath
    * def kc = new KafkaConsumer(topic, consumerProps, "test.*", "[?(@.message =~ /hi.*/)]")
    # Create a producer
    * def kp = new KafkaProducer()
    # Sending a message without key ...
    * kp.send(topic, { message: "hello world"} )
    # Sending a message with a key
    * kp.send(topic, "the_key", { message: "hello again"})
    # Sending a message with a key that starts with test.
    * kp.send(topic, "test_key", { message: "hello from test"})
    # Sending another message with a key that starts with test and the value that starts with hi. .
    * kp.send(topic, "test_key2", { message : "hi from test"} )
    # Remember to close the producer ....
    * kp.close()

    # Read the output. The call to take() will block until some data is available.
    # The data read from the topic is always a JSON and consists of a key and value
    * json output1 = kc.take()
    # Remember to close before doing the match. Otherwise if the test fails
    # you will not be able to close the consumer
    * kc.close()

    # Print it - the most advanced form of debugging
    * print output1

    # Doing the match
    # The consumer is expected to consume only the keys that start with 'test' and the values start with 'hi'
    # See https://intuit.github.io/karate/#karate-expressions
    * match output1 == { key : 'test_key2', value : {message: hi from test} }