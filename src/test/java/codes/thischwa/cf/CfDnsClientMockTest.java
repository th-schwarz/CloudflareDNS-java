package codes.thischwa.cf;

import codes.thischwa.cf.fluent.ZoneOperations;
import codes.thischwa.cf.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
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
        PagingRequest pagingRequest = PagingRequest.of(10, 1);

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

        List<RecordEntity> records = spyClient.recordList(zone, "test");

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

        List<RecordEntity> records = spyClient.recordList(zone, "test", RecordType.A);

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

        List<RecordEntity> records = spyClient.recordList(zone, RecordType.A);

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
        doThrow(new CloudflareNotFoundException("Not found")).when(spyClient).recordList(any(), anyString(), any());

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
}
