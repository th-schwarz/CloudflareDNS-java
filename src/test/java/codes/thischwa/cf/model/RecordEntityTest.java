package codes.thischwa.cf.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class RecordEntityTest {

    @Test
    void testBuildWithAttributes() {
        RecordEntity rec = RecordEntity.build("example.com", RecordType.A, 120, "1.2.3.4");
        assertEquals("example.com", rec.getName());
        assertEquals("A", rec.getType());
        assertEquals(120, rec.getTtl());
        assertEquals("1.2.3.4", rec.getContent());
    }

    @Test
    void testBuildWithIdAndContent() {
        RecordEntity rec = RecordEntity.build("id-123", "1.2.3.4");
        assertEquals("id-123", rec.getId());
        assertEquals("1.2.3.4", rec.getContent());
    }

    @Test
    void testBuildWithIdAndAttributes() {
        RecordEntity rec = RecordEntity.build("id-123", "example.com", "A", 120, "1.2.3.4");
        assertEquals("id-123", rec.getId());
        assertEquals("example.com", rec.getName());
        assertEquals("A", rec.getType());
        assertEquals(120, rec.getTtl());
        assertEquals("1.2.3.4", rec.getContent());
    }

    @Test
    void testBuildWithInvalidType() {
        assertThrows(IllegalArgumentException.class, () -> 
            RecordEntity.build("id-123", "example.com", "INVALID", 120, "1.2.3.4")
        );
    }

    @Test
    void testGetSld() {
        RecordEntity rec = new RecordEntity();
        assertNull(rec.getSubdomain());

        rec.setName("sub.example.com");
        assertEquals("sub", rec.getSubdomain());

        rec.setName("example.com");
        assertEquals("example", rec.getSubdomain());

        rec.setName("host");
        assertEquals("host", rec.getSubdomain());
        
        rec.setName(".dotstart");
        assertEquals(".dotstart", rec.getSubdomain());
    }

    @Test
    void testGetSldWithZoneName() {
        RecordEntity rec = new RecordEntity();
        rec.setName("sub.example.com");
        rec.setZoneName("example.com");
        assertEquals("sub", rec.getSubdomain());

        rec.setName("my.sub.example.com");
        rec.setZoneName("example.com");
        assertEquals("my.sub", rec.getSubdomain());
    }

    @Test
    void testGetSldApexWithZoneName() {
        RecordEntity rec = new RecordEntity();
        rec.setName("example.com");
        rec.setZoneName("example.com");
        assertEquals("@", rec.getSubdomain());

        rec.setName("Example.COM");
        assertEquals("@", rec.getSubdomain());
    }

    @Test
    void testGetSldZoneNameIsNoLabelSuffix() {
        // "notexample.com" ends with "example.com", but is not part of the zone
        RecordEntity rec = new RecordEntity();
        rec.setName("notexample.com");
        rec.setZoneName("example.com");
        assertEquals("notexample", rec.getSubdomain());
    }

    @Test
    void testGetSldCaseInsensitive() {
        RecordEntity rec = new RecordEntity();
        rec.setName("WWW.Example.com");
        rec.setZoneName("example.com");
        assertEquals("WWW", rec.getSubdomain());
    }

    @Test
    @SuppressWarnings("removal")
    void testDeprecatedGetSldDelegatesToGetSubdomain() {
        RecordEntity rec = new RecordEntity();
        rec.setName("my.sub.example.com");
        rec.setZoneName("example.com");
        assertEquals(rec.getSubdomain(), rec.getSld());
    }
}
