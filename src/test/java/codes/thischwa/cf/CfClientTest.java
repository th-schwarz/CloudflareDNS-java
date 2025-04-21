package codes.thischwa.cf;

import codes.thischwa.cf.model.RecordEntity;
import codes.thischwa.cf.model.RecordType;
import codes.thischwa.cf.model.ZoneEntity;
import java.time.LocalDate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

@Slf4j
public class CfClientTest {

  private static final String ZONE_STR = "mein-d-ns.de";
  private static final String SLD_STR = "devsld";
  private static final int TTL = 60;

  private static final String API_EMAIL = System.getenv("API_EMAIL");
  private static final String API_KEY = System.getenv("API_KEY");

  private final CfDnsClient client = new CfDnsClient(API_EMAIL, API_KEY);

  @Test
  void testList() throws Exception {
    List<ZoneEntity> zList = client.zoneListAll();
    assertEquals(1, zList.size());

    List<RecordEntity> rList = client.sldListAll(zList.get(0), "test");
    assertFalse(rList.isEmpty());

    assertThrows(CloudflareNotFoundException.class,
      () -> client.sldListAll(zList.get(0), "not-existing"));

    client.setEmptyResultThrowsException(false);
    rList = client.sldListAll(zList.get(0), "not-existing");
    assertTrue(rList.isEmpty());
    client.setEmptyResultThrowsException(true);
  }

  @Test
  void testDns() throws Exception {
    ZoneEntity z = client.zoneInfo(ZONE_STR);
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
    r = client.sldInfo(z, "test", RecordType.AAAA);
    assertEquals("f76c420362220e0c8bab80cd08028214", r.getId());
    assertEquals("test.mein-d-ns.de", r.getName());
    assertEquals(RecordType.AAAA.getType(), r.getType());
    assertEquals("2a0a:4cc0:c0:2e4::1", r.getContent());

    String domain = SLD_STR + "." + ZONE_STR;
    client.recordDeleteTypeIfExists(z, SLD_STR, RecordType.A, RecordType.AAAA);
    RecordEntity createdRe1 =
      client.recordCreate(z, RecordEntity.build(domain, RecordType.A, TTL, "130.0.0.3"));
    assertNotNull(createdRe1.getId());
    assertEquals(domain, createdRe1.getName());
    assertEquals(RecordType.A.getType(), createdRe1.getType());
    assertEquals(TTL, createdRe1.getTtl());
    assertEquals("130.0.0.3", createdRe1.getContent());
    assertNotNull(createdRe1.getCreatedOn());
    assertNotNull(createdRe1.getModifiedOn());

    r = client.sldInfo(z, SLD_STR, RecordType.A);
    assertEquals("130.0.0.3", r.getContent());
    RecordEntity createdRe2 =
      client.recordCreate(z, SLD_STR, TTL, RecordType.AAAA, "2a0a:4cc0:c0:2e4::1");
    r = client.sldInfo(z, SLD_STR, RecordType.AAAA);
    assertEquals("2a0a:4cc0:c0:2e4::1", r.getContent());
    assertEquals(RecordType.AAAA.getType(), r.getType());

    createdRe2.setContent("2a0a:4cc0:c0:2e4::2");
    client.recordUpdate(z, createdRe2);
    r = client.sldInfo(z, SLD_STR, RecordType.AAAA);
    assertEquals("2a0a:4cc0:c0:2e4::2", r.getContent());

    r = client.sldInfo(z, SLD_STR, RecordType.A);
    assertEquals("130.0.0.3", r.getContent());
    assertTrue(client.recordDelete(z, createdRe2));
    assertThrows(CloudflareNotFoundException.class,
      () -> client.sldInfo(z, SLD_STR, RecordType.AAAA));

    client.recordDeleteTypeIfExists(z, SLD_STR, RecordType.A);
    assertThrows(CloudflareNotFoundException.class, () -> client.sldInfo(z, SLD_STR, RecordType.A));
  }

  @Test
  void testException() {
    assertThrows(IllegalArgumentException.class, () -> new CfDnsClient(null, "key"));
    assertThrows(IllegalArgumentException.class, () -> new CfDnsClient("email", null));
    assertThrows(IllegalArgumentException.class, () -> new CfDnsClient("email", ""));
    assertThrows(IllegalArgumentException.class, () -> new CfDnsClient("", "key"));
  }
}
