package codes.thischwa.cf;

import codes.thischwa.cf.model.ResponseResultInfo;
import java.io.Serial;
import java.util.List;

/**
 * Represents a custom exception for errors encountered while interacting with the Cloudflare API.
 *
 * <p>If the error was caused by an API response, {@link #getStatusCode()} returns the HTTP status
 * code and {@link #getErrors()} the errors reported by Cloudflare.
 */
public class CloudflareApiException extends Exception {

  /**
   * Value of {@link #getStatusCode()} if the exception was not caused by an HTTP response.
   */
  public static final int NO_STATUS_CODE = -1;

  @Serial
  private static final long serialVersionUID = 2L;

  /**
   * The HTTP status code of the response, or {@link #NO_STATUS_CODE}.
   */
  private final int statusCode;

  /**
   * The errors reported by Cloudflare. Transient, because the error class isn't serializable.
   */
  private final transient List<ResponseResultInfo.Error> errors;

  /**
   * Constructs a new CloudflareApiException with the specified detail message.
   *
   * @param message the detail message, which provides more information about the exception.
   */
  public CloudflareApiException(String message) {
    this(message, NO_STATUS_CODE, null);
  }

  /**
   * Constructs a new CloudflareApiException with the specified detail message and cause.
   *
   * @param message the detail message, which provides additional context or information about the
   *                exception.
   * @param cause   the cause of this exception, which is the underlying throwable that triggered this
   *                exception.
   */
  public CloudflareApiException(String message, Throwable cause) {
    super(message, cause);
    this.statusCode = NO_STATUS_CODE;
    this.errors = List.of();
  }

  /**
   * Constructs a new CloudflareApiException caused by an API response.
   *
   * @param message    the detail message
   * @param statusCode the HTTP status code of the response, or {@link #NO_STATUS_CODE}
   * @param errors     the errors reported by Cloudflare, may be {@code null}
   */
  public CloudflareApiException(String message, int statusCode,
                                List<ResponseResultInfo.Error> errors) {
    super(message);
    this.statusCode = statusCode;
    this.errors = errors == null ? List.of() : List.copyOf(errors);
  }

  /**
   * Returns the HTTP status code of the response that caused this exception.
   *
   * @return the HTTP status code, or {@link #NO_STATUS_CODE} if the exception wasn't caused by an
   *     HTTP response
   */
  public int getStatusCode() {
    return statusCode;
  }

  /**
   * Returns the errors reported by Cloudflare.
   *
   * @return an unmodifiable list of errors, never {@code null}
   */
  public List<ResponseResultInfo.Error> getErrors() {
    return errors == null ? List.of() : errors;
  }
}
