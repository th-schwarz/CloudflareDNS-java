package codes.thischwa.cf;

import static org.junit.jupiter.api.Assertions.*;

import codes.thischwa.cf.model.RecordEntity;
import codes.thischwa.cf.model.RecordType;
import codes.thischwa.cf.model.ZoneEntity;

import java.time.LocalDate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

// TODO: #testDns should be clean-up it's test data
@Slf4j
public class CfClientTest {

  private static final String zoneStr = "mein-d-ns.de";
  private static final String sldStr = "devsld";
  private static int ttl = 60;

  private final String email = System.getenv("API_EMAIL");
  private final String apiKey = System.getenv("API_KEY");
  private final String apiToken = System.getenv("API_TOKEN");

  private final CfDnsClient client = new CfDnsClient(email, apiKey, apiToken);

  @Test
  void testList() throws Exception {
    List<ZoneEntity> zList = client.zoneListAll();
    assertEquals(1, zList.size());

    List<RecordEntity> rList = client.sldListAll(zList.get(0), "test");
    assertFalse(rList.isEmpty());

    assertThrows(
        CloudflareNotFoundException.class, () -> client.sldListAll(zList.get(0), "notexisting"));

    client.setEmptyResultThrowsException(false);
    rList = client.sldListAll(zList.get(0), "notexisting");
    assertTrue(rList.isEmpty());
    client.setEmptyResultThrowsException(true);
  }

  @Test
  void testDns() throws Exception {
    ZoneEntity z = client.zoneInfo(zoneStr);
    assertEquals("0a83dd6e7f8c46039f2517bbded8115e", z.getId());
    assertEquals("mein-d-ns.de", z.getName());
    assertEquals("active", z.getStatus());
    assertEquals(2, z.getNameServers().size());
    assertTrue(z.getNameServers().contains("sergi.ns.cloudflare.com"));
    assertEquals(4, z.getOriginalNameServers().size());
    assertTrue(z.getOriginalNameServers().contains("a.ns14.net"));
    assertNotNull(z.getActivatedOn());
    assertNotNull(z.getModifiedOn());
    assertNotNull(z.getCreatedOn());
    assertEquals(LocalDate.of(2025, 1, 20), z.getCreatedOn().toLocalDate());

    RecordEntity r = client.sldInfo(z, "test", RecordType.A);
    assertEquals("b345fec8769a2980811a8ff901b4e158", r.getId());
    assertEquals("test.mein-d-ns.de", r.getName());
    assertEquals("A", r.getType());
    assertEquals("129.0.0.3", r.getContent());

    RecordEntity createdRe1 =
        client.recordCreate(
            z, RecordEntity.build(sldStr + "." + zoneStr, RecordType.A, ttl, "130.0.0.3"));
    r = client.sldInfo(z, sldStr, RecordType.A);
    assertEquals("130.0.0.3", r.getContent());
    RecordEntity createdRe2 =
        client.recordCreate(
            z,
            RecordEntity.build(
                sldStr + "." + zoneStr, RecordType.AAAA, ttl, "2a0a:4cc0:c0:2e4::1"));
    r = client.sldInfo(z, sldStr, RecordType.AAAA);
    assertEquals("2a0a:4cc0:c0:2e4::1", r.getContent());

    createdRe2.setContent("2a0a:4cc0:c0:2e4::2");
    client.recordUpdate(z, createdRe2);
    r = client.sldInfo(z, sldStr, RecordType.AAAA);
    assertEquals("2a0a:4cc0:c0:2e4::2", r.getContent());

    r = client.sldInfo(z, sldStr, RecordType.A);
    assertEquals("130.0.0.3", r.getContent());
    assertTrue(client.recordDelete(z, createdRe2));
    assertThrows(
        CloudflareNotFoundException.class, () -> client.sldInfo(z, sldStr, RecordType.AAAA));

    client.recordDeleteTypeIfExists(z, sldStr, RecordType.A);
    assertThrows(CloudflareNotFoundException.class, () -> client.sldInfo(z, sldStr, RecordType.A));
  }
}
