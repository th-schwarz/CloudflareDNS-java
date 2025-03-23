package codes.thischwa.cf.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PagingRequestTest {

    @Test
    public void testBuildPath() {
        String result = PagingRequest.defaultPaging().addQueryString("/zones");
        assertEquals("/zones?page=1&perPage=5000000", result);
    }

    @Test
    public void testBuildPathAdditional() {
        String result = new PagingRequest( 10, 100).addQueryString("/zones?foo=bar");
        assertEquals("/zones?foo=bar&page=10&perPage=100", result);
    }

}
