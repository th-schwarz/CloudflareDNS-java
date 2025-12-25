package codes.thischwa.cf.model;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents a batch entry containing different types of operations on record entities.
 *
 * <p>A BatchEntry groups together collections of operations (patches, posts, puts, and deletes)
 * intended to be performed as part of a single batch process. Each operation corresponds to a specific
 * type of action on DNS record entities.
 *
 * <ul>
 *   <li><b>patches</b>: A list of {@link RecordEntity} objects representing partial updates to existing records.
 *   <li><b>posts</b>: A list of {@link RecordEntity} objects to be created as new DNS records.
 *   <li><b>puts</b>: A list of {@link RecordEntity} objects representing updates or replacements for existing records.
 *   <li><b>deletes</b>: A list of {@link RecordEntity} objects with name, type, and content to be removed.
 * </ul>
 *
 * <p>This class is used as both a request body for batch operations and to represent the batch response.
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BatchEntry extends AbstractEntity {

  List<RecordEntity> patches;

  List<RecordEntity> posts;

  List<RecordEntity> puts;

  List<RecordEntity> deletes;

  @Override
  public String getId() {
    return "";
  }
}
