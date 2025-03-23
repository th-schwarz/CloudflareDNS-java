package codes.thischwa.cf.model;

/** Represents the API response of the Cloudflare API containing a single DNS record entity. */
public class RecordSingleResponse extends AbstractSingleResponse<RecordEntity> {

  /**
   * Constructs a new instance of the RecordSingleResponse class.
   *
   * <p>This constructor initializes the RecordSingleResponse object by invoking the superclass
   * constructor. The RecordSingleResponse represents a specific API response structure that
   * encapsulates a single DNS record entity, providing mechanisms to interact with such data in the
   * context of the Cloudflare API.
   */
  public RecordSingleResponse() {
    super();
  }
}
