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
   * @param subdomain the subdomain relative to the zone (e.g. "www"), or "@" for the zone apex
   * @return a RecordOperations instance for chaining record-specific operations
   * @throws CloudflareApiException if the zone cannot be found or accessed
   */
  RecordOperations getRecord(String subdomain) throws CloudflareApiException;

  /**
   * Selects a record with specific types within the zone for further operations.
   * <p>The type filter does not restrict a subsequent {@link RecordOperations#delete(RecordType...)} call;
   * that method uses its own type arguments, and calling it without arguments deletes all record types
   * for the selected subdomain. A {@link CloudflareApiException} from an individual deletion is
   * logged and does not stop later attempts; see {@link RecordOperations#delete(RecordType...)}.
   *
   * @param subdomain the subdomain relative to the zone (e.g. "www"), or "@" for the zone apex
   * @param types     optional DNS record types to filter by
   * @return a RecordOperations instance for chaining record-specific operations
   * @throws CloudflareApiException if the zone cannot be found or accessed
   */
  RecordOperations getRecord(String subdomain, @Nullable RecordType... types) throws CloudflareApiException;

  /**
   * Lists all DNS records within the zone, optionally filtered by types.
   *
   * @param types optional DNS record types to filter by
   * @return a list of RecordEntity objects matching the criteria
   * @throws CloudflareApiException if an error occurs while retrieving records
   */
  java.util.List<codes.thischwa.cf.model.RecordEntity> list(@Nullable RecordType... types) throws CloudflareApiException;
}
