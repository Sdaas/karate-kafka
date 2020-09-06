package karate.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Map;

public class MyGenericSerializer<T> implements Serializer<T> {

    private final IntegerSerializer integerSerializer = new IntegerSerializer();
    private final LongSerializer longSerializer = new LongSerializer();
    private final StringSerializer stringSerializer = new StringSerializer();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MyGenericSerializer() {
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public byte[] serialize(String topic, T data) {

            // Stupid code
            if( data instanceof Integer) {
                return integerSerializer.serialize(topic, (Integer) data);
            }
            else if( data instanceof Long) {
                return longSerializer.serialize(topic, (Long) data);
            }
            else if( data instanceof String) {
                return stringSerializer.serialize(topic, (String) data);
            }
            else {
                try {
                    return this.objectMapper.writeValueAsBytes(data);
                } catch (JsonProcessingException e) {
                    throw new SerializationException(e);
                }
            }
    }

    @Override
    public void close() {

    }
}