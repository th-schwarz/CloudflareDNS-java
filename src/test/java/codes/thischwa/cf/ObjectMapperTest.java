package codes.thischwa.cf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import codes.thischwa.cf.model.AbstractResponse;
import codes.thischwa.cf.model.BatchResponse;
import codes.thischwa.cf.model.RecordEntity;
import codes.thischwa.cf.model.RecordMultipleResponse;
import codes.thischwa.cf.model.RecordSingleResponse;
import codes.thischwa.cf.model.RecordType;
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

  @Test
  void testSubdomainIsNotSerialized() {
    RecordEntity rec = RecordEntity.build("www.example.com", RecordType.A, 60, "1.2.3.4");
    rec.setZoneName("example.com");
    String json = mapper.writeValueAsString(rec);
    assertTrue(json.contains("\"name\":\"www.example.com\""), json);
    assertFalse(json.contains("subdomain"), json);
    assertFalse(json.contains("sld"), json);
  }

  @Test
  void testRecordFieldsAreMappedInSnakeCase() {
    String json = """
        {"id":"r1","name":"_sip._tcp.example.com","type":"SRV","ttl":300,"priority":10,
         "comment":"sip","tags":["env:prod"],
         "data":{"weight":5,"port":5060,"target":"sip.example.com"},
         "created_on":"2025-01-20T08:56:34.736214Z","modified_on":"2025-01-21T08:56:34Z"}
        """;
    RecordEntity rec = mapper.readValue(json, RecordEntity.class);
    assertEquals(10, rec.getPriority());
    assertEquals("sip", rec.getComment());
    assertEquals(List.of("env:prod"), rec.getTags());
    assertEquals(5060, rec.getData().get("port"));
    assertEquals(java.time.Instant.parse("2025-01-20T08:56:34.736214Z"), rec.getCreatedOn());
    assertEquals(java.time.Instant.parse("2025-01-21T08:56:34Z"), rec.getModifiedOn());

    String out = mapper.writeValueAsString(rec);
    assertTrue(out.contains("\"priority\":10"), out);
    assertTrue(out.contains("\"created_on\":\"2025-01-20T08:56:34.736214Z\""), out);
  }
}
