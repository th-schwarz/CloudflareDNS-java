package codes.thischwa.cf.model;

/**
 * Represents a response that contains a single {@link BatchEntry} as the result.
 *
 * <p>This class is used for API responses where the primary result is a batch entry,
 * which includes collections of operations such as patches, posts, puts, and deletes
 * performed on DNS record entities.
 *
 * <p>Extends {@code AbstractSingleResponse} with {@code BatchEntry} as the generic type,
 * ensuring that the response result is a batch of operations.
 */
public class BatchResponse extends AbstractSingleResponse<BatchEntry> {

  BatchResponse() {
    super();
  }
}
