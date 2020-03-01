package com.daasworld.order;

import com.daasworld.order.domain.Contact;
import com.daasworld.order.domain.Customer;
import com.daasworld.order.domain.LineItem;
import com.daasworld.order.domain.Order;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Properties;

public class OrderProducer {

    private static Logger logger = LoggerFactory.getLogger(OrderProducer.class.getName());
    private KafkaProducer<Integer, Order> producer;

    private Properties getProducerConfig() {

        // create producer properties
        // See https://kafka.apache.org/documentation/#producerconfigs for Producer configuration
        Properties pp = new Properties();

        // all the mandatory configurations
        pp.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        pp.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, IntegerSerializer.class.getName());
        pp.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, OrderJsonSerializer.class.getName());

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
        ProducerRecord<Integer,Order> record = new ProducerRecord<>("order-input", id, order);

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
        Properties pp = getProducerConfig();
        producer = new KafkaProducer<>(pp);
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
            LineItem item = new LineItem(123, 5, 10);
            ArrayList<LineItem> items = new ArrayList<>();
            items.add(item);
            Contact c = new Contact("john@gmail.com", "858-123-4455");
            Customer customer = new Customer("john", "doe", c);
            Order order = new Order(customer);
            order.setLineItems(items);

            // for debugging
            p.inspect(order);

            p.produce(order);

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
