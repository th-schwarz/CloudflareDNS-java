package codes.thischwa.cf.fluent;

import codes.thischwa.cf.CloudflareApiException;
import codes.thischwa.cf.model.RecordType;
import org.jetbrains.annotations.Nullable;

/**
 * Fluent interface for zone-level operations.
 * Provides a chainable API for accessing and manipulating DNS records within a specific zone.
 */
public interface ZoneOperations {

  /**
   * Selects a record (subdomain) within the zone for further operations.
   *
   * @param sld the second-level domain (subdomain) name
   * @return a RecordOperations instance for chaining record-specific operations
   * @throws CloudflareApiException if the zone cannot be found or accessed
   */
  RecordOperations record(String sld) throws CloudflareApiException;

  /**
   * Selects a record with specific types within the zone for further operations.
   *
   * @param sld   the second-level domain (subdomain) name
   * @param types optional DNS record types to filter by
   * @return a RecordOperations instance for chaining record-specific operations
   * @throws CloudflareApiException if the zone cannot be found or accessed
   */
  RecordOperations record(String sld, @Nullable RecordType... types) throws CloudflareApiException;

  /**
   * Lists all DNS records within the zone, optionally filtered by types.
   *
   * @param types optional DNS record types to filter by
   * @return a list of RecordEntity objects matching the criteria
   * @throws CloudflareApiException if an error occurs while retrieving records
   */
  java.util.List<codes.thischwa.cf.model.RecordEntity> list(@Nullable RecordType... types) throws CloudflareApiException;
}
