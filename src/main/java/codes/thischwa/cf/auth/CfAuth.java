package codes.thischwa.cf.auth;

import org.apache.hc.core5.http.ClassicHttpRequest;

/**
 * Interface for Cloudflare authentication mechanisms.
 * Implementations of this interface provide different methods of authentication
 * with the Cloudflare API (e.g., API token, email/key combination).
 */
public interface CfAuth {

  /**
   * Applies authentication headers to the given HTTP request.
   *
   * @param request the HTTP request to authenticate
   */
  void applyAuth(ClassicHttpRequest request);

}
