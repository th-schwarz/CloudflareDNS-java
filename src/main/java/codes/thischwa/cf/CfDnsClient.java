package codes.thischwa.cf;

import codes.thischwa.cf.fluent.ZoneOperations;
import codes.thischwa.cf.fluent.ZoneOperationsImpl;
import codes.thischwa.cf.model.AbstractMultipleResponse;
import codes.thischwa.cf.model.AbstractResponse;
import codes.thischwa.cf.model.BatchEntry;
import codes.thischwa.cf.model.BatchResponse;
import codes.thischwa.cf.model.PagingRequest;
import codes.thischwa.cf.model.RecordEntity;
import codes.thischwa.cf.model.RecordMultipleResponse;
import codes.thischwa.cf.model.RecordSingleResponse;
import codes.thischwa.cf.model.RecordType;
import codes.thischwa.cf.model.ResponseEntity;
import codes.thischwa.cf.model.ResultInfo;
import codes.thischwa.cf.model.ZoneEntity;
import codes.thischwa.cf.model.ZoneMultipleResponse;
import java.net.IDN;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

/**
 * CfDnsClient is a client interface to interact with Cloudflare DNS service. It allows managing DNS
 * records and zones within the Cloudflare system, including creating, updating, retrieving, and
 * deleting DNS records.
 *
 * <p>The client is thread-safe and should be reused. It holds a pool of HTTP connections, which is
 * released by {@link #close()}.
 *
 * <p>Example with API token authentication (recommended):
 * <pre><code>
 * // Create a new CfDnsClient instance with API token
 * try (CfDnsClient cfDnsClient = new CfDnsClientBuilder()
 *     .withApiTokenAuth("your-api-token")
 *     .build()) {
 *
 *   // Retrieve a zone
 *   ZoneEntity zone = cfDnsClient.zoneGet("example.com");
 *   System.out.println("Zone ID: " + zone.getId());
 *
 *   // Retrieve records of a subdomain
 *   List&lt;RecordEntity&gt; records = cfDnsClient.recordListBySubdomain(zone, "www");
 *   records.forEach(record ->
 *       System.out.println("Record Type: " + record.getType() + ", Value: " + record.getContent())
 *   );
 *
 *   // Create a record for the subdomain "api"
 *   RecordEntity created = cfDnsClient.recordCreateSubdomain(zone, "api", 60, RecordType.A, "192.168.1.10");
 *   System.out.println("Created Record ID: " + created.getId());
 * }
 * </code></pre>
 *
 * <p>Example with email/key authentication (legacy):
 * <pre><code>
 * CfDnsClient cfDnsClient = new CfDnsClientBuilder()
 *     .withEmailKeyAuth("email@example.com", "your-api-key")
 *     .build();
 * </code></pre>
 *
 * <p>Example with exception throwing enabled:
 * <pre><code>
 * // Throws exception when results are empty
 * CfDnsClient cfDnsClient = new CfDnsClientBuilder()
 *     .withApiTokenAuth("your-api-token")
 *     .withEmptyResultThrowsException(true)
 *     .build();
 * </code></pre>
 *
 * <p>Example specifying the Cloudflare API base URL:
 * <pre><code>
 * CfDnsClient cfDnsClient = new CfDnsClientBuilder()
 *     .withApiTokenAuth("your-api-token")
 *     .withBaseUrl(CfDnsClientBuilder.DEFAULT_BASEURL)
 *     .build();
 * </code></pre>
 */
@Slf4j
public class CfDnsClient extends CfBasicHttpClient {

  /**
   * Page size used when fetching all DNS records.
   */
  static final int RECORDS_PER_PAGE = 1000;

  /**
   * Sort order for listing DNS records. Cloudflare doesn't guarantee a stable order without it,
   * so pages could overlap or miss records.
   */
  static final String RECORD_ORDER = "order=name&direction=asc";

  /**
   * Max. attempts to fetch all pages consistently.
   */
  private static final int MAX_PAGING_ATTEMPTS = 3;

  /**
   * Conventional name for the zone apex (the zone name itself).
   */
  public static final String APEX = "@";

  private final ResponseValidator responseValidator;

  private final boolean emptyResultThrowsException;

  private final int recordsPerPage;

  /**
   * Constructs a new instance of {@code CfDnsClient}.
   *
   * @param emptyResultThrowsException A boolean value indicating whether an exception should be
   *                                   thrown when the result is empty. Applies to both single and
   *                                   multiple result requests. Default is false.
   * @param baseUrl                    The base URL for the Cloudflare API endpoint.
   * @param auth                       The authentication mechanism to use (ApiTokenAuth or EmailKeyAuth)
   */
  CfDnsClient(boolean emptyResultThrowsException, String baseUrl, CfDnsClientBuilder.CfAuth auth) {
    this(emptyResultThrowsException, baseUrl, auth, RECORDS_PER_PAGE);
  }

  /**
   * Constructs a new instance of {@code CfDnsClient} with a custom page size for fetching all DNS
   * records. Intended for tests, which need to fetch multiple pages with only a few records.
   */
  CfDnsClient(boolean emptyResultThrowsException, String baseUrl, CfDnsClientBuilder.CfAuth auth,
              int recordsPerPage) {
    this(emptyResultThrowsException, baseUrl, auth, recordsPerPage,
        CfDnsClientBuilder.DEFAULT_CONNECT_TIMEOUT, CfDnsClientBuilder.DEFAULT_RESPONSE_TIMEOUT);
  }

  /**
   * Constructs a new instance of {@code CfDnsClient} with all settings.
   */
  CfDnsClient(boolean emptyResultThrowsException, String baseUrl, CfDnsClientBuilder.CfAuth auth,
              int recordsPerPage, Duration connectTimeout, Duration responseTimeout) {
    super(baseUrl, auth, connectTimeout, responseTimeout);
    this.responseValidator = new ResponseValidator(emptyResultThrowsException);
    this.emptyResultThrowsException = emptyResultThrowsException;
    this.recordsPerPage = recordsPerPage;
  }

  /**
   * Characters which can't be part of a DNS name, but are typical for injection attempts. Further
   * validation is done by Cloudflare; the values are URL-encoded anyway. A slash isn't rejected,
   * because it's valid in classless reverse delegation names (RFC 2317).
   */
  private static final Pattern INVALID_NAME_CHARS = Pattern.compile("[\\s\\p{Cntrl}\\\\?#&;|<>\"'`]");

  /**
   * Validates a zone name or subdomain before it's used in a request.
   *
   * @throws IllegalArgumentException if the name is {@code null}, blank or contains invalid
   *                                  characters
   */
  private static String validateName(String name, String kind) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException(kind + " must not be null or blank");
    }
    if (INVALID_NAME_CHARS.matcher(name).find()) {
      throw new IllegalArgumentException("Invalid characters in " + kind + ": " + name);
    }
    return name;
  }

  private static String buildFqdn(ZoneEntity zone, String subdomain) {
    validateName(subdomain, "subdomain");
    if (APEX.equals(subdomain)) {
      return zone.getName();
    }
    String asciiSubdomain = subdomain;
    if (!subdomain.chars().allMatch(character -> character <= 0x7f)) {
      try {
        // IDN implements IDNA2003 (Unicode 3.2), newer code points (e.g. emojis) must be allowed
        asciiSubdomain = IDN.toASCII(subdomain, IDN.ALLOW_UNASSIGNED);
      } catch (IllegalArgumentException exception) {
        throw new IllegalArgumentException("Invalid internationalized subdomain: " + subdomain,
            exception);
      }
    }
    return asciiSubdomain + "." + zone.getName();
  }

  /**
   * Groups a list of DNS records by their fully qualified domain name (FQDN).
   *
   * @param records A list of {@link RecordEntity} objects to be grouped by FQDN.
   * @return A map where the key is the FQDN (name field) and the value is a list of {@link RecordEntity}
   *     objects that share that FQDN.
   */
  public static Map<String, List<RecordEntity>> groupRecordsByFqdn(List<RecordEntity> records) {
    if (records == null) {
      return new HashMap<>();
    }
    return records.stream()
        .collect(Collectors.groupingBy(RecordEntity::getName));
  }

  /**
   * Provides fluent API access to operations in a specific zone.
   * This method returns a ZoneOperations interface that allows chaining operations
   * on DNS records within the specified zone.
   *
   * <p>Example:
   * <pre><code>
   * client.zone("example.com")
   *       .getRecord("api")
   *       .create(RecordType.A, "192.168.1.1", 60);
   * </code></pre>
   *
   * @param zoneName the name of the DNS zone (e.g., "example.com")
   * @return a ZoneOperations instance for chaining operations
   * @throws CloudflareNotFoundException if the zone doesn't exist
   * @throws CloudflareApiException      if the zone cannot be accessed
   * @throws IllegalArgumentException if the name is blank or contains invalid characters
   */
  public ZoneOperations zone(String zoneName) throws CloudflareApiException {
    ZoneEntity zoneEntity = zoneGet(zoneName);
    if (zoneEntity == null) {
      throw new CloudflareNotFoundException("Zone not found: " + zoneName);
    }
    return new ZoneOperationsImpl(this, zoneEntity);
  }

  /**
   * Retrieves a list of all zones from the Cloudflare API. If there are more zones than fit on one
   * page, all pages are fetched.
   *
   * @return A list of ZoneEntity objects representing the zones retrieved from the Cloudflare API.
   * @throws CloudflareApiException If an error occurs during the API request or response handling.
   */
  public List<ZoneEntity> zoneList() throws CloudflareApiException {
    return getAllPages(CfRequest.ZONE_LIST.buildPath(), ZoneMultipleResponse.class,
        PagingRequest.DEFAULT_PAGE_SIZE);
  }

  /**
   * Retrieves a single page of DNS zones using the provided paging request parameters.
   * Cloudflare requires a page size between 5 and 50 for this endpoint.
   *
   * @param pagingRequest the pagination request object containing parameters for paging and
   *                      filtering zone data
   * @return a list of {@code ZoneEntity} objects representing the DNS zones retrieved from the API
   * @throws CloudflareApiException if there is an error during the API request or response
   *                                processing
   * @throws IllegalArgumentException if the page size is outside the range 5 to 50
   */
  public List<ZoneEntity> zoneList(PagingRequest pagingRequest) throws CloudflareApiException {
    Objects.requireNonNull(pagingRequest, "pagingRequest must not be null");
    int perPage = pagingRequest.perPage();
    if (perPage < PagingRequest.MIN_ZONES_PER_PAGE
        || perPage > PagingRequest.MAX_ZONES_PER_PAGE) {
      throw new IllegalArgumentException("Zone page size must be between "
          + PagingRequest.MIN_ZONES_PER_PAGE + " and " + PagingRequest.MAX_ZONES_PER_PAGE
          + ", but was " + perPage);
    }
    String endpoint = pagingRequest.addQueryString(CfRequest.ZONE_LIST.buildPath());
    ZoneMultipleResponse response = getRequest(endpoint, ZoneMultipleResponse.class);
    checkResponse(response);
    return response.getResult();
  }

  /**
   * Retrieves detailed information about a specific zone by its name.
   *
   * @param name The name of the zone to retrieve information for.
   * @return A {@link ZoneEntity} object that contains details of the specified zone, or
   *     {@code null} if the zone doesn't exist and 'emptyResultThrowsException' is disabled.
   * @throws CloudflareNotFoundException If the zone doesn't exist and 'emptyResultThrowsException'
   *                                     is enabled.
   * @throws CloudflareApiException      If an error occurs while making the API request or
   *                                     processing the response.
   * @throws IllegalArgumentException if the name is blank or contains invalid characters
   */
  @Nullable
  public ZoneEntity zoneGet(String name) throws CloudflareApiException {
    String endpoint = CfRequest.ZONE_INFO.buildPath(validateName(name, "zone name"));
    ZoneMultipleResponse response = getRequest(endpoint, ZoneMultipleResponse.class);
    checkResponse(response, true);
    List<ZoneEntity> zones = response.getResult();
    return (zones == null || zones.isEmpty()) ? null : zones.get(0);
  }

  /**
   * Retrieves all DNS records for a specified zone. If there are more records than fit on one
   * page, all pages are fetched.
   *
   * @param zone The zone entity containing information about the target zone.
   * @return A list of RecordEntity objects representing the DNS records of the specified zone.
   * @throws CloudflareApiException If an error occurs during the API request or response processing.
   */
  public List<RecordEntity> recordList(ZoneEntity zone) throws CloudflareApiException {
    return recordList(zone, null);
  }

  /**
   * Retrieves DNS records for a specified zone, with optional paging support.
   *
   * @param zone          The zone entity containing information about the target zone.
   * @param pagingRequest The paging request containing parameters such as page size and number.
   *                      If {@code null}, all pages are fetched using an internal page size of 1,000.
   *                      For a specific page, Cloudflare allows a page size between 1 and 5,000,000.
   * @return A list of RecordEntity objects representing the DNS records of the specified zone.
   * @throws CloudflareApiException If an error occurs during the API request or response processing.
   */
  public List<RecordEntity> recordList(ZoneEntity zone, @Nullable PagingRequest pagingRequest)
      throws CloudflareApiException {
    String path = appendQuery(CfRequest.RECORD_LIST.buildPath(zone.getId()), RECORD_ORDER);
    if (pagingRequest == null) {
      return filterAndSetZoneRecords(zone, null,
          getAllPages(path, RecordMultipleResponse.class, recordsPerPage));
    }
    RecordMultipleResponse resp = getRequest(pagingRequest.addQueryString(path), RecordMultipleResponse.class);
    checkResponse(resp);
    return filterAndSetZoneRecords(zone, null, resp.getResult());
  }

  /**
   * Retrieves DNS records for the specified subdomain within a zone.
   * Optionally, filters by one or more DNS record types.
   *
   * @param zone      The zone entity containing information about the domain zone.
   * @param subdomain The subdomain relative to the zone (e.g. "www" for "www.example.com");
   *                  Unicode subdomains are converted to Punycode. {@link #APEX} indicates the
   *                  zone apex.
   * @param types     Optional DNS record types to filter the results. If none are given, records of
   *                  all types are returned.
   * @return A list of {@code RecordEntity} objects representing the DNS records for the specified domain.
   * @throws CloudflareNotFoundException if no records were found and 'emptyResultThrowsException'
   *                                     is enabled
   * @throws CloudflareApiException      if an error occurs while interacting with the Cloudflare API
   * @throws IllegalArgumentException if the subdomain is blank, contains invalid characters or
   *                                  cannot be converted to Punycode
   */
  public List<RecordEntity> recordListBySubdomain(ZoneEntity zone, String subdomain,
                                                  @Nullable RecordType... types)
      throws CloudflareApiException {
    String fqdn = buildFqdn(zone, subdomain);
    String endpoint = appendQuery(
        addTypeFilter(CfRequest.RECORD_LIST_NAME.buildPath(zone.getId(), fqdn), types), RECORD_ORDER);
    List<RecordEntity> recs = getAllPages(endpoint, RecordMultipleResponse.class, recordsPerPage);
    return filterAndSetZoneRecords(zone, types, recs);
  }

  /**
   * Retrieves all DNS records of the given types for a zone.
   *
   * @param zone  The zone entity containing information about the domain zone.
   * @param types Optional DNS record types to filter the results. If none are given, records of all
   *              types are returned.
   * @return A list of {@code RecordEntity} objects representing the DNS records for the specified zone.
   * @throws CloudflareNotFoundException if no records were found and 'emptyResultThrowsException'
   *                                     is enabled
   * @throws CloudflareApiException      if an error occurs while interacting with the Cloudflare API
   */
  public List<RecordEntity> recordListByType(ZoneEntity zone, @Nullable RecordType... types)
      throws CloudflareApiException {
    String endpoint = appendQuery(
        addTypeFilter(CfRequest.RECORD_LIST.buildPath(zone.getId()), types), RECORD_ORDER);
    List<RecordEntity> recs = getAllPages(endpoint, RecordMultipleResponse.class, recordsPerPage);
    return filterAndSetZoneRecords(zone, types, recs);
  }

  /**
   * Creates a new DNS record for a given subdomain within the specified zone.
   *
   * @param zone      The ZoneEntity representing the DNS zone where the record is to be created.
   * @param subdomain The subdomain relative to the zone (e.g. "api" for "api.example.com");
   *                  Unicode subdomains are converted to Punycode. {@link #APEX} indicates the
   *                  zone apex.
   * @param ttl       The time-to-live (TTL) value for the DNS record in seconds.
   * @param type      The RecordType specifying the type of the DNS record (e.g., A, AAAA, CNAME).
   * @param content   The content of the DNS record (e.g., IP address for A/AAAA records, target
   *                  domain for CNAME).
   * @return The created RecordEntity object containing details of the newly created DNS record.
   * @throws CloudflareApiException If an error occurs while communicating with the Cloudflare API
   *                                or creating the record.
   * @throws IllegalArgumentException if the subdomain is blank, contains invalid characters or
   *                                  cannot be converted to Punycode
   */
  public RecordEntity recordCreateSubdomain(ZoneEntity zone, String subdomain, int ttl, RecordType type,
                                            String content) throws CloudflareApiException {
    String fqdn = buildFqdn(zone, subdomain);
    return recordCreate(zone, fqdn, ttl, type, content);
  }

  /**
   * Creates a DNS record in the specified DNS zone with the provided details.
   *
   * @param zone    the DNS zone in which the record will be created
   * @param name    the name of the DNS record (e.g., www.example.com)
   * @param ttl     the time-to-live (TTL) value for the DNS record
   * @param type    the type of the DNS record (e.g., A, AAAA, CNAME)
   * @param content the content or value of the DNS record
   * @return the created DNS record as a {@link RecordEntity} object
   * @throws CloudflareApiException if an error occurs while interacting with the Cloudflare API
   */
  public RecordEntity recordCreate(ZoneEntity zone, String name, int ttl, RecordType type,
                                   String content) throws CloudflareApiException {
    RecordEntity rec = RecordEntity.build(name, type, ttl, content);
    return recordCreate(zone, rec);
  }

  /**
   * Creates a new DNS record in the specified zone using the Cloudflare API.
   *
   * @param zone The zone entity where the record will be created. Contains details such as zone
   *             ID.
   * @param rec  The record entity representing the DNS record to be created, including its
   *             attributes. Read-only attributes (e.g. ID, timestamps) are ignored, the given
   *             object isn't modified.
   * @return The created record entity as returned by the Cloudflare API.
   * @throws CloudflareApiException If an error occurs while interacting with the Cloudflare API.
   */
  public RecordEntity recordCreate(ZoneEntity zone, RecordEntity rec)
      throws CloudflareApiException {
    String endpoint = CfRequest.RECORD_CREATE.buildPath(zone.getId());
    RecordEntity payload = writableCopy(rec, false);
    RecordSingleResponse resp = postRequest(endpoint, payload, RecordSingleResponse.class);
    checkResponse(resp);
    log.info("Record {} of type {} successful created.", rec.getSubdomain(), rec.getType());
    RecordEntity retRec = resp.getResult();
    assignZone(retRec, zone);
    return retRec;
  }

  /**
   * Deletes a DNS record of the specified type within a given zone on the Cloudflare API.
   *
   * @param zone The zone entity that specifies the zone in which the record exists.
   * @param rec  The record entity that represents the DNS record to be deleted.
   * @return {@code true} if the DNS record was successfully deleted; {@code false} otherwise.
   * @throws CloudflareApiException if there is an issue during the API communication, or the
   *                                request fails for any reason.
   */
  public boolean recordDelete(ZoneEntity zone, RecordEntity rec) throws CloudflareApiException {
    boolean changed = recordDelete(zone, rec.getId());
    if (changed) {
      log.debug("Record {} of the type [{}] successful deleted.", rec.getSubdomain(), rec.getType());
    } else {
      log.warn("Record {} of the type [{}] was not deleted.", rec.getSubdomain(), rec.getType());
    }
    return changed;
  }

  /**
   * Deletes a DNS record of the specified type within a given zone on the Cloudflare API.
   *
   * @param zone The zone entity that specifies the zone in which the record exists.
   * @param id   The ID of the DNS record to be deleted.
   * @return {@code true} if the API confirms the deletion of the record with the given ID;
   *     {@code false} otherwise.
   * @throws CloudflareApiException if there is an issue during the API communication or the request
   *                                fails for any reason.
   */
  public boolean recordDelete(ZoneEntity zone, String id) throws CloudflareApiException {
    String endpoint = CfRequest.RECORD_DELETE.buildPath(zone.getId(), id);
    RecordSingleResponse resp = deleteRequest(endpoint, RecordSingleResponse.class);
    checkResponse(resp);
    RecordEntity deleted = resp.getResult();
    boolean confirmed = deleted != null && id.equals(deleted.getId());
    if (confirmed) {
      log.debug("Record id#{} successfully deleted.", id);
    }
    return confirmed;
  }

  /**
   * Updates an existing DNS record in a specified Cloudflare zone (partial update, attributes
   * which are {@code null} remain unchanged).
   *
   * @param zone the zone entity containing the ID of the target zone
   * @param rec  the record entity containing the ID of the DNS record to be updated and its updated
   *             data. Read-only attributes (e.g. timestamps) are ignored, the given object isn't
   *             modified.
   * @return the updated record entity as returned by the Cloudflare API
   * @throws CloudflareApiException if an error occurs while interacting with the Cloudflare API
   */
  public RecordEntity recordUpdate(ZoneEntity zone, RecordEntity rec)
      throws CloudflareApiException {
    String endpoint = CfRequest.RECORD_UPDATE.buildPath(zone.getId(), rec.getId());
    RecordEntity payload = writableCopy(rec, false);
    RecordSingleResponse resp = patchRequest(endpoint, payload, RecordSingleResponse.class);
    checkResponse(resp);
    log.info("Record {} of type {} successful updated.", rec.getSubdomain(), rec.getType());
    RecordEntity retRec = resp.getResult();
    assignZone(retRec, zone);
    return retRec;
  }

  /**
   * Attempts to delete DNS records of the specified types within a given zone. If no record types
   * are specified, all types for the subdomain are selected. If no matching record exists, this is
   * treated as a no-op. Deletions are attempted independently; an error deleting an individual
   * record is logged and processing continues. No per-record result is returned, so a normal return
   * does not guarantee that every selected record was deleted.
   *
   * @param zone        The DNS zone entity in which the record exists.
   * @param subdomain   The subdomain relative to the zone for which the records are being checked.
   * @param recordTypes The DNS record types to delete; if empty or {@code null}, all types are selected.
   * @throws CloudflareApiException If an error occurs while retrieving the records to delete.
   * @throws IllegalArgumentException if the subdomain is blank or contains invalid characters
   */
  public void recordDeleteTypeIfExists(ZoneEntity zone, String subdomain, RecordType... recordTypes)
      throws CloudflareApiException {
    String fqdn = buildFqdn(zone, subdomain);
    List<RecordEntity> recs;
    try {
      recs = recordListBySubdomain(zone, subdomain, recordTypes);
    } catch (CloudflareNotFoundException e) {
      log.trace("No record of type {} found for domain {}.", recordTypes, fqdn);
      return;
    }
    for (RecordEntity rec : recs) {
      try {
        recordDelete(zone, rec);
        log.info("Record {} of type {} successful deleted.", fqdn, recordTypes);
      } catch (CloudflareApiException e) {
        log.error("Failed to delete record {} of type {} for zone {}: {}", fqdn, recordTypes, zone.getName(), e.getMessage());
      }
    }
  }

  /**
   * Processes a batch of DNS record operations (POST, PUT, PATCH, DELETE) for a specified zone.
   * This method builds and cleans the input records, sends the batch request to the Cloudflare API,
   * and returns a result containing processed batch entries.
   *
   * @param zone          The zone entity to which the records belong.
   * @param postRecords   A list of DNS records to be created (POST). This parameter is nullable.
   * @param putRecords    A list of DNS records to be fully replaced (PUT). This parameter is nullable.
   * @param patchRecords  A list of DNS records to be partially updated (PATCH). This parameter is nullable.
   * @param deleteRecords A list of DNS records to be deleted (DELETE), only the IDs are used. This
   *                      parameter is nullable.
   * @return The resulting {@link BatchEntry} containing the processed records after the batch operation.
   * @throws CloudflareApiException If an error occurs while communicating with the Cloudflare API.
   */
  public BatchEntry recordBatch(ZoneEntity zone, @Nullable List<RecordEntity> postRecords, @Nullable List<RecordEntity> putRecords,
                                @Nullable List<RecordEntity> patchRecords, @Nullable List<RecordEntity> deleteRecords)
      throws CloudflareApiException {
    BatchEntry batchEntry = new BatchEntry();
    // build 'clean' record entries, the given records aren't modified
    if (postRecords != null) {
      batchEntry.setPosts(postRecords.stream().map(rec -> writableCopy(rec, false)).toList());
    }
    if (putRecords != null) {
      batchEntry.setPuts(putRecords.stream().map(rec -> writableCopy(rec, true)).toList());
    }
    if (patchRecords != null) {
      batchEntry.setPatches(patchRecords.stream().map(rec -> writableCopy(rec, true)).toList());
    }
    if (deleteRecords != null) {
      batchEntry.setDeletes(deleteRecords.stream().map(CfDnsClient::idOnly).toList());
    }

    String endpoint = CfRequest.RECORD_BATCH.buildPath(zone.getId());
    BatchResponse resp = postRequest(endpoint, batchEntry, BatchResponse.class);
    checkResponse(resp);

    // set zone id
    BatchEntry result = resp.getResult();
    setZoneForBatchResults(result, zone);
    return result;
  }

  private List<RecordEntity> filterAndSetZoneRecords(ZoneEntity zone, @Nullable RecordType[] types,
                                                     @Nullable List<RecordEntity> recs)
      throws CloudflareNotFoundException {
    if (recs == null) {
      recs = List.of();
    }
    List<RecordEntity> filtered;
    if (types != null && types.length > 0) {
      Set<String> allowedTypes = Arrays.stream(types).filter(Objects::nonNull)
          .map(RecordType::getType).collect(Collectors.toSet());
      filtered = recs.stream()
          .filter(rec -> allowedTypes.contains(rec.getType()))
          .collect(Collectors.toCollection(ArrayList::new));
    } else {
      filtered = new ArrayList<>(recs);
    }
    filtered.forEach(rec -> assignZone(rec, zone));

    // special exception for an empty result, normally it's done in the RecordValidator
    if (filtered.isEmpty() && emptyResultThrowsException) {
      throw new CloudflareNotFoundException("No records exist after filtering zone: " + zone.getName());
    }
    return filtered;
  }


  /**
   * Creates a copy of the record containing only the attributes which can be written to the API.
   * Read-only attributes like timestamps, zone info, 'proxiable' or 'locked' are omitted.
   */
  private static RecordEntity writableCopy(RecordEntity rec, boolean withId) {
    RecordEntity copy = new RecordEntity();
    if (withId) {
      copy.setId(rec.getId());
    }
    copy.setName(rec.getName());
    copy.setType(rec.getType());
    copy.setContent(rec.getContent());
    copy.setTtl(rec.getTtl());
    copy.setProxied(rec.getProxied());
    copy.setPriority(rec.getPriority());
    copy.setComment(rec.getComment());
    copy.setTags(rec.getTags() == null ? null : new ArrayList<>(rec.getTags()));
    copy.setData(rec.getData() == null ? null : new LinkedHashMap<>(rec.getData()));
    return copy;
  }

  private static RecordEntity idOnly(RecordEntity rec) {
    RecordEntity copy = new RecordEntity();
    copy.setId(rec.getId());
    return copy;
  }

  /**
   * Adds the server-side type filter to the endpoint. The API supports only one type, so the
   * filter is only added if exactly one type is given. Multiple types are filtered client-side.
   */
  private static String addTypeFilter(String endpoint, @Nullable RecordType[] types) {
    if (types == null || types.length != 1 || types[0] == null) {
      return endpoint;
    }
    return appendQuery(endpoint, "type=" + types[0].getType());
  }

  private static String appendQuery(String endpoint, String query) {
    return endpoint + (endpoint.contains("?") ? "&" : "?") + query;
  }

  private void setZoneForBatchResults(@Nullable BatchEntry result, ZoneEntity zone) {
    if (result == null) {
      return;
    }
    for (List<RecordEntity> recs : Arrays.asList(result.getPosts(), result.getPuts(),
        result.getPatches(), result.getDeletes())) {
      if (recs != null) {
        recs.forEach(rec -> assignZone(rec, zone));
      }
    }
  }

  private static void assignZone(@Nullable RecordEntity rec, ZoneEntity zone) {
    if (rec != null) {
      rec.setZoneId(zone.getId());
      rec.setZoneName(zone.getName());
    }
  }

  /**
   * Fetches all pages of a paginated endpoint and returns the combined results.
   *
   * <p>Entries are de-duplicated by their ID. If fewer entries than announced by the API were
   * fetched (e.g. because of concurrent changes), the pages are fetched again, up to
   * {@value #MAX_PAGING_ATTEMPTS} times.
   *
   * @param endpoint     the endpoint path, may already contain a query string
   * @param responseType the expected response type
   * @param perPage      the page size to request
   */
  private <E extends ResponseEntity, R extends AbstractMultipleResponse<E>> List<E> getAllPages(
      String endpoint, Class<R> responseType, int perPage) throws CloudflareApiException {
    List<E> result = List.of();
    for (int attempt = 1; attempt <= MAX_PAGING_ATTEMPTS; attempt++) {
      PagingResult<E> paged = fetchAllPagesOnce(endpoint, responseType, perPage);
      result = paged.entries();
      if (paged.expectedCount() < 0 || result.size() >= paged.expectedCount()) {
        return result;
      }
      log.debug("Fetched {} of {} entries from {} (attempt {}), fetching again.", result.size(),
          paged.expectedCount(), endpoint, attempt);
    }
    log.warn("Paging of {} was inconsistent, the result may be incomplete.", endpoint);
    return result;
  }

  private record PagingResult<E>(List<E> entries, int expectedCount) {
  }

  private <E extends ResponseEntity, R extends AbstractMultipleResponse<E>> PagingResult<E> fetchAllPagesOnce(
      String endpoint, Class<R> responseType, int perPage) throws CloudflareApiException {
    Map<String, E> byId = new LinkedHashMap<>();
    List<E> withoutId = new ArrayList<>();
    int expectedCount = -1;
    PagingRequest pagingRequest = new PagingRequest(1, perPage);
    while (true) {
      int page = pagingRequest.page();
      String pagedEndpoint = pagingRequest.addQueryString(endpoint);
      R resp = getRequest(pagedEndpoint, responseType);
      checkResponse(resp);
      List<E> pageResult = resp.getResult();
      if (pageResult != null) {
        for (E entry : pageResult) {
          if (entry.getId() == null) {
            withoutId.add(entry);
          } else {
            byId.putIfAbsent(entry.getId(), entry);
          }
        }
      }
      ResultInfo info = resp.getResultInfo();
      if (info != null && page == 1) {
        expectedCount = info.totalCount();
      }
      log.debug("Fetched page {} of {} from {}: {} entries, total {}", page,
          info == null ? "?" : info.totalPages(), endpoint,
          pageResult == null ? 0 : pageResult.size(), info == null ? "?" : info.totalCount());
      if (info == null || pageResult == null || pageResult.isEmpty() || page >= info.totalPages()) {
        List<E> all = new ArrayList<>(byId.values());
        all.addAll(withoutId);
        return new PagingResult<>(all, info == null || page == 1 ? -1 : expectedCount);
      }
      pagingRequest = pagingRequest.next();
    }
  }

  private void checkResponse(AbstractResponse resp) throws CloudflareApiException {
    checkResponse(resp, false);
  }

  private void checkResponse(AbstractResponse resp, boolean singleResultExpected)
      throws CloudflareApiException {
    responseValidator.validate(resp, singleResultExpected);
  }

}
