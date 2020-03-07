package demo.order;

import com.fasterxml.jackson.core.JsonProcessingException;
import demo.order.domain.Contact;
import demo.order.domain.Customer;
import demo.order.domain.LineItem;
import demo.order.domain.Order;
import org.apache.kafka.common.serialization.*;
import org.apache.kafka.streams.*;

import java.util.ArrayList;
import java.util.Properties;

import static org.junit.Assert.assertEquals;

import org.apache.kafka.streams.test.TestRecord;
import org.junit.*;

public class OrderTotalStreamTest {

    private TopologyTestDriver testDriver;
    private Topology topology;
    private OrderTotalStream orderTotal;
    private Properties config;
    private TestInputTopic<Integer,Order> testInputTopic;
    private TestOutputTopic<Integer,Order> testOutputTopic;

    @Before
    public void setup(){

        orderTotal = new OrderTotalStream();
        topology = orderTotal.createTopology();
        config = orderTotal.getConfig();
        testDriver = new TopologyTestDriver(topology,config);

        String inputTopicName = orderTotal.getInputTopic();
        String outputTopicName = orderTotal.getOutputTopic();

        Serializer<Integer> integerSerializer = new IntegerSerializer();
        Deserializer<Integer> integerDeserializer = new IntegerDeserializer();
        Serializer<Order> orderSerializer = new OrderJsonSerializer<>();
        Deserializer<Order> orderDeserializer = new OrderJsonDeserializer();

        testInputTopic = testDriver.createInputTopic( inputTopicName, integerSerializer, orderSerializer);
        testOutputTopic = testDriver.createOutputTopic(outputTopicName, integerDeserializer, orderDeserializer);
    }

    @After
    public void closeTestDriver(){
        testDriver.close();
    }


    @Test
    // Order with Zero Line Items
    public void zeroLineItemOrder() {

        Contact contact = new Contact("foo@bar.com", "801-555-1212");
        Customer customer = new Customer("John", "Doe", contact);
        Order order = new Order(customer);
        int orderId = 1234;

        // put the order into the input topic, and read back from the output topic
        testInputTopic.pipeInput(orderId, order);
        TestRecord<Integer,Order> outRecord = testOutputTopic.readRecord();
        int outOrderId = outRecord.getKey();
        Order outOrder = outRecord.getValue();

        assertEquals("expected orderId to be the same", outOrderId, orderId);
        assertEquals("expected order to have 0 line items", 0, outOrder.getLineItems().size());
        assertEquals("expected the total to be 0", 0, outOrder.getTotal());
        assertEquals("expected the customer to be the same", outOrder.getCustomer(), order.getCustomer());
    }

    @Test
    // Order with Zero Line Items
    public void testTotal() {

        Contact contact = new Contact("foo@bar.com", "801-555-1212");
        Customer customer = new Customer("John", "Doe", contact);
        Order order = new Order(customer);

        // Add a few line items to this order
        ArrayList<LineItem> items = new ArrayList<>();
        items.add(new LineItem(111,10,20)); // total 200
        items.add(new LineItem(222,5,15)); // total 75
        items.add(new LineItem(333,7,20)); // total 140
        order.setLineItems(items);
        int orderId = 5678;

        // put the order into the input topic, and read back from the output topic
        testInputTopic.pipeInput(orderId, order);
        TestRecord<Integer,Order> outRecord = testOutputTopic.readRecord();
        int outOrderId = outRecord.getKey();
        Order outOrder = outRecord.getValue();

        assertEquals("expected orderId to be the same", outOrderId, orderId);
        assertEquals("expected order to have 3 line items", 3, outOrder.getLineItems().size());
        assertEquals("expected the total to be 415", 415, outOrder.getTotal());
        assertEquals("expected the customer to be the same", outOrder.getCustomer(), order.getCustomer());
    }
}
