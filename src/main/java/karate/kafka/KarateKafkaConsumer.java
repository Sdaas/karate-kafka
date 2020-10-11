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

public class KarateKafkaConsumer implements Runnable {

  private static Logger logger = LoggerFactory.getLogger(KarateKafkaConsumer.class.getName());
  private KafkaConsumer<Object, Object> kafka;
  private CountDownLatch startupLatch = new CountDownLatch(1);
  private CountDownLatch shutdownLatch = new CountDownLatch(1);
  private boolean partitionsAssigned = false;
  private Pattern keyFilter; // Java regular expression
  private String valueFilter; // Json path expression

  private BlockingQueue<String> outputList = new LinkedBlockingQueue<>();

  public KarateKafkaConsumer(String kafkaTopic, Map<String, String> consumerProperties) {
    this(kafkaTopic, consumerProperties, null, null);
  }

  public KarateKafkaConsumer(String kafkaTopic) {
    this(kafkaTopic, null, null);
  }

  public KarateKafkaConsumer( String kafkaTopic, String keyFilterExpression, String valueFilterExpression) {
    Properties cp = getDefaultProperties();
    setKeyValueFilters(keyFilterExpression, valueFilterExpression);
    create(kafkaTopic, cp);
  }

  public KarateKafkaConsumer(
      String kafkaTopic,
      Map<String, String> consumerProperties,
      String keyFilterExpression,
      String valueFilterExpression) {

    setKeyValueFilters(keyFilterExpression, valueFilterExpression);
    Properties cp = new Properties();
    for (String key : consumerProperties.keySet()) {
      String value = consumerProperties.get(key);
      cp.setProperty(key, value);
    }
    create(kafkaTopic, cp);
  }

  // All constructors eventually call this ....
  private void create(String kafkaTopic, Properties cp) {

    // Create the consumer and subscribe to the topic
    kafka = new KafkaConsumer<Object, Object>(cp);
    kafka.subscribe(Collections.singleton(kafkaTopic));
    // Start the thread which will poll the topic for data.
    Thread t = new Thread(this);
    t.start();

    // And we will wait until topics have been assigned to this consumer
    // Once topics have been assigned to this consumer, the latch is set. Until then we wait ...
    // and wait ... and wait ...
    logger.debug("Waiting for consumer to be ready..");
    try {
      startupLatch.await();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    logger.debug("consumer is ready");
  }

  /**
   * Sets the predicate to filter kafka records based on key or/and value
   *
   * @param keyFilterExpression Java regular expression pattern
   * @param valueFilterExpression <a href="https://github.com/json-path/JsonPath">JsonPath</a>
   *     expression
   */
  private void setKeyValueFilters(String keyFilterExpression, String valueFilterExpression) {
    if (!isEmpty(keyFilterExpression)) {
      this.keyFilter = Pattern.compile(keyFilterExpression);
    }
    if (!isEmpty(valueFilterExpression)) {
      this.valueFilter = valueFilterExpression;
    }
  }

  public static Properties getDefaultProperties() {
    // Consumer Configuration
    Properties cp = new Properties();
    cp.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");

    cp.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    cp.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    cp.setProperty(ConsumerConfig.GROUP_ID_CONFIG, "karate-kafka-default-consumer-group");
    return cp;
  }

  public void close() {
    logger.debug("asking consumer to shutdown ...");
    kafka.wakeup();
    try {
      shutdownLatch.await();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public void signalWhenReady() {

    if (!partitionsAssigned) {
      logger.debug("checking partition assignment");
      Set<TopicPartition> partitions = kafka.assignment();
      if (partitions.size() > 0) {
        partitionsAssigned = true;
        logger.debug("partitions assigned to consumer ...");
        startupLatch.countDown();
      }
    }
  }

  public void run() {

    // Until consumer.poll() is caled, the consumer is just idling.
    // Only after poll() is invoked, it will initiate a connection to the cluster, get assigned
    // partitions and attempt to fetch messages.
    // So we will call poll() and then wait for some time until the partition is assigned.
    // Then raise a signal ( countdown latch ) so that the constructor can return

    try {
      while (true) {
        // Read for records and handle it
        ConsumerRecords<Object, Object> records = kafka.poll(Duration.ofMillis(500));
        signalWhenReady();
        if (records != null) {
          for (ConsumerRecord record : records) {
            logger.debug("*** Consumer got data ****");

            Object key = record.key();
            Object value = record.value();
            Headers recordHeaders = record.headers();

            logger.debug("Partition : " + record.partition() + " Offset : " + record.offset());
            if (key == null) logger.debug("Key : null");
            else logger.debug("Key : " + key + " Type: " + key.getClass().getName());
            logger.debug("Value : " + value + " Type: " + value.getClass().getName());

            // We want to return a String that can be interpreted by Karate as a JSON
            String str = convertToJsonString(key, value, recordHeaders);

            if (!isNull(keyFilter) && !filterByKey(key)) {
              continue;
            }
            if (!isNull(valueFilter) && !filterByValue(value)) {
              continue;
            }
            logger.debug("Consuming record. key: " + key + ", value: " + value);
            outputList.put(str);
          }
        }
      }
    } catch (WakeupException e) {
      logger.debug("Got WakeupException");
      // nothing to do here
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      logger.debug("consumer is shutting down ...");
      kafka.close();
      logger.debug("consumer is now shut down.");
      shutdownLatch.countDown();
    }
  }

  private String convertToJsonString(Object key, Object value, Headers recordHeaders){
    ObjectMapper objectMapper = new ObjectMapper();
    Map<String,String> map = new HashMap<>();
    for(Header h : recordHeaders) {
      String headerKey = h.key();
      String headerValue = new String(h.value());
      map.put(headerKey,headerValue);
    }

    if( map.size() == 0 ) {
      // There were no headers
      return  "{key: " + key + ", value: " + value + "}";
    }
    else {
      // headers are present ...
      String headers;
      try {
        headers = objectMapper.writeValueAsString(map);
      } catch (JsonProcessingException e) {
        headers =  "error";
        logger.error("Unable to parse header");
      }
      return "{key: " + key + ", value: " + value + ", headers: " + headers + "}";
    }
  }

  /**
   * @param value The kafka record value
   * @return
   */
  private boolean filterByValue(Object value) {
    try {
      return !isNull(value)
          && !JsonPath.parse(value.toString()).read(valueFilter, List.class).isEmpty();
    } catch (JsonPathException e) {
      logger.error("Exception while trying to filter value", e);
    }
    return false;
  }

  /**
   * Checks whether the given string matches the keyFilter regular expression
   *
   * @param key String to be checked for pattern matching
   * @return true if the key matches the keyFilter pattern. False otherwise
   */
  private boolean filterByKey(Object key) {
    return !isNull(key) && keyFilter.matcher(key.toString()).find();
  }

  /**
   * @return The next available kafka record in the Queue (head of the queue). If no record is
   *     available, then the call is blocked.
   * @throws InterruptedException - if interrupted while waiting
   */
  public synchronized String take() throws InterruptedException {
    logger.debug("take() called");
    return outputList.take(); // wait if necessary for data to become available
  }

  /**
   * @param n  The number of records to read
   * @return The next available kafka record in the Queue (head of the queue). If no record is
   *     available, then the call is blocked.
   * @throws InterruptedException - if interrupted while waiting
   */
  public synchronized String take(int n) throws InterruptedException {
    logger.debug("take(n) called");
    List<String> list = new ArrayList<>();
    for(int i=0; i<n; i++){
      list.add(outputList.take()); // wait if necessary for data to become available
    }
    // We want to return a String that can be interpreted by Karate as a JSON
    String str = list.toString();
    return str;
  }

  /**
   * @param timeout  maximum time in milliseconds to wait for a record
   * @return The next available kafka record in the Queue (head of the queue). If no record is
   *     available for timeout milliseconds, then return null
   * @throws InterruptedException - if interrupted while waiting
   */
  public synchronized String poll(long timeout) throws InterruptedException {
    logger.debug("poll() called");
    return outputList.poll(timeout, TimeUnit.MILLISECONDS);
  }

}
