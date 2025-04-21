package codes.thischwa.cf.model;

import java.util.List;
import lombok.Data;

/**
 * Represents the result of a response with metadata about its success and associated messages or
 * errors.
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
  private List<String> errors;
  private List<String> messages;
}
