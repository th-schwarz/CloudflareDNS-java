package codes.thischwa.cf.model;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class BatchEntryTest {

    @Test
    void testBatchEntry() {
        BatchEntry entry = new BatchEntry();
        assertEquals("", entry.getId());

        List<RecordEntity> patches = new ArrayList<>();
        List<RecordEntity> posts = new ArrayList<>();
        List<RecordEntity> puts = new ArrayList<>();
        List<RecordEntity> deletes = new ArrayList<>();

        entry.setPatches(patches);
        entry.setPosts(posts);
        entry.setPuts(puts);
        entry.setDeletes(deletes);

        assertSame(patches, entry.getPatches());
        assertSame(posts, entry.getPosts());
        assertSame(puts, entry.getPuts());
        assertSame(deletes, entry.getDeletes());
    }

    @Test
    void testLombokMethods() {
        BatchEntry entry1 = new BatchEntry();
        BatchEntry entry2 = new BatchEntry();
        assertEquals(entry1, entry2);
        assertEquals(entry1.hashCode(), entry2.hashCode());

        List<RecordEntity> patches = List.of(new RecordEntity());
        entry1.setPatches(patches);
        assertNotEquals(entry1, entry2);
        
        entry2.setPatches(patches);
        assertEquals(entry1, entry2);
        
        assertTrue(entry1.toString().contains("patches="));
    }
}
