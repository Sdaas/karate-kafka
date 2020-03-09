package wordcount;

import demo.wordcount.WordCountStream;
import org.apache.kafka.common.serialization.*;
import org.apache.kafka.streams.*;

import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.*;



public class WordCountStreamTest {

    private TopologyTestDriver testDriver;
    private Topology topology;
    private WordCountStream wordCount;
    private Properties config;
    private TestInputTopic<String,String> testInputTopic;
    private TestOutputTopic<String,Long> testOutputTopic;

    @Before
    public void setup(){

        wordCount = new WordCountStream();
        topology = wordCount.createTopology();
        config = wordCount.getConfig();
        testDriver = new TopologyTestDriver(topology,config);

        String inputTopicName = wordCount.getInputTopic();
        String outputTopicName = wordCount.getOutputTopic();

        Serializer<String> stringSerializer = new StringSerializer();
        Deserializer<String> stringDeserializer = new StringDeserializer();
        Deserializer<Long> longDeserializer = new LongDeserializer();

        testInputTopic = testDriver.createInputTopic( inputTopicName, stringSerializer, stringSerializer);
        testOutputTopic = testDriver.createOutputTopic(outputTopicName, stringDeserializer, longDeserializer);
    }

    @After
    public void closeTestDriver(){
        testDriver.close();
    }


    @Test
    // Test input with multiple words on one line
    public void singleLineInput() {

        String input = "hello Kafka hello";
        testInputTopic.pipeInput(input);

        Map<String,Long> out = testOutputTopic.readKeyValuesToMap();
        assertTrue("expect the output to have the word hello", out.containsKey("hello") );
        assertEquals("expected hello to have a count of 2", new Long(2), out.get("hello"));
        assertTrue("expect the output to have the word kafka", out.containsKey("kafka") );
        assertEquals("expected kafka to have a count of 1", new Long(1), out.get("kafka"));
    }
}
