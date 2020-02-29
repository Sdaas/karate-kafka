package com.daasworld.demo;

import com.daasworld.demo.domain.Order;
import com.sun.tools.corba.se.idl.constExpr.Or;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
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
        Deserializer<Order> valueDeserializer = new MyJsonDeserializer<>(Order.class);
        Properties cp = getDefaultProperties();
        consumer= new KafkaConsumer<>(cp, keyDeserializer, valueDeserializer);

        //and subscribe to the topic
        consumer.subscribe(Collections.singleton("test-topic"));
    }

    public static Properties getDefaultProperties(){
        // Consumer Configuration
        Properties cp = new Properties();
        cp.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        cp.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, IntegerDeserializer.class.getName());
        cp.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName()); // never used though
        cp.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "karate-kafka-default-consumer-group");
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
                        Order order = (Order) record.value();
                        logger.info("Key : " + record.key() + " Value : " + record.value());

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
