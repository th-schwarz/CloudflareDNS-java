package codes.thischwa.cf.model;

/**
 * Represents the API response of the Cloudflare API containing multiple {@link RecordEntity}
 * instances.
 */
public class RecordMultipleResponse extends AbstractMultipleResponse<RecordEntity> {

  /**
   * Constructs an instance of RecordMultipleResponse.
   *
   * <p>This class represents a response containing multiple DNS getRecord entities from the
   * Cloudflare API. It inherits functionality from AbstractMultipleResponse to handle multiple
   * records of type RecordEntity.
   */
  public RecordMultipleResponse() {
    super();
  }
}
