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
 *   <li>A locked status to indicate the immutability of the record.
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
  @Nullable
  private String zoneId;
  @Nullable
  private String zoneName;
  @Nullable
  private LocalDateTime modifiedOn;
  @Nullable
  private LocalDateTime createdOn;

  /**
   * Initializes a new instance of the RecordEntity class and invokes the parent constructor from
   * the AbstractEntity class. The RecordEntity class represents a DNS record entity within a
   * specific zone, encapsulating attributes such as type, name, content, TTL, and other related
   * metadata.
   */
  public RecordEntity() {
    super();
  }

  /**
   * Retrieves the name of the DNS record.
   * If the name contains a dot ('.'), only the substring before the first dot is returned.
   *
   * @return the name of the DNS record, potentially truncated before the first dot,
   * or the full name if no dot is present.
   */
  public String getName() {
    if (name != null) {
      int pos = name.indexOf('.');
      if (pos > 0) {
        return name.substring(0, pos);
      }
    }
    return name;
  }

  /**
   * Builds and returns a {@link RecordEntity} instance with the specified attributes.
   *
   * @param name    the name of the DNS record
   * @param type    the {@link RecordType} of the DNS record
   * @param ttl     the time-to-live (TTL) value for the DNS record
   * @param content the content of the DNS record, typically an IP address
   * @return a {@link RecordEntity} populated with the provided attributes
   */
  public static RecordEntity build(String name, RecordType type, Integer ttl, String content) {
    RecordEntity rec = new RecordEntity();
    rec.setName(name);
    rec.setType(type.getType());
    rec.setTtl(ttl);
    rec.setContent(content);
    return rec;
  }

  /**
   * Builds and returns a {@link RecordEntity} instance with the specified ID and content.
   *
   * @param id      the unique identifier for the DNS record
   * @param content the content of the DNS record, typically an IP address or other record data
   * @return a {@link RecordEntity} populated with the provided ID and content
   */
  public static RecordEntity build(String id, String content) {
    RecordEntity rec = new RecordEntity();
    rec.setId(id);
    rec.setContent(content);
    return rec;
  }

  /**
   * Builds and returns a {@link RecordEntity} instance with the specified attributes.
   *
   * @param id      the unique identifier for the DNS record
   * @param name    the name of the DNS record
   * @param type    the type of the DNS record, represented as a string (e.g., "A", "CNAME")
   * @param ttl     the time-to-live (TTL) value for the DNS record
   * @param content the content of the DNS record, typically an IP address or other record data
   * @return a {@link RecordEntity} populated with the provided attributes
   */
  public static RecordEntity build(String id, String name, String type, Integer ttl, String content) {
    RecordEntity rec = build(name, RecordType.valueOf(type), ttl, content);
    rec.setId(id);
    return rec;
  }
}
