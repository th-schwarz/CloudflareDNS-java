package codes.thischwa.cf.fluent;

import codes.thischwa.cf.CloudflareApiException;
import codes.thischwa.cf.model.RecordEntity;
import codes.thischwa.cf.model.RecordType;
import java.util.List;

/**
 * Fluent interface for record-level operations.
 * Provides a chainable API for CRUD operations on DNS records.
 */
public interface RecordOperations {

  /**
   * Retrieves DNS records for the selected subdomain.
   *
   * @return a list of RecordEntity objects matching the criteria
   * @throws CloudflareApiException if an error occurs while retrieving records
   */
  List<RecordEntity> get() throws CloudflareApiException;

  /**
   * Creates a new DNS record with the specified parameters.
   *
   * @param type    the DNS record type (e.g., A, AAAA, CNAME)
   * @param content the content of the DNS record (e.g., IP address)
   * @param ttl     the time-to-live value in seconds
   * @return the created RecordEntity
   * @throws CloudflareApiException if an error occurs while creating the record
   */
  RecordEntity create(RecordType type, String content, int ttl) throws CloudflareApiException;

  /**
   * Updates an existing DNS record with new content.
   *
   * @param newContent the new content for the DNS record
   * @return the updated RecordEntity
   * @throws CloudflareApiException if an error occurs while updating the record
   */
  RecordEntity update(String newContent) throws CloudflareApiException;

  /**
   * Deletes DNS records of the specified types.
   *
   * @param types the DNS record types to delete
   * @throws CloudflareApiException if an error occurs while deleting records
   */
  void delete(RecordType... types) throws CloudflareApiException;
}
