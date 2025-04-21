package codes.thischwa.cf;

import codes.thischwa.cf.model.ZoneMultipleResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

public class ObjectMapperTest {

  @Test
  void testObjectMapper() throws IOException {
    ObjectMapper mapper = JsonConf.initObjectMapper();
    ZoneMultipleResponse resp =
      mapper.readValue(this.getClass().getResourceAsStream("/zone-list-response.json"),
        ZoneMultipleResponse.class);
    assertNotNull(resp.getResponseResultInfo());
  }
}
