package com.daasworld.demo;

import com.daasworld.demo.domain.Order;
import com.daasworld.karate.MyJsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.kafka.common.serialization.*;
import org.apache.kafka.connect.json.JsonDeserializer;
import org.apache.kafka.connect.json.JsonSerializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.chrono.ThaiBuddhistDate;
import java.util.Properties;

public class OrderTotalStream {

    private static Logger logger = LoggerFactory.getLogger(OrderTotalStream.class.getName());

    public static void main(String[] args) {

        Properties config = new Properties();
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        // we are not going to use these anyway TODO fix this
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        // The stream's application Id. The Consumer Group ID will be set to this value
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "order-domain-demo-order-total-stream");

        // Specify the Serdes for the input Stream and output streams. In both cases, the key is Long,
        // and the value is a Json of Order.class

        Serializer<Integer> keySerializer = new IntegerSerializer();
        Deserializer<Integer> keyDeserializer = new IntegerDeserializer();
        Serde<Integer> keySerde = Serdes.serdeFrom(keySerializer, keyDeserializer);

        Serializer<Order> valueSerializer = new OrderJsonSerializer<>();
        Deserializer<Order> valueDeserializer = new MyJsonDeserializer<Order>(Order.class);
        Serde<Order> valueSerde = Serdes.serdeFrom(valueSerializer, valueDeserializer);

        Consumed<Integer, Order> consumed = Consumed.with(keySerde,valueSerde);
        Produced<Integer,Order> produced = Produced.with(keySerde, valueSerde);


        // The processing topology
        StreamsBuilder builder = new StreamsBuilder();
        KStream<Integer,Order> orderStream = builder.stream("order-input",consumed);
        orderStream.to("order-output",produced);

        // Execute everything
        KafkaStreams stream = new KafkaStreams(builder.build(),config);
        stream.start();

        // shutdown hook to correctly close the streams application gracefully ...
        Runtime.getRuntime().addShutdownHook(new Thread(stream::close));
    }
}
