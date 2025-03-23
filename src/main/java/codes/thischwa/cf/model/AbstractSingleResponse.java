package codes.thischwa.cf.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents a base abstract response model for handling single response entities within an API
 * response.
 *
 * <p>This class extends {@code AbstractResponse}, inheriting common response attributes such as
 * success status, error messages, and informational messages. It introduces a single generic type
 * parameter {@code <T>} which extends {@code ResponseEntity}, enforcing type consistency for the
 * result attribute.
 *
 * <p>Primary Attribute: {@code result}: Represents the single entity result of the response. This
 * is a generic type that ensures flexibility and adaptability for different entity models.
 *
 * @param <T> The type of the response entity that extends {@code ResponseEntity}.
 */
@EqualsAndHashCode(callSuper = true)
@Data
public abstract class AbstractSingleResponse<T extends ResponseEntity> extends AbstractResponse {
  private T result;
}
