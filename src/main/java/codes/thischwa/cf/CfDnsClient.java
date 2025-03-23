package codes.thischwa.cf;

import codes.thischwa.cf.model.AbstractResponse;
import codes.thischwa.cf.model.PagingRequest;
import codes.thischwa.cf.model.RecordEntity;
import codes.thischwa.cf.model.RecordMultipleResponse;
import codes.thischwa.cf.model.RecordSingleResponse;
import codes.thischwa.cf.model.RecordType;
import codes.thischwa.cf.model.ZoneEntity;
import codes.thischwa.cf.model.ZoneMultipleResponse;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * CfDnsClient is a client interface to interact with Cloudflare DNS service. It allows managing DNS
 * records and zones within the Cloudflare system, including creating, updating, retrieving, and
 * deleting DNS records.
 *
 * <p>Example:
 * <pre><code>
 * // Create a new CfDnsClient instance
 * CfDnsClient client = new CfDnsClient(
 *     "email@example.com",
 *     "yourApiKey",
 *     "yourApiToken"
 * );
 *
 * // Retrieve a zone
 * ZoneEntity zone = cfDnsClient.zoneInfo("example.com");
 * System.out.println("Zone ID: " + zone.getId());
 *
 * // Retrieve records of a zone
 * List<RecordEntity> records = cfDnsClient.sldListAll(zone, "sld");
 * records.forEach(record ->
 *     System.out.println("Record Type: " + record.getType() + ", Value: " + record.getContent())
 * );
 * </code></pre>
 */
@Setter
@Slf4j
public class CfDnsClient extends CfBasicHttpClient {
  private static final String DEFAULT_BASEURL = "https://api.cloudflare.com/client/v4";

  private boolean emptyResultThrowsException;

  /**
   * Constructs a CfDnsClient instance for interacting with the Cloudflare DNS API.
   *
   * @param authEmail The email address associated with the Cloudflare account, used for
   *     authentication.
   * @param authKey The API key of the Cloudflare account, used as part of the authentication
   *     process.
   * @param authToken The API token for accessing specific resources within the Cloudflare account.
   */
  public CfDnsClient(String authEmail, String authKey, String authToken) {
    this(DEFAULT_BASEURL, authEmail, authKey, authToken);
  }

  /**
   * Constructs a CfDnsClient instance for interacting with the Cloudflare DNS API.
   *
   * @param baseUrl The base URL of the Cloudflare API to be used for requests.
   * @param authEmail The email address associated with the Cloudflare account, used for
   *     authentication.
   * @param authKey The API key of the Cloudflare account, used as part of the authentication
   *     process.
   * @param authToken The API token for accessing specific resources within the Cloudflare account.
   */
  public CfDnsClient(String baseUrl, String authEmail, String authKey, String authToken) {
    this(true, baseUrl, authEmail, authKey, authToken);
  }

  /**
   * Constructs a new instance of {@code CfDnsClient}, which facilitates interactions with the
   * Cloudflare DNS API.
   *
   * @param emptyResultThrowsException Specifies if an exception should be thrown when the API
   *     response is empty. Default is true.
   * @param baseUrl The base URL for the Cloudflare API endpoint.
   * @param authEmail The email associated with the Cloudflare account for authentication.
   * @param authKey The API key for authenticating the client with Cloudflare services.
   * @param authToken The authentication token used for authorized access to Cloudflare API.
   */
  public CfDnsClient(
      boolean emptyResultThrowsException,
      String baseUrl,
      String authEmail,
      String authKey,
      String authToken) {
    super(baseUrl, authEmail, authKey, authToken);
    this.emptyResultThrowsException = emptyResultThrowsException;
  }

  /**
   * Retrieves a list of all zones from the Cloudflare API. <br>
   * This method sends a GET request to the Cloudflare API endpoint for listing zones, processes the
   * response, and returns the resulting list of ZoneEntity objects.
   *
   * @return A list of ZoneEntity objects representing the zones retrieved from the Cloudflare API.
   * @throws CloudflareApiException If an error occurs during the API request or response handling.
   */
  public List<ZoneEntity> zoneListAll() throws CloudflareApiException {
    return zoneListAll(PagingRequest.defaultPaging());
  }

  /**
   * Retrieves a list of all zones from the Cloudflare API. <br>
   * This method sends a GET request to the Cloudflare API endpoint for listing zones, processes the
   * response, and returns the resulting list of ZoneEntity objects.
   *
   * @return A list of ZoneEntity objects representing the zones retrieved from the Cloudflare API.
   * @throws CloudflareApiException If an error occurs during the API request or response handling.
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
   *     the response.
   */
  public ZoneEntity zoneInfo(String name) throws CloudflareApiException {
    String endpoint = CfRequest.ZONE_INFO.buildPath(name);
    ZoneMultipleResponse response = getRequest(endpoint, ZoneMultipleResponse.class);
    checkResponse(response, true);
    return response.getResult().get(0);
  }

  /**
   * Retrieves all record entities for a specific second-level domain (SLD) within a given DNS zone.
   *
   * @param zone The DNS zone entity for which the SLD records are to be fetched.
   * @param sld The second-level domain name for which the records are retrieved.
   * @return A list of {@code RecordEntity} objects representing the DNS records associated with the
   *     provided SLD.
   * @throws CloudflareApiException If an error occurs while interacting with the Cloudflare API.
   */
  public List<RecordEntity> sldListAll(ZoneEntity zone, String sld) throws CloudflareApiException {
    return sldListAll(zone, sld, PagingRequest.defaultPaging());
  }

  /**
   * Retrieves all record entities for a specific second-level domain (SLD) within a given DNS zone.
   *
   * @param zone The DNS zone entity for which the SLD records are to be fetched.
   * @param sld The second-level domain name for which the records are retrieved.
   * @param pagingRequest The paging request.
   * @return A list of {@code RecordEntity} objects representing the DNS records associated with the
   *     provided SLD.
   * @throws CloudflareApiException If an error occurs while interacting with the Cloudflare API.
   */
  public List<RecordEntity> sldListAll(ZoneEntity zone, String sld, PagingRequest pagingRequest)
      throws CloudflareApiException {
    String fqdn = sld + "." + zone.getName();
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
   * @param zone the zone entity that contains information about the DNS zone
   * @param sld the second-level domain (SLD) for which the record information is requested
   * @param type the type of DNS record (e.g., A, AAAA, CNAME) being queried
   * @return the record entity containing detailed information about the requested SLD and record
   *     type
   * @throws CloudflareApiException if an error occurs during interaction with the Cloudflare API
   */
  public RecordEntity sldInfo(ZoneEntity zone, String sld, RecordType type)
      throws CloudflareApiException {
    String fqdn = sld + "." + zone.getName();
    String endpoint = CfRequest.RECORD_INFO_NAME_TYPE.buildPath(zone.getId(), fqdn, type);
    RecordMultipleResponse resp = getRequest(endpoint, RecordMultipleResponse.class);
    checkResponse(resp, true);
    return resp.getResult().get(0);
  }

  /**
   * Creates a new DNS record in the specified zone using the Cloudflare API.
   *
   * @param zone The zone entity where the record will be created. Contains details such as zone ID.
   * @param rec The record entity representing the DNS record to be created, including its
   *     attributes.
   * @return The created record entity as returned by the Cloudflare API.
   * @throws CloudflareApiException If an error occurs while interacting with the Cloudflare API.
   */
  public RecordEntity recordCreate(ZoneEntity zone, RecordEntity rec)
      throws CloudflareApiException {
    String endpoint = CfRequest.RECORD_CREATE.buildPath(zone.getId());
    RecordSingleResponse resp = postRequest(endpoint, rec, RecordSingleResponse.class);
    checkResponse(resp);
    return resp.getResult();
  }

  /**
   * Deletes a DNS record of the specified type within a given zone on the Cloudflare API.
   *
   * @param zone The zone entity that specifies the zone in which the record exists.
   * @param rec The record entity that represents the DNS record to be deleted.
   * @return {@code true} if the DNS record was successfully deleted; {@code false} otherwise.
   * @throws CloudflareApiException if there is an issue during the API communication or the request
   *     fails for any reason.
   */
  public boolean recordDelete(ZoneEntity zone, RecordEntity rec) throws CloudflareApiException {
    boolean changed = recordDelete(zone, rec.getId());
    if (changed) {
      log.info("Record {} of the type {} successful deleted.", rec.getName(), rec.getType());
    } else {
      log.warn("Record {} of the type {} was not deleted.", rec.getName(), rec.getType());
    }
    return changed;
  }

  /**
   * Deletes a DNS record of the specified type within a given zone on the Cloudflare API.
   *
   * @param zone The zone entity that specifies the zone in which the record exists.
   * @param id The record entity that represents the DNS record to be deleted.
   * @return {@code true} if the DNS record was successfully deleted; {@code false} otherwise.
   * @throws CloudflareApiException if there is an issue during the API communication or the request
   *     fails for any reason.
   */
  public boolean recordDelete(ZoneEntity zone, String id) throws CloudflareApiException {
    String endpoint = CfRequest.RECORD_DELETE.buildPath(zone.getId(), id);
    RecordSingleResponse resp = deleteRequest(endpoint, RecordSingleResponse.class);
    checkResponse(resp);
    return resp.getResult().getId().equals(id);
  }

  /**
   * Updates an existing DNS record in a specified Cloudflare zone.
   *
   * @param zone the zone entity containing the ID of the target zone
   * @param rec the record entity containing the ID of the DNS record to be updated and its updated
   *     data
   * @return the updated record entity as returned by the Cloudflare API
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
    return resp.getResult();
  }

  /**
   * Attempts to delete a DNS record of a specific type for a given zone and second-level domain
   * (SLD), if it exists.
   *
   * @param zone The zone in which the DNS record resides. It provides information about the domain.
   * @param sld The second-level domain (SLD) of the fully qualified domain name (FQDN) for which
   *     the record is being deleted.
   * @param type The type of the DNS record to be deleted (e.g., A, CNAME, TXT).
   * @throws CloudflareApiException If an error occurs while interacting with the Cloudflare API.
   */
  public void recordDeleteTypeIfExists(ZoneEntity zone, String sld, RecordType type)
      throws CloudflareApiException {
    String fqdn = sld + "." + zone.getName();
    try {
      RecordEntity rec = sldInfo(zone, sld, type);
      recordDelete(zone, rec);
      log.debug("Record {} of type {} successful deleted.", fqdn, type);
    } catch (CloudflareNotFoundException e) {
      log.debug("Record {} of type {} does not exist.", fqdn, type);
    }
  }

  private void checkResponse(AbstractResponse resp) throws CloudflareApiException {
    checkResponse(resp, false);
  }

  private void checkResponse(AbstractResponse resp, boolean singleResultExpected)
      throws CloudflareApiException {
    if (!resp.isSuccess()) {
      String errors =
          resp.getErrors().stream().map(Object::toString).collect(Collectors.joining(", "));
      throw new CloudflareApiException("Error in response: " + errors);
    }

    if (resp instanceof RecordMultipleResponse respMulti) {
      if (singleResultExpected && respMulti.getResultInfo().getTotalCount() > 1) {
        throw new CloudflareApiException(
            "Unexpected result count: " + respMulti.getResultInfo().getTotalCount());
      }
      if (emptyResultThrowsException && respMulti.getResultInfo().getTotalCount() == 0) {
        throw new CloudflareNotFoundException("No result found");
      }
    }
  }
}
