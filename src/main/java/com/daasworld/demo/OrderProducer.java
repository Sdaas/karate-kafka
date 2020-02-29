package com.daasworld.demo;

import com.daasworld.demo.domain.Contact;
import com.daasworld.demo.domain.Customer;
import com.daasworld.demo.domain.LineItem;
import com.daasworld.demo.domain.Order;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.connect.json.JsonSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class OrderProducer {

    private static Logger logger = LoggerFactory.getLogger(OrderProducer.class.getName());
    private KafkaProducer<Integer, Order> producer;

    private Properties getProducerConfig() {

        // create producer properties
        // See https://kafka.apache.org/documentation/#producerconfigs for Producer configuration
        Properties pp = new Properties();
        pp.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");

        // no default serializer - specified when the producer is created

        // create a safe producer
        pp.setProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG,"true");
        pp.setProperty(ProducerConfig.ACKS_CONFIG,"all");
        pp.setProperty(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION,"5");

        // high throughput producer
        pp.setProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG,"snappy");
        pp.setProperty(ProducerConfig.LINGER_MS_CONFIG,"20"); // linger for 20ms
        pp.setProperty(ProducerConfig.BATCH_SIZE_CONFIG, Integer.toString(32*1024)); // 32KBytes

        return pp;
    }

    public void produce(Order order){
        Integer id = order.getId();
        ProducerRecord<Integer,Order> record = new ProducerRecord<>("test-topic", id, order);

        producer.send(record, (recordMetadata, e) -> {
            if ( e != null ) {
                // there was an error sending it
                logger.error("Error sending record");
            }
            else {
                logger.info("sent Order id=" + order.getId());
            }
        });
    }

    public OrderProducer() {

        Serializer<Integer> keySerializer = new IntegerSerializer();
        Serializer<Order> valueSerializer = new MyJsonSerializer<>();
        Properties pp = getProducerConfig();
        producer = new KafkaProducer<>(pp, keySerializer, valueSerializer);
    }

    private String inspect(Order order ){
        ObjectMapper objectMapper = new ObjectMapper();
        String str = null;
        try {
            str = objectMapper.writeValueAsString(order);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return str;
    }

    public static void main(String[] args) {
        OrderProducer p = new OrderProducer();
        while(true) {

            // Create a domain object
            LineItem item = new LineItem(123, 10);
            Contact c = new Contact("john@gmail.com", "858-123-4455");
            Customer customer = new Customer("john", "doe", c);
            Order order = new Order(customer,item);

            p.inspect(order);

            p.produce(order);

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
