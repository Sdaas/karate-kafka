package demo.order;

import demo.order.domain.Contact;
import demo.order.domain.Customer;
import demo.order.domain.LineItem;
import demo.order.domain.Order;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class OrderProducer {

    private static Logger logger = LoggerFactory.getLogger(OrderProducer.class.getName());
    private KafkaProducer<Integer, Order> producer;
    private AtomicBoolean shutdown = new AtomicBoolean(false);
    private CountDownLatch shutdownLatch = new CountDownLatch(1);

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

    public void produceForever() {

        while(! shutdown.get()) {
            Order order = createRandomOrder();
            // for debugging
            inspect(order);
            produce(order);
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        logger.info("shutting down producer...");
        producer.close();
        shutdownLatch.countDown();
        logger.info("producer has been shutdown.");
    }

    public void close() {
        logger.info("asking producer to shutdown ...");
        shutdown.set(true);
        try {
            shutdownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.info("producer has been shutdown.");
    }

    private static Order createRandomOrder(){

        // Create a domain object
        Random r = new Random();
        ArrayList<LineItem> items = new ArrayList<>();
        int itemCount = r.nextInt(5) + 1;
        for( int i=0; i<itemCount; i++){
            int itemId = r.nextInt(1000);
            int quantity = r.nextInt(10) + 1;
            int price = r.nextInt(100);
            LineItem item = new LineItem(itemId, quantity, price);
            items.add(item);
        }

        Contact c = new Contact("john@gmail.com", "858-123-4455");
        Customer customer = new Customer("john", "doe", c);

        Order order = new Order(customer);
        order.setLineItems(items);

        return order;
    }

    public static void main(String[] args) {
        OrderProducer p = new OrderProducer();

        // Adding shutdown hooks for clean shutdown.
        Runtime.getRuntime().addShutdownHook(new Thread("producer-shutdown-hook") {
            @Override
            public void run() {
                // This call will block until the shutdown is complete
                p.close();
            }
        });

        System.out.println("Ctrl-C to exit ...");
        p.produceForever();
    }
}
