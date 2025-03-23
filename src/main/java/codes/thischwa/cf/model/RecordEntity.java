package codes.thischwa.cf.model;

import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a DNS record entity within a specific zone.
 *
 * <p>Attributes defined in this class include:
 *
 * <ul>
 *   <li>DNS record type such as "A" or "CNAME".
 *   <li>Name of the DNS record.
 *   <li>Content of the DNS record, such as an IP address.
 *   <li>Flags indicating whether the record is proxiable or proxied.
 *   <li>TTL (Time-To-Live) for the DNS record.
 *   <li>A locked status to indicate immutability of the record.
 *   <li>Zone-specific metadata including zone ID and name.
 *   <li>Timestamps for creation and modification.
 * </ul>
 *
 * <p>Provides a static factory method {@code build} for creating a DNS record with specific
 * attributes.
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class RecordEntity extends AbstractEntity {
  private String type;
  private String name;
  private String content;
  private Boolean proxiable;
  private Boolean proxied;
  private Integer ttl;
  private Boolean locked;
  @Nullable private String zoneId;
  @Nullable private String zoneName;
  @Nullable private LocalDateTime modifiedOn;
  @Nullable private LocalDateTime createdOn;

  /**
   * Builds and returns a {@link RecordEntity} instance with the specified attributes.
   *
   * @param name the name of the DNS record
   * @param type the {@link RecordType} of the DNS record
   * @param ttl the time-to-live (TTL) value for the DNS record
   * @param ip the content of the DNS record, typically an IP address
   * @return a {@link RecordEntity} populated with the provided attributes
   */
  public static RecordEntity build(String name, RecordType type, Integer ttl, String ip) {
    RecordEntity rec = new RecordEntity();
    rec.setName(name);
    rec.setType(type.getType());
    rec.setTtl(ttl);
    rec.setContent(ip);
    return rec;
  }
}
