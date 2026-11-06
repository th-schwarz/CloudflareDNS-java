package codes.thischwa.cf.fluent;

import codes.thischwa.cf.CfDnsClient;
import codes.thischwa.cf.CloudflareApiException;
import codes.thischwa.cf.model.RecordEntity;
import codes.thischwa.cf.model.RecordType;
import codes.thischwa.cf.model.ZoneEntity;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Implementation of RecordOperations for fluent API access to record-level operations.
 */
public class RecordOperationsImpl implements RecordOperations {

  private final CfDnsClient client;
  private final ZoneEntity zone;
  private final String subdomain;
  private final RecordType[] types;

  /**
   * Constructs a RecordOperationsImpl instance.
   *
   * @param client    the CfDnsClient instance to use for operations
   * @param zone      the ZoneEntity representing the DNS zone
   * @param subdomain the subdomain relative to the zone, or "@" for the zone apex
   * @param types     optional array of RecordType to filter by
   */
  public RecordOperationsImpl(CfDnsClient client, ZoneEntity zone, String subdomain, @Nullable RecordType[] types) {
    this.client = client;
    this.zone = zone;
    this.subdomain = subdomain;
    this.types = types;
  }

  @Override
  public List<RecordEntity> get() throws CloudflareApiException {
    return client.recordListBySubdomain(zone, subdomain, types);
  }

  @Override
  public RecordEntity create(RecordType type, String content, int ttl) throws CloudflareApiException {
    return client.recordCreateSubdomain(zone, subdomain, ttl, type, content);
  }

  @Override
  public RecordEntity update(String newContent) throws CloudflareApiException {
    List<RecordEntity> recs = get();
    if (recs.isEmpty()) {
      throw new CloudflareApiException("No recs found to update for subdomain: " + subdomain);
    }
    if (recs.size() > 1) {
      throw new CloudflareApiException("Multiple recs found. Please use recordUpdate() directly for precise control.");
    }
    // partial update: only the content is changed
    return client.recordUpdate(zone, RecordEntity.build(recs.get(0).getId(), newContent));
  }

  @Override
  public void delete(RecordType... types) throws CloudflareApiException {
    client.recordDeleteTypeIfExists(zone, subdomain, types);
  }
}
