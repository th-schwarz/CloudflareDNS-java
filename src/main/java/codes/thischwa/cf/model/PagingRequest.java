package codes.thischwa.cf.model;

import java.util.Map;
import lombok.Data;

/**
 * Represents a request model for paginated data.
 *
 * <p>This class encapsulates the page number and the number of items per page for a paginated
 * request, along with utility methods for constructing and retrieving pagination parameters. Page
 * numbers must be at least 1, and page sizes must be between 1 and 5,000,000 for DNS record requests.
 * Zone requests have the more restrictive Cloudflare limit of 5 to 50 items per page.
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
   * Default page size for a single request, which is within the limits for zones and DNS records.
   */
  private static final int DEFAULT_PAGE_SIZE = 50;

  /**
   * Maximum number of DNS records allowed per page by Cloudflare.
   */
  public static final int MAX_PER_PAGE = 5_000_000;

  private int page;
  private int perPage;

  PagingRequest(int page, int perPage) {
    this.page = validatePage(page);
    this.perPage = validatePerPage(perPage);
  }

  /**
   * Creates a new {@code PagingRequest} instance with the specified page number and items per page.
   * The page must be at least 1, and the page size must be between 1 and 5,000,000. Zone requests
   * have a more restrictive page-size range of 5 to 50, enforced by {@code zoneList}.
   *
   * @param page    the page number to be requested
   * @param perPage the number of items to be included per page
   * @return a new {@code PagingRequest} instance with the provided parameters
   * @throws IllegalArgumentException if the page or page size is outside the allowed range
   */
  public static PagingRequest of(int page, int perPage) {
    return new PagingRequest(page, perPage);
  }

  /**
   * Creates a default {@code PagingRequest} instance for page 1 with 50 items per page. This is a
   * request for one page; it does not fetch additional pages.
   *
   * @return a default {@code PagingRequest} instance with predefined pagination parameters
   */
  public static PagingRequest defaultPaging() {
    return new PagingRequest(1, DEFAULT_PAGE_SIZE);
  }

  /**
   * Sets the page number.
   *
   * @param page the page number, which must be at least 1
   * @throws IllegalArgumentException if the page number is less than 1
   */
  public void setPage(int page) {
    this.page = validatePage(page);
  }

  /**
   * Sets the number of items per page.
   *
   * @param perPage the page size, between 1 and 5,000,000 for DNS record requests
   * @throws IllegalArgumentException if the page size is outside the allowed range
   */
  public void setPerPage(int perPage) {
    this.perPage = validatePerPage(perPage);
  }

  /**
   * Retrieves the pagination parameters in a key-value map format.
   *
   * @return a map containing the pagination parameters, where the key "page" indicates the current
   *     page number and the key "perPage" indicates the number of items per page. The map key is
   *     intentionally named "perPage"; Cloudflare's query parameter is "per_page". Use
   *     {@link #addQueryString(String)} to build a Cloudflare-compatible query string.
   */
  public Map<String, String> getPagingParams() {
    return Map.of("page", String.valueOf(page), "perPage", String.valueOf(perPage));
  }

  /**
   * Appends a query string with pagination parameters ({@code page} and {@code per_page}) to the
   * provided endpoint. Unlike {@link #getPagingParams()}, this method uses Cloudflare's
   * {@code per_page} query parameter name.
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

  private static int validatePage(int page) {
    if (page < 1) {
      throw new IllegalArgumentException("page must be at least 1");
    }
    return page;
  }

  private static int validatePerPage(int perPage) {
    if (perPage < 1 || perPage > MAX_PER_PAGE) {
      throw new IllegalArgumentException("perPage must be between 1 and " + MAX_PER_PAGE);
    }
    return perPage;
  }
}
