package codes.thischwa.cf.model;

/**
 * Represents metadata for paginated results.
 *
 * <p>This class contains information about the current page, page size, total pages, and result
 * counts, which can be utilized in managing and navigating through paginated data.
 *
 * <ul>
 *   <li><b>page:</b> The current page number.
 *   <li><b>perPage:</b> The number of results per page.
 *   <li><b>totalPages:</b> The total number of pages available.
 *   <li><b>count:</b> The number of results on the current page.
 *   <li><b>totalCount:</b> The total number of results across all pages.
 * </ul>
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
