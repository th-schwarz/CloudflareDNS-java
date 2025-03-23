package codes.thischwa.cf.model;

/** Represents the API response of the Cloudflare API containing multiple DNS record entities. */
public class RecordMultipleResponse extends AbstractMultipleResponse<RecordEntity> {

  /**
   * Constructs an instance of RecordMultipleResponse.
   *
   * <p>This class represents a response containing multiple DNS record entities from the Cloudflare
   * API. It inherits functionality from AbstractMultipleResponse to handle multiple records of type
   * RecordEntity.
   */
  public RecordMultipleResponse() {
    super();
  }
}
