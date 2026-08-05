package codes.thischwa.cf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import codes.thischwa.cf.model.AbstractResponse;
import codes.thischwa.cf.model.BatchResponse;
import codes.thischwa.cf.model.RecordMultipleResponse;
import codes.thischwa.cf.model.RecordSingleResponse;
import codes.thischwa.cf.model.ResponseResultInfo;
import codes.thischwa.cf.model.ZoneMultipleResponse;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

public class ObjectMapperTest {

  private final ObjectMapper mapper = JsonConf.initObjectMapper();

  @Test
  void testObjectMapper() {
    ZoneMultipleResponse resp =
        mapper.readValue(this.getClass().getResourceAsStream("/zone-list-response.json"),
            ZoneMultipleResponse.class);
    assertNotNull(resp.getResponseResultInfo());
  }

  @Test
  void testErrorResponse() {
    List<Class<? extends AbstractResponse>> respClasses =
        List.of(RecordSingleResponse.class, RecordMultipleResponse.class, ZoneMultipleResponse.class, BatchResponse.class);
    respClasses.forEach(this::assertErrorResponse);
  }

  private void assertErrorResponse(Class<? extends AbstractResponse> clazz) {
    InputStream in = this.getClass().getResourceAsStream("/error-response.json");
    try {
      AbstractResponse resp = mapper.readValue(in, clazz);
      assertNotNull(resp);
      assertNotNull(resp.getResponseResultInfo());
      ResponseResultInfo resultInfo = resp.getResponseResultInfo();
      assertFalse(resultInfo.isSuccess());
      assertEquals(1, resultInfo.getErrors().size());
      assertEquals(81053, resultInfo.getErrors().get(0).getCode());
    } catch (JacksonException e) {
      fail("fail for " + clazz + ": " + e.getMessage());
    }
  }
}
