package codes.thischwa.cf;

import codes.thischwa.cf.fluent.ZoneOperations;
import codes.thischwa.cf.fluent.ZoneOperationsImpl;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

/**
 * CfDnsClient is a client interface to interact with Cloudflare DNS service. It allows managing DNS
 * records and zones within the Cloudflare system, including creating, updating, retrieving, and
 * deleting DNS records.
 *
 * <p>Example with API token authentication (recommended):
 * <pre><code>
 * // Create a new CfDnsClient instance with API token
 * CfDnsClient cfDnsClient = new CfDnsClientBuilder()
 *     .withApiTokenAuth("your-api-token")
 *     .build();
 *
 * // Retrieve a zone
 * ZoneEntity zone = cfDnsClient.zoneGet("example.com");
 * System.out.println("Zone ID: " + zone.getId());
 *
 * // Retrieve records of a subdomain
 * List&lt;RecordEntity&gt; records = cfDnsClient.recordList(zone, "sld");
 * records.forEach(record ->
 *     System.out.println("Record Type: " + record.getType() + ", Value: " + record.getContent())
 * );
 *
 * // Create a record for the subdomain "api"
 * RecordEntity created = cfDnsClient.recordCreateSld(zone, "api", 60, RecordType.A, "192.168.1.10");
 * System.out.println("Created Record ID: " + created.getId());
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
 * <p>Example with custom base URL:
 * <pre><code>
 * CfDnsClient cfDnsClient = new CfDnsClientBuilder()
 *     .withApiTokenAuth("your-api-token")
 *     .withBaseUrl("https://custom-api.example.com")
 *     .build();
 * </code></pre>
 */
@Slf4j
public class CfDnsClient extends CfBasicHttpClient {

  private final ResponseValidator responseValidator;

  private final boolean emptyResultThrowsException;

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
    super(baseUrl, auth);
    this.responseValidator = new ResponseValidator(emptyResultThrowsException);
    this.emptyResultThrowsException = emptyResultThrowsException;
  }

  private static String buildFqdn(ZoneEntity zone, String sld) {
    return sld + "." + zone.getName();
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
   * @throws CloudflareApiException if the zone cannot be found or accessed
   */
  public ZoneOperations zone(String zoneName) throws CloudflareApiException {
    ZoneEntity zoneEntity = zoneGet(zoneName);
    return new ZoneOperationsImpl(this, zoneEntity);
  }

  /**
   * Retrieves a list of all zones from the Cloudflare API.
   *
   * @return A list of ZoneEntity objects representing the zones retrieved from the Cloudflare API.
   * @throws CloudflareApiException If an error occurs during the API request or response handling.
   */
  public List<ZoneEntity> zoneList() throws CloudflareApiException {
    return zoneList(PagingRequest.defaultPaging());
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
  public List<ZoneEntity> zoneList(PagingRequest pagingRequest) throws CloudflareApiException {
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
  public ZoneEntity zoneGet(String name) throws CloudflareApiException {
    String endpoint = CfRequest.ZONE_INFO.buildPath(name);
    ZoneMultipleResponse response = getRequest(endpoint, ZoneMultipleResponse.class);
    checkResponse(response, true);
    return response.getResult().get(0);
  }

  /**
   * Retrieves a list of DNS records for a specified zone, with optional paging support.
   *
   * @param zone          The zone entity containing information about the target zone.
   * @return A list of RecordEntity objects representing the DNS records of the specified zone.
   * @throws CloudflareApiException If an error occurs during the API request or response processing.
   */
  public List<RecordEntity> recordList(ZoneEntity zone) throws CloudflareApiException {
    return recordList(zone, (PagingRequest) null);
  }

  /**
   * Retrieves a list of DNS records for a specified zone, with optional paging support.
   *
   * @param zone          The zone entity containing information about the target zone.
   * @param pagingRequest The paging request containing parameters such as page size and number.
   * @return A list of RecordEntity objects representing the DNS records of the specified zone.
   * @throws CloudflareApiException If an error occurs during the API request or response processing.
   */
  public List<RecordEntity> recordList(ZoneEntity zone, @Nullable PagingRequest pagingRequest) throws CloudflareApiException {
    PagingRequest pr = pagingRequest == null ? PagingRequest.defaultPaging() : pagingRequest;
    String endpoint = pr.addQueryString(CfRequest.RECORD_LIST.buildPath(zone.getId()));
    RecordMultipleResponse resp = getRequest(endpoint, RecordMultipleResponse.class);
    checkResponse(resp);
    return resp.getResult();
  }

  /**
   * Retrieves DNS records for the specified second-level domain (SLD) within a zone.
   *
   * @param zone the zone entity representing the DNS zone to query
   * @param sld  the second-level domain (SLD) to filter the records
   * @return a list of RecordEntity objects that match the specified SLD within the zone
   * @throws CloudflareNotFoundException if the specified SLD is not found in the zone
   * @throws CloudflareApiException      if an error occurs while interacting with the Cloudflare API
   */
  public List<RecordEntity> recordList(ZoneEntity zone, String sld) throws CloudflareApiException {
    return recordList(zone, sld, (RecordType[]) null);
  }

  /**
   * Retrieves DNS records for the specified second-level domain (SLD) within a zone.
   * Optionally filters by one or more DNS getRecord types.
   *
   * @param zone  The zone entity containing information about the domain zone.
   * @param sld   The second-level domain (SLD) for which to retrieve DNS records.
   * @param types Optional parameter specifying one or more DNS getRecord types to filter the results.
   * @return A list of {@code RecordEntity} objects representing the DNS records for the specified domain.
   * @throws CloudflareNotFoundException if the specified SLD is not found in the zone
   * @throws CloudflareApiException      if an error occurs while interacting with the Cloudflare API
   */
  public List<RecordEntity> recordList(ZoneEntity zone, String sld, @Nullable RecordType... types)
      throws CloudflareApiException {
    String fqdn = buildFqdn(zone, sld);
    String endpoint = CfRequest.RECORD_LIST_NAME.buildPath(zone.getId(), fqdn);
    RecordMultipleResponse resp = getRequest(endpoint, RecordMultipleResponse.class);
    checkResponse(resp, false);
    List<RecordEntity> recs = resp.getResult();
    return filterAndSetZoneRecords(zone, types, recs);
  }

  /**
   * Retrieves a list of all DNS records for a given zone.
   * Optionally, filters by one or more DNS getRecord types.
   *
   * @param zone  The zone entity containing information about the domain zone.
   * @param types Optional parameter specifying one or more DNS getRecord types to filter the results.
   * @return A list of {@code RecordEntity} objects representing the DNS records for the specified zone.
   * @throws CloudflareApiException if an error occurs while interacting with the Cloudflare API
   */
  public List<RecordEntity> recordList(ZoneEntity zone, RecordType... types)
      throws CloudflareApiException {
    String endpoint = CfRequest.RECORD_LIST.buildPath(zone.getId());
    RecordMultipleResponse resp = getRequest(endpoint, RecordMultipleResponse.class);
    checkResponse(resp, false);
    List<RecordEntity> recs = resp.getResult();
    return filterAndSetZoneRecords(zone, types, recs);
  }

  /**
   * Creates a new DNS getRecord for a given second-level domain (SLD) within the specified zone.
   *
   * @param zone    The ZoneEntity representing the DNS zone where the getRecord is to be created.
   * @param sld     The second-level domain (SLD) for which the DNS getRecord is being created.
   * @param ttl     The time-to-live (TTL) value for the DNS getRecord in seconds.
   * @param type    The RecordType specifying the type of the DNS getRecord (e.g., A, AAAA, CNAME).
   * @param content The content of the DNS getRecord (e.g., IP address for A/AAAA records, target
   *                domain for CNAME).
   * @return The created RecordEntity object containing details of the newly created DNS getRecord.
   * @throws CloudflareApiException If an error occurs while communicating with the Cloudflare API
   *                                or creating the getRecord.
   */
  public RecordEntity recordCreateSld(ZoneEntity zone, String sld, int ttl, RecordType type,
                                      String content) throws CloudflareApiException {
    String fqdn = buildFqdn(zone, sld);
    return recordCreate(zone, fqdn, ttl, type, content);
  }

  /**
   * Creates a DNS getRecord in the specified DNS zone with the provided details.
   *
   * @param zone    the DNS zone in which the getRecord will be created
   * @param name    the name of the DNS getRecord (e.g., www.example.com)
   * @param ttl     the time-to-live (TTL) value for the DNS getRecord
   * @param type    the type of the DNS getRecord (e.g., A, AAAA, CNAME)
   * @param content the content or value of the DNS getRecord
   * @return the created DNS getRecord as a {@link RecordEntity} object
   * @throws CloudflareApiException if an error occurs while interacting with the Cloudflare API
   */
  public RecordEntity recordCreate(ZoneEntity zone, String name, int ttl, RecordType type,
                                   String content) throws CloudflareApiException {
    RecordEntity rec = RecordEntity.build(name, type, ttl, content);
    return recordCreate(zone, rec);
  }

  /**
   * Creates a new DNS getRecord in the specified zone using the Cloudflare API.
   *
   * @param zone The zone entity where the getRecord will be created. Contains details such as zone
   *             ID.
   * @param rec  The getRecord entity representing the DNS getRecord to be created, including its
   *             attributes.
   * @return The created getRecord entity as returned by the Cloudflare API.
   * @throws CloudflareApiException If an error occurs while interacting with the Cloudflare API.
   */
  public RecordEntity recordCreate(ZoneEntity zone, RecordEntity rec)
      throws CloudflareApiException {
    String endpoint = CfRequest.RECORD_CREATE.buildPath(zone.getId());
    RecordSingleResponse resp = postRequest(endpoint, rec, RecordSingleResponse.class);
    checkResponse(resp);
    log.info("Record {} of type {} successful created.", rec.getSld(), rec.getType());
    RecordEntity retRec = resp.getResult();
    retRec.setZoneId(zone.getId());
    return retRec;
  }

  /**
   * Deletes a DNS getRecord of the specified type within a given zone on the Cloudflare API.
   *
   * @param zone The zone entity that specifies the zone in which the getRecord exists.
   * @param rec  The getRecord entity that represents the DNS getRecord to be deleted.
   * @return {@code true} if the DNS getRecord was successfully deleted; {@code false} otherwise.
   * @throws CloudflareApiException if there is an issue during the API communication, or the
   *                                request fails for any reason.
   */
  public boolean recordDelete(ZoneEntity zone, RecordEntity rec) throws CloudflareApiException {
    boolean changed = recordDelete(zone, rec.getId());
    if (changed) {
      log.debug("Record {} of the type [{}] successful deleted.", rec.getSld(), rec.getType());
    } else {
      log.warn("Record {} of the type [{}] was not deleted.", rec.getSld(), rec.getType());
    }
    return changed;
  }

  /**
   * Deletes a DNS getRecord of the specified type within a given zone on the Cloudflare API.
   *
   * @param zone The zone entity that specifies the zone in which the getRecord exists.
   * @param id   The getRecord entity that represents the DNS getRecord to be deleted.
   * @return {@code true} if the DNS getRecord was successfully deleted; {@code false} otherwise.
   * @throws CloudflareApiException if there is an issue during the API communication or the request
   *                                fails for any reason.
   */
  public boolean recordDelete(ZoneEntity zone, String id) throws CloudflareApiException {
    String endpoint = CfRequest.RECORD_DELETE.buildPath(zone.getId(), id);
    RecordSingleResponse resp = deleteRequest(endpoint, RecordSingleResponse.class);
    checkResponse(resp);
    log.debug("Record id#{} successful deleted.", id);
    return resp.getResult().getId().equals(id);
  }

  /**
   * Updates an existing DNS getRecord in a specified Cloudflare zone.
   *
   * @param zone the zone entity containing the ID of the target zone
   * @param rec  the getRecord entity containing the ID of the DNS getRecord to be updated and its updated
   *             data
   * @return the updated getRecord entity as returned by the Cloudflare API
   * @throws CloudflareApiException if an error occurs while interacting with the Cloudflare API
   */
  public RecordEntity recordUpdate(ZoneEntity zone, RecordEntity rec)
      throws CloudflareApiException {
    // reset all dates, it causes an API issue
    rec.setModifiedOn(null);
    rec.setCreatedOn(null);
    String endpoint = CfRequest.RECORD_UPDATE.buildPath(zone.getId(), rec.getId());
    RecordSingleResponse resp = patchRequest(endpoint, rec, RecordSingleResponse.class);
    checkResponse(resp);
    log.info("Record {} of type {} successful updated.", rec.getSld(), rec.getType());
    return resp.getResult();
  }

  /**
   * Deletes DNS records of a specific type within a given zone if they exist. If no getRecord of the
   * specified type exists, it logs this occurrence without throwing an exception.
   *
   * @param zone        The DNS zone entity in which the getRecord exists.
   * @param sld         The second-level domain for which the getRecord is being checked.
   * @param recordTypes The types of DNS records that should be deleted if they exist.
   * @throws CloudflareApiException If an error occurs during API communication.
   */
  public void recordDeleteTypeIfExists(ZoneEntity zone, String sld, RecordType... recordTypes)
      throws CloudflareApiException {
    String fqdn = buildFqdn(zone, sld);
    List<RecordEntity> recs;
    try {
      recs = recordList(zone, sld, recordTypes);
    } catch (CloudflareNotFoundException e) {
      log.trace("No getRecord of type {} found for domain {}.", recordTypes, fqdn);
      return;
    }
    for (RecordEntity rec : recs) {
      try {
        recordDelete(zone, rec);
        log.info("Record {} of type {} successful deleted.", fqdn, recordTypes);
      } catch (CloudflareApiException e) {
        log.error("Failed to delete getRecord {} of type {} for zone {}: {}", fqdn, recordTypes, zone.getName(), e.getMessage());
      }
    }
  }

  /**
   * Processes a batch of DNS getRecord operations (POST, PUT, PATCH, DELETE) for a specified zone.
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
    // build 'clean' getRecord entries
    if (postRecords != null) {
      batchEntry.setPosts(cleanRecordsForPostOrPut(postRecords));
    }
    if (putRecords != null) {
      batchEntry.setPuts(cleanRecordsForPostOrPut(putRecords));
    }
    if (patchRecords != null) {
      batchEntry.setPatches(cleanRecordsForPatch(patchRecords));
    }
    if (deleteRecords != null) {
      batchEntry.setDeletes(cleanRecordsForDelete(deleteRecords));
    }

    String endpoint = CfRequest.RECORD_BATCH.buildPath(zone.getId());
    BatchResponse resp = postRequest(endpoint, batchEntry, BatchResponse.class);
    checkResponse(resp);

    // set zone id
    BatchEntry result = resp.getResult();
    setZoneIdForBatchResults(result, zone.getId());
    return result;
  }

  private List<RecordEntity> filterAndSetZoneRecords(ZoneEntity zone, @Nullable RecordType[] types, List<RecordEntity> recs)
      throws CloudflareNotFoundException {
    List<RecordEntity> filtered;
    if (types != null && types.length > 0) {
      Set<RecordType> allowedTypes = new HashSet<>(Arrays.asList(types));
      filtered = recs.stream()
          .filter(rec -> allowedTypes.contains(RecordType.valueOf(rec.getType())))
          .collect(Collectors.toList());
    } else {
      filtered = new ArrayList<>(recs);
    }
    filtered.forEach(rec -> rec.setZoneId(zone.getId()));

    // special exception for an empty result, normally it's done in the RecordValidator
    if (filtered.isEmpty() && emptyResultThrowsException) {
      throw new CloudflareNotFoundException("No records exist after filtering zone: " + zone.getName());
    }
    return filtered;
  }


  private List<RecordEntity> cleanRecordsForPostOrPut(List<RecordEntity> records) {
    List<RecordEntity> cleaned = new ArrayList<>();
    records.forEach(
        rec -> cleaned.add(RecordEntity.build(rec.getId(), rec.getSld(), rec.getType(), rec.getTtl(), rec.getContent())));
    return cleaned;
  }

  private List<RecordEntity> cleanRecordsForPatch(List<RecordEntity> records) {
    List<RecordEntity> cleaned = new ArrayList<>();
    records.forEach(rec -> cleaned.add(RecordEntity.build(rec.getId(), rec.getContent())));
    return cleaned;
  }

  private List<RecordEntity> cleanRecordsForDelete(List<RecordEntity> records) {
    List<RecordEntity> cleaned = new ArrayList<>();
    records.forEach(
        rec -> cleaned.add(RecordEntity.build(rec.getId(), rec.getSld(), rec.getType(), null, rec.getContent())));
    return cleaned;
  }

  private void setZoneIdForBatchResults(BatchEntry result, String zoneId) {
    if (result.getPosts() != null) {
      result.getPosts().forEach(rec -> rec.setZoneId(zoneId));
    }
    if (result.getPuts() != null) {
      result.getPuts().forEach(rec -> rec.setZoneId(zoneId));
    }
    if (result.getPatches() != null) {
      result.getPatches().forEach(rec -> rec.setZoneId(zoneId));
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
