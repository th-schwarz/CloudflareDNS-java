package codes.thischwa.cf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import codes.thischwa.cf.model.BatchEntry;
import codes.thischwa.cf.model.RecordEntity;
import codes.thischwa.cf.model.RecordType;
import codes.thischwa.cf.model.ZoneEntity;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@Slf4j
public class CfClientTest {

  private static final String ZONE_STR = "mein-d-ns.de";
  private static final String SLD_STR = "devsld";
  private static final int TTL = 60;

  private static final String API_EMAIL = System.getenv("API_EMAIL");
  private static final String API_KEY = System.getenv("API_KEY");

  private final CfDnsClient client = new CfDnsClient(true, API_EMAIL, API_KEY);

  @BeforeAll
  static void checkEnv() {
    assumeTrue(API_EMAIL != null && !API_EMAIL.isBlank(), "API_EMAIL not set; skipping pen tests");
    assumeTrue(API_KEY != null && !API_KEY.isBlank(), "API_KEY not set; skipping pen tests");
  }

  @Test
  void testUnknownSld() throws Exception {
    ZoneEntity zone = client.zoneGet(ZONE_STR);
    assertThrows(CloudflareNotFoundException.class, () -> client.recordList(zone, "unknown", RecordType.A));
  }

  @Test
  void testAddHost() throws Exception {
    ZoneEntity zone = client.zoneGet(ZONE_STR);
    client.recordDeleteTypeIfExists(zone, SLD_STR, RecordType.A, RecordType.AAAA);
    RecordEntity record = RecordEntity.build(SLD_STR, RecordType.A, TTL, "127.0.0.1");
    RecordEntity createdRecord = client.recordCreate(zone, record);
    assertNotNull(createdRecord.getId());
    assertEquals(SLD_STR, createdRecord.getSld());
    assertEquals(RecordType.A.getType(), createdRecord.getType());
    assertEquals(TTL, createdRecord.getTtl());
    assertEquals("127.0.0.1", createdRecord.getContent());
    assertNotNull(createdRecord.getCreatedOn());

    client.recordDeleteTypeIfExists(zone, SLD_STR, RecordType.A);
    assertThrows(CloudflareNotFoundException.class,
        () -> client.recordList(zone, SLD_STR, RecordType.A));

    record = RecordEntity.build(SLD_STR + "." + ZONE_STR, RecordType.A, TTL, "127.1.0.1");
    createdRecord = client.recordCreate(zone, record);
    assertNotNull(createdRecord.getId());
    assertEquals(SLD_STR, createdRecord.getSld());
    assertEquals(RecordType.A.getType(), createdRecord.getType());
    assertEquals(TTL, createdRecord.getTtl());
    assertEquals("127.1.0.1", createdRecord.getContent());
    assertNotNull(createdRecord.getCreatedOn());

    client.recordDeleteTypeIfExists(zone, SLD_STR, RecordType.A);
  }

  @Test
  void testZoneListAnlFailedSldList() throws Exception {
    List<ZoneEntity> zList = client.zoneList();
    assertEquals(1, zList.size());

    assertThrows(CloudflareNotFoundException.class,
        () -> client.recordList(zList.get(0), "not-existing"));
  }

  @Test
  void testEmptyResultThrowsException() throws Exception {
    List<ZoneEntity> zList = client.zoneList();
    CfDnsClient client = new CfDnsClient(true, API_EMAIL, API_KEY);
    assertThrows(CloudflareNotFoundException.class,
        () -> client.recordList(zList.get(0), "not-existing"));
  }

  @Test
  void testDns() throws Exception {
    // starting point: already existing zone 'mein-d-ns.de'
    ZoneEntity z = client.zoneGet(ZONE_STR);
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
      assertEquals(randomSld+ "." + ZONE_STR, createdRe1.getName());
      assertEquals(randomSld, createdRe1.getSld());
      assertEquals(RecordType.A.getType(), createdRe1.getType());
      assertEquals(z.getId(), createdRe1.getZoneId());
      assertEquals(TTL, createdRe1.getTtl());
      assertEquals("130.0.0.3", createdRe1.getContent());
      assertNotNull(createdRe1.getCreatedOn());
      assertNotNull(createdRe1.getModifiedOn());

      // verify recordList for A
      List<RecordEntity> aRecords = client.recordList(z, randomSld, RecordType.A);
      assertEquals(1, aRecords.size());
      r = aRecords.get(0);
      assertEquals("130.0.0.3", r.getContent());

      // create AAAA record using recordCreateSld
      createdRe2 =
          client.recordCreateSld(z, randomSld, TTL, RecordType.AAAA, "2a0a:4cc0:c0:2e4::1");
      List<RecordEntity> aaaaRecords = client.recordList(z, randomSld, RecordType.AAAA);
      assertEquals(1, aaaaRecords.size());
      r = aaaaRecords.get(0);
      assertEquals(z.getId(), r.getZoneId());
      assertEquals("2a0a:4cc0:c0:2e4::1", r.getContent());
      assertEquals(RecordType.AAAA.getType(), r.getType());

      // test recordList
      List<RecordEntity> rList = client.recordList(z, randomSld);
      assertEquals(2, rList.size());
      for (RecordEntity re : rList) {
        assertEquals(z.getId(), re.getZoneId());
        if (Objects.equals(re.getType(), RecordType.A.getType())) {
          assertEquals("130.0.0.3", re.getContent());
        } else if (Objects.equals(re.getType(), RecordType.AAAA.getType())) {
          assertEquals("2a0a:4cc0:c0:2e4::1", re.getContent());
        } else {
          throw new IllegalStateException("Unexpected record type: " + re.getType());
        }
      }

      // test recordList without SLD
      List<RecordEntity> fullList = client.recordList(z);
      assertTrue(fullList.size() >= 2);
      assertTrue(fullList.stream().anyMatch(re -> re.getId().equals(createdRe1.getId())));
      assertTrue(fullList.stream().anyMatch(re -> re.getId().equals(createdRe2.getId())));

      // test recordList with types without SLD
      List<RecordEntity> aList = client.recordList(z, RecordType.A);
      assertFalse(aList.isEmpty());
      assertTrue(aList.stream().anyMatch(re -> re.getId().equals(createdRe1.getId())));
      assertTrue(aList.stream().noneMatch(re -> re.getId().equals(createdRe2.getId())));

      // test fluent api list
      List<RecordEntity> fluentList = client.zone(ZONE_STR).list(RecordType.A);
      assertFalse(fluentList.isEmpty());
      assertTrue(fluentList.stream().anyMatch(re -> re.getId().equals(createdRe1.getId())));

      // update AAAA record
      createdRe2.setContent("2a0a:4cc0:c0:2e4::2");
      client.recordUpdate(z, createdRe2);
      aaaaRecords = client.recordList(z, randomSld, RecordType.AAAA);
      assertEquals(1, aaaaRecords.size());
      r = aaaaRecords.get(0);
      assertEquals("2a0a:4cc0:c0:2e4::2", r.getContent());

      // verify A record still intact
      aRecords = client.recordList(z, randomSld, RecordType.A);
      assertEquals(1, aRecords.size());
      r = aRecords.get(0);
      assertEquals("130.0.0.3", r.getContent());

      // delete AAAA record and verify it's gone
      assertTrue(client.recordDelete(z, createdRe2));
      assertThrows(CloudflareNotFoundException.class,
          () -> client.recordList(z, randomSld, RecordType.AAAA));

      // delete A record using helper and verify it's gone
      client.recordDeleteTypeIfExists(z, randomSld, RecordType.A);
      assertThrows(CloudflareNotFoundException.class,
          () -> client.recordList(z, randomSld, RecordType.A));
    } finally {
      // cleanup in case of failures during test
      try {
        client.recordDeleteTypeIfExists(z, randomSld, RecordType.A, RecordType.AAAA);
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

  @Test
  void testRecordEntityInvalidType() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> RecordEntity.build("id123", "example.com", "INVALID_TYPE", 60, "192.168.1.1"));
    assertTrue(exception.getMessage().contains("Invalid record type: INVALID_TYPE"));
    assertTrue(exception.getMessage().contains("Must be one of:"));
  }

  private static final String IP_PREFIX = "130.0.0.";
  private static final String UPDATED_IP_PREFIX = "130.1.0.";


  @Test
  void testBatch() throws Exception {
    // starting point: already existing zone 'mein-d-ns.de'
    ZoneEntity zone = client.zoneGet(ZONE_STR);

    List<String> sldNames = createSldNames();
    List<RecordEntity> initialRecords = createInitialRecords(sldNames);

    cleanupRecords(zone, sldNames);

    try {
      testBatchPost(zone, initialRecords, sldNames);
      testBatchPatch(zone, sldNames);
      testBatchDelete(zone, sldNames);
      testBatchPut(zone, sldNames);
    } finally {
      cleanupRecords(zone, sldNames);
    }
  }

  private List<String> createSldNames() {
    return List.of(SLD_STR + "-1", SLD_STR + "-2", SLD_STR + "-3");
  }

  private List<RecordEntity> createInitialRecords(List<String> sldNames) {
    List<RecordEntity> records = new ArrayList<>();
    for (int i = 0; i < sldNames.size(); i++) {
      records.add(RecordEntity.build(sldNames.get(i), RecordType.A, TTL, IP_PREFIX + (i + 1)));
    }
    return records;
  }

  private void cleanupRecords(ZoneEntity zone, List<String> sldNames) {
    sldNames.forEach(sld -> {
      try {
        client.recordDeleteTypeIfExists(zone, sld, RecordType.A);
      } catch (CloudflareApiException e) {
        throw new RuntimeException(e);
      }
    });
  }

  private void testBatchPost(ZoneEntity zone, List<RecordEntity> records, List<String> sldNames) throws Exception {
    // Use only first 2 records for POST
    List<RecordEntity> postRecords = records.subList(0, 2);
    BatchEntry batchEntry = client.recordBatch(zone, postRecords, null, null, null);
    assertEquals(2, batchEntry.getPosts().size());

    RecordEntity batchedRecord = batchEntry.getPosts().get(0);
    assertValidBatchedRecord(batchedRecord, postRecords.get(0));

    // Verify only the first 2 records
    for (int i = 0; i < 2; i++) {
      List<RecordEntity> records1 = client.recordList(zone, sldNames.get(i), RecordType.A);
      assertEquals(1, records1.size());
      assertEquals(IP_PREFIX + (i + 1), records1.get(0).getContent());
    }
  }

  private void testBatchPatch(ZoneEntity zone, List<String> sldNames) throws Exception {
    // Use first 2 records for PATCH
    List<RecordEntity> patchRecords = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      List<RecordEntity> records = client.recordList(zone, sldNames.get(i), RecordType.A);
      RecordEntity record = records.get(0);
      record.setContent(UPDATED_IP_PREFIX + (i + 1));
      patchRecords.add(record);
    }

    client.recordBatch(zone, null, null, patchRecords, null);

    // Verify both records were updated
    for (int i = 0; i < 2; i++) {
      List<RecordEntity> updatedRecords = client.recordList(zone, sldNames.get(i), RecordType.A);
      assertEquals(1, updatedRecords.size());
      assertEquals(UPDATED_IP_PREFIX + (i + 1), updatedRecords.get(0).getContent());
    }
  }

  private void testBatchDelete(ZoneEntity zone, List<String> sldNames) throws Exception {
    // Delete first 2 records
    List<RecordEntity> deleteRecords = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      List<RecordEntity> records = client.recordList(zone, sldNames.get(i), RecordType.A);
      deleteRecords.add(records.get(0));
    }

    client.recordBatch(zone, null, null, null, deleteRecords);

    // Verify both records are deleted
    for (int i = 0; i < 2; i++) {
      String sldName = sldNames.get(i);
      assertThrows(CloudflareNotFoundException.class,
          () -> client.recordList(zone, sldName, RecordType.A));
    }
  }

  private void testBatchPut(ZoneEntity zone, List<String> sldNames) throws Exception {
    // Create 2 new records first for PUT test
    List<RecordEntity> newRecords = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      RecordEntity record = RecordEntity.build(sldNames.get(i), RecordType.A, TTL, IP_PREFIX + (i + 1));
      newRecords.add(record);
    }
    client.recordBatch(zone, newRecords, null, null, null);

    // Now use PUT to replace them
    List<RecordEntity> putRecords = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      List<RecordEntity> records = client.recordList(zone, sldNames.get(i), RecordType.A);
      RecordEntity record = records.get(0);
      record.setContent(UPDATED_IP_PREFIX + (i + 1));
      putRecords.add(record);
    }
    client.recordBatch(zone, null, putRecords, null, null);

    // Verify both records were updated
    for (int i = 0; i < 2; i++) {
      List<RecordEntity> updatedRecords = client.recordList(zone, sldNames.get(i), RecordType.A);
      assertEquals(1, updatedRecords.size());
      assertEquals(UPDATED_IP_PREFIX + (i + 1), updatedRecords.get(0).getContent());
    }
  }

  private void assertValidBatchedRecord(RecordEntity batchedRecord, RecordEntity originalRecord) {
    assertNotNull(batchedRecord.getId());
    assertEquals(originalRecord.getSld(), batchedRecord.getSld());
    assertEquals(originalRecord.getType(), batchedRecord.getType());
    assertNotNull(batchedRecord.getCreatedOn());
  }

  @Test
  void testFluentApi() throws Exception {
    ZoneEntity zone = client.zoneGet(ZONE_STR);
    String fluentSld = "fluent-" + System.currentTimeMillis();

    try {
      // Test fluent create
      RecordEntity created = client.zone(ZONE_STR)
          .record(fluentSld)
          .create(RecordType.A, "192.168.100.1", TTL);

      assertNotNull(created.getId());
      assertEquals(fluentSld, created.getSld());
      assertEquals("192.168.100.1", created.getContent());

      // Test fluent get
      List<RecordEntity> records = client.zone(ZONE_STR)
          .record(fluentSld, RecordType.A)
          .get();

      assertEquals(1, records.size());
      assertEquals("192.168.100.1", records.get(0).getContent());

      // Test fluent update
      RecordEntity updated = client.zone(ZONE_STR)
          .record(fluentSld, RecordType.A)
          .update("192.168.100.2");

      assertEquals("192.168.100.2", updated.getContent());

      // Test fluent delete
      client.zone(ZONE_STR)
          .record(fluentSld)
          .delete(RecordType.A);

      assertThrows(CloudflareNotFoundException.class,
          () -> client.zone(ZONE_STR).record(fluentSld, RecordType.A).get());

    } finally {
      try {
        client.recordDeleteTypeIfExists(zone, fluentSld, RecordType.A);
      } catch (Exception e) { /* ignore */ }
    }
  }

  @Test
  void testGroupRecordsByFqdn_withValidRecords() {
    // Arrange
    RecordEntity rec1 = RecordEntity.build("1", "example.com.", "A", 300, "192.168.1.1");
    RecordEntity rec2 = RecordEntity.build("2", "example.com.", "AAAA", 300, "::1");
    RecordEntity rec3 = RecordEntity.build("3", "sub.example.com.", "CNAME", 300, "example.com.");
    List<RecordEntity> records = Arrays.asList(rec1, rec2, rec3);

    // Act
    Map<String, List<RecordEntity>> groupedRecords = CfDnsClient.groupRecordsByFqdn(records);

    // Assert
    assertNotNull(groupedRecords, "Resulting map should not be null.");
    assertEquals(2, groupedRecords.size(), "The grouping should result in 2 FQDN keys.");
    assertEquals(2, groupedRecords.get("example.com.").size(), "The key 'example.com.' should have 2 records.");
    assertEquals(1, groupedRecords.get("sub.example.com.").size(), "The key 'sub.example.com.' should have 1 record.");
  }

  @Test
  void testGroupRecordsByFqdn_withMultipleRecordsSameFqdn() {
    // Arrange
    RecordEntity rec1 = RecordEntity.build("1", "example.com.", "A", 300, "192.168.1.1");
    RecordEntity rec2 = RecordEntity.build("2", "example.com.", "AAAA", 300, "::1");
    List<RecordEntity> records = Arrays.asList(rec1, rec2);

    // Act
    Map<String, List<RecordEntity>> groupedRecords = CfDnsClient.groupRecordsByFqdn(records);

    // Assert
    assertNotNull(groupedRecords, "Resulting map should not be null.");
    assertEquals(1, groupedRecords.size(), "The grouping should result in 1 FQDN key.");
    assertEquals(2, groupedRecords.get("example.com.").size(), "The key 'example.com.' should have 2 records.");
  }


}
