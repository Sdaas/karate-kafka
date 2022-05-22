import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class JsonPathTest {

  private static Logger logger = LoggerFactory.getLogger(JsonPathTest.class.getName());

  @Test
  public void test_json_path_predicates() {
    String payload = "{\"id\":\"one\"}";
    String filterExpressionOne = "[?(@.id == 'two')]";
    Assertions.assertTrue(JsonPath.parse(payload).read(filterExpressionOne, List.class).isEmpty());
    filterExpressionOne = "[?(@.id == 'one')]";
    Assertions.assertFalse(JsonPath.parse(payload).read(filterExpressionOne, List.class).isEmpty());
  }
}
