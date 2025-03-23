package codes.thischwa.cf;

/**
 * This exception is thrown to indicate that a requested resource was not found during interaction
 * with the Cloudflare API.
 *
 * <p>It extends {@link CloudflareApiException} to provide specific errors related to situations
 * where Cloudflare responds with a "not found" operation.
 */
public class CloudflareNotFoundException extends CloudflareApiException {

  /**
   * Constructs a new CloudflareNotFoundException with the specified detail message.
   *
   * @param message the detail message, which provides additional context about the "not found" error
   *                encountered during interaction with the Cloudflare API.
   */
  public CloudflareNotFoundException(String message) {
    super(message);
  }

  /**
   * Constructs a new CloudflareNotFoundException with the specified detail message and cause.
   *
   * @param message the detail message, which provides additional context about the "not found" error
   *                encountered during interaction with the Cloudflare API.
   * @param cause the cause of this exception, which is the underlying throwable that triggered this exception.
   */
  public CloudflareNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a new CloudflareNotFoundException with the specified cause.
   *
   * @param cause the cause of this exception, which is the underlying throwable
   *              that triggered this exception.
   */
  public CloudflareNotFoundException(Throwable cause) {
    super(cause);
  }
}
