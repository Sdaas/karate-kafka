package com.daasworld.order;

import com.daasworld.order.domain.Order;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;

public class OrderConsumer{

    private static Logger logger = LoggerFactory.getLogger(OrderConsumer.class.getName());
    private KafkaConsumer<Integer, Order> consumer;


    public OrderConsumer() {

        // Create the consumer
        Deserializer<Integer> keyDeserializer = new IntegerDeserializer();
        Deserializer<Order> valueDeserializer = new OrderJsonDeserializer();
        Properties cp = getDefaultProperties();
        consumer= new KafkaConsumer<>(cp, keyDeserializer, valueDeserializer);

        //and subscribe to the topic
        consumer.subscribe(Collections.singleton("order-output"));
    }

    public static Properties getDefaultProperties(){
        // Consumer Configuration
        Properties cp = new Properties();
        // mandatory configuration properties. See https://kafka.apache.org/documentation/#consumerconfigs
        cp.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        cp.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class.getName());
        cp.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, OrderJsonDeserializer.class.getName());
        cp.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "order-demo-consumer-group");
        return cp;
    }

    public void process(){

        logger.info("consumer is reading ...");
        try {
            while (true) {
                // Read for records and handle it
                ConsumerRecords<Integer,Order> records = consumer.poll(Duration.ofMillis(100));
                if( records != null ){
                    for(ConsumerRecord record : records ) {
                        logger.info("*** Consumer got data ****");
                        logger.info("Key : " + record.key());
                        logger.info("Value : " + record.value());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            consumer.close();
            logger.info("consumer is now shut down.");
        }
    }

    public static void main(String[] args) {

        OrderConsumer c = new OrderConsumer();
        c.process();

        //TODO Shutdown code
    }
}
