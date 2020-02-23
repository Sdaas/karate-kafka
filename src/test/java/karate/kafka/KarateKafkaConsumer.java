package karate.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public class KarateKafkaConsumer {

    private static Logger logger = LoggerFactory.getLogger(Producer.class.getName());

    private KafkaConsumer<String,String> kafkaConsumer;
    private String kafkaTopic;
    LinkedBlockingQueue<Map<String,String>> output = new LinkedBlockingQueue<>();

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
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    keepReading();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        t. start();
    }

    public void close() {
        logger.info("Shutting down ...");
        kafkaConsumer.close();
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

    private void keepReading() throws InterruptedException {
        System.out.println("*** Consumer is reading ****");
        while(true) {
                ConsumerRecords<String,String> records = kafkaConsumer.poll(Duration.ofMillis(100));
                if( records != null ){
                    for(ConsumerRecord record : records ) {
                        System.out.println("*** Consumer got data ****");
                        logger.info("Partition : " + record.partition() + " Offset : " + record.offset());
                        logger.info("Key : " + record.key() + " Value : " + record.value());

                        HashMap<String,String> map = new HashMap<>();
                        map.put("key", (String) record.key());
                        map.put("value", (String) record.value());

                        output.put(map);
                    }
                }
        }
    }

    public synchronized Map<String,String> read() throws InterruptedException {
        return output.take();  // wait if necessary for data to become avalable
    }
}
