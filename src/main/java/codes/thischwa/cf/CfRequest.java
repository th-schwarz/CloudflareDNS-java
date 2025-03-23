package codes.thischwa.cf;

import lombok.Getter;

/**
 * Enum CfRequest encapsulates various API endpoint paths for managing DNS zones and records in a
 * cohesive and reusable manner. Each enum constant represents a specific API request path.
 */
@Getter
public enum CfRequest {

  // for handling zones
  ZONE_LIST("/zones"),
  ZONE_INFO("/zones?name=%s"),

  // for handling records
  RECORD_CREATE("/zones/%s/dns_records"),
  RECORD_INFO_NAME("/zones/%s/dns_records?name=%s"),
  RECORD_INFO_NAME_TYPE("/zones/%s/dns_records?name=%s&type=%s"),
  RECORD_UPDATE("/zones/%s/dns_records/%s"),
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
