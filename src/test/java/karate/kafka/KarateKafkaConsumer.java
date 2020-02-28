package karate.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
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

    private LinkedBlockingQueue<Map<Object,Object>> outputList = new LinkedBlockingQueue<>();
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public KarateKafkaConsumer(String kafkaTopic, Map<String,String> map) {

        Properties cp = new Properties();
        for( String key : map.keySet()){
            String value = (String) map.get(key);
            cp.setProperty(key,value);
        }
        create(kafkaTopic, cp);
    }

    public KarateKafkaConsumer(String kafkaTopic) {
        Properties cp = getDefaultProperties();
        create(kafkaTopic, cp);
    }

    // All constructors eventually call this ....
    private void create(String kafkaTopic, Properties cp) {
        this.kafkaTopic = kafkaTopic;

        // Create the consumer and subscribe to the topic
        kafkaConsumer= new KafkaConsumer<String, String>(cp);
        kafkaConsumer.subscribe(Collections.singleton(kafkaTopic));

        // Start the thread
        Thread t = new Thread(this);
        t.start();
    }

    public static Properties getDefaultProperties(){
        // Consumer Configuration
        Properties cp = new Properties();
        cp.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        cp.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        cp.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,StringDeserializer.class.getName());
        cp.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "karate-kafka-default-consumer-group");
        return cp;
    }

    public void close() {
        logger.info("consumer is shutting down ...");
        closed.set(true);
        kafkaConsumer.wakeup();
    }

    private Object convert( String str ) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(str, Map.class);
        } catch (JsonProcessingException e) {
            // So this was not a JSON .. then it must be a normal string
            return str;
        }
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

                        HashMap<Object,Object> map = new HashMap<>();
                        String key = (String) record.key();
                        String value = (String) record.value();

                        // Now on the producer side, key/value could have been String or a JSON. So we have to
                        // try both ... Thats what the convert() method does

                        if( key != null ) map.put("key", convert(key));
                        map.put("value", convert(value));

                        outputList.put(map);
                    }
                }
            }
        } catch (WakeupException e) {
            // Ignore exception if closing
            if (!closed.get()) throw e;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            kafkaConsumer.close();
            logger.info("consumer is now shut down.");
        }
    }

    public synchronized Map<Object,Object> take() throws InterruptedException {
        return outputList.take();  // wait if necessary for data to become available
    }
}
