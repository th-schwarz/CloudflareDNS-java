package codes.thischwa.cf.fluent;

import codes.thischwa.cf.CfDnsClient;
import codes.thischwa.cf.CloudflareApiException;
import codes.thischwa.cf.model.RecordEntity;
import codes.thischwa.cf.model.RecordType;
import codes.thischwa.cf.model.ZoneEntity;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Implementation of ZoneOperations for fluent API access to zone-level operations.
 */
public class ZoneOperationsImpl implements ZoneOperations {

  private final CfDnsClient client;
  private final ZoneEntity zone;

  /**
   * Constructs a ZoneOperationsImpl instance.
   *
   * @param client the CfDnsClient instance to use for operations
   * @param zone   the ZoneEntity representing the DNS zone
   */
  public ZoneOperationsImpl(CfDnsClient client, ZoneEntity zone) {
    this.client = client;
    this.zone = zone;
  }

  @Override
  public RecordOperations record(String sld) throws CloudflareApiException {
    return new RecordOperationsImpl(client, zone, sld, null);
  }

  @Override
  public RecordOperations record(String sld, @Nullable RecordType... types) throws CloudflareApiException {
    return new RecordOperationsImpl(client, zone, sld, types);
  }

  @Override
  public List<RecordEntity> list(@Nullable RecordType... types) throws CloudflareApiException {
    return client.recordList(zone, types);
  }
}
