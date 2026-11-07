package codes.thischwa.cf.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PagingRequestTest {

  @Test
  void testBuildPath() {
    String result = PagingRequest.defaultPaging().addQueryString("/zones");
    assertEquals("/zones?page=1&per_page=50", result);
  }

  @Test
  void testBuildPathWithExistingQuery() {
    String result = PagingRequest.forRecords(10, 100).addQueryString("/zones?foo=bar");
    assertEquals("/zones?foo=bar&page=10&per_page=100", result);
  }

  @Test
  void testPagingRequestBoundaries() {
    assertDoesNotThrow(() -> PagingRequest.forRecords(1, 1));
    assertDoesNotThrow(() -> PagingRequest.forRecords(1, PagingRequest.MAX_RECORDS_PER_PAGE));
    assertThrows(IllegalArgumentException.class, () -> PagingRequest.forRecords(0, 1));
    assertThrows(IllegalArgumentException.class, () -> PagingRequest.forRecords(1, 0));
    assertThrows(IllegalArgumentException.class,
        () -> PagingRequest.forRecords(1, PagingRequest.MAX_RECORDS_PER_PAGE + 1));
  }

  @Test
  void testZonePageSizeBoundaries() {
    assertDoesNotThrow(() -> PagingRequest.forZones(1, PagingRequest.MIN_ZONES_PER_PAGE));
    assertDoesNotThrow(() -> PagingRequest.forZones(1, PagingRequest.MAX_ZONES_PER_PAGE));
    assertThrows(IllegalArgumentException.class,
        () -> PagingRequest.forZones(1, PagingRequest.MIN_ZONES_PER_PAGE - 1));
    assertThrows(IllegalArgumentException.class,
        () -> PagingRequest.forZones(1, PagingRequest.MAX_ZONES_PER_PAGE + 1));
  }

  @Test
  void testNextAndWithPageReturnNewRequests() {
    PagingRequest request = PagingRequest.forRecords(2, 50);

    assertEquals(PagingRequest.forRecords(3, 50), request.next());
    assertEquals(PagingRequest.forRecords(5, 50), request.withPage(5));
    assertEquals(PagingRequest.forRecords(2, 50), request);
  }

  @Test
  void testRecordMethods() {
    PagingRequest first = PagingRequest.forRecords(1, 10);
    PagingRequest second = PagingRequest.forRecords(1, 10);

    assertEquals(first, second);
    assertEquals(first.hashCode(), second.hashCode());
    assertTrue(first.toString().contains("page=1"));
    assertEquals(1, first.page());
    assertEquals(10, first.perPage());
  }

  @Test
  void testValidationMessagesContainInvalidValues() {
    IllegalArgumentException pageException = assertThrows(IllegalArgumentException.class,
        () -> PagingRequest.forRecords(0, 10));
    assertTrue(pageException.getMessage().contains("0"));

    IllegalArgumentException perPageException = assertThrows(IllegalArgumentException.class,
        () -> PagingRequest.forRecords(1, 0));
    assertTrue(perPageException.getMessage().contains("0"));
  }

  @Test
  void testAddQueryStringRejectsNullEndpoint() {
    assertThrows(NullPointerException.class, () -> PagingRequest.defaultPaging().addQueryString(null));
  }
}
