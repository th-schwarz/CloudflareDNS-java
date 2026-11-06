package codes.thischwa.cf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        String result = CfRequest.RECORD_LIST_NAME.buildPath("zone123", "sub.domain.com");
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
      String result = CfRequest.RECORD_LIST_NAME.buildPath("zone123", "sld.domain.com");
      assertEquals("/zones/zone123/dns_records?name=sld.domain.com", result);
    }

    @Test
    public void testBuildPathInvalidArguments() {
        assertThrows(
                IllegalArgumentException.class,
            () -> CfRequest.RECORD_UPDATE.buildPath("zone123"));
    }

    @Test
    public void testQueryValueIsUrlEncoded() {
        assertEquals("/zones?name=example.com%26account.id%3D1",
            CfRequest.ZONE_INFO.buildPath("example.com&account.id=1"));
        assertEquals("/zones/zone123/dns_records?name=..%2F..%2Fx.example.com",
            CfRequest.RECORD_LIST_NAME.buildPath("zone123", "../../x.example.com"));
        assertEquals("/zones/zone123/dns_records?name=%24%28reboot%29.example.com",
            CfRequest.RECORD_LIST_NAME.buildPath("zone123", "$(reboot).example.com"));
        assertEquals("/zones/zone123/dns_records?name=%F0%9F%A5%B7.example.com",
            CfRequest.RECORD_LIST_NAME.buildPath("zone123", "\uD83E\uDD77.example.com"));
        assertEquals("/zones/zone123/dns_records?name=*.example.com",
            CfRequest.RECORD_LIST_NAME.buildPath("zone123", "*.example.com"));
    }

    @Test
    public void testInvalidIdentifiersAreRejected() {
        for (String id : new String[] {"..", "../..", "a/b", "a b", "id?x=1", "id#x", "", "%2e%2e",
            "a".repeat(65)}) {
            assertThrows(IllegalArgumentException.class,
                () -> CfRequest.RECORD_DELETE.buildPath("zone123", id), "id: " + id);
            assertThrows(IllegalArgumentException.class,
                () -> CfRequest.RECORD_LIST.buildPath(id), "zone id: " + id);
        }
        assertThrows(IllegalArgumentException.class, () -> CfRequest.RECORD_LIST.buildPath((Object) null));
        assertEquals("/zones/0123456789abcdef0123456789abcdef/dns_records",
            CfRequest.RECORD_LIST.buildPath("0123456789abcdef0123456789abcdef"));
    }
}
