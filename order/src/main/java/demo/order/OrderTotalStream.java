package demo.order;


import demo.order.domain.LineItem;
import demo.order.domain.Order;
import org.apache.kafka.common.serialization.*;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.errors.LogAndContinueExceptionHandler;
import org.apache.kafka.streams.kstream.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Properties;


public class OrderTotalStream {

    private static Logger logger = LoggerFactory.getLogger(OrderTotalStream.class.getName());
    private String inputTopic;
    private String outputTopic;
    private String applicationName;

    public OrderTotalStream(){
        this.inputTopic = "order-input";
        this.outputTopic = "order-output";
        this.applicationName = "order-total-stream";
    }

    public String getInputTopic(){
        return inputTopic;
    }

    public String getOutputTopic(){
        return outputTopic;
    }

    public Properties getConfig(){
        Properties config = new Properties();
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, applicationName);
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
        config.put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndContinueExceptionHandler.class.getName());

        return config;
    }

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

    public Topology createTopology() {

        String inputTopic = getInputTopic();
        String outputTopic = getOutputTopic();

        // Specify the Serdes for the input Stream and output streams. In both cases, the key is Long,
        // and the value is a Json of Order.class

        Serializer<Integer> keySerializer = new IntegerSerializer();
        Deserializer<Integer> keyDeserializer = new IntegerDeserializer();
        Serde<Integer> keySerde = Serdes.serdeFrom(keySerializer, keyDeserializer);

        Serializer<Order> valueSerializer = new OrderJsonSerializer<>();
        Deserializer<Order> valueDeserializer = new OrderJsonDeserializer();
        Serde<Order> valueSerde = Serdes.serdeFrom(valueSerializer, valueDeserializer);

        Consumed<Integer, Order> consumed = Consumed.with(keySerde,valueSerde);
        Produced<Integer,Order> produced = Produced.with(keySerde, valueSerde);

        // The processing topology
        StreamsBuilder builder = new StreamsBuilder();
        KStream<Integer,Order> orderStream = builder.stream(inputTopic,consumed);

        orderStream.peek((key, order) -> {
            logger.info("processing record");
            logger.info("key = " + key);
            logger.info("order = " + order);
        }).mapValues( new OrderMapper() )
                .to(outputTopic,produced);

        return builder.build();
    }

    public static void main(String[] args) {

        // Create the topology
        OrderTotalStream orderTotal = new OrderTotalStream();
        Topology topology = orderTotal.createTopology();
        Properties config = orderTotal.getConfig();

        // Execute everything
        KafkaStreams stream = new KafkaStreams(topology,config);
        stream.start();

        // shutdown hook to correctly close the streams application gracefully ...
        Runtime.getRuntime().addShutdownHook(new Thread(stream::close));
    }
}
