package codes.thischwa.cf;

import codes.thischwa.cf.model.RecordEntity;
import codes.thischwa.cf.model.RecordType;
import codes.thischwa.cf.model.ZoneEntity;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@Slf4j
public class CfClientTest {

  private static final String ZONE_STR = "mein-d-ns.de";
  private static final String SLD_STR = "devsld";
  private static final int TTL = 60;

  private static final String API_EMAIL = System.getenv("API_EMAIL");
  private static final String API_KEY = System.getenv("API_KEY");

  private final CfDnsClient client = new CfDnsClient(API_EMAIL, API_KEY);

  @BeforeAll
  static void checkEnv() {
    assumeTrue(API_EMAIL != null && !API_EMAIL.isBlank(), "API_EMAIL not set; skipping pen tests");
    assumeTrue(API_KEY != null && !API_KEY.isBlank(), "API_KEY not set; skipping pen tests");
  }

  @Test
  void testZoneListAnlFailedSldList() throws Exception {
    List<ZoneEntity> zList = client.zoneListAll();
    assertEquals(1, zList.size());

    assertThrows(CloudflareNotFoundException.class,
      () -> client.sldListAll(zList.get(0), "not-existing"));
  }

  @Test
  void testEmptyResultThrowsException() throws Exception {
    List<ZoneEntity> zList = client.zoneListAll();
    CfDnsClient client = new CfDnsClient(true, API_EMAIL, API_KEY);
    assertThrows(CloudflareNotFoundException.class,
        () -> client.sldListAll(zList.get(0), "not-existing"));
  }

  @Test
  void testDns() throws Exception {
    // starting point: already existing zone 'mein-d-ns.de'
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

    // for all other functions use a newly created SLD and ensure cleanup at the end
    String randomSld = SLD_STR + "-" + System.currentTimeMillis();
    String domain = randomSld + "." + ZONE_STR;

    RecordEntity r;
    RecordEntity createdRe1;
    RecordEntity createdRe2;

    try {
      // ensure clean state
      client.recordDeleteTypeIfExists(z, randomSld, RecordType.A, RecordType.AAAA);

      // create A record using recordCreate with full domain
      createdRe1 =
          client.recordCreate(z, RecordEntity.build(domain, RecordType.A, TTL, "130.0.0.3"));
      assertNotNull(createdRe1.getId());
      assertEquals(domain, createdRe1.getName());
      assertEquals(RecordType.A.getType(), createdRe1.getType());
      assertEquals(TTL, createdRe1.getTtl());
      assertEquals("130.0.0.3", createdRe1.getContent());
      assertNotNull(createdRe1.getCreatedOn());
      assertNotNull(createdRe1.getModifiedOn());

      // verify sldInfo for A
      r = client.sldInfo(z, randomSld, RecordType.A);
      assertEquals("130.0.0.3", r.getContent());

      // create AAAA record using recordCreateSld
      createdRe2 =
          client.recordCreateSld(z, randomSld, TTL, RecordType.AAAA, "2a0a:4cc0:c0:2e4::1");
      r = client.sldInfo(z, randomSld, RecordType.AAAA);
      assertEquals("2a0a:4cc0:c0:2e4::1", r.getContent());
      assertEquals(RecordType.AAAA.getType(), r.getType());

      // test sldListAll
      List<RecordEntity> rList = client.sldListAll(z, randomSld);
      assertEquals(2, rList.size());
      for (RecordEntity re : rList) {
        if (Objects.equals(re.getType(), RecordType.A.getType())) {
          assertEquals("130.0.0.3", re.getContent());
        } else if (Objects.equals(re.getType(), RecordType.AAAA.getType())) {
          assertEquals("2a0a:4cc0:c0:2e4::1", re.getContent());
        } else {
          throw new IllegalStateException("Unexpected record type: " + re.getType());
        }
      }

      // update AAAA record
      createdRe2.setContent("2a0a:4cc0:c0:2e4::2");
      client.recordUpdate(z, createdRe2);
      r = client.sldInfo(z, randomSld, RecordType.AAAA);
      assertEquals("2a0a:4cc0:c0:2e4::2", r.getContent());

      // verify A record still intact
      r = client.sldInfo(z, randomSld, RecordType.A);
      assertEquals("130.0.0.3", r.getContent());

      // delete AAAA record and verify it's gone
      assertTrue(client.recordDelete(z, createdRe2));
      assertThrows(CloudflareNotFoundException.class,
          () -> client.sldInfo(z, randomSld, RecordType.AAAA));

      // delete A record using helper and verify it's gone
      client.recordDeleteTypeIfExists(z, randomSld, RecordType.A);
      assertThrows(CloudflareNotFoundException.class,
          () -> client.sldInfo(z, randomSld, RecordType.A));
    } finally {
      // cleanup in case of failures during test
      try {
        client.recordDeleteTypeIfExists(z, randomSld, RecordType.AAAA);
      } catch (Exception e) { /* ignore */ }
      try {
        client.recordDeleteTypeIfExists(z, randomSld, RecordType.A);
      } catch (Exception e) { /* ignore */ }
    }
  }

  @Test
  void testException() {
    assertThrows(IllegalArgumentException.class, () -> new CfDnsClient(null, "key"));
    assertThrows(IllegalArgumentException.class, () -> new CfDnsClient("email", null));
    assertThrows(IllegalArgumentException.class, () -> new CfDnsClient("email", ""));
    assertThrows(IllegalArgumentException.class, () -> new CfDnsClient("", "key"));
  }
}
