package codes.thischwa.cf.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PagingRequestTest {

    @Test
    void testBuildPath() {
        String result = PagingRequest.defaultPaging().addQueryString("/zones");
        assertEquals("/zones?page=1&per_page=1000", result);
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
