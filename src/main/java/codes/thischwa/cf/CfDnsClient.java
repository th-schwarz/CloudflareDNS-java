package codes.thischwa.cf;

import codes.thischwa.cf.model.AbstractResponse;
import codes.thischwa.cf.model.BatchEntry;
import codes.thischwa.cf.model.BatchResponse;
import codes.thischwa.cf.model.PagingRequest;
import codes.thischwa.cf.model.RecordEntity;
import codes.thischwa.cf.model.RecordMultipleResponse;
import codes.thischwa.cf.model.RecordSingleResponse;
import codes.thischwa.cf.model.RecordType;
import codes.thischwa.cf.model.ZoneEntity;
import codes.thischwa.cf.model.ZoneMultipleResponse;
import java.util.ArrayList;
import java.util.List;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

/**
 * CfDnsClient is a client interface to interact with Cloudflare DNS service. It allows managing DNS
 * records and zones within the Cloudflare system, including creating, updating, retrieving, and
 * deleting DNS records.
 *
 * <p>Example:
 * <pre><code>
 * // Create a new CfDnsClient instance
 * CfDnsClient cfDnsClient = new CfDnsClient(
 *     "email@example.com",
 *     "yourApiKey"
 * );
 * // Retrieve a zone
 * ZoneEntity zone = cfDnsClient.zoneInfo("example.com");
 * System.out.println("Zone ID: " + zone.getId());
 * // Retrieve records of a subdomain
 * List&lt;{@link RecordEntity}&gt; records = cfDnsClient.sldListAll(zone, "sld");
 * records.forEach(record ->
 *     System.out.println("Record Type: " + record.getType() + ", Value: " + record.getContent())
 * );
 * // Create a record for the subdomain "api"
 * RecordEntity created = client.recordCreateSld(zone, "api", 60, RecordType.A, "192.168.10);
 * System.out.println("Created Record ID: " + created.getId());
 * </code></pre>
 */
@Setter
@Slf4j
public class CfDnsClient extends CfBasicHttpClient {
  private static final String DEFAULT_BASEURL = "https://api.cloudflare.com/client/v4";

  private final ResponseValidator responseValidator;

  /**
   * Constructs a new instance of {@code CfDnsClient}.
   *
   * @param authEmail The email address associated with the Cloudflare account, used for
   *                  authentication.
   * @param authKey   The API key of the Cloudflare account, used as part of the authentication
   *                  process.
   */
  public CfDnsClient(String authEmail, String authKey) {
    this(DEFAULT_BASEURL, authEmail, authKey);
  }

  /**
   * Constructs a new instance of {@code CfDnsClient}.
   *
   * @param baseUrl   The base URL of the Cloudflare API to be used for requests.
   * @param authEmail The email address associated with the Cloudflare account, used for
   *                  authentication.
   * @param authKey   The API key of the Cloudflare account, used as part of the authentication
   *                  process.
   */
  public CfDnsClient(String baseUrl, String authEmail, String authKey) {
    this(true, baseUrl, authEmail, authKey);
  }

  /**
   * Constructs a new instance of {@code CfDnsClient}.
   *
   * @param emptyResultThrowsException A boolean value indicating whether an exception should be
   *                                   thrown when the result is empty, it's valid for 'list
   *                                   requests' only. Default is true.
   * @param authEmail                  The email address associated with the Cloudflare account,
   *                                   used for authentication.
   * @param authKey                    The API key of the Cloudflare account, used as part of the
   *                                   authentication process.
   */
  public CfDnsClient(boolean emptyResultThrowsException, String authEmail, String authKey) {
    this(emptyResultThrowsException, DEFAULT_BASEURL, authEmail, authKey);
  }

  /**
   * Constructs a new instance of {@code CfDnsClient}.
   *
   * @param emptyResultThrowsException A boolean value indicating whether an exception should be
   *                                   thrown when the result is empty, it's valid for 'list
   *                                   requests' only. Default is true.
   * @param baseUrl                    The base URL for the Cloudflare API endpoint.
   * @param authEmail                  The email associated with the Cloudflare account for
   *                                   authentication.
   * @param authKey                    The API key for authenticating the client with Cloudflare
   *                                   services.
   */
  public CfDnsClient(boolean emptyResultThrowsException, String baseUrl, String authEmail,
                     String authKey) {
    super(baseUrl, authEmail, authKey);
    this.responseValidator = new ResponseValidator(emptyResultThrowsException);
  }

  private static String buildFqdn(ZoneEntity zone, String sld) {
    return sld + "." + zone.getName();
  }

  /**
   * Retrieves a list of all zones from the Cloudflare API.
   *
   * @return A list of ZoneEntity objects representing the zones retrieved from the Cloudflare API.
   * @throws CloudflareApiException If an error occurs during the API request or response handling.
   */
  public List<ZoneEntity> zoneListAll() throws CloudflareApiException {
    return zoneListAll(PagingRequest.defaultPaging());
  }

  /**
   * Retrieves a list of all DNS zones using the provided paging request parameters.
   *
   * @param pagingRequest the pagination request object containing parameters for paging and
   *                      filtering zone data
   * @return a list of {@code ZoneEntity} objects representing the DNS zones retrieved from the API
   * @throws CloudflareApiException if there is an error during the API request or response
   *                                processing
   */
  public List<ZoneEntity> zoneListAll(PagingRequest pagingRequest) throws CloudflareApiException {
    String endpoint = pagingRequest.addQueryString(CfRequest.ZONE_LIST.buildPath());
    ZoneMultipleResponse response = getRequest(endpoint, ZoneMultipleResponse.class);
    checkResponse(response);
    return response.getResult();
  }

  /**
   * Retrieves detailed information about a specific zone by its name.
   *
   * @param name The name of the zone to retrieve information for.
   * @return A {@link ZoneEntity} object that contains details of the specified zone.
   * @throws CloudflareApiException If an error occurs while making the API request or processing
   *                                the response.
   */
  public ZoneEntity zoneInfo(String name) throws CloudflareApiException {
    String endpoint = CfRequest.ZONE_INFO.buildPath(name);
    ZoneMultipleResponse response = getRequest(endpoint, ZoneMultipleResponse.class);
    checkResponse(response, true);
    return response.getResult().get(0);
  }

  /**
   * Retrieves all record entities for a specific second-level domain (SLD) within a given DNS
   * zone.
   *
   * @param zone The DNS zone entity for which the SLD records are to be fetched.
   * @param sld  The second-level domain name for which the records are retrieved.
   * @return A list of {@code RecordEntity} associated with the desired SLD.
   * @throws CloudflareApiException If an error occurs while interacting with the Cloudflare API.
   */
  public List<RecordEntity> sldListAll(ZoneEntity zone, String sld) throws CloudflareApiException {
    return sldListAll(zone, sld, PagingRequest.defaultPaging());
  }

  /**
   * Retrieves all record entities for a specific second-level domain (SLD) within a given DNS
   * zone.
   *
   * @param zone          The DNS zone entity for which the SLD records are to be fetched.
   * @param sld           The second-level domain name for which the records are retrieved.
   * @param pagingRequest The paging request.
   * @return A list of {@code RecordEntity} associated with the desired SLD.
   * @throws CloudflareApiException If an error occurs while interacting with the Cloudflare API.
   */
  public List<RecordEntity> sldListAll(ZoneEntity zone, String sld, PagingRequest pagingRequest)
      throws CloudflareApiException {
    String fqdn = buildFqdn(zone, sld);
    String endpoint =
        pagingRequest.addQueryString(CfRequest.RECORD_INFO_NAME.buildPath(zone.getId(), fqdn));
    RecordMultipleResponse resp = getRequest(endpoint, RecordMultipleResponse.class);
    checkResponse(resp);
    return resp.getResult();
  }

  /**
   * Retrieves detailed information about a specific second-level domain (SLD) record for a given
   * zone and record type from the Cloudflare API.
   *
   * @param zone  the zone entity that contains information about the DNS zone
   * @param sld   the second-level domain (SLD) for which the record information is requested
   * @param types the type of DNS record (e.g., A, AAAA, CNAME) being queried, nullable
   * @return a list of {@code RecordEntity} objects representing the requested record(s)
   * @throws CloudflareApiException if an error occurs during interaction with the Cloudflare API
   */

  public List<RecordEntity> sldInfo(ZoneEntity zone, String sld, @Nullable RecordType... types)
      throws CloudflareApiException {
    String fqdn = buildFqdn(zone, sld);
    String endpoint = buildEndpointWithTypeFilters(zone.getId(), fqdn, types);
    RecordMultipleResponse resp = getRequest(endpoint, RecordMultipleResponse.class);
    checkResponse(resp, false);
    return resp.getResult();
  }

  private String buildEndpointWithTypeFilters(String zoneId, String fqdn, @Nullable RecordType... types) {
    String baseEndpoint = CfRequest.RECORD_INFO_NAME.buildPath(zoneId, fqdn);
    if (types == null || types.length == 0) {
      return baseEndpoint;
    }
    StringBuilder queryParams = new StringBuilder();
    for (RecordType type : types) {
      queryParams.append("&");
      queryParams.append("type=").append(type);
    }
    return baseEndpoint + queryParams;
  }

  /**
   * Creates a new DNS record for a given second-level domain (SLD) within the specified zone.
   *
   * @param zone    The ZoneEntity representing the DNS zone where the record is to be created.
   * @param sld     The second-level domain (SLD) for which the DNS record is being created.
   * @param ttl     The time-to-live (TTL) value for the DNS record in seconds.
   * @param type    The RecordType specifying the type of the DNS record (e.g., A, AAAA, CNAME).
   * @param content The content of the DNS record (e.g., IP address for A/AAAA records, target
   *                domain for CNAME).
   * @return The created RecordEntity object containing details of the newly created DNS record.
   * @throws CloudflareApiException If an error occurs while communicating with the Cloudflare API
   *                                or creating the record.
   */
  public RecordEntity recordCreateSld(ZoneEntity zone, String sld, int ttl, RecordType type,
                                      String content) throws CloudflareApiException {
    String fqdn = buildFqdn(zone, sld);
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
   *             attributes.
   * @return The created record entity as returned by the Cloudflare API.
   * @throws CloudflareApiException If an error occurs while interacting with the Cloudflare API.
   */
  public RecordEntity recordCreate(ZoneEntity zone, RecordEntity rec)
      throws CloudflareApiException {
    String endpoint = CfRequest.RECORD_CREATE.buildPath(zone.getId());
    RecordSingleResponse resp = postRequest(endpoint, rec, RecordSingleResponse.class);
    checkResponse(resp);
    log.info("Record {} of type {} successful created.", rec.getName(), rec.getType());
    return resp.getResult();
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
      log.debug("Record {} of the type [{}] successful deleted.", rec.getName(), rec.getType());
    } else {
      log.warn("Record {} of the type [{}] was not deleted.", rec.getName(), rec.getType());
    }
    return changed;
  }

  /**
   * Deletes a DNS record of the specified type within a given zone on the Cloudflare API.
   *
   * @param zone The zone entity that specifies the zone in which the record exists.
   * @param id   The record entity that represents the DNS record to be deleted.
   * @return {@code true} if the DNS record was successfully deleted; {@code false} otherwise.
   * @throws CloudflareApiException if there is an issue during the API communication or the request
   *                                fails for any reason.
   */
  public boolean recordDelete(ZoneEntity zone, String id) throws CloudflareApiException {
    String endpoint = CfRequest.RECORD_DELETE.buildPath(zone.getId(), id);
    RecordSingleResponse resp = deleteRequest(endpoint);
    checkResponse(resp);
    log.debug("Record id#{} successful deleted.", id);
    return resp.getResult().getId().equals(id);
  }

  /**
   * Updates an existing DNS record in a specified Cloudflare zone.
   *
   * @param zone the zone entity containing the ID of the target zone
   * @param rec  the record entity containing the ID of the DNS record to be updated and its updated
   *             data
   * @return the updated record entity as returned by the Cloudflare API
   * @throws CloudflareApiException if an error occurs while interacting with the Cloudflare API
   */
  public RecordEntity recordUpdate(ZoneEntity zone, RecordEntity rec)
      throws CloudflareApiException {
    // reset all dates, it causes an API issue
    rec.setModifiedOn(null);
    rec.setCreatedOn(null);
    String endpoint = CfRequest.RECORD_UPDATE.buildPath(zone.getId(), rec.getId());
    RecordSingleResponse resp = patchRequest(endpoint, rec);
    checkResponse(resp);
    log.info("Record {} of type {} successful updated.", rec.getName(), rec.getType());
    return resp.getResult();
  }

  /**
   * Deletes DNS records of a specific type within a given zone if they exist. If no record of the
   * specified type exists, it logs this occurrence without throwing an exception.
   *
   * @param zone        The DNS zone entity in which the record exists.
   * @param sld         The second-level domain for which the record is being checked.
   * @param recordTypes The types of DNS records that should be deleted, if they exist.
   * @throws CloudflareApiException If an error occurs during API communication.
   */
  public void recordDeleteTypeIfExists(ZoneEntity zone, String sld, RecordType... recordTypes)
      throws CloudflareApiException {
    String fqdn = buildFqdn(zone, sld);
    List<RecordEntity> recs;
    try {
      recs = sldInfo(zone, sld, recordTypes);
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
   * @param deleteRecords A list of DNS records to be deleted (DELETE). This parameter is nullable.
   * @return The resulting {@link BatchEntry} containing the processed records after the batch operation.
   * @throws CloudflareApiException If an error occurs while communicating with the Cloudflare API.
   */
  public BatchEntry recordBatch(ZoneEntity zone, @Nullable List<RecordEntity> postRecords, @Nullable List<RecordEntity> putRecords,
                                @Nullable List<RecordEntity> patchRecords, @Nullable List<RecordEntity> deleteRecords)
      throws CloudflareApiException {
    BatchEntry batchEntry = new BatchEntry();
    // build 'clean' record entries
    if (postRecords != null) {
      List<RecordEntity> cleanedPosts = new ArrayList<>();
      postRecords.forEach(
          rec -> cleanedPosts.add(RecordEntity.build(rec.getId(), rec.getName(), rec.getType(), rec.getTtl(), rec.getContent())));
      batchEntry.setPosts(cleanedPosts);
    }
    if (putRecords != null) {
      List<RecordEntity> cleanedPuts = new ArrayList<>();
      putRecords.forEach(
          rec -> cleanedPuts.add(RecordEntity.build(rec.getId(), rec.getName(), rec.getType(), rec.getTtl(), rec.getContent())));
      batchEntry.setPuts(cleanedPuts);
    }
    if (patchRecords != null) {
      List<RecordEntity> cleanedPatches = new ArrayList<>();
      patchRecords.forEach(rec -> cleanedPatches.add(RecordEntity.build(rec.getId(), rec.getContent())));
      batchEntry.setPatches(cleanedPatches);
    }
    if (deleteRecords != null) {
      List<RecordEntity> cleanedDeletes = new ArrayList<>();
      deleteRecords.forEach(
          rec -> cleanedDeletes.add(RecordEntity.build(rec.getId(), rec.getName(), rec.getType(), null, rec.getContent())));
      batchEntry.setDeletes(cleanedDeletes);
    }

    String endpoint = CfRequest.RECORD_BATCH.buildPath(zone.getId());
    BatchResponse resp = postRequest(endpoint, batchEntry, BatchResponse.class);
    checkResponse(resp);

    // set zone id
    BatchEntry result = resp.getResult();
    if (result.getPosts() != null) {
      result.getPosts().forEach(rec -> rec.setZoneId(zone.getId()));
    }
    if (result.getPuts() != null) {
      result.getPuts().forEach(rec -> rec.setZoneId(zone.getId()));
    }
    if (result.getPatches() != null) {
      result.getPatches().forEach(rec -> rec.setZoneId(zone.getId()));
    }
    return result;
  }

  private void checkResponse(AbstractResponse resp) throws CloudflareApiException {
    checkResponse(resp, false);
  }

  private void checkResponse(AbstractResponse resp, boolean singleResultExpected)
      throws CloudflareApiException {
    responseValidator.validate(resp, singleResultExpected);
  }
}
