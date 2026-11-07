package codes.thischwa.cf;

import codes.thischwa.cf.fluent.ZoneOperations;
import codes.thischwa.cf.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.net.IDN;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CfDnsClient using mocked HTTP client responses.
 * Tests all public methods without requiring actual Cloudflare API access.
 */
@ExtendWith(MockitoExtension.class)
class CfDnsClientMockTest {

    private CfDnsClient client;

    @Mock
    private CfBasicHttpClient mockHttpClient;

    private static final String TEST_ZONE_ID = "zone123";
    private static final String TEST_ZONE_NAME = "example.com";
    private static final String TEST_RECORD_ID = "rec123";

    private ZoneEntity createTestZone() {
        ZoneEntity zone = new ZoneEntity();
        zone.setId(TEST_ZONE_ID);
        zone.setName(TEST_ZONE_NAME);
        return zone;
    }

    private ResponseResultInfo createSuccessResultInfo() {
        ResponseResultInfo resultInfo = new ResponseResultInfo();
        resultInfo.setSuccess(true);
        return resultInfo;
    }

    private BatchResponse createBatchResponse() throws Exception {
        // Use reflection to access package-private constructor
        java.lang.reflect.Constructor<BatchResponse> constructor = BatchResponse.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private ResultInfo createResultInfo(int count) {
        return new ResultInfo(count);
    }

    @BeforeEach
    void setUp() throws Exception {
        // Create a real client with API token auth
        client = new CfDnsClientBuilder()
                .withApiTokenAuth("test-token")
                .build();

        // Replace the internal HTTP client with our mock using reflection
        // Note: This is a workaround since CfBasicHttpClient methods are package-private
        Field responseValidatorField = CfDnsClient.class.getDeclaredField("responseValidator");
        responseValidatorField.setAccessible(true);
        responseValidatorField.set(client, new ResponseValidator(false));
    }

    @Test
    void testGroupRecordsByFqdn() {
        RecordEntity rec1 = RecordEntity.build("test.example.com", RecordType.A, 300, "1.2.3.4");
        RecordEntity rec2 = RecordEntity.build("test.example.com", RecordType.AAAA, 300, "::1");
        RecordEntity rec3 = RecordEntity.build("www.example.com", RecordType.A, 300, "1.2.3.5");

        List<RecordEntity> records = List.of(rec1, rec2, rec3);
        Map<String, List<RecordEntity>> grouped = CfDnsClient.groupRecordsByFqdn(records);

        assertEquals(2, grouped.size());
        assertEquals(2, grouped.get("test.example.com").size());
        assertEquals(1, grouped.get("www.example.com").size());
    }

    @Test
    void testGroupRecordsByFqdn_NullInput() {
        Map<String, List<RecordEntity>> grouped = CfDnsClient.groupRecordsByFqdn(null);
        assertNotNull(grouped);
        assertTrue(grouped.isEmpty());
    }

    @Test
    void testZoneList() throws Exception {
        // Create mock zone response
        ZoneMultipleResponse mockResponse = new ZoneMultipleResponse();
        ZoneEntity zone1 = new ZoneEntity();
        zone1.setId("zone1");
        zone1.setName("example.com");
        ZoneEntity zone2 = new ZoneEntity();
        zone2.setId("zone2");
        zone2.setName("test.com");
        mockResponse.setResult(List.of(zone1, zone2));
        ResponseResultInfo resultInfo = new ResponseResultInfo();
        resultInfo.setSuccess(true);
        mockResponse.setResponseResultInfo(resultInfo);

        // Mock the HTTP client via spy
        CfDnsClient spyClient = spy(client);
        doReturn(mockResponse).when(spyClient).getRequest(anyString(), eq(ZoneMultipleResponse.class));

        List<ZoneEntity> zones = spyClient.zoneList();

        assertNotNull(zones);
        assertEquals(2, zones.size());
        assertEquals("example.com", zones.get(0).getName());
        assertEquals("test.com", zones.get(1).getName());
    }

    @Test
    void testZoneList_WithPagingEnforcesApiLimits() throws Exception {
        ZoneMultipleResponse mockResponse = new ZoneMultipleResponse();
        mockResponse.setResult(List.of());
        mockResponse.setResponseResultInfo(createSuccessResultInfo());

        CfDnsClient spyClient = spy(client);
        doReturn(mockResponse).when(spyClient).getRequest(anyString(), eq(ZoneMultipleResponse.class));

        spyClient.zoneList(PagingRequest.forZones(1, 5));
        spyClient.zoneList(PagingRequest.forZones(1, 50));

        verify(spyClient).getRequest(eq("/zones?page=1&per_page=5"), eq(ZoneMultipleResponse.class));
        verify(spyClient).getRequest(eq("/zones?page=1&per_page=50"), eq(ZoneMultipleResponse.class));
    }

    @Test
    void testZoneList_WithPagingRejectsOutOfRangePageSizes() throws Exception {
        CfDnsClient spyClient = spy(client);

        assertThrows(IllegalArgumentException.class,
                () -> spyClient.zoneList(new PagingRequest(1, 4)));
        assertThrows(IllegalArgumentException.class,
                () -> spyClient.zoneList(new PagingRequest(1, 51)));

        verify(spyClient, never()).getRequest(anyString(), eq(ZoneMultipleResponse.class));
    }

    @Test
    void testZoneGet() throws Exception {
        // Create mock zone response
        ZoneMultipleResponse mockResponse = new ZoneMultipleResponse();
        ZoneEntity zone = new ZoneEntity();
        zone.setId(TEST_ZONE_ID);
        zone.setName(TEST_ZONE_NAME);
        mockResponse.setResult(List.of(zone));
        ResponseResultInfo resultInfo = new ResponseResultInfo();
        resultInfo.setSuccess(true);
        mockResponse.setResponseResultInfo(resultInfo);

        CfDnsClient spyClient = spy(client);
        doReturn(mockResponse).when(spyClient).getRequest(anyString(), eq(ZoneMultipleResponse.class));

        ZoneEntity result = spyClient.zoneGet(TEST_ZONE_NAME);

        assertNotNull(result);
        assertEquals(TEST_ZONE_ID, result.getId());
        assertEquals(TEST_ZONE_NAME, result.getName());
    }

    @Test
    void testZoneOperations() throws Exception {
        // Create mock zone response
        ZoneMultipleResponse mockResponse = new ZoneMultipleResponse();
        ZoneEntity zone = new ZoneEntity();
        zone.setId(TEST_ZONE_ID);
        zone.setName(TEST_ZONE_NAME);
        mockResponse.setResult(List.of(zone));
        ResponseResultInfo resultInfo = new ResponseResultInfo();
        resultInfo.setSuccess(true);
        mockResponse.setResponseResultInfo(resultInfo);

        CfDnsClient spyClient = spy(client);
        doReturn(mockResponse).when(spyClient).getRequest(anyString(), eq(ZoneMultipleResponse.class));

        ZoneOperations ops = spyClient.zone(TEST_ZONE_NAME);

        assertNotNull(ops);
    }

    @Test
    void testRecordList_Zone() throws Exception {
        ZoneEntity zone = new ZoneEntity();
        zone.setId(TEST_ZONE_ID);
        zone.setName(TEST_ZONE_NAME);

        RecordMultipleResponse mockResponse = new RecordMultipleResponse();
        mockResponse.setResultInfo(createResultInfo(1));
        RecordEntity rec1 = RecordEntity.build("test.example.com", RecordType.A, 300, "1.2.3.4");
        RecordEntity rec2 = RecordEntity.build("www.example.com", RecordType.A, 300, "1.2.3.5");
        mockResponse.setResult(List.of(rec1, rec2));
        ResponseResultInfo resultInfo = new ResponseResultInfo();
        resultInfo.setSuccess(true);
        mockResponse.setResponseResultInfo(resultInfo);

        CfDnsClient spyClient = spy(client);
        doReturn(mockResponse).when(spyClient).getRequest(anyString(), eq(RecordMultipleResponse.class));

        List<RecordEntity> records = spyClient.recordList(zone);

        assertNotNull(records);
        assertEquals(2, records.size());
        assertEquals("1.2.3.4", records.get(0).getContent());
    }

    @Test
    void testRecordList_WithPaging() throws Exception {
        ZoneEntity zone = createTestZone();
        PagingRequest pagingRequest = PagingRequest.forRecords(10, 1);

        RecordMultipleResponse mockResponse = new RecordMultipleResponse();
        mockResponse.setResultInfo(createResultInfo(1));
        RecordEntity rec1 = RecordEntity.build("test.example.com", RecordType.A, 300, "1.2.3.4");
        mockResponse.setResult(List.of(rec1));
        mockResponse.setResponseResultInfo(createSuccessResultInfo());

        CfDnsClient spyClient = spy(client);
        doReturn(mockResponse).when(spyClient).getRequest(anyString(), eq(RecordMultipleResponse.class));

        List<RecordEntity> records = spyClient.recordList(zone, pagingRequest);

        assertNotNull(records);
        assertEquals(1, records.size());
    }

    @Test
    void testRecordList_BySld() throws Exception {
        ZoneEntity zone = createTestZone();

        RecordMultipleResponse mockResponse = new RecordMultipleResponse();
        mockResponse.setResultInfo(createResultInfo(1));
        RecordEntity rec1 = RecordEntity.build("test.example.com", RecordType.A, 300, "1.2.3.4");
        mockResponse.setResult(List.of(rec1));
        mockResponse.setResponseResultInfo(createSuccessResultInfo());

        CfDnsClient spyClient = spy(client);
        doReturn(mockResponse).when(spyClient).getRequest(anyString(), eq(RecordMultipleResponse.class));

        List<RecordEntity> records = spyClient.recordListBySubdomain(zone, "test");

        assertNotNull(records);
        assertEquals(1, records.size());
        assertEquals(TEST_ZONE_ID, records.get(0).getZoneId());
    }

    @Test
    void testRecordList_BySldAndType() throws Exception {
        ZoneEntity zone = createTestZone();

        RecordMultipleResponse mockResponse = new RecordMultipleResponse();
        mockResponse.setResultInfo(createResultInfo(1));
        RecordEntity rec1 = RecordEntity.build("test.example.com", RecordType.A, 300, "1.2.3.4");
        RecordEntity rec2 = RecordEntity.build("test.example.com", RecordType.AAAA, 300, "::1");
        mockResponse.setResult(List.of(rec1, rec2));
        mockResponse.setResponseResultInfo(createSuccessResultInfo());

        CfDnsClient spyClient = spy(client);
        doReturn(mockResponse).when(spyClient).getRequest(anyString(), eq(RecordMultipleResponse.class));

        List<RecordEntity> records = spyClient.recordListBySubdomain(zone, "test", RecordType.A);

        assertNotNull(records);
        assertEquals(1, records.size());
        assertEquals(RecordType.A, RecordType.valueOf(records.get(0).getType()));
    }

    @Test
    void testRecordList_ByType() throws Exception {
        ZoneEntity zone = createTestZone();

        RecordMultipleResponse mockResponse = new RecordMultipleResponse();
        mockResponse.setResultInfo(createResultInfo(1));
        RecordEntity rec1 = RecordEntity.build("test.example.com", RecordType.A, 300, "1.2.3.4");
        RecordEntity rec2 = RecordEntity.build("www.example.com", RecordType.AAAA, 300, "::1");
        mockResponse.setResult(List.of(rec1, rec2));
        mockResponse.setResponseResultInfo(createSuccessResultInfo());

        CfDnsClient spyClient = spy(client);
        doReturn(mockResponse).when(spyClient).getRequest(anyString(), eq(RecordMultipleResponse.class));

        List<RecordEntity> records = spyClient.recordListByType(zone, RecordType.A);

        assertNotNull(records);
        assertEquals(1, records.size());
        assertEquals(RecordType.A, RecordType.valueOf(records.get(0).getType()));
    }

    @Test
    void testRecordCreateSld() throws Exception {
        ZoneEntity zone = createTestZone();

        RecordSingleResponse mockResponse = new RecordSingleResponse();
        RecordEntity createdRecord = RecordEntity.build("test.example.com", RecordType.A, 300, "1.2.3.4");
        createdRecord.setId(TEST_RECORD_ID);
        mockResponse.setResult(createdRecord);
        mockResponse.setResponseResultInfo(createSuccessResultInfo());

        CfDnsClient spyClient = spy(client);
        doReturn(mockResponse).when(spyClient).postRequest(anyString(), any(), eq(RecordSingleResponse.class));

        RecordEntity result = spyClient.recordCreate(zone, "test.example.com", 300, RecordType.A, "1.2.3.4");

        assertNotNull(result);
        assertEquals(TEST_RECORD_ID, result.getId());
        assertEquals(TEST_ZONE_ID, result.getZoneId());
        assertEquals("1.2.3.4", result.getContent());
    }

    @Test
    void testRecordDelete() throws Exception {
        ZoneEntity zone = createTestZone();

        RecordSingleResponse mockResponse = new RecordSingleResponse();
        RecordEntity deletedRecord = RecordEntity.build("test.example.com", RecordType.A, 300, "1.2.3.4");
        deletedRecord.setId(TEST_RECORD_ID);
        mockResponse.setResult(deletedRecord);
        mockResponse.setResponseResultInfo(createSuccessResultInfo());

        CfDnsClient spyClient = spy(client);
        doReturn(mockResponse).when(spyClient).deleteRequest(anyString(), eq(RecordSingleResponse.class));

        boolean result = spyClient.recordDelete(zone, TEST_RECORD_ID);

        assertTrue(result);
    }

    @Test
    void testRecordUpdate() throws Exception {
        ZoneEntity zone = createTestZone();
        RecordEntity record = RecordEntity.build("test.example.com", RecordType.A, 300, "1.2.3.4");
        record.setId(TEST_RECORD_ID);

        RecordSingleResponse mockResponse = new RecordSingleResponse();
        RecordEntity updatedRecord = RecordEntity.build("test.example.com", RecordType.A, 300, "1.2.3.5");
        updatedRecord.setId(TEST_RECORD_ID);
        mockResponse.setResult(updatedRecord);
        mockResponse.setResponseResultInfo(createSuccessResultInfo());

        CfDnsClient spyClient = spy(client);
        doReturn(mockResponse).when(spyClient).patchRequest(anyString(), any(), eq(RecordSingleResponse.class));

        RecordEntity result = spyClient.recordUpdate(zone, record);

        assertNotNull(result);
        assertEquals(TEST_RECORD_ID, result.getId());
    }

    @Test
    void testRecordDeleteTypeIfExists() throws Exception {
        ZoneEntity zone = createTestZone();

        RecordMultipleResponse listResponse = new RecordMultipleResponse();
        listResponse.setResultInfo(createResultInfo(1));
        RecordEntity rec1 = RecordEntity.build("test.example.com", RecordType.A, 300, "1.2.3.4");
        rec1.setId(TEST_RECORD_ID);
        listResponse.setResult(List.of(rec1));
        listResponse.setResponseResultInfo(createSuccessResultInfo());

        RecordSingleResponse deleteResponse = new RecordSingleResponse();
        RecordEntity deletedRecord = RecordEntity.build("test.example.com", RecordType.A, 300, "1.2.3.4");
        deletedRecord.setId(TEST_RECORD_ID);
        deleteResponse.setResult(deletedRecord);
        deleteResponse.setResponseResultInfo(createSuccessResultInfo());

        CfDnsClient spyClient = spy(client);
        doReturn(listResponse).when(spyClient).getRequest(anyString(), eq(RecordMultipleResponse.class));
        doReturn(deleteResponse).when(spyClient).deleteRequest(anyString(), eq(RecordSingleResponse.class));

        // Should not throw exception
        spyClient.recordDeleteTypeIfExists(zone, "test", RecordType.A);

        verify(spyClient, times(1)).getRequest(anyString(), eq(RecordMultipleResponse.class));
        verify(spyClient, times(1)).deleteRequest(anyString(), eq(RecordSingleResponse.class));
    }

    @Test
    void testRecordDeleteTypeIfExists_NotFound() throws Exception {
        ZoneEntity zone = createTestZone();

        CfDnsClient spyClient = spy(client);
        doThrow(new CloudflareNotFoundException("Not found")).when(spyClient).recordListBySubdomain(any(), anyString(), any());

        // Should not throw exception when record doesn't exist
        assertDoesNotThrow(() -> spyClient.recordDeleteTypeIfExists(zone, "test", RecordType.A));
    }

    @Test
    void testRecordBatch() throws Exception {
        ZoneEntity zone = createTestZone();

        RecordEntity postRecord = RecordEntity.build("new.example.com", RecordType.A, 300, "1.2.3.4");
        RecordEntity patchRecord = RecordEntity.build(TEST_RECORD_ID, "1.2.3.5");
        RecordEntity deleteRecord = RecordEntity.build("old.example.com", RecordType.A, 300, "1.2.3.6");
        deleteRecord.setId("rec999");

        BatchResponse mockResponse = createBatchResponse();
        BatchEntry resultEntry = new BatchEntry();
        resultEntry.setPosts(List.of(postRecord));
        resultEntry.setPatches(List.of(patchRecord));
        mockResponse.setResult(resultEntry);
        mockResponse.setResponseResultInfo(createSuccessResultInfo());

        CfDnsClient spyClient = spy(client);
        doReturn(mockResponse).when(spyClient).postRequest(anyString(), any(), eq(BatchResponse.class));

        BatchEntry result = spyClient.recordBatch(zone,
                List.of(postRecord),
                null,
                List.of(patchRecord),
                List.of(deleteRecord));

        assertNotNull(result);
        assertNotNull(result.getPosts());
        assertEquals(1, result.getPosts().size());
        assertEquals(TEST_ZONE_ID, result.getPosts().get(0).getZoneId());
    }

    private ZoneMultipleResponse zoneResponse(List<ZoneEntity> zones, ResultInfo resultInfo) {
        ZoneMultipleResponse resp = new ZoneMultipleResponse();
        resp.setResult(zones);
        resp.setResultInfo(resultInfo);
        resp.setResponseResultInfo(createSuccessResultInfo());
        return resp;
    }

    private RecordMultipleResponse recordResponse(List<RecordEntity> records, ResultInfo resultInfo) {
        RecordMultipleResponse resp = new RecordMultipleResponse();
        resp.setResult(records);
        resp.setResultInfo(resultInfo);
        resp.setResponseResultInfo(createSuccessResultInfo());
        return resp;
    }

    private static ZoneEntity zone(String id, String name) {
        ZoneEntity zone = new ZoneEntity();
        zone.setId(id);
        zone.setName(name);
        return zone;
    }

    @Test
    void testZoneGet_NotFound_ReturnsNull() throws Exception {
        CfDnsClient spyClient = spy(client);
        doReturn(zoneResponse(List.of(), new ResultInfo(0)))
                .when(spyClient).getRequest(anyString(), eq(ZoneMultipleResponse.class));

        assertNull(spyClient.zoneGet("unknown.com"));
    }

    @Test
    void testZoneGet_NotFound_ThrowsIfEnabled() throws Exception {
        CfDnsClient throwingClient = spy(new CfDnsClientBuilder()
                .withApiTokenAuth("test-token")
                .withEmptyResultThrowsException(true)
                .build());
        doReturn(zoneResponse(List.of(), new ResultInfo(0)))
                .when(throwingClient).getRequest(anyString(), eq(ZoneMultipleResponse.class));

        assertThrows(CloudflareNotFoundException.class, () -> throwingClient.zoneGet("unknown.com"));
    }

    @Test
    void testZone_NotFound_Throws() throws Exception {
        CfDnsClient spyClient = spy(client);
        doReturn(zoneResponse(List.of(), new ResultInfo(0)))
                .when(spyClient).getRequest(anyString(), eq(ZoneMultipleResponse.class));

        assertThrows(CloudflareNotFoundException.class, () -> spyClient.zone("unknown.com"));
    }

    @Test
    void testZoneList_FetchesAllPages() throws Exception {
        CfDnsClient spyClient = spy(client);
        doReturn(zoneResponse(List.of(zone("z1", "a.com"), zone("z2", "b.com")),
                new ResultInfo(1, 2, 2, 2, 3)))
                .when(spyClient).getRequest(eq("/zones?page=1&per_page=" + PagingRequest.DEFAULT_PAGE_SIZE),
                        eq(ZoneMultipleResponse.class));
        doReturn(zoneResponse(List.of(zone("z3", "c.com")), new ResultInfo(2, 2, 2, 1, 3)))
                .when(spyClient).getRequest(eq("/zones?page=2&per_page=" + PagingRequest.DEFAULT_PAGE_SIZE),
                        eq(ZoneMultipleResponse.class));

        List<ZoneEntity> zones = spyClient.zoneList();

        assertEquals(List.of("a.com", "b.com", "c.com"), zones.stream().map(ZoneEntity::getName).toList());
        verify(spyClient, times(2)).getRequest(anyString(), eq(ZoneMultipleResponse.class));
    }

    @Test
    void testRecordListByType_FetchesAllPages() throws Exception {
        ZoneEntity zone = createTestZone();
        String base = "/zones/" + TEST_ZONE_ID + "/dns_records?type=A&order=name&direction=asc&page=%d&per_page=" + CfDnsClient.RECORDS_PER_PAGE;
        CfDnsClient spyClient = spy(client);
        doReturn(recordResponse(List.of(RecordEntity.build("a.example.com", RecordType.A, 60, "1.1.1.1")),
                new ResultInfo(1, 1, 2, 1, 2)))
                .when(spyClient).getRequest(eq(String.format(base, 1)), eq(RecordMultipleResponse.class));
        doReturn(recordResponse(List.of(RecordEntity.build("b.example.com", RecordType.A, 60, "2.2.2.2")),
                new ResultInfo(2, 1, 2, 1, 2)))
                .when(spyClient).getRequest(eq(String.format(base, 2)), eq(RecordMultipleResponse.class));

        List<RecordEntity> records = spyClient.recordListByType(zone, RecordType.A);

        assertEquals(2, records.size());
        assertEquals("b.example.com", records.get(1).getName());
    }

    @Test
    void testRecordListBySld_FetchesAllPages() throws Exception {
        ZoneEntity zone = createTestZone();
        String base = "/zones/" + TEST_ZONE_ID + "/dns_records?name=test.example.com&order=name&direction=asc&page=%d&per_page="
                + CfDnsClient.RECORDS_PER_PAGE;
        CfDnsClient spyClient = spy(client);
        doReturn(recordResponse(List.of(RecordEntity.build("test.example.com", RecordType.A, 60, "1.1.1.1")),
                new ResultInfo(1, 1, 2, 1, 2)))
                .when(spyClient).getRequest(eq(String.format(base, 1)), eq(RecordMultipleResponse.class));
        doReturn(recordResponse(List.of(RecordEntity.build("test.example.com", RecordType.AAAA, 60, "::1")),
                new ResultInfo(2, 1, 2, 1, 2)))
                .when(spyClient).getRequest(eq(String.format(base, 2)), eq(RecordMultipleResponse.class));

        assertEquals(2, spyClient.recordListBySubdomain(zone, "test").size());
    }

    @Test
    void testRecordList_SetsZoneNameForSld() throws Exception {
        ZoneEntity zone = createTestZone();
        CfDnsClient spyClient = spy(client);
        doReturn(recordResponse(List.of(
                RecordEntity.build("example.com", RecordType.A, 60, "1.1.1.1"),
                RecordEntity.build("my.sub.example.com", RecordType.A, 60, "2.2.2.2")),
                new ResultInfo(2)))
                .when(spyClient).getRequest(anyString(), eq(RecordMultipleResponse.class));

        List<RecordEntity> records = spyClient.recordList(zone);

        assertEquals(TEST_ZONE_NAME, records.get(0).getZoneName());
        assertEquals("@", records.get(0).getSubdomain());
        assertEquals("my.sub", records.get(1).getSubdomain());
    }

    @Test
    void testRecordListByApex() throws Exception {
        ZoneEntity zone = createTestZone();
        CfDnsClient spyClient = spy(client);
        doReturn(recordResponse(List.of(), new ResultInfo(0)))
                .when(spyClient).getRequest(anyString(), eq(RecordMultipleResponse.class));

        spyClient.recordListBySubdomain(zone, CfDnsClient.APEX);

        verify(spyClient).getRequest(
                eq("/zones/" + TEST_ZONE_ID + "/dns_records?name=example.com&order=name&direction=asc&page=1&per_page="
                        + CfDnsClient.RECORDS_PER_PAGE),
                eq(RecordMultipleResponse.class));
    }

    @Test
    void testRecordDelete_NoResult_ReturnsFalse() throws Exception {
        RecordSingleResponse mockResponse = new RecordSingleResponse();
        mockResponse.setResponseResultInfo(createSuccessResultInfo());
        CfDnsClient spyClient = spy(client);
        doReturn(mockResponse).when(spyClient).deleteRequest(anyString(), eq(RecordSingleResponse.class));

        assertFalse(spyClient.recordDelete(createTestZone(), TEST_RECORD_ID));
    }

    @Test
    void testRecordDelete_OtherId_ReturnsFalse() throws Exception {
        RecordSingleResponse mockResponse = new RecordSingleResponse();
        RecordEntity other = new RecordEntity();
        other.setId("other");
        mockResponse.setResult(other);
        mockResponse.setResponseResultInfo(createSuccessResultInfo());
        CfDnsClient spyClient = spy(client);
        doReturn(mockResponse).when(spyClient).deleteRequest(anyString(), eq(RecordSingleResponse.class));

        assertFalse(spyClient.recordDelete(createTestZone(), TEST_RECORD_ID));
    }

    @Test
    void testRecordBatch_UsesFullRecordName() throws Exception {
        ZoneEntity zone = createTestZone();
        RecordEntity apex = RecordEntity.build("example.com", RecordType.A, 300, "1.2.3.4");
        RecordEntity multiLevel = RecordEntity.build("a.b.example.com", RecordType.A, 300, "1.2.3.5");

        BatchResponse mockResponse = createBatchResponse();
        mockResponse.setResult(new BatchEntry());
        mockResponse.setResponseResultInfo(createSuccessResultInfo());
        CfDnsClient spyClient = spy(client);
        doReturn(mockResponse).when(spyClient).postRequest(anyString(), any(), eq(BatchResponse.class));

        spyClient.recordBatch(zone, List.of(apex, multiLevel), List.of(multiLevel), null, List.of(apex));

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(spyClient).postRequest(anyString(), payload.capture(), eq(BatchResponse.class));
        BatchEntry sent = (BatchEntry) payload.getValue();
        assertEquals(List.of("example.com", "a.b.example.com"),
                sent.getPosts().stream().map(RecordEntity::getName).toList());
        assertEquals("a.b.example.com", sent.getPuts().get(0).getName());
        // deletes only need the ID
        assertNull(sent.getDeletes().get(0).getName());
    }

    @Test
    void testRecordList_NullPagingIsUnambiguousAndFetchesAll() throws Exception {
        CfDnsClient spyClient = spy(client);
        doReturn(recordResponse(List.of(RecordEntity.build("a.example.com", RecordType.A, 60, "1.1.1.1")),
                new ResultInfo(1)))
                .when(spyClient).getRequest(anyString(), eq(RecordMultipleResponse.class));

        assertEquals(1, spyClient.recordList(createTestZone(), null).size());
        verify(spyClient).getRequest(eq("/zones/" + TEST_ZONE_ID
                + "/dns_records?order=name&direction=asc&page=1&per_page=" + CfDnsClient.RECORDS_PER_PAGE),
                eq(RecordMultipleResponse.class));
    }

    @Test
    void testRecordList_ExplicitPagingRequestIsPreserved() throws Exception {
        CfDnsClient spyClient = spy(client);
        doReturn(recordResponse(List.of(), new ResultInfo(0)))
                .when(spyClient).getRequest(anyString(), eq(RecordMultipleResponse.class));

        spyClient.recordList(createTestZone(), PagingRequest.forRecords(2, 42));

        verify(spyClient).getRequest(eq("/zones/" + TEST_ZONE_ID
                + "/dns_records?order=name&direction=asc&page=2&per_page=42"),
                eq(RecordMultipleResponse.class));
    }

    @Test
    void testRecordListByType_SingleTypeIsFilteredServerSide() throws Exception {
        CfDnsClient spyClient = spy(client);
        doReturn(recordResponse(List.of(), new ResultInfo(0)))
                .when(spyClient).getRequest(anyString(), eq(RecordMultipleResponse.class));

        spyClient.recordListByType(createTestZone(), RecordType.MX);
        spyClient.recordListBySubdomain(createTestZone(), "www", RecordType.TXT);

        verify(spyClient).getRequest(eq("/zones/" + TEST_ZONE_ID + "/dns_records?type=MX&order=name&direction=asc&page=1&per_page="
                + CfDnsClient.RECORDS_PER_PAGE), eq(RecordMultipleResponse.class));
        verify(spyClient).getRequest(eq("/zones/" + TEST_ZONE_ID
                + "/dns_records?name=www.example.com&type=TXT&order=name&direction=asc&page=1&per_page="
                + CfDnsClient.RECORDS_PER_PAGE), eq(RecordMultipleResponse.class));
    }

    @Test
    void testRecordListByType_MultipleTypesAreFilteredClientSide() throws Exception {
        RecordEntity unknownType = new RecordEntity();
        unknownType.setName("x.example.com");
        unknownType.setType("UNKNOWN_FUTURE_TYPE");
        CfDnsClient spyClient = spy(client);
        doReturn(recordResponse(List.of(
                RecordEntity.build("a.example.com", RecordType.A, 60, "1.1.1.1"),
                RecordEntity.build("a.example.com", RecordType.AAAA, 60, "::1"),
                RecordEntity.build("a.example.com", RecordType.TXT, 60, "txt"),
                unknownType), new ResultInfo(4)))
                .when(spyClient).getRequest(anyString(), eq(RecordMultipleResponse.class));

        // an unknown record type in the response must not break the filter
        List<RecordEntity> records = spyClient.recordListByType(createTestZone(), RecordType.A, RecordType.AAAA);

        assertEquals(List.of("A", "AAAA"), records.stream().map(RecordEntity::getType).toList());
        verify(spyClient).getRequest(eq("/zones/" + TEST_ZONE_ID + "/dns_records?order=name&direction=asc&page=1&per_page="
                + CfDnsClient.RECORDS_PER_PAGE), eq(RecordMultipleResponse.class));
    }

    private static RecordEntity fetchedMxRecord() {
        RecordEntity rec = RecordEntity.build("example.com", RecordType.MX, 300, "mail.example.com");
        rec.setId(TEST_RECORD_ID);
        rec.setPriority(10);
        rec.setProxied(false);
        rec.setComment("primary mx");
        rec.setTags(List.of("env:prod"));
        rec.setZoneId(TEST_ZONE_ID);
        rec.setZoneName(TEST_ZONE_NAME);
        rec.setLocked(false);
        rec.setProxiable(false);
        rec.setCreatedOn(java.time.Instant.parse("2025-01-01T00:00:00Z"));
        rec.setModifiedOn(java.time.Instant.parse("2025-01-02T00:00:00Z"));
        return rec;
    }

    private static void assertOnlyWritableFields(RecordEntity sent) {
        assertEquals("example.com", sent.getName());
        assertEquals("MX", sent.getType());
        assertEquals("mail.example.com", sent.getContent());
        assertEquals(10, sent.getPriority());
        assertEquals(false, sent.getProxied());
        assertEquals("primary mx", sent.getComment());
        assertEquals(List.of("env:prod"), sent.getTags());
        assertNull(sent.getZoneId());
        assertNull(sent.getZoneName());
        assertNull(sent.getLocked());
        assertNull(sent.getProxiable());
        assertNull(sent.getCreatedOn());
        assertNull(sent.getModifiedOn());
    }

    private RecordSingleResponse singleResponse(RecordEntity rec) {
        RecordSingleResponse resp = new RecordSingleResponse();
        resp.setResult(rec);
        resp.setResponseResultInfo(createSuccessResultInfo());
        return resp;
    }

    @Test
    void testRecordUpdate_DoesNotModifyGivenRecordAndSendsWritableFields() throws Exception {
        RecordEntity rec = fetchedMxRecord();
        RecordEntity before = fetchedMxRecord();
        CfDnsClient spyClient = spy(client);
        doReturn(singleResponse(fetchedMxRecord()))
                .when(spyClient).patchRequest(anyString(), any(), eq(RecordSingleResponse.class));

        spyClient.recordUpdate(createTestZone(), rec);

        assertEquals(before, rec);
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(spyClient).patchRequest(eq("/zones/" + TEST_ZONE_ID + "/dns_records/" + TEST_RECORD_ID),
                payload.capture(), eq(RecordSingleResponse.class));
        assertOnlyWritableFields((RecordEntity) payload.getValue());
    }

    @Test
    void testRecordCreate_SendsWritableFieldsWithoutId() throws Exception {
        RecordEntity rec = fetchedMxRecord();
        CfDnsClient spyClient = spy(client);
        doReturn(singleResponse(fetchedMxRecord()))
                .when(spyClient).postRequest(anyString(), any(), eq(RecordSingleResponse.class));

        spyClient.recordCreate(createTestZone(), rec);

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(spyClient).postRequest(anyString(), payload.capture(), eq(RecordSingleResponse.class));
        RecordEntity sent = (RecordEntity) payload.getValue();
        assertOnlyWritableFields(sent);
        assertNull(sent.getId());
        assertEquals(TEST_RECORD_ID, rec.getId());
    }

    @Test
    void testRecordBatch_KeepsPriorityDataAndProxied() throws Exception {
        RecordEntity srv = RecordEntity.build("_sip._tcp.example.com", RecordType.SRV, 300, null);
        srv.setId("srv1");
        srv.setPriority(10);
        srv.setData(Map.of("priority", 10, "weight", 5, "port", 5060, "target", "sip.example.com"));
        RecordEntity proxied = RecordEntity.build("www.example.com", RecordType.A, 1, "1.2.3.4");
        proxied.setId("www1");
        proxied.setProxied(true);

        BatchResponse mockResponse = createBatchResponse();
        mockResponse.setResult(new BatchEntry());
        mockResponse.setResponseResultInfo(createSuccessResultInfo());
        CfDnsClient spyClient = spy(client);
        doReturn(mockResponse).when(spyClient).postRequest(anyString(), any(), eq(BatchResponse.class));

        spyClient.recordBatch(createTestZone(), List.of(srv), List.of(proxied), List.of(srv), null);

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(spyClient).postRequest(anyString(), payload.capture(), eq(BatchResponse.class));
        BatchEntry sent = (BatchEntry) payload.getValue();
        RecordEntity sentPost = sent.getPosts().get(0);
        assertNull(sentPost.getId());
        assertEquals(10, sentPost.getPriority());
        assertEquals(5060, sentPost.getData().get("port"));
        assertEquals("www1", sent.getPuts().get(0).getId());
        assertEquals(true, sent.getPuts().get(0).getProxied());
        assertEquals("srv1", sent.getPatches().get(0).getId());
        assertEquals(10, sent.getPatches().get(0).getPriority());
    }

    @Test
    void testInvalidSubdomainIsRejectedBeforeRequest() throws Exception {
        CfDnsClient spyClient = spy(client);
        ZoneEntity zone = createTestZone();
        for (String sub : List.of("; rm -rf /", "| cat /etc/passwd", "`shutdown -h now`",
                "<script>alert('x')</script>", "\"quoted\"", "a b", "x?y", "x#y", "x&y", "x\ty", " ", "")) {
            assertThrows(IllegalArgumentException.class, () -> spyClient.recordListBySubdomain(zone, sub), sub);
            assertThrows(IllegalArgumentException.class,
                    () -> spyClient.recordCreateSubdomain(zone, sub, 60, RecordType.A, "1.2.3.4"), sub);
        }
        assertThrows(IllegalArgumentException.class, () -> spyClient.recordListBySubdomain(zone, null));
        String tooLongIdnLabel = "ä".repeat(64);
        assertThrows(IllegalArgumentException.class,
                () -> spyClient.recordListBySubdomain(zone, tooLongIdnLabel));
        assertThrows(IllegalArgumentException.class,
                () -> spyClient.recordCreateSubdomain(zone, tooLongIdnLabel, 60, RecordType.A,
                        "1.2.3.4"));
        assertThrows(IllegalArgumentException.class, () -> spyClient.zoneGet("example.com&account.id=1"));
        assertThrows(IllegalArgumentException.class, () -> spyClient.zoneGet(" "));
        verify(spyClient, never()).getRequest(anyString(), any());
        verify(spyClient, never()).postRequest(anyString(), any(), any());
    }

    @Test
    void testUnusualButValidSubdomainIsEncoded() throws Exception {
        CfDnsClient spyClient = spy(client);
        doReturn(recordResponse(List.of(), new ResultInfo(0)))
                .when(spyClient).getRequest(anyString(), eq(RecordMultipleResponse.class));

        spyClient.recordListBySubdomain(createTestZone(), "$(reboot)");
        spyClient.recordListBySubdomain(createTestZone(), "0/26");

        verify(spyClient).getRequest(eq("/zones/" + TEST_ZONE_ID
                + "/dns_records?name=%24%28reboot%29.example.com&order=name&direction=asc&page=1&per_page=" + CfDnsClient.RECORDS_PER_PAGE),
                eq(RecordMultipleResponse.class));
        verify(spyClient).getRequest(eq("/zones/" + TEST_ZONE_ID
                + "/dns_records?name=0%2F26.example.com&order=name&direction=asc&page=1&per_page=" + CfDnsClient.RECORDS_PER_PAGE),
                eq(RecordMultipleResponse.class));
    }

    @Test
    void testIdnSubdomainIsConvertedToPunycode() throws Exception {
        CfDnsClient spyClient = spy(client);
        ZoneEntity zone = createTestZone();
        doReturn(recordResponse(List.of(), new ResultInfo(0)))
                .when(spyClient).getRequest(anyString(), eq(RecordMultipleResponse.class));
        doReturn(singleResponse(RecordEntity.build(
                "xn--bcher-kva.example.com", RecordType.A, 60, "1.2.3.4")))
                .when(spyClient).postRequest(anyString(), any(), eq(RecordSingleResponse.class));

        spyClient.recordListBySubdomain(zone, "bücher");
        spyClient.recordListBySubdomain(zone, "shop.bücher");
        spyClient.recordListBySubdomain(zone, "xn--bcher-kva");
        // code points newer than IDNA2003 (Unicode 3.2) like emojis must not be rejected
        spyClient.recordListBySubdomain(zone, "emoji-🥷-忍者");
        spyClient.recordCreateSubdomain(zone, "bücher", 60, RecordType.A, "1.2.3.4");

        String base = "/zones/" + TEST_ZONE_ID + "/dns_records?name=";
        String suffix = "&order=name&direction=asc&page=1&per_page=" + CfDnsClient.RECORDS_PER_PAGE;
        verify(spyClient, times(2)).getRequest(eq(base + "xn--bcher-kva.example.com" + suffix),
                eq(RecordMultipleResponse.class));
        verify(spyClient).getRequest(eq(base + "shop.xn--bcher-kva.example.com" + suffix),
                eq(RecordMultipleResponse.class));
        String emojiPunycode = IDN.toASCII("emoji-🥷-忍者", IDN.ALLOW_UNASSIGNED);
        assertTrue(emojiPunycode.startsWith("xn--"), emojiPunycode);
        verify(spyClient).getRequest(eq(base + emojiPunycode + ".example.com" + suffix),
                eq(RecordMultipleResponse.class));

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(spyClient).postRequest(eq("/zones/" + TEST_ZONE_ID + "/dns_records"),
                payload.capture(), eq(RecordSingleResponse.class));
        assertEquals("xn--bcher-kva.example.com", ((RecordEntity) payload.getValue()).getName());
    }

    @Test
    void testRecordIdPathTraversalIsRejected() throws Exception {
        CfDnsClient spyClient = spy(client);
        ZoneEntity zone = createTestZone();
        assertThrows(IllegalArgumentException.class, () -> spyClient.recordDelete(zone, "../.."));
        RecordEntity rec = RecordEntity.build("../..", "1.2.3.4");
        assertThrows(IllegalArgumentException.class, () -> spyClient.recordUpdate(zone, rec));
        ZoneEntity badZone = zone("../../accounts", "example.com");
        assertThrows(IllegalArgumentException.class, () -> spyClient.recordList(badZone));
        verify(spyClient, never()).deleteRequest(anyString(), any());
        verify(spyClient, never()).patchRequest(anyString(), any(), any());
        verify(spyClient, never()).getRequest(anyString(), any());
    }

    private static RecordEntity rec(String id, String name) {
        RecordEntity rec = RecordEntity.build(name, RecordType.A, 60, "1.1.1.1");
        rec.setId(id);
        return rec;
    }

    private static String recordsPage(int page) {
        return "/zones/" + TEST_ZONE_ID + "/dns_records?order=name&direction=asc&page=" + page
                + "&per_page=" + CfDnsClient.RECORDS_PER_PAGE;
    }

    @Test
    void testRecordList_InconsistentPagesAreFetchedAgain() throws Exception {
        CfDnsClient spyClient = spy(client);
        // 1st attempt: page 2 repeats a record of page 1 (unstable order), so record c is missing
        doReturn(recordResponse(List.of(rec("a", "a.example.com"), rec("b", "b.example.com")),
                new ResultInfo(1, 2, 2, 2, 3)))
                .when(spyClient).getRequest(eq(recordsPage(1)), eq(RecordMultipleResponse.class));
        doReturn(recordResponse(List.of(rec("b", "b.example.com")), new ResultInfo(2, 2, 2, 1, 3)),
                recordResponse(List.of(rec("c", "c.example.com")), new ResultInfo(2, 2, 2, 1, 3)))
                .when(spyClient).getRequest(eq(recordsPage(2)), eq(RecordMultipleResponse.class));

        List<RecordEntity> records = spyClient.recordList(createTestZone());

        assertEquals(List.of("a", "b", "c"), records.stream().map(RecordEntity::getId).toList());
        verify(spyClient, times(2)).getRequest(eq(recordsPage(1)), eq(RecordMultipleResponse.class));
        verify(spyClient, times(2)).getRequest(eq(recordsPage(2)), eq(RecordMultipleResponse.class));
    }

    @Test
    void testRecordList_PersistentlyInconsistentPagesReturnDeduplicatedResult() throws Exception {
        CfDnsClient spyClient = spy(client);
        doReturn(recordResponse(List.of(rec("a", "a.example.com"), rec("b", "b.example.com")),
                new ResultInfo(1, 2, 2, 2, 3)))
                .when(spyClient).getRequest(eq(recordsPage(1)), eq(RecordMultipleResponse.class));
        doReturn(recordResponse(List.of(rec("b", "b.example.com")), new ResultInfo(2, 2, 2, 1, 3)))
                .when(spyClient).getRequest(eq(recordsPage(2)), eq(RecordMultipleResponse.class));

        List<RecordEntity> records = spyClient.recordList(createTestZone());

        // no duplicates, after 3 attempts the incomplete result is returned
        assertEquals(List.of("a", "b"), records.stream().map(RecordEntity::getId).toList());
        verify(spyClient, times(3)).getRequest(eq(recordsPage(1)), eq(RecordMultipleResponse.class));
    }
}
