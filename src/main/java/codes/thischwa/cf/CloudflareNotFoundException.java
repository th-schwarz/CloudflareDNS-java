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
   * @param message the detail message, which provides additional context about the "not found"
   *                error encountered during interaction with the Cloudflare API.
   */
  public CloudflareNotFoundException(String message) {
    super(message);
  }
}
