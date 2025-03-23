package codes.thischwa.cf.model;

import java.time.LocalDateTime;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Represents a DNS zone entity in the Cloudflare DNS system. <br>
 *
 * <p>This class encapsulates all relevant data and metadata associated with a zone, including but
 * not limited to the following attributes:
 *
 * <ul>
 *   <li>Zone name.
 *   <li>Development mode status.
 *   <li>Active and original name servers linked to the zone.
 *   <li>Timestamps indicating when the zone was created, modified, or activated.
 *   <li>Current operational status of the zone (e.g., active, inactive).
 *   <li>Boolean flag indicating whether the zone is paused.
 *   <li>Zone type, representing the nature of the DNS zone (e.g., full, partial).
 * </ul>
 *
 * <p>This class extends {@link AbstractEntity} to inherit basic entity properties and to provide a
 * consistent interface across domain models.
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ZoneEntity extends AbstractEntity {
  private String name;
  private Integer developmentMode;
  private Set<String> nameServers;
  private Set<String> originalNameServers;
  private LocalDateTime createdOn;
  private LocalDateTime modifiedOn;
  private LocalDateTime activatedOn;
  private String status;
  private Boolean paused;
  private String type;
}
