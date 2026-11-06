package codes.thischwa.cf.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
 *   <li>Priority, required for MX and URI records (for SRV records it's part of the data).
 *   <li>Structured data, required for e.g. SRV, CAA, LOC or TLSA records instead of content.
 *   <li>An optional comment and tags.
 *   <li>A locked status to indicate the immutability of the record.
 *   <li>Zone-specific metadata including zone ID and name.
 *   <li>Timestamps for creation and modification (UTC).
 * </ul>
 *
 * <p>Provides a static factory method {@code build} for creating a DNS record with specific
 * attributes.
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class RecordEntity extends AbstractEntity {
  private static final String APEX = "@";

  private String type;
  private String name;
  private String content;
  private Boolean proxiable;
  private Boolean proxied;
  private Integer ttl;
  /**
   * Priority of the record, required for MX and URI records. For SRV records the priority is
   * part of {@link #data}.
   */
  @Nullable
  private Integer priority;
  /**
   * Comment of the record, only used by Cloudflare.
   */
  @Nullable
  private String comment;
  /**
   * Custom tags of the record in the form "name:value", only used by Cloudflare.
   */
  @Nullable
  private List<String> tags;
  /**
   * Structured content of the record, e.g. for SRV records the keys "priority", "weight", "port"
   * and "target".
   * The keys are the same as in the Cloudflare API.
   */
  @Nullable
  private Map<String, Object> data;
  private Boolean locked;
  @Nullable
  private String zoneId;
  @Nullable
  private String zoneName;
  @Nullable
  private Instant modifiedOn;
  @Nullable
  private Instant createdOn;

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
    rec.name = name;
    rec.type = type.getType();
    rec.ttl = ttl;
    rec.content = content;
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
    rec.content = content;
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
   * @throws IllegalArgumentException if the type string is not a valid RecordType
   */
  public static RecordEntity build(String id, String name, String type, Integer ttl, String content) {
    RecordType recordType;
    try {
      recordType = RecordType.valueOf(type);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid record type: " + type + ". Must be one of: "
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
   * Retrieves the subdomain of the DNS record, i.e. the record name relative to its zone.
   *
   * <p>If the zone name is known, the zone name is stripped from the record name, e.g.
   * {@code my.sub.example.com} results in {@code my.sub}. A record at the zone apex results in
   * {@code "@"}. If the zone name is unknown, only the substring before the first dot is returned.
   *
   * @return the short name of the DNS record, {@code "@"} for the zone apex, or the full name if
   *     no dot is present
   */
  @JsonIgnore
  @Nullable
  public String getSubdomain() {
    if (name == null) {
      return null;
    }

    if (zoneName != null && !zoneName.isEmpty()) {
      String lowerName = name.toLowerCase(Locale.ROOT);
      String lowerZone = zoneName.toLowerCase(Locale.ROOT);
      if (lowerName.equals(lowerZone)) {
        return APEX;
      }
      if (lowerName.endsWith("." + lowerZone)) {
        return name.substring(0, name.length() - zoneName.length() - 1);
      }
    }

    int firstDotPosition = name.indexOf('.');
    if (firstDotPosition > 0) {
      return name.substring(0, firstDotPosition);
    }

    return name;
  }

  /**
   * Retrieves the subdomain of the DNS record.
   *
   * @return the subdomain of the DNS record
   * @deprecated The name was misleading, use {@link #getSubdomain()} instead.
   */
  @Deprecated(since = "0.6.0", forRemoval = true)
  @JsonIgnore
  @Nullable
  public String getSld() {
    return getSubdomain();
  }
}
