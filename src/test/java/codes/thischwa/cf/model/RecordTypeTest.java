package codes.thischwa.cf.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RecordTypeTest {

    @Test
    void testGetType() {
        assertEquals("A", RecordType.A.getType());
        assertEquals("AAAA", RecordType.AAAA.getType());
        assertEquals("CNAME", RecordType.CNAME.getType());
        assertEquals("TXT", RecordType.TXT.getType());
        assertEquals("SRV", RecordType.SRV.getType());
        assertEquals("LOC", RecordType.LOC.getType());
        assertEquals("MX", RecordType.MX.getType());
        assertEquals("NS", RecordType.NS.getType());
        assertEquals("CAA", RecordType.CAA.getType());
        assertEquals("CERT", RecordType.CERT.getType());
        assertEquals("DNSKEY", RecordType.DNSKEY.getType());
        assertEquals("DS", RecordType.DS.getType());
        assertEquals("NAPTR", RecordType.NAPTR.getType());
        assertEquals("SMIMEA", RecordType.SMIMEA.getType());
        assertEquals("SSHFP", RecordType.SSHFP.getType());
        assertEquals("TLSA", RecordType.TLSA.getType());
        assertEquals("URI", RecordType.URI.getType());
    }

    @Test
    void testToString() {
        assertEquals("A", RecordType.A.toString());
        assertEquals("CNAME", RecordType.CNAME.toString());
    }

    @Test
    void testValueOf() {
        assertEquals(RecordType.A, RecordType.valueOf("A"));
        assertEquals(RecordType.CNAME, RecordType.valueOf("CNAME"));
    }
}
