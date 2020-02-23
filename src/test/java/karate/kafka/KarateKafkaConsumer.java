package karate.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class KarateKafkaConsumer implements Runnable{

    private static Logger logger = LoggerFactory.getLogger(KarateKafkaConsumer.class.getName());

    private KafkaConsumer<String,String> kafkaConsumer;
    private String kafkaTopic;
    LinkedBlockingQueue<Map<String,String>> output = new LinkedBlockingQueue<>();
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public KarateKafkaConsumer(Map<String,String> params) {
        String kafkaTopic = params.get("topic"); // TODO Add error handling if this is not present in the param map
        create(kafkaTopic);
    }

    public KarateKafkaConsumer(String kafkaTopic) {
        create(kafkaTopic);
    }

    // All constructors eventually call this ....
    private void create(String kafkaTopic) {
        this.kafkaTopic = kafkaTopic;
        Properties cp = getDefaultKafkaConsumerProperties();

        // Create the consumer and subscribe to the topic
        kafkaConsumer= new KafkaConsumer<String, String>(cp);
        kafkaConsumer.subscribe(Collections.singleton(kafkaTopic));

        // Start the thread
        Thread t = new Thread(this);
        t.start();
    }

    private Properties getDefaultKafkaConsumerProperties(){
        // Consumer Configuration
        Properties cp = new Properties();
        cp.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        cp.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        cp.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,StringDeserializer.class.getName());
        cp.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "consumer_group1");
        cp.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"); // from beginning of topic

        return cp;
    }

    public void close() {
        logger.info("consumer is shutting down ...");
        closed.set(true);
        kafkaConsumer.wakeup();
    }

    public void run(){
        logger.info("consumer is reading ...");
        try {
            while (!closed.get()) {
                // Read for records and handle it
                ConsumerRecords<String,String> records = kafkaConsumer.poll(Duration.ofMillis(100));
                if( records != null ){
                    for(ConsumerRecord record : records ) {
                        logger.info("*** Consumer got data ****");
                        logger.info("Partition : " + record.partition() + " Offset : " + record.offset());
                        logger.info("Key : " + record.key() + " Value : " + record.value());

                        HashMap<String,String> map = new HashMap<>();
                        map.put("key", (String) record.key());
                        map.put("value", (String) record.value());

                        output.put(map);
                    }
                }
            }
        } catch (WakeupException e) {
            // Ignore exception if closing
            if (!closed.get()) throw e;
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            kafkaConsumer.close();
            logger.info("consumer is now shut down.");
        }
    }

    public synchronized Map<String,String> read() throws InterruptedException {
        return output.take();  // wait if necessary for data to become available
    }
}
