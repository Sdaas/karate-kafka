package demo.wordcount;

import org.apache.kafka.common.serialization.*;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.errors.LogAndContinueExceptionHandler;
import org.apache.kafka.streams.kstream.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;


public class WordCountStream {

    private static Logger logger = LoggerFactory.getLogger(demo.wordcount.WordCountStream.class.getName());
    private static final CountDownLatch latch = new CountDownLatch(1);
    private String inputTopic;
    private String outputTopic;
    private String applicationName;

    public WordCountStream(){
        this.inputTopic = "words-input";
        this.outputTopic = "words-output";
        this.applicationName = "wordcount-stream";
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
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());

        config.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG,1000);

        return config;
    }

    public Topology createTopology() {

        String inputTopic = getInputTopic();
        String outputTopic = getOutputTopic();

        // The processing topology.
        StreamsBuilder builder = new StreamsBuilder();
        KStream<String,String> wordStream = builder.stream(inputTopic);


        wordStream.peek((key, value) -> logger.info("Stream got : " + value))
                .flatMapValues(value -> Arrays.asList(value.toLowerCase(Locale.getDefault()).split("\\W+")))
                .map((key, value) -> new KeyValue<String, String>(value, value))
                .groupByKey()
                .count()
                .toStream()
                .peek((key,value) -> logger.info("Output : " + key + " " + value))
                .mapValues(value -> Long.toString(value))
                .to(outputTopic);

        return builder.build();
    }

    public static void main(String[] args) {

        // Create the topology
        WordCountStream wc = new WordCountStream();
        Topology topology = wc.createTopology();

        // print out the topology
        System.out.println(topology.describe());

        // Execute everything
        Properties config = wc.getConfig();
        KafkaStreams stream = new KafkaStreams(topology,config);


        // Delete the application's local state.
        // This will ensure ( for example ) that the word count from the previous run have been forgotten
        stream.cleanUp();

        // Adding shutdown hooks for clean shutdown.
        Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook") {
            @Override
            public void run() {
                System.out.println("Shutting down the stream ...");
                stream.close();
                latch.countDown();
            }
        });

        try {
            System.out.println("Starting the stream ...");
            stream.start();
            System.out.println("Ctrl-C to exit ...");
            latch.await();
            System.out.println("Stream has been shutdown.");
        } catch (Throwable e) {
            System.exit(1);
        }
        System.exit(0);
    }
}
