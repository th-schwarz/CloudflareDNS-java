package codes.thischwa.cf;

import org.apache.hc.core5.http.ClassicHttpRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Builder class for configuring and creating instances of {@link CfDnsClient}.
 * This class provides a fluent API for customizing the client settings,
 * such as the base URL and authentication mechanism.
 */
public class CfDnsClientBuilder {

  public static final String DEFAULT_BASEURL = "https://api.cloudflare.com/client/v4";
  private boolean emptyResultThrowsException;
  private CfAuth auth;

  @Nullable
  private String baseUrl;

  /**
   * Constructs a new instance of `CfDnsClientBuilder`.
   *
   * <p>This class serves as a builder for creating and configuring instances of a CfDnsClient. It provides
   * a fluent API to set various optional configurations, such as API authentication methods and base
   * URL, before constructing the client.
   *
   * <p>By using this constructor, you can initiate the building process with default settings, which can
   * later be overridden using the provided builder methods.
   */
  public CfDnsClientBuilder() {
  }

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
   * @param baseUrl the base URL to be used for API requests
   * @return the current instance of {@code CfDnsClientBuilder}, enabling method chaining
   */
  public CfDnsClientBuilder withBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
    return this;
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
   */
  public CfDnsClient build() {
    String url = baseUrl == null ? DEFAULT_BASEURL : baseUrl;
    return new CfDnsClient(emptyResultThrowsException, url, auth);
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
    ApiTokenAuth(@NotNull String apiToken) {
      if (apiToken.isBlank()) {
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
    EmailKeyAuth(@NotNull String authEmail, @NotNull String authKey) {
      if (authEmail.isBlank()) {
        throw new IllegalArgumentException("Authentication email must not be null or blank!");
      }
      if (authKey.isBlank()) {
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
