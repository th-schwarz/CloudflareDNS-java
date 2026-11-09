package codes.thischwa.cf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Method;
import java.time.Duration;
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
  void testEmptyResultThrowsExceptionToggle() {
    // Test toggling the flag multiple times (last one wins)
    CfDnsClient client = new CfDnsClientBuilder()
        .withApiTokenAuth("test-token")
        .withEmptyResultThrowsException(true)
        .withEmptyResultThrowsException(false)  // This should override
        .build();

    assertNotNull(client);
  }

  @Test
  void testBaseUrlMustUseHttps() {
    CfDnsClientBuilder builder = new CfDnsClientBuilder().withApiTokenAuth("test-token");
    assertThrows(IllegalArgumentException.class, () -> builder.withBaseUrl("http://api.example.com"));
    assertThrows(IllegalArgumentException.class, () -> builder.withBaseUrl("ftp://api.example.com"));
    assertThrows(IllegalArgumentException.class, () -> builder.withBaseUrl("api.example.com"));
    assertThrows(IllegalArgumentException.class, () -> builder.withBaseUrl("https://"));
    assertThrows(IllegalArgumentException.class, () -> builder.withBaseUrl("https://api example.com"));
    assertThrows(IllegalArgumentException.class, () -> builder.withBaseUrl("https://api.example.com/v4?x=1"));
    assertThrows(IllegalArgumentException.class, () -> builder.withBaseUrl("https://api.example.com/v4#x"));
  }

  @Test
  void testBaseUrlAllowsOnlyCloudflareHttps() {
    assertNotNull(new CfDnsClientBuilder().withApiTokenAuth("t")
        .withBaseUrl("HTTPS://API.CLOUDFLARE.COM/client/v4/").build());
    assertNotNull(new CfDnsClientBuilder().withApiTokenAuth("t").withBaseUrl(null).build());
  }

  @Test
  void testBaseUrlTrailingSlashIsRemoved() throws ReflectiveOperationException {
    Method validateBaseUrl = CfDnsClientBuilder.class.getDeclaredMethod("validateBaseUrl", String.class);
    validateBaseUrl.setAccessible(true);

    assertEquals("HTTPS://API.CLOUDFLARE.COM/client/v4",
        validateBaseUrl.invoke(null, "HTTPS://API.CLOUDFLARE.COM/client/v4/"));
  }

  @Test
  void testInvalidTimeoutsAreRejected() {
    CfDnsClientBuilder builder = new CfDnsClientBuilder();
    assertThrows(IllegalArgumentException.class, () -> builder.withConnectTimeout(null));
    assertThrows(IllegalArgumentException.class, () -> builder.withConnectTimeout(Duration.ZERO));
    assertThrows(IllegalArgumentException.class, () -> builder.withResponseTimeout(Duration.ofSeconds(-1)));
    // would become 0 ms = no timeout at all
    assertThrows(IllegalArgumentException.class, () -> builder.withResponseTimeout(Duration.ofNanos(500_000)));
    assertThrows(IllegalArgumentException.class, () -> builder.withConnectTimeout(Duration.ofNanos(1)));
    // would overflow when converted to milliseconds
    assertThrows(IllegalArgumentException.class,
        () -> builder.withResponseTimeout(Duration.ofSeconds(Long.MAX_VALUE)));
    assertNotNull(builder.withApiTokenAuth("t").withResponseTimeout(Duration.ofMillis(1)).build());
    assertNotNull(builder.withApiTokenAuth("t")
        .withConnectTimeout(Duration.ofSeconds(1))
        .withResponseTimeout(Duration.ofSeconds(2))
        .build());
  }

  @Test
  void testNullCredentialsAreRejected() {
    assertThrows(IllegalArgumentException.class, () -> new CfDnsClientBuilder().withApiTokenAuth(null));
    assertThrows(IllegalArgumentException.class, () -> new CfDnsClientBuilder().withEmailKeyAuth(null, "key"));
    assertThrows(IllegalArgumentException.class, () -> new CfDnsClientBuilder().withEmailKeyAuth("a@b.de", null));
  }

  @Test
  void testBuildWithoutAuthFails() {
    assertThrows(IllegalStateException.class, () -> new CfDnsClientBuilder().build());
    assertThrows(IllegalStateException.class,
        () -> new CfDnsClientBuilder().withBaseUrl("https://api.example.com").build());
  }
}
