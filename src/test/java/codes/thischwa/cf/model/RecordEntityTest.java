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
        assertNull(rec.getSld());

        rec.setName("sub.example.com");
        assertEquals("sub", rec.getSld());

        rec.setName("example.com");
        assertEquals("example", rec.getSld());

        rec.setName("host");
        assertEquals("host", rec.getSld());
        
        rec.setName(".dotstart");
        assertEquals(".dotstart", rec.getSld());
    }

    @Test
    void testGetSldWithZoneName() {
        RecordEntity rec = new RecordEntity();
        rec.setName("sub.example.com");
        rec.setZoneName("example.com");
        assertEquals("sub", rec.getSld());

        rec.setName("my.sub.example.com");
        rec.setZoneName("example.com");
        assertEquals("my.sub", rec.getSld());
    }
}
