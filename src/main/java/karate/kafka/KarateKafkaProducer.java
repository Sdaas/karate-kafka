package karate.kafka;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


// This is just a wrapper around a Kafka Producer
public class KarateKafkaProducer {

    private static Logger logger = LoggerFactory.getLogger(KarateKafkaProducer.class.getName());

    private KafkaProducer<Object,Object> kafka;

    public KarateKafkaProducer() {
        Properties pp = getDefaultProperties();
        createKafkaProducer(pp);
    }

    private void createKafkaProducer(Properties pp) {
        kafka = new KafkaProducer<>(pp);
    }

    public KarateKafkaProducer(Map<String,String> map) {
        Properties pp = new Properties();
        for( String key : map.keySet()){
            String value = map.get(key);
            pp.setProperty(key,value);
        }
        createKafkaProducer(pp);
    }

    public void send(String topic, Object value) {
        ProducerRecord<Object, Object> record = new ProducerRecord<>(topic, value);
        kafka.send(record);
    }

    public void send(String topic, Object key, Object value) {
        send(topic, key, value, null, null );
    }

    // key and value can anything - int, long, string, json (Map<String,Object>)
    // but the headers must be properties. Currently we support only String in the header ( not long/int etc )
    public void send(String topic, Object key, Object value, Map<String,String> headers) {
        send(topic, key, value, headers, null );
    }

    public void send(String topic, Object recordKey, Object value, Map<String,String> headers, java.util.function.Consumer<String> handler) {

        ProducerRecord<Object, Object> record = new ProducerRecord<>(topic, recordKey, value);
        if( headers != null ) {
            Headers recordHeaders = record.headers();
            for(String headerKey : headers.keySet()) {
                byte[] headerValue = headers.get(headerKey).getBytes();
                recordHeaders.add( new RecordHeader(headerKey,headerValue));
            }
        }
        kafka.send(record, new Callback() {

            @Override
            public void onCompletion(RecordMetadata recordMetadata, Exception e) {
                if ( e != null ) {
                    // there was an error sending it
                    logger.error("Error sending message");
                    logger.error(e.getMessage());
                    if( handler != null )
                        handler.accept(e.getMessage());
                } else {
                    // the data was successfully sent
                    logger.debug("Successfully sent message");
                    logger.debug("topic: " + recordMetadata.topic() + " partition: " + recordMetadata.partition() + " offset: " + recordMetadata.offset());

                    if( handler != null ) {
                        HashMap<String, String> map = new HashMap<>();
                        map.put("topic", recordMetadata.topic());
                        map.put("partition", Integer.toString(recordMetadata.partition()));
                        if (recordMetadata.hasOffset()) {
                            map.put("offset", Long.toString(recordMetadata.offset()));
                        }
                        if (recordMetadata.hasTimestamp()) {
                            map.put("timestamp", Long.toString(recordMetadata.timestamp()));
                        }
                        handler.accept(map.toString());
                    }
                }
            }
        });
    }

    public void close() {
        logger.debug("producer is shutting down ...");
        kafka.close();
    }

    public static Properties getDefaultProperties(){
        // create producer properties
        // See https://kafka.apache.org/documentation/#producerconfigs for Producer configuration
        Properties pp = new Properties();
        pp.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");

        pp.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, MyGenericSerializer.class.getName());
        pp.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, MyGenericSerializer.class.getName());

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
