package codes.thischwa.cf.model;

import java.util.Objects;

/**
 * Immutable pagination parameters for Cloudflare list endpoints. Page numbers start at 1.
 *
 * <p>Use {@link #forZones(int, int)} for zone requests and {@link #forRecords(int, int)} for DNS
 * record requests. For example:
 *
 * <pre>{@code
 * List<RecordEntity> records = client.recordList(zone, PagingRequest.forRecords(1, 100));
 * List<ZoneEntity> zones = client.zoneList(PagingRequest.forZones(1, 50));
 * }</pre>
 */
public record PagingRequest(int page, int perPage) {

  /** Default page size, valid for zones and DNS records. */
  public static final int DEFAULT_PAGE_SIZE = 50;

  /** Maximum number of DNS records allowed per page by Cloudflare. */
  public static final int MAX_RECORDS_PER_PAGE = 5_000_000;

  /** Minimum number of zones allowed per page by Cloudflare. */
  public static final int MIN_ZONES_PER_PAGE = 5;

  /** Maximum number of zones allowed per page by Cloudflare. */
  public static final int MAX_ZONES_PER_PAGE = 50;

  /** Validates the page number and page size. */
  public PagingRequest {
    if (page < 1) {
      throw new IllegalArgumentException("page must be at least 1, but was " + page);
    }
    if (perPage < 1 || perPage > MAX_RECORDS_PER_PAGE) {
      throw new IllegalArgumentException("perPage must be between 1 and " + MAX_RECORDS_PER_PAGE
          + ", but was " + perPage);
    }
  }

  /**
   * Creates pagination parameters for a DNS record request.
   *
   * @param page the 1-based page number
   * @param perPage the number of records per page, from 1 to {@value #MAX_RECORDS_PER_PAGE}
   * @return the validated pagination request
   * @throws IllegalArgumentException if either value is outside its allowed range
   */
  public static PagingRequest forRecords(int page, int perPage) {
    return new PagingRequest(page, perPage);
  }

  /**
   * Creates pagination parameters for a zone request.
   *
   * @param page the 1-based page number
   * @param perPage the number of zones per page, from {@value #MIN_ZONES_PER_PAGE} to
   *     {@value #MAX_ZONES_PER_PAGE}
   * @return the validated pagination request
   * @throws IllegalArgumentException if either value is outside its allowed range
   */
  public static PagingRequest forZones(int page, int perPage) {
    if (perPage < MIN_ZONES_PER_PAGE || perPage > MAX_ZONES_PER_PAGE) {
      throw new IllegalArgumentException("Zone page size must be between " + MIN_ZONES_PER_PAGE
          + " and " + MAX_ZONES_PER_PAGE + ", but was " + perPage);
    }
    return new PagingRequest(page, perPage);
  }

  /**
   * Creates a request for page 1 with {@value #DEFAULT_PAGE_SIZE} items per page.
   * This requests one page; it does not fetch additional pages.
   *
   * @return the default pagination request
   */
  public static PagingRequest defaultPaging() {
    return new PagingRequest(1, DEFAULT_PAGE_SIZE);
  }

  /**
   * Returns a request for the next page, retaining the current page size.
   *
   * @return the next page request
   */
  public PagingRequest next() {
    return new PagingRequest(page + 1, perPage);
  }

  /**
   * Returns a copy of this request with the specified page number.
   *
   * @param page the 1-based page number
   * @return the updated pagination request
   */
  public PagingRequest withPage(int page) {
    return new PagingRequest(page, perPage);
  }

  /**
   * Appends Cloudflare-compatible pagination parameters ({@code page} and {@code per_page}) to an
   * endpoint.
   *
   * @param endpoint the base URL or API endpoint, optionally containing a query string
   * @return the endpoint with pagination parameters appended
   * @throws NullPointerException if {@code endpoint} is {@code null}
   */
  public String addQueryString(String endpoint) {
    Objects.requireNonNull(endpoint, "endpoint must not be null");
    return endpoint + (endpoint.contains("?") ? "&" : "?")
        + "page=" + page + "&per_page=" + perPage;
  }
}
