package codes.thischwa.cf;

import codes.thischwa.cf.model.ResponseResultInfo;
import java.io.Serial;
import java.util.List;

/**
 * This exception is thrown to indicate that a requested resource was not found during interaction
 * with the Cloudflare API.
 *
 * <p>It is thrown if Cloudflare responds with HTTP status 404, or if a result is empty and
 * 'emptyResultThrowsException' is enabled.
 */
public class CloudflareNotFoundException extends CloudflareApiException {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Constructs a new CloudflareNotFoundException with the specified detail message.
   *
   * @param message the detail message, which provides additional context about the "not found"
   *                error encountered during interaction with the Cloudflare API.
   */
  public CloudflareNotFoundException(String message) {
    super(message);
  }

  /**
   * Constructs a new CloudflareNotFoundException caused by an API response.
   *
   * @param message    the detail message
   * @param statusCode the HTTP status code of the response
   * @param errors     the errors reported by Cloudflare, may be {@code null}
   */
  public CloudflareNotFoundException(String message, int statusCode,
                                     List<ResponseResultInfo.Error> errors) {
    super(message, statusCode, errors);
  }
}
