package codes.thischwa.cf.model;

import java.util.Map;
import lombok.Data;

/**
 * Represents a request model for paginated data.
 *
 * <p>This class encapsulates the page number and the number of items per page for a paginated
 * request, along with utility methods for constructing and retrieving pagination parameters.
 *
 * <p>Key functionalities:
 *
 * <ul>
 *   <li>Creating a {@code PagingRequest} instance with specific pagination values using the {@code
 *       of} method.
 *   <li>Creating a default {@code PagingRequest} with predefined pagination values.
 *   <li>Retrieving pagination parameters as a key-value map.
 *   <li>Generating a query string representation for the pagination parameters.
 * </ul>
 */
@Data
public class PagingRequest {
  /**
   * Default page size for retrieving all records in a single request.
   * Set to a very high value to effectively disable pagination when fetching all records.
   */
  private static final int DEFAULT_ALL_RECORDS_PAGE_SIZE = 1000;

  private int page;
  private int perPage;

  PagingRequest(int page, int perPage) {
    this.page = page;
    this.perPage = perPage;
  }

  /**
   * Creates a new {@code PagingRequest} instance with the specified page number and items per page.
   *
   * @param page    the page number to be requested
   * @param perPage the number of items to be included per page
   * @return a new {@code PagingRequest} instance with the provided parameters
   */
  public static PagingRequest of(int page, int perPage) {
    return new PagingRequest(page, perPage);
  }

  /**
   * Creates a default {@code PagingRequest} instance with a page number set to 1 and a high number
   * of items per page to accommodate large dataset requests and effectively retrieve all records.
   *
   * @return a default {@code PagingRequest} instance with predefined pagination parameters
   */
  public static PagingRequest defaultPaging() {
    return new PagingRequest(1, DEFAULT_ALL_RECORDS_PAGE_SIZE);
  }

  /**
   * Retrieves the pagination parameters in a key-value map format.
   *
   * @return a map containing the pagination parameters, where the key "page" indicates the current
   *     page number and the key "perPage" indicates the number of items per page.
   */
  public Map<String, String> getPagingParams() {
    return Map.of("page", String.valueOf(page), "perPage", String.valueOf(perPage));
  }

  /**
   * Appends a query string with pagination parameters (page and perPage) to the provided endpoint.
   *
   * @param endpoint the base URL or API endpoint to which the query string will be appended
   * @return the complete URL with the appended query string for pagination
   */
  public String addQueryString(String endpoint) {
    return endpoint + queryString(endpoint.contains("?"));
  }

  private String queryString(boolean add) {
    String qs = "page=" + page + "&per_page=" + perPage;
    return add ? "&" + qs : "?" + qs;
  }
}
