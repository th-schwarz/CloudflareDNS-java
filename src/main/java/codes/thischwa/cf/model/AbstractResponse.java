package codes.thischwa.cf.model;

import java.util.List;
import lombok.Data;

/**
 * Abstract base class for API response models.
 *
 * <p>This class encapsulates common attributes used to represent the result of an API request. It
 * can be extended to define more specific response structures.
 *
 * <p>Attributes:
 *
 * <ol>
 *   <li><b>success</b>: Indicates whether the API request was successful.
 *   <li><b>errors</b>: A list of error messages, if any, returned by the API.
 *   <li><b>messages</b>: A list of informational or status messages accompanying the response.
 * </ol>
 *
 * <p>This structure is designed for consistency and ease of extending response models in
 * applications that require uniform response structures.
 */
@Data
public abstract class AbstractResponse {
  private boolean success;
  private List<String> errors;
  private List<String> messages;
}
