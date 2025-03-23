package codes.thischwa.cf.model;

import lombok.Data;

/**
 * Represents a base abstract entity class for modeling domain objects with a unique identifier.
 *
 * <p>This class provides a fundamental contract for entities by implementing the {@link
 * ResponseEntity} interface. The primary attribute of this class is the `id` field, which serves as
 * a unique identifier for all derived entities.
 *
 * <p>Commonly extended by other entity classes to maintain a consistent entity structure across the
 * domain models. This encourages code reusability and consistency within the system.
 */
@Data
public class AbstractEntity implements ResponseEntity {
  private String id;
}
