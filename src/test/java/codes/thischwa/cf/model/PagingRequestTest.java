package codes.thischwa.cf.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PagingRequestTest {

    @Test
    void testBuildPath() {
        String result = PagingRequest.defaultPaging().addQueryString("/zones");
        assertEquals("/zones?page=1&per_page=50", result);
    }

    @Test
    void testPagingRequestBoundaries() {
        assertDoesNotThrow(() -> PagingRequest.of(1, 1));
        assertDoesNotThrow(() -> PagingRequest.of(1, 5_000_000));
        assertThrows(IllegalArgumentException.class, () -> PagingRequest.of(0, 1));
        assertThrows(IllegalArgumentException.class, () -> PagingRequest.of(1, 0));
        assertThrows(IllegalArgumentException.class, () -> PagingRequest.of(1, 5_000_001));
    }

    @Test
    void testSettersValidatePagingValues() {
        PagingRequest request = PagingRequest.of(1, 50);

        assertThrows(IllegalArgumentException.class, () -> request.setPage(0));
        assertThrows(IllegalArgumentException.class, () -> request.setPerPage(5_000_001));
        assertEquals(1, request.getPage());
        assertEquals(50, request.getPerPage());
    }

    @Test
    void testBuildPathAdditional() {
        String result = new PagingRequest( 10, 100).addQueryString("/zones?foo=bar");
        assertEquals("/zones?foo=bar&page=10&per_page=100", result);
    }

    @Test
    void testGetPagingParams() {
        PagingRequest request = PagingRequest.of(2, 50);
        java.util.Map<String, String> params = request.getPagingParams();
        assertEquals("2", params.get("page"));
        assertEquals("50", params.get("perPage"));
    }

    @Test
    void testLombokMethods() {
        PagingRequest req1 = PagingRequest.of(1, 10);
        PagingRequest req2 = PagingRequest.of(1, 10);
        assertEquals(req1, req2);
        assertEquals(req1.hashCode(), req2.hashCode());
        assertTrue(req1.toString().contains("page=1"));
        
        req1.setPage(5);
        assertEquals(5, req1.getPage());
        req1.setPerPage(20);
        assertEquals(20, req1.getPerPage());
    }

}
