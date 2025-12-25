package codes.thischwa.cf.model;

import java.util.List;
import lombok.Data;

/**
 * Represents the result of a response with metadata about its success and associated messages or
 * errors.
 *
 * <p>This class provides a structure to capture the outcome of an operation, including:
 * <ul>
 * <li>Whether the operation was successful.
 * <li>A list of error messages if the operation failed.
 * <li>A list of informational or success messages.
 * </ul>
 * It can be used to standardize the response format in an application.
 */
@Data
public class ResponseResultInfo {
  private boolean success;
  private List<Error> errors;
  private List<String> messages;

  /**
   * Represents an error with a specific code and message.
   *
   * <p>This class is used to encapsulate error information, including a numerical error code
   * and a corresponding descriptive message. It is often used as part of a collection of errors
   * to provide detailed diagnostics for failed operations or processes.
   */
  @Data
  public static class Error {
    private int code;
    private String message;

    /**
     * Constructs a new instance of the {@code Error} class with default values for its properties.
     *
     * <p>This no-argument constructor initializes an {@code Error} object without setting
     * specific values for the error code or message. It is primarily used when an error needs to
     * be created and set up later, or when default values are acceptable.
     */
    public Error() {
    }

    /**
     * Constructs an instance of the {@code Error} class with a specified error code and message.
     *
     * @param code    the numerical code representing the error
     * @param message the descriptive message providing details about the error
     */
    public Error(int code, String message) {
      this.code = code;
      this.message = message;
    }

    @Override
    public String toString() {
      return String.format("%d: %s", code, message);
    }
  }
}
