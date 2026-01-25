package codes.thischwa.cf.model;

/**
 * Represents metadata for paginated results.
 *
 * <p>This class contains information about the current page, page size, total pages, and result
 * counts, which can be utilized in managing and navigating through paginated data.
 *
 * @param page The current page number.
 * @param perPage The number of results per page.
 * @param totalPages The total number of pages available.
 * @param count The number of results on the current page.
 * @param totalCount The total number of results across all pages.
 */
public record ResultInfo(int page, int perPage, int totalPages, int count, int totalCount) {

  /**
   * Constructs a ResultInfo instance with the specified total count and default values for other
   * fields. Just to use in tests!
   *
   * @param totalCount the total number of results across all pages
   */
  public ResultInfo(int totalCount) {
    this(0, 0, 0, 0, totalCount);
  }
}
