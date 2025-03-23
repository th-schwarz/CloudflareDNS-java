package codes.thischwa.cf;

import java.io.Serial;

/**
 * Represents a custom exception for errors encountered while interacting with the Cloudflare API.
 */
public class CloudflareApiException extends Exception {

  @Serial private static final long serialVersionUID = 1L;

  /**
   * Constructs a new CloudflareApiException with the specified detail message.
   *
   * @param message the detail message, which provides more information about the exception.
   */
  public CloudflareApiException(String message) {
    super(message);
  }

  /**
   * Constructs a new CloudflareApiException with the specified detail message and cause.
   *
   * @param message the detail message, which provides additional context or information about the exception.
   * @param cause the cause of this exception, which is the underlying throwable that triggered this exception.
   */
  public CloudflareApiException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a new CloudflareApiException with the specified cause.
   *
   * @param cause the cause of this exception, which is the underlying throwable
   *              that triggered this exception.
   */
  public CloudflareApiException(Throwable cause) {
    super(cause);
  }
}
