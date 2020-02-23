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
    private String kafkaTopic;


    public KarateKafkaProducer(Map<String,String> params) {
        String kafkaTopic = params.get("topic"); // TODO Add error handling if this is not present in the param map
        create(kafkaTopic);
    }
    public KarateKafkaProducer(String kafkaTopic) {
        create(kafkaTopic);
    }

    // All constructors eventually call this ....
    private void create(String kafkaTopic) {
        this.kafkaTopic = kafkaTopic;
        Properties pp = getDefaultKafkaProducerProperties();
        kafka = new KafkaProducer<>(pp);
    }

    // The map must contain two keys
    // eventKey : the string to be used as the key for the Kafka event
    // eventValue : the string to be used as the value for the Kafka event
    public void send(Map<String,String> data) {

        String key = data.get("key"); // TODO Add error handling. What happens if key is missing ?
        String value = data.get("value"); // TODO Add error handling. What happens if value is missing ?

        ProducerRecord<String, String> record = new ProducerRecord<String, String>(kafkaTopic, key, value);
        // send to Kafka. Remember this is async ....
        kafka.send(record, new Callback() {

            @Override
            public void onCompletion(RecordMetadata recordMetadata, Exception e) {
                if ( e != null ) {
                    logger.error("something bad happened");
                } else {
                    // the data was successfully sent
                    logger.info("record sent successfully");
                    logger.info("topic      : " + recordMetadata.topic());
                    logger.info("partition  : " + recordMetadata.partition());
                    logger.info("offset     : " + recordMetadata.offset());
                }
            }
        });
    }

    public void close() {
        logger.info("producer is shutting down ...");
        kafka.close();
    }

    private Properties getDefaultKafkaProducerProperties(){
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
