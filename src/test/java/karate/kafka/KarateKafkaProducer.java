package karate.kafka;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Properties;


// This is just a wrapper around a Kafka Producer
public class KarateKafkaProducer {

    private static Logger logger = LoggerFactory.getLogger(KarateKafkaProducer.class.getName());

    private KafkaProducer<String,String> kafka;

    public KarateKafkaProducer(Map<String,Object> map) {
        Properties pp = new Properties();
        for( String key : map.keySet()){
            String value = (String) map.get(key);
            pp.setProperty(key,value);
        }
        kafka = new KafkaProducer<String, String>(pp);
    }

    public KarateKafkaProducer() {
        Properties pp = getDefaultProperties();
        kafka = new KafkaProducer<String, String>(pp);
    }

    public void send(String topic, String value) {
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, value);
        // send to Kafka. Remember this is async
        kafka.send(record);
    }

    public void send(String topic, String key, String value) {
        send(topic, key, value, null);
    }

    public void send(String topic, String key, String value, java.util.function.Consumer<String> handler) {

        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);
        if(handler == null) {
            kafka.send(record);
        }
        else {
            kafka.send(record, new Callback() {

                @Override
                public void onCompletion(RecordMetadata recordMetadata, Exception e) {
                    if ( e != null ) {
                        // there was an error sending it
                        handler.accept(e.getMessage());
                    } else {
                        // the data was successfully sent
                        handler.accept(recordMetadata.toString());
                    }
                }
            });
        }
    }

    public void close() {
        logger.info("producer is shutting down ...");
        kafka.close();
    }

    public static Properties getDefaultProperties(){
        // create producer properties
        // See https://kafka.apache.org/documentation/#producerconfigs for Producer configuration
        Properties pp = new Properties();
        pp.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        pp.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        pp.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

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

}
