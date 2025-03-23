package codes.thischwa.cf.model;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Abstract base class for response models that contain multiple result entries.
 *
 * <p>This class is designed to handle API responses where multiple entities are part of the result
 * set, such as paginated or batched data. It extends {@link AbstractResponse} to include additional
 * attributes specific to multi-entity responses.
 *
 * <p>Attributes:
 *
 * <ul>
 *   <li>`resultInfo`: Provides metadata about the result set, such as pagination details like page
 *       number, total count, number of results per page, etc.
 *   <li>`result`: A list of entities representing the main body of the response. The type of
 *       entities in the result list is determined by the generic parameter {@code T}, which must
 *       extend {@link ResponseEntity}.
 * </ul>
 *
 * <p>Subclasses can be created by specifying the entity type that the response should handle.
 */
@EqualsAndHashCode(callSuper = true)
@Data
public abstract class AbstractMultipleResponse<T extends ResponseEntity> extends AbstractResponse {
  private ResultInfo resultInfo;
  private List<T> result;
}
