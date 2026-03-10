package codes.thischwa.cf.fluent;

import codes.thischwa.cf.CfDnsClient;
import codes.thischwa.cf.CloudflareApiException;
import codes.thischwa.cf.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the Fluent API package (ZoneOperations and RecordOperations).
 */
class FluentApiTest {

    private CfDnsClient mockClient;
    private ZoneEntity testZone;

    private static final String TEST_ZONE_ID = "zone123";
    private static final String TEST_ZONE_NAME = "example.com";
    private static final String TEST_SLD = "test";
    private static final String TEST_RECORD_ID = "rec123";

    @BeforeEach
    void setUp() {
        mockClient = mock(CfDnsClient.class);
        testZone = new ZoneEntity();
        testZone.setId(TEST_ZONE_ID);
        testZone.setName(TEST_ZONE_NAME);
    }

    @Test
    void testZoneOperations_GetRecord() throws CloudflareApiException {
        ZoneOperations zoneOps = new ZoneOperationsImpl(mockClient, testZone);

        RecordOperations recordOps = zoneOps.getRecord(TEST_SLD);

        assertNotNull(recordOps);
        assertInstanceOf(RecordOperationsImpl.class, recordOps);
    }

    @Test
    void testZoneOperations_GetRecordWithTypes() throws CloudflareApiException {
        ZoneOperations zoneOps = new ZoneOperationsImpl(mockClient, testZone);

        RecordOperations recordOps = zoneOps.getRecord(TEST_SLD, RecordType.A, RecordType.AAAA);

        assertNotNull(recordOps);
        assertInstanceOf(RecordOperationsImpl.class, recordOps);
    }

    @Test
    void testZoneOperations_List() throws CloudflareApiException {
        RecordEntity rec1 = RecordEntity.build("test.example.com", RecordType.A, 300, "1.2.3.4");
        RecordEntity rec2 = RecordEntity.build("www.example.com", RecordType.A, 300, "1.2.3.5");
        List<RecordEntity> expectedRecords = List.of(rec1, rec2);

        when(mockClient.recordList(eq(testZone), any(RecordType[].class)))
                .thenReturn(expectedRecords);

        ZoneOperations zoneOps = new ZoneOperationsImpl(mockClient, testZone);
        List<RecordEntity> result = zoneOps.list();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(mockClient, times(1)).recordList(eq(testZone), any(RecordType[].class));
    }

    @Test
    void testZoneOperations_ListWithTypes() throws CloudflareApiException {
        RecordEntity rec1 = RecordEntity.build("test.example.com", RecordType.A, 300, "1.2.3.4");
        List<RecordEntity> expectedRecords = List.of(rec1);

        when(mockClient.recordList(eq(testZone), any(RecordType[].class)))
                .thenReturn(expectedRecords);

        ZoneOperations zoneOps = new ZoneOperationsImpl(mockClient, testZone);
        List<RecordEntity> result = zoneOps.list(RecordType.A);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(mockClient, times(1)).recordList(eq(testZone), any(RecordType[].class));
    }

    @Test
    void testRecordOperations_Get() throws CloudflareApiException {
        RecordEntity rec1 = RecordEntity.build("test.example.com", RecordType.A, 300, "1.2.3.4");
        List<RecordEntity> expectedRecords = List.of(rec1);

        when(mockClient.recordList(eq(testZone), eq(TEST_SLD), any()))
                .thenReturn(expectedRecords);

        RecordOperations recordOps = new RecordOperationsImpl(mockClient, testZone, TEST_SLD, null);
        List<RecordEntity> result = recordOps.get();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("1.2.3.4", result.get(0).getContent());
        verify(mockClient, times(1)).recordList(eq(testZone), eq(TEST_SLD), any());
    }

    @Test
    void testRecordOperations_GetWithTypes() throws CloudflareApiException {
        RecordEntity rec1 = RecordEntity.build("test.example.com", RecordType.A, 300, "1.2.3.4");
        List<RecordEntity> expectedRecords = List.of(rec1);

        RecordType[] types = {RecordType.A};
        when(mockClient.recordList(eq(testZone), eq(TEST_SLD), eq(types)))
                .thenReturn(expectedRecords);

        RecordOperations recordOps = new RecordOperationsImpl(mockClient, testZone, TEST_SLD, types);
        List<RecordEntity> result = recordOps.get();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(mockClient, times(1)).recordList(eq(testZone), eq(TEST_SLD), eq(types));
    }

    @Test
    void testRecordOperations_Create() throws CloudflareApiException {
        RecordEntity createdRecord = RecordEntity.build("test.example.com", RecordType.A, 300, "1.2.3.4");
        createdRecord.setId(TEST_RECORD_ID);

        when(mockClient.recordCreateSld(eq(testZone), eq(TEST_SLD), eq(300), eq(RecordType.A), eq("1.2.3.4")))
                .thenReturn(createdRecord);

        RecordOperations recordOps = new RecordOperationsImpl(mockClient, testZone, TEST_SLD, null);
        RecordEntity result = recordOps.create(RecordType.A, "1.2.3.4", 300);

        assertNotNull(result);
        assertEquals(TEST_RECORD_ID, result.getId());
        assertEquals("1.2.3.4", result.getContent());
        verify(mockClient, times(1)).recordCreateSld(eq(testZone), eq(TEST_SLD), eq(300), eq(RecordType.A), eq("1.2.3.4"));
    }

    @Test
    void testRecordOperations_Update_Success() throws CloudflareApiException {
        RecordEntity existingRecord = RecordEntity.build("test.example.com", RecordType.A, 300, "1.2.3.4");
        existingRecord.setId(TEST_RECORD_ID);

        RecordEntity updatedRecord = RecordEntity.build("test.example.com", RecordType.A, 300, "1.2.3.5");
        updatedRecord.setId(TEST_RECORD_ID);

        when(mockClient.recordList(eq(testZone), eq(TEST_SLD), any()))
                .thenReturn(List.of(existingRecord));
        when(mockClient.recordUpdate(eq(testZone), any(RecordEntity.class)))
                .thenReturn(updatedRecord);

        RecordOperations recordOps = new RecordOperationsImpl(mockClient, testZone, TEST_SLD, null);
        RecordEntity result = recordOps.update("1.2.3.5");

        assertNotNull(result);
        assertEquals("1.2.3.5", result.getContent());
        verify(mockClient, times(1)).recordList(eq(testZone), eq(TEST_SLD), any());
        verify(mockClient, times(1)).recordUpdate(eq(testZone), any(RecordEntity.class));
    }

    @Test
    void testRecordOperations_Update_NoRecordsFound() throws CloudflareApiException {
        when(mockClient.recordList(eq(testZone), eq(TEST_SLD), any()))
                .thenReturn(List.of());

        RecordOperations recordOps = new RecordOperationsImpl(mockClient, testZone, TEST_SLD, null);

        CloudflareApiException exception = assertThrows(CloudflareApiException.class, () -> {
            recordOps.update("1.2.3.5");
        });

        assertTrue(exception.getMessage().contains("No recs found"));
        verify(mockClient, times(1)).recordList(eq(testZone), eq(TEST_SLD), any());
        verify(mockClient, never()).recordUpdate(any(), any());
    }

    @Test
    void testRecordOperations_Update_MultipleRecordsFound() throws CloudflareApiException {
        RecordEntity rec1 = RecordEntity.build("test.example.com", RecordType.A, 300, "1.2.3.4");
        RecordEntity rec2 = RecordEntity.build("test.example.com", RecordType.AAAA, 300, "::1");

        when(mockClient.recordList(eq(testZone), eq(TEST_SLD), any()))
                .thenReturn(List.of(rec1, rec2));

        RecordOperations recordOps = new RecordOperationsImpl(mockClient, testZone, TEST_SLD, null);

        CloudflareApiException exception = assertThrows(CloudflareApiException.class, () -> {
            recordOps.update("1.2.3.5");
        });

        assertTrue(exception.getMessage().contains("Multiple recs found"));
        verify(mockClient, times(1)).recordList(eq(testZone), eq(TEST_SLD), any());
        verify(mockClient, never()).recordUpdate(any(), any());
    }

    @Test
    void testRecordOperations_Delete() throws CloudflareApiException {
        doNothing().when(mockClient).recordDeleteTypeIfExists(eq(testZone), eq(TEST_SLD), any(RecordType[].class));

        RecordOperations recordOps = new RecordOperationsImpl(mockClient, testZone, TEST_SLD, null);
        recordOps.delete(RecordType.A, RecordType.AAAA);

        verify(mockClient, times(1)).recordDeleteTypeIfExists(eq(testZone), eq(TEST_SLD), any(RecordType[].class));
    }

    @Test
    void testRecordOperations_DeleteSingleType() throws CloudflareApiException {
        doNothing().when(mockClient).recordDeleteTypeIfExists(eq(testZone), eq(TEST_SLD), any(RecordType[].class));

        RecordOperations recordOps = new RecordOperationsImpl(mockClient, testZone, TEST_SLD, null);
        recordOps.delete(RecordType.A);

        verify(mockClient, times(1)).recordDeleteTypeIfExists(eq(testZone), eq(TEST_SLD), any(RecordType[].class));
    }

    @Test
    void testFluentApiChaining() throws CloudflareApiException {
        // Test that fluent API chaining works correctly
        RecordEntity createdRecord = RecordEntity.build("test.example.com", RecordType.A, 300, "1.2.3.4");
        createdRecord.setId(TEST_RECORD_ID);

        when(mockClient.recordCreateSld(eq(testZone), eq(TEST_SLD), eq(300), eq(RecordType.A), eq("1.2.3.4")))
                .thenReturn(createdRecord);

        ZoneOperations zoneOps = new ZoneOperationsImpl(mockClient, testZone);
        RecordEntity result = zoneOps.getRecord(TEST_SLD).create(RecordType.A, "1.2.3.4", 300);

        assertNotNull(result);
        assertEquals(TEST_RECORD_ID, result.getId());
    }

    @Test
    void testConstructorFields() {
        // Test that constructor properly initializes fields
        RecordType[] types = {RecordType.A, RecordType.AAAA};
        RecordOperationsImpl recordOps = new RecordOperationsImpl(mockClient, testZone, TEST_SLD, types);

        assertNotNull(recordOps);
    }

    @Test
    void testZoneOperationsImplConstructor() {
        ZoneOperationsImpl zoneOps = new ZoneOperationsImpl(mockClient, testZone);

        assertNotNull(zoneOps);
    }
}
