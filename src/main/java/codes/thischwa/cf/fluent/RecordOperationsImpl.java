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
  private final String sld;
  private final RecordType[] types;

  /**
   * Constructs a RecordOperationsImpl instance.
   *
   * @param client the CfDnsClient instance to use for operations
   * @param zone   the ZoneEntity representing the DNS zone
   * @param sld    the subdomain (second-level domain) name
   * @param types  optional array of RecordType to filter by
   */
  public RecordOperationsImpl(CfDnsClient client, ZoneEntity zone, String sld, @Nullable RecordType[] types) {
    this.client = client;
    this.zone = zone;
    this.sld = sld;
    this.types = types;
  }

  @Override
  public List<RecordEntity> get() throws CloudflareApiException {
    return client.recordList(zone, sld, types);
  }

  @Override
  public RecordEntity create(RecordType type, String content, int ttl) throws CloudflareApiException {
    return client.recordCreateSld(zone, sld, ttl, type, content);
  }

  @Override
  public RecordEntity update(String newContent) throws CloudflareApiException {
    List<RecordEntity> records = get();
    if (records.isEmpty()) {
      throw new CloudflareApiException("No records found to update for subdomain: " + sld);
    }
    if (records.size() > 1) {
      throw new CloudflareApiException("Multiple records found. Please use recordUpdate() directly for precise control.");
    }
    RecordEntity record = records.get(0);
    record.setContent(newContent);
    return client.recordUpdate(zone, record);
  }

  @Override
  public void delete(RecordType... types) throws CloudflareApiException {
    client.recordDeleteTypeIfExists(zone, sld, types);
  }
}
