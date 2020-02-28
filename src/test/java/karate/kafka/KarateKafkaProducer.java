package karate.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
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

    public void send(String topic, Object valueObject) {
        String value = convertToString(valueObject);
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, value);
        kafka.send(record);
    }

   public void send(String topic, Object key, Object value) {
        send(topic, key, value, null);
    }

    //TODO Better Error handling
    private String convertToString(Object o) {
        if( o instanceof String) {
            return (String) o;
        }
        else if( o instanceof Map) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                return mapper.writeValueAsString(o); // convert map to a Json string
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                throw new IllegalArgumentException("Unable to convert key/value to String");
            }
        }
        else throw new IllegalArgumentException("Expected String or HashMap. Instead got " + o.getClass().getName().toString());
    }

    public void send(String topic, Object keyObject, Object valueObject, java.util.function.Consumer<String> handler) {

        String key = convertToString(keyObject);
        String value = convertToString(valueObject);
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

                        HashMap<String, String> map = new HashMap<>();
                        map.put("topic", recordMetadata.topic());
                        map.put("partition", Integer.toString(recordMetadata.partition()));
                        if(recordMetadata.hasOffset()) {
                            map.put("offset", Long.toString(recordMetadata.offset()));
                        }
                        if(recordMetadata.hasTimestamp()) {
                            map.put("timestamp", Long.toString(recordMetadata.timestamp()));
                        }
                        handler.accept(map.toString());
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
