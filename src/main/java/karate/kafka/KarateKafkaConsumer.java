package karate.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.JsonPathException;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static com.jayway.jsonpath.internal.Utils.isEmpty;
import static java.util.Objects.isNull;

public class KarateKafkaConsumer extends AbstractKarateKafkaConsumer {

  public KarateKafkaConsumer(String kafkaTopic, Map<String, String> consumerProperties) {
    super(kafkaTopic, consumerProperties, null, null);
  }

  public KarateKafkaConsumer(String kafkaTopic) {
    super(kafkaTopic, null, null);
  }

  public KarateKafkaConsumer( String kafkaTopic, String keyFilterExpression, String valueFilterExpression) {
    super(kafkaTopic, keyFilterExpression, valueFilterExpression);
  }

  public KarateKafkaConsumer(
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
    return value;
  }
}
