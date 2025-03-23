package codes.thischwa.cf;

import lombok.Getter;

/**
 * Enum CfRequest encapsulates various API endpoint paths for managing DNS zones and records in a
 * cohesive and reusable manner. Each enum constant represents a specific API request path.
 */
@Getter
public enum CfRequest {

  /**
   * Represents the API endpoint path for retrieving the list of DNS zones.
   */
  ZONE_LIST("/zones"),
  /**
   * Represents the API endpoint path for retrieving information about a specific DNS zone
   * by its name. The endpoint path supports a placeholder for the zone name, which needs
   * to be provided to construct the complete path.
   */
  ZONE_INFO("/zones?name=%s"),

  /**
   * Represents the API endpoint path for creating a new DNS record within a specific DNS zone.
   * The endpoint path includes a placeholder for the zone identifier, which needs to
   * be provided to construct the complete path.
   */
  RECORD_CREATE("/zones/%s/dns_records"),
  /**
   * Represents the API endpoint path for retrieving information about a DNS record within a specific
   * DNS zone by its name. The endpoint path includes placeholders for the zone identifier and
   * the record name, which need to be provided to construct the complete path.
   */
  RECORD_INFO_NAME("/zones/%s/dns_records?name=%s"),
  /**
   * Represents the API endpoint path for retrieving information about a DNS record within a specific
   * DNS zone by its name and type.
   * The endpoint path includes placeholders for the zone identifier, record name, and record type,
   * which need to be provided to construct the complete path.
   */
  RECORD_INFO_NAME_TYPE("/zones/%s/dns_records?name=%s&type=%s"),
  /**
   * Represents the API endpoint path for updating an existing DNS record within a specific DNS zone.
   * The endpoint path includes placeholders for the zone identifier and the record identifier,
   * which need to be provided to construct the complete path.
   */
  RECORD_UPDATE("/zones/%s/dns_records/%s"),
  /**
   * Represents the API endpoint path for deleting an existing DNS record within a specific DNS zone.
   * The endpoint path includes placeholders for the zone identifier and the record identifier,
   * which need to be provided to construct the complete path.
   */
  RECORD_DELETE("/zones/%s/dns_records/%s");

  private static final char varIdentification = '%';
  private final String path;

  CfRequest(String path) {
    this.path = path;
  }

  /**
   * Constructs the complete API endpoint path by formatting the base path with the provided
   * arguments.
   *
   * @param vars the arguments to format the path string with; these are typically specific
   *     identifiers or parameters required by the API endpoint.
   * @return the fully constructed API endpoint path as a string.
   */
  String buildPath(Object... vars) {
    long varCount = path.chars().filter(c -> c == varIdentification).count();
    if (varCount != vars.length) {
      throw new IllegalArgumentException(
          String.format(
              "The number of variables (%d) does not match the number of parameters (%d) in the path string: %s",
              vars.length, varCount, path));
    }
    return String.format(path, vars);
  }
}
