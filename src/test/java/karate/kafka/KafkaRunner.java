package karate.kafka;

import com.intuit.karate.Results;
import com.intuit.karate.Runner;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class KafkaRunner {

    @Test
    void testSerial() {
        Results results = Runner.path("classpath:karate/kafka").tags("~@ignore").parallel(1);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }
}