package codes.thischwa.cf.auth;

import org.apache.hc.core5.http.ClassicHttpRequest;
import org.jetbrains.annotations.NotNull;

/**
 * Authentication mechanism using Cloudflare API token.
 * This is the recommended authentication method for the Cloudflare API.
 */
public class ApiTokenAuth implements CfAuth {

  private final String apiToken;

  /**
   * Creates a new API token authentication object.
   *
   * @param apiToken the Cloudflare API token
   * @throws IllegalArgumentException if the API token is null or blank
   */
  public ApiTokenAuth(@NotNull String apiToken) {
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
