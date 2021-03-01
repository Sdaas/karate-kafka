package karate.kafka;

import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.JsonEncoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

public class KarateKafkaAvroConsumer extends AbstractKarateKafkaConsumer {

  public KarateKafkaAvroConsumer(String kafkaTopic, Map<String, String> consumerProperties) {
    super(kafkaTopic, consumerProperties, null, null);
  }

  public KarateKafkaAvroConsumer(String kafkaTopic) {
    super(kafkaTopic, null, null);
  }

  public KarateKafkaAvroConsumer(String kafkaTopic, String keyFilterExpression, String valueFilterExpression) {
    super(kafkaTopic, keyFilterExpression, valueFilterExpression);
  }

  public KarateKafkaAvroConsumer(
      String kafkaTopic,
      Map<String, String> consumerProperties,
      String keyFilterExpression,
      String valueFilterExpression) {
    super(kafkaTopic, consumerProperties, keyFilterExpression, valueFilterExpression);
  }

  public static Properties getDefaultProperties() {
    return AbstractKarateKafkaConsumer.getDefaultProperties();
  }


  @Override
  Object convertValue(Object value) {
    GenericRecord record = (GenericRecord) value;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      DatumWriter<GenericRecord> writer = new GenericDatumWriter<>(record.getSchema());
      JsonEncoder encoder = EncoderFactory.get().jsonEncoder(record.getSchema(), baos);
      writer.write(record, encoder);
      encoder.flush();
      return baos.toString();
    } catch (IOException e) {
      logger.error("Unable serialize the AVRO record", e);
    }
    return null;
  }

}
