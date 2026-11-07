package codes.thischwa.cf;

import java.net.URI;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.jetbrains.annotations.Nullable;

/**
 * Builder class for configuring and creating instances of {@link CfDnsClient}.
 * This class provides a fluent API for customizing the client settings,
 * such as the base URL and authentication mechanism.
 */
public class CfDnsClientBuilder {

  /**
   * The default base URL for the Cloudflare v4 API requests made by the {@code CfDnsClient}.
   */
  public static final String DEFAULT_BASEURL = "https://api.cloudflare.com/client/v4";

  /**
   * The default timeout for establishing a connection.
   */
  public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);

  /**
   * The default timeout for waiting for the response data.
   */
  public static final Duration DEFAULT_RESPONSE_TIMEOUT = Duration.ofSeconds(30);

  /**
   * Hosts for which plain HTTP is allowed (e.g. for local tests).
   */
  private static final Set<String> LOOPBACK_HOSTS = Set.of("localhost", "127.0.0.1", "[::1]", "::1");

  private boolean emptyResultThrowsException;
  private CfAuth auth;
  private Duration connectTimeout = DEFAULT_CONNECT_TIMEOUT;
  private Duration responseTimeout = DEFAULT_RESPONSE_TIMEOUT;

  @Nullable
  private String baseUrl;

  /**
   * Configures whether an exception should be thrown when an empty result is encountered
   * during operations performed by the `CfDnsClient`.
   *
   * @param emptyResultThrowsException a boolean flag indicating if an exception should be thrown
   *                                   when an empty result is returned. If set to `true`, operations
   *                                   that result in an empty response will throw an exception;
   *                                   otherwise, they will not.
   * @return the current instance of {@code CfDnsClientBuilder}, allowing for method chaining
   *         to further configure the builder.
   */
  public CfDnsClientBuilder withEmptyResultThrowsException(boolean emptyResultThrowsException) {
    this.emptyResultThrowsException = emptyResultThrowsException;
    return this;
  }

  /**
   * Sets the base URL to be used by the {@code CfDnsClient}.
   * This method allows configuring the base URL for API requests, overriding any default value.
   *
   * <p>To protect the credentials, the URL must use HTTPS. Plain HTTP is only allowed for
   * loopback hosts (e.g. {@code localhost}) for testing purposes.
   *
   * @param baseUrl the base URL to be used for API requests, {@code null} resets it to
   *                {@link #DEFAULT_BASEURL}
   * @return the current instance of {@code CfDnsClientBuilder}, enabling method chaining
   * @throws IllegalArgumentException if the URL is invalid or doesn't use HTTPS
   */
  public CfDnsClientBuilder withBaseUrl(@Nullable String baseUrl) {
    this.baseUrl = baseUrl == null ? null : validateBaseUrl(baseUrl);
    return this;
  }

  /**
   * Sets the timeout for establishing a connection. Default is {@link #DEFAULT_CONNECT_TIMEOUT}.
   *
   * @param connectTimeout the connect timeout, must be positive
   * @return the current instance of {@code CfDnsClientBuilder}, enabling method chaining
   * @throws IllegalArgumentException if the timeout is {@code null}, zero or negative
   */
  public CfDnsClientBuilder withConnectTimeout(Duration connectTimeout) {
    this.connectTimeout = validateTimeout(connectTimeout, "Connect timeout");
    return this;
  }

  /**
   * Sets the timeout for waiting for the response data. Default is
   * {@link #DEFAULT_RESPONSE_TIMEOUT}.
   *
   * @param responseTimeout the response timeout, must be positive
   * @return the current instance of {@code CfDnsClientBuilder}, enabling method chaining
   * @throws IllegalArgumentException if the timeout is {@code null}, zero or negative
   */
  public CfDnsClientBuilder withResponseTimeout(Duration responseTimeout) {
    this.responseTimeout = validateTimeout(responseTimeout, "Response timeout");
    return this;
  }

  private static Duration validateTimeout(Duration timeout, String name) {
    if (timeout == null || timeout.isZero() || timeout.isNegative()) {
      throw new IllegalArgumentException(name + " must be positive: " + timeout);
    }
    long millis;
    try {
      millis = timeout.toMillis();
    } catch (ArithmeticException e) {
      throw new IllegalArgumentException(name + " is too large: " + timeout, e);
    }
    // the HTTP client works with milliseconds, 0 ms would disable the timeout
    if (millis < 1) {
      throw new IllegalArgumentException(name + " must be at least 1 ms: " + timeout);
    }
    return timeout;
  }

  private static String validateBaseUrl(String baseUrl) {
    URI uri;
    try {
      uri = URI.create(baseUrl.strip());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid base URL: " + baseUrl, e);
    }
    String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
    String host = uri.getHost();
    if (host == null || host.isBlank()) {
      throw new IllegalArgumentException("Base URL must contain a host: " + baseUrl);
    }
    if (uri.getRawQuery() != null || uri.getRawFragment() != null) {
      throw new IllegalArgumentException("Base URL must not contain a query or fragment: " + baseUrl);
    }
    boolean https = "https".equals(scheme);
    boolean loopbackHttp = "http".equals(scheme) && LOOPBACK_HOSTS.contains(host.toLowerCase(Locale.ROOT));
    if (!https && !loopbackHttp) {
      throw new IllegalArgumentException("Base URL must use HTTPS: " + baseUrl);
    }
    String normalized = uri.toString();
    // avoid double slashes when appending the endpoint paths
    while (normalized.endsWith("/")) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }
    return normalized;
  }

  /**
   * Configures the authentication method for the {@code CfDnsClient} to use an API token.
   * This is the recommended way to authenticate with the Cloudflare API, as it provides
   * enhanced security and ease of use compared to other authentication mechanisms.
   *
   * @param apiToken the Cloudflare API token. This token is required for authenticating
   *                 API requests and must not be null or blank.
   * @return the current instance of {@code CfDnsClientBuilder}, allowing for method chaining
   *         to further configure the builder.
   * @throws IllegalArgumentException if the {@code apiToken} is null or blank.
   */
  public CfDnsClientBuilder withApiTokenAuth(String apiToken) {
    this.auth = new ApiTokenAuth(apiToken);
    return this;
  }

  /**
   * Configures the authentication method for the {@code CfDnsClient} to use an email and API key.
   * This approach uses the legacy authentication mechanism provided by Cloudflare, where requests
   * are authenticated with a combination of an account's email address and API key.
   *
   * @param authEmail the email address associated with the Cloudflare account. This must not be null or blank.
   * @param authKey   the API key of the Cloudflare account. This must not be null or blank.
   * @return the current instance of {@code CfDnsClientBuilder}, allowing for method chaining
   *         to further configure the builder.
   * @throws IllegalArgumentException if {@code authEmail} or {@code authKey} is null or blank.
   */
  public CfDnsClientBuilder withEmailKeyAuth(String authEmail, String authKey) {
    this.auth = new EmailKeyAuth(authEmail, authKey);
    return this;
  }

  /**
   * Builds and returns a configured instance of {@code CfDnsClient}.
   *
   * <p>The method constructs a new {@code CfDnsClient} object based on the
   * options set in the {@code CfDnsClientBuilder}. If no base URL has been
   * explicitly configured, a default base URL will be used.
   *
   * @return a new instance of {@code CfDnsClient} configured with the
   *         specified options such as base URL, authentication details,
   *         and the exception-handling policy for empty results.
   * @throws IllegalStateException if no authentication was configured
   */
  public CfDnsClient build() {
    if (auth == null) {
      throw new IllegalStateException(
          "No authentication configured, use withApiTokenAuth(...) or withEmailKeyAuth(...)");
    }
    String url = baseUrl == null ? DEFAULT_BASEURL : baseUrl;
    return new CfDnsClient(emptyResultThrowsException, url, auth, CfDnsClient.RECORDS_PER_PAGE,
        connectTimeout, responseTimeout);
  }

  /**
   * Interface for Cloudflare authentication mechanisms.
   * Implementations of this interface provide different methods of authentication
   * with the Cloudflare API (e.g., API token, email/key combination).
   */
  interface CfAuth {

    /**
     * Applies authentication headers to the given HTTP request.
     *
     * @param request the HTTP request to authenticate
     */
    void applyAuth(ClassicHttpRequest request);

  }

  /**
   * Authentication mechanism using Cloudflare API token.
   * This is the recommended authentication method for the Cloudflare API.
   */
  static class ApiTokenAuth implements CfAuth {

    private final String apiToken;

    /**
     * Creates a new API token authentication object.
     *
     * @param apiToken the Cloudflare API token
     * @throws IllegalArgumentException if the API token is null or blank
     */
    ApiTokenAuth(String apiToken) {
      if (apiToken == null || apiToken.isBlank()) {
        throw new IllegalArgumentException("API token must not be null or blank!");
      }
      this.apiToken = apiToken;
    }

    @Override
    public void applyAuth(ClassicHttpRequest request) {
      request.addHeader("Authorization", "Bearer " + apiToken);
    }
  }

  /**
   * Authentication mechanism using Cloudflare account email and API key.
   * This is the legacy authentication method for the Cloudflare API.
   */
  static class EmailKeyAuth implements CfAuth {

    private final String authEmail;
    private final String authKey;

    /**
     * Creates a new email/key authentication object.
     *
     * @param authEmail the email address associated with the Cloudflare account
     * @param authKey   the API key of the Cloudflare account
     * @throws IllegalArgumentException if email or key is null or blank
     */
    EmailKeyAuth(String authEmail, String authKey) {
      if (authEmail == null || authEmail.isBlank()) {
        throw new IllegalArgumentException("Authentication email must not be null or blank!");
      }
      if (authKey == null || authKey.isBlank()) {
        throw new IllegalArgumentException("Authentication key must not be null or blank!");
      }
      this.authEmail = authEmail;
      this.authKey = authKey;
    }

    @Override
    public void applyAuth(ClassicHttpRequest request) {
      request.addHeader("X-Auth-Email", authEmail);
      request.addHeader("X-Auth-Key", authKey);
    }
  }
}
