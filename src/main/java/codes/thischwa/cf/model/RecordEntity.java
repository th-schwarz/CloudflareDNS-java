package codes.thischwa.cf.model;

import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a DNS getRecord entity within a specific zone.
 *
 * <p>Attributes defined in this class include:
 *
 * <ul>
 *   <li>DNS getRecord type such as "A" or "CNAME".
 *   <li>Name of the DNS getRecord.
 *   <li>Content of the DNS getRecord, such as an IP address.
 *   <li>Flags indicating whether the getRecord is proxiable or proxied.
 *   <li>TTL (Time-To-Live) for the DNS getRecord.
 *   <li>A locked status to indicate the immutability of the getRecord.
 *   <li>Zone-specific metadata including zone ID and name.
 *   <li>Timestamps for creation and modification.
 * </ul>
 *
 * <p>Provides a static factory method {@code build} for creating a DNS getRecord with specific
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
   * the AbstractEntity class. The RecordEntity class represents a DNS getRecord entity within a
   * specific zone, encapsulating attributes such as type, name, content, TTL, and other related
   * metadata.
   */
  public RecordEntity() {
    super();
  }

  /**
   * Builds and returns a {@link RecordEntity} instance with the specified attributes.
   *
   * @param name    the name of the DNS getRecord
   * @param type    the {@link RecordType} of the DNS getRecord
   * @param ttl     the time-to-live (TTL) value for the DNS getRecord
   * @param content the content of the DNS getRecord, typically an IP address
   * @return a {@link RecordEntity} populated with the provided attributes
   */
  public static RecordEntity build(String name, RecordType type, Integer ttl, String content) {
    RecordEntity rec = new RecordEntity();
    rec.name = name;
    rec.type = type.getType();
    rec.ttl = ttl;
    rec.content = content;
    return rec;
  }

  /**
   * Builds and returns a {@link RecordEntity} instance with the specified ID and content.
   *
   * @param id      the unique identifier for the DNS getRecord
   * @param content the content of the DNS getRecord, typically an IP address or other getRecord data
   * @return a {@link RecordEntity} populated with the provided ID and content
   */
  public static RecordEntity build(String id, String content) {
    RecordEntity rec = new RecordEntity();
    rec.setId(id);
    rec.content = content;
    return rec;
  }

  /**
   * Builds and returns a {@link RecordEntity} instance with the specified attributes.
   *
   * @param id      the unique identifier for the DNS getRecord
   * @param name    the name of the DNS getRecord
   * @param type    the type of the DNS getRecord, represented as a string (e.g., "A", "CNAME")
   * @param ttl     the time-to-live (TTL) value for the DNS getRecord
   * @param content the content of the DNS getRecord, typically an IP address or other getRecord data
   * @return a {@link RecordEntity} populated with the provided attributes
   * @throws IllegalArgumentException if the type string is not a valid RecordType
   */
  public static RecordEntity build(String id, String name, String type, Integer ttl, String content) {
    RecordType recordType;
    try {
      recordType = RecordType.valueOf(type);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid getRecord type: " + type + ". Must be one of: "
          + java.util.Arrays.toString(RecordType.values()), e);
    }
    RecordEntity rec = new RecordEntity();
    rec.setId(id);
    rec.name = name;
    rec.type = recordType.getType();
    rec.ttl = ttl;
    rec.content = content;
    return rec;
  }

  /**
   * Retrieves the short name (subdomain) of the DNS getRecord.
   * If the name contains a dot ('.'), only the substring before the first dot is returned.
   * This is useful for getting the subdomain part of a fully qualified domain name.
   *
   * @return the short name of the DNS getRecord (substring before the first dot),
   *     or the full name if no dot is present
   */
  public String getSld() {
    if (name == null) {
      return null;
    }

    if (zoneName != null && name.endsWith(zoneName)) {
      int zoneNameLength = zoneName.length();
      int dotSeparatorLength = 1;
      return name.substring(0, name.length() - zoneNameLength - dotSeparatorLength);
    }

    int firstDotPosition = name.indexOf('.');
    if (firstDotPosition > 0) {
      return name.substring(0, firstDotPosition);
    }

    return name;
  }
}