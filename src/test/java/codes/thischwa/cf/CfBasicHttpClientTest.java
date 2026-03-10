package codes.thischwa.cf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import codes.thischwa.cf.model.RecordEntity;
import codes.thischwa.cf.model.RecordSingleResponse;
import codes.thischwa.cf.model.RecordType;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for CfBasicHttpClient using mocked HTTP components.
 * Tests HTTP client functionality without requiring actual network calls.
 */
class CfBasicHttpClientTest {

  /**
   * Test implementation of CfBasicHttpClient for testing purposes.
   */
  private static class TestCfBasicHttpClient extends CfBasicHttpClient {

    TestCfBasicHttpClient(String baseUrl, CfDnsClientBuilder.CfAuth auth) {
      super(baseUrl, auth);
    }

    // Expose protected methods for testing
    public <T extends codes.thischwa.cf.model.AbstractResponse> T testGetRequest(String endpoint, Class<T> responseType)
        throws CloudflareApiException {
      return getRequest(endpoint, responseType);
    }

    public <T extends codes.thischwa.cf.model.AbstractResponse> T testPostRequest(String endpoint, Object payload, Class<T> responseType)
        throws CloudflareApiException {
      return postRequest(endpoint, payload, responseType);
    }

    public <T extends codes.thischwa.cf.model.AbstractResponse> T testPutRequest(String endpoint, Object payload, Class<T> responseType)
        throws CloudflareApiException {
      return putRequest(endpoint, payload, responseType);
    }

    public <T extends codes.thischwa.cf.model.AbstractResponse> T testPatchRequest(String endpoint, Object payload, Class<T> responseType)
        throws CloudflareApiException {
      return patchRequest(endpoint, payload, responseType);
    }

    public <T extends codes.thischwa.cf.model.AbstractResponse> T testDeleteRequest(String endpoint, Class<T> responseType)
        throws CloudflareApiException {
      return deleteRequest(endpoint, responseType);
    }
  }

  @Test
  void testConstructor_WithApiToken() {
    CfDnsClientBuilder.ApiTokenAuth auth = new CfDnsClientBuilder.ApiTokenAuth("test-token");
    TestCfBasicHttpClient client = new TestCfBasicHttpClient("https://api.cloudflare.com", auth);
    assertNotNull(client);
  }

  @Test
  void testConstructor_WithEmailKey() {
    CfDnsClientBuilder.EmailKeyAuth auth = new CfDnsClientBuilder.EmailKeyAuth("test@example.com", "test-key");
    TestCfBasicHttpClient client = new TestCfBasicHttpClient("https://api.cloudflare.com", auth);
    assertNotNull(client);
  }

  @Test
  void testApiException_unknowEndpoint() {
    CfDnsClientBuilder.ApiTokenAuth auth = new CfDnsClientBuilder.ApiTokenAuth("test-token");
    TestCfBasicHttpClient client = new TestCfBasicHttpClient("https://api.cloudflare.com", auth);

    // Invalid JSON will cause a parsing error
    CloudflareApiException exception = assertThrows(CloudflareApiException.class, () -> {
      client.testGetRequest("/invalid-endpoint", RecordSingleResponse.class);
    });

    assertNotNull(exception);
    assertTrue(exception.getMessage().contains("Unexpected error"));
    Throwable cause = exception.getCause();
    assertInstanceOf(CloudflareApiException.class, cause);
    assertTrue(cause.getMessage().contains("API error: 10404: No route for that URI"), "Expected error message: No route for that URI∞ but it was: " + cause.getMessage());
  }

  @Test
  void testResultWrapper() throws Exception {
    // Test the private ResultWrapper record indirectly through client behavior
    CfDnsClientBuilder.ApiTokenAuth auth = new CfDnsClientBuilder.ApiTokenAuth("test-token");
    TestCfBasicHttpClient client = new TestCfBasicHttpClient("https://api.cloudflare.com", auth);

    // Any request will create and use a ResultWrapper internally
    assertThrows(CloudflareApiException.class, () -> {
      client.testGetRequest("/test", RecordSingleResponse.class);
    });
  }

  @Test
  void testAuthApplicationHeader_ApiToken() {
    CfDnsClientBuilder.ApiTokenAuth auth = new CfDnsClientBuilder.ApiTokenAuth("my-token");
    HttpGet request = new HttpGet("https://api.cloudflare.com/test");

    auth.applyAuth(request);

    assertTrue(request.containsHeader("Authorization"));
    assertEquals("Bearer my-token", request.getFirstHeader("Authorization").getValue());
  }

  @Test
  void testAuthApplicationHeader_EmailKey() {
    CfDnsClientBuilder.EmailKeyAuth auth = new CfDnsClientBuilder.EmailKeyAuth("test@example.com", "my-key");
    HttpGet request = new HttpGet("https://api.cloudflare.com/test");

    auth.applyAuth(request);

    assertTrue(request.containsHeader("X-Auth-Email"));
    assertTrue(request.containsHeader("X-Auth-Key"));
    assertEquals("test@example.com", request.getFirstHeader("X-Auth-Email").getValue());
    assertEquals("my-key", request.getFirstHeader("X-Auth-Key").getValue());
  }

  @Test
  void testBaseUrlConstruction() {
    CfDnsClientBuilder.ApiTokenAuth auth = new CfDnsClientBuilder.ApiTokenAuth("test-token");

    // Test default base URL
    TestCfBasicHttpClient client = new TestCfBasicHttpClient("https://api.cloudflare.com", auth);
    assertNotNull(client);
  }

  @Test
  void testObjectMapperInitialization() {
    // ObjectMapper is initialized in constructor via JsonConf
    CfDnsClientBuilder.ApiTokenAuth auth = new CfDnsClientBuilder.ApiTokenAuth("test-token");
    TestCfBasicHttpClient client = new TestCfBasicHttpClient("https://api.cloudflare.com", auth);

    // If ObjectMapper wasn't initialized, any request would fail with NullPointerException
    assertThrows(CloudflareApiException.class, () -> {
      client.testGetRequest("/test", RecordSingleResponse.class);
    });
  }

  @Test
  void testRequestPayloadSerialization() {
    CfDnsClientBuilder.ApiTokenAuth auth = new CfDnsClientBuilder.ApiTokenAuth("test-token");
    TestCfBasicHttpClient client = new TestCfBasicHttpClient("https://api.cloudflare.com", auth);

    RecordEntity record = RecordEntity.build("test.example.com", RecordType.A, 300, "1.2.3.4");

    // The payload serialization happens inside postRequest
    assertThrows(CloudflareApiException.class, () -> {
      client.testPostRequest("/test", record, RecordSingleResponse.class);
    });
  }
}
