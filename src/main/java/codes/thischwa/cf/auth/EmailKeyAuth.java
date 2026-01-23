package codes.thischwa.cf.auth;

import org.apache.hc.core5.http.ClassicHttpRequest;
import org.jetbrains.annotations.NotNull;

/**
 * Authentication mechanism using Cloudflare account email and API key.
 * This is the legacy authentication method for the Cloudflare API.
 */
public class EmailKeyAuth implements CfAuth {

  private final String authEmail;
  private final String authKey;

  /**
   * Creates a new email/key authentication object.
   *
   * @param authEmail the email address associated with the Cloudflare account
   * @param authKey   the API key of the Cloudflare account
   * @throws IllegalArgumentException if email or key is null or blank
   */
  public EmailKeyAuth(@NotNull String authEmail, @NotNull String authKey) {
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
