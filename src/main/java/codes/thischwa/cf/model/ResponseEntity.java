package codes.thischwa.cf.model;

/**
 * Represents a contract for entities that have a unique identifier.
 *
 * <p>This interface is primarily used as a common abstraction for domain objects that require a
 * unique identifier, enabling type consistency and code reusability.
 */
public interface ResponseEntity {

  /**
   * Retrieves the unique identifier of the entity.
   *
   * @return the unique identifier as a String
   */
  String getId();
}
