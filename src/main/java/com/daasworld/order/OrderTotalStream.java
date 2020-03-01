package com.daasworld.order;

import com.daasworld.order.domain.LineItem;
import com.daasworld.order.domain.Order;
import com.daasworld.karate.MyJsonDeserializer;
import org.apache.kafka.common.serialization.*;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Properties;


public class OrderTotalStream {

    private static Logger logger = LoggerFactory.getLogger(OrderTotalStream.class.getName());

    private static class OrderMapper implements ValueMapper<Order, Order> {

        @Override
        public Order apply(Order order) {

            logger.info("Processing order Id: " + order.getId());
            int orderTotal = 0;
            ArrayList<LineItem> lineItems = order.getLineItems();

            for(LineItem item : lineItems){
                int lineTotal = item.getPrice() * item.getQuantity();
                item.setTotal(lineTotal);
                orderTotal += lineTotal;
            }
            order.setLineItems(lineItems);
            order.setTotal(orderTotal);
            logger.info("Total: " + orderTotal);
            return order;
        }
    }

    public static void main(String[] args) {

        Properties config = new Properties();
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        // we are not going to use these anyway TODO fix this
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        // The stream's application Id. The Consumer Group ID will be set to this value
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "order-total-stream");

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
        orderStream.peek((key, order) -> {
            logger.info("processing record");
            logger.info("key = " + key);
            logger.info("order = " + order);
        }).mapValues( new OrderMapper() )
                .to("order-output",produced);

        // Execute everything
        KafkaStreams stream = new KafkaStreams(builder.build(),config);
        stream.start();

        // shutdown hook to correctly close the streams application gracefully ...
        Runtime.getRuntime().addShutdownHook(new Thread(stream::close));
    }
}
