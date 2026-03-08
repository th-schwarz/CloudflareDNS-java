package codes.thischwa.cf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for CfDnsClientBuilder and its authentication classes.
 */
class CfDnsClientBuilderTest {

  @Test
  void testBuildWithApiToken() {
    CfDnsClient client = new CfDnsClientBuilder()
        .withApiTokenAuth("test-token")
        .build();

    assertNotNull(client);
  }

  @Test
  void testBuildWithEmailKey() {
    CfDnsClient client = new CfDnsClientBuilder()
        .withEmailKeyAuth("test@example.com", "test-key")
        .build();

    assertNotNull(client);
  }

  @Test
  void testBuildWithCustomBaseUrl() {
    CfDnsClient client = new CfDnsClientBuilder()
        .withApiTokenAuth("test-token")
        .withBaseUrl("https://custom-api.example.com")
        .build();

    assertNotNull(client);
  }

  @Test
  void testBuildWithDefaultBaseUrl() {
    CfDnsClient client = new CfDnsClientBuilder()
        .withApiTokenAuth("test-token")
        .build();

    assertNotNull(client);
  }

  @Test
  void testBuildWithEmptyResultThrowsException() {
    CfDnsClient client = new CfDnsClientBuilder()
        .withApiTokenAuth("test-token")
        .withEmptyResultThrowsException(true)
        .build();

    assertNotNull(client);
  }

  @Test
  void testBuildWithEmptyResultDoesNotThrowException() {
    CfDnsClient client = new CfDnsClientBuilder()
        .withApiTokenAuth("test-token")
        .withEmptyResultThrowsException(false)
        .build();

    assertNotNull(client);
  }

  @Test
  void testBuilderMethodChaining() {
    CfDnsClient client = new CfDnsClientBuilder()
        .withApiTokenAuth("test-token")
        .withBaseUrl("https://custom-api.example.com")
        .withEmptyResultThrowsException(true)
        .build();

    assertNotNull(client);
  }

  @Test
  void testApiTokenAuth_BlankToken() {
    assertThrows(IllegalArgumentException.class, () -> new CfDnsClientBuilder.ApiTokenAuth("   "));
  }

  @Test
  void testEmailKeyAuth_ValidCredentials() {
    CfDnsClientBuilder.EmailKeyAuth auth = new CfDnsClientBuilder.EmailKeyAuth(
        "test@example.com",
        "valid-key"
    );
    assertNotNull(auth);
  }

  @Test
  void testEmailKeyAuth_BlankEmail() {
    assertThrows(IllegalArgumentException.class, () -> new CfDnsClientBuilder.EmailKeyAuth("   ", "valid-key"));
  }

  @Test
  void testEmailKeyAuth_BlankKey() {
    assertThrows(IllegalArgumentException.class, () -> new CfDnsClientBuilder.EmailKeyAuth("test@example.com", "   "));
  }

  @Test
  void testEmailKeyAuth_BothBlank() {
    assertThrows(IllegalArgumentException.class, () -> new CfDnsClientBuilder.EmailKeyAuth("   ", "   "));
  }

  @Test
  void testDefaultBaseUrl() {
    assertEquals("https://api.cloudflare.com/client/v4", CfDnsClientBuilder.DEFAULT_BASEURL);
  }

  @Test
  void testBuilderWithMultipleConfigurations() {
    // Test switching auth methods in the same builder (last one wins)
    CfDnsClient client = new CfDnsClientBuilder()
        .withEmailKeyAuth("test@example.com", "old-key")
        .withApiTokenAuth("new-token")  // This should override the email/key auth
        .build();

    assertNotNull(client);
  }

  @Test
  void testBuilderWithMultipleBaseUrls() {
    // Test setting base URL multiple times (last one wins)
    CfDnsClient client = new CfDnsClientBuilder()
        .withApiTokenAuth("test-token")
        .withBaseUrl("https://old-api.example.com")
        .withBaseUrl("https://new-api.example.com")  // This should override
        .build();

    assertNotNull(client);
  }

  @Test
  void testEmptyResultThrowsExceptionToggle() {
    // Test toggling the flag multiple times (last one wins)
    CfDnsClient client = new CfDnsClientBuilder()
        .withApiTokenAuth("test-token")
        .withEmptyResultThrowsException(true)
        .withEmptyResultThrowsException(false)  // This should override
        .build();

    assertNotNull(client);
  }
}
