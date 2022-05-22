package karate;

import com.intuit.karate.Results;
import com.intuit.karate.Runner;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
class KarateTests {

    private static final File dockerComposeFile = new File("docker-compose.yml");
    private static final DockerComposeContainer environment = new DockerComposeContainer(dockerComposeFile);

    @BeforeAll
    static void beforeAll() throws IOException {
        // This will block until containers are up
        System.out.println("Starting test containers ...");
        environment.start();
        System.out.println("environment started");
        Runtime.getRuntime().addShutdownHook(new Thread(()->stopContainers()));
    }

    private static void stopContainers(){
        environment.stop();

    }
    @AfterAll
    static void afterAll() throws Throwable {
        stopContainers();
    }

    @Test
    void testall() {
        Results results = Runner.path("classpath:karate").parallel(1);
        assertEquals(0, results.getFailCount(), results.getErrorMessages());
    }

}

