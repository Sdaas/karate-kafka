package karate.kafka;

import com.intuit.karate.junit5.Karate;

class KafkaRunner {

    @Karate.Test
    Karate testAll() {
        return Karate.run().relativeTo(getClass());
    }

}