package codes.thischwa.cf.model;

import lombok.Data;

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
@Data
public class ResultInfo {
  private int page;
  private int perPage;
  private int totalPages;
  private int count;
  private int totalCount;
}
