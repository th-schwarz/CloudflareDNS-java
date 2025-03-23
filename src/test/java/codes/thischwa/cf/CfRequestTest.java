package codes.thischwa.cf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import codes.thischwa.cf.model.RecordType;
import org.junit.jupiter.api.Test;

public class CfRequestTest {

    @Test
    public void testBuildPathWithSingleVariable() {
        String result = CfRequest.RECORD_CREATE.buildPath("zone123");
        assertEquals("/zones/zone123/dns_records", result);
    }

    @Test
    public void testBuildPathWithMultipleVariables() {
        String result = CfRequest.RECORD_UPDATE.buildPath("zone123", "record456");
        assertEquals("/zones/zone123/dns_records/record456", result);
    }

    @Test
    public void testBuildPathWithoutVariables() {
        String result = CfRequest.ZONE_LIST.buildPath();
        assertEquals("/zones", result);
    }

    @Test
    public void testBuildRecordInfoName() {
        String result = CfRequest.RECORD_INFO_NAME.buildPath("zone123", "sub.domain.com");
        assertEquals("/zones/zone123/dns_records?name=sub.domain.com", result);
    }

    @Test
    public void testBuildRecordDelete() {
        String result = CfRequest.RECORD_DELETE.buildPath("zone123", "record789");
        assertEquals("/zones/zone123/dns_records/record789", result);
    }

    @Test
    public void testBuildZoneInfo() {
        String result = CfRequest.ZONE_INFO.buildPath("zone123");
        assertEquals("/zones?name=zone123", result);
    }

    @Test
    public void testBuildRecordInfo() {
        String result = CfRequest.RECORD_INFO_NAME_TYPE.buildPath("zone123", "sld.domain.com", RecordType.A);
        assertEquals("/zones/zone123/dns_records?name=sld.domain.com&type=A", result);
    }

    @Test
    public void testBuildPathInvalidArguments() {
        assertThrows(
                IllegalArgumentException.class,
                () -> CfRequest.RECORD_INFO_NAME_TYPE.buildPath("zone123", "sld.domain.com"));
    }
}
