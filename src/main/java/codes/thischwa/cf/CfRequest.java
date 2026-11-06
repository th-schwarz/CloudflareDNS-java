package codes.thischwa.cf;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
   * Represents the API endpoint path for retrieving information about a specific DNS zone by its
   * name. The endpoint path supports a placeholder for the zone name, which needs to be provided to
   * construct the complete path.
   */
  ZONE_INFO("/zones?name={value}"),

  /**
   * Represents the API endpoint path for retrieving information about DNS records within a
   * specific DNS zone. The endpoint path includes a placeholder for the zone identifier, which
   * needs to be provided to construct the complete path.
   */
  RECORD_LIST("/zones/{id}/dns_records"),
  /**
   * Represents the API endpoint path for retrieving information about a DNS record within a
   * specific DNS zone by its name. The endpoint path includes placeholders for the zone identifier
   * and the record name, which need to be provided to construct the complete path.
   */
  RECORD_LIST_NAME("/zones/{id}/dns_records?name={value}"),
  /**
   * Represents the API endpoint path for creating a new DNS record within a specific DNS zone. The
   * endpoint path includes a placeholder for the zone identifier, which needs to be provided to
   * construct the complete path.
   */
  RECORD_CREATE("/zones/{id}/dns_records"),
  /**
   * Represents the API endpoint path for updating an existing DNS record within a specific DNS
   * zone. The endpoint path includes placeholders for the zone identifier and the record
   * identifier, which need to be provided to construct the complete path.
   */
  RECORD_UPDATE("/zones/{id}/dns_records/{id}"),
  /**
   * Represents the API endpoint path for performing batch operations on DNS records within a specific zone.
   * The placeholder in the path is intended to be replaced by a zone identifier.
   * This constant is used to construct the URL for interacting with the batch DNS records API.
   */
  RECORD_BATCH("/zones/{id}/dns_records/batch"),
  /**
   * Represents the API endpoint path for deleting an existing DNS record within a specific DNS
   * zone. The endpoint path includes placeholders for the zone identifier and the record
   * identifier, which need to be provided to construct the complete path.
   */
  RECORD_DELETE("/zones/{id}/dns_records/{id}");

  private static final String ID_PLACEHOLDER = "{id}";
  private static final Pattern PLACEHOLDER = Pattern.compile("\\{(id|value)}");

  /**
   * Allowed characters of Cloudflare identifiers (zone and record IDs). Prevents e.g. path
   * traversal with IDs like "../..".
   */
  private static final Pattern ID_PATTERN = Pattern.compile("[A-Za-z0-9]{1,64}");

  private final String path;

  CfRequest(String path) {
    this.path = path;
  }

  /**
   * Constructs the complete API endpoint path by replacing the placeholders with the provided
   * arguments. Identifiers ({@code {id}}) are validated, query values ({@code {value}}) are
   * URL-encoded.
   *
   * @param vars the arguments to replace the placeholders with, in the order of the placeholders
   * @return the fully constructed API endpoint path as a string.
   * @throws IllegalArgumentException if the number of arguments doesn't match the placeholders, an
   *                                  argument is {@code null} or an identifier is invalid
   */
  String buildPath(Object... vars) {
    Matcher matcher = PLACEHOLDER.matcher(path);
    long varCount = matcher.results().count();
    if (varCount != vars.length) {
      throw new IllegalArgumentException(
          String.format(
              "The number of variables (%d) does not match the number of parameters (%d) in the path string: %s",
              vars.length, varCount, path));
    }
    matcher.reset();
    StringBuilder sb = new StringBuilder();
    int i = 0;
    while (matcher.find()) {
      Object arg = vars[i++];
      if (arg == null) {
        throw new IllegalArgumentException("Path variable must not be null: " + path);
      }
      String value = arg.toString();
      String replacement;
      if (ID_PLACEHOLDER.equals(matcher.group())) {
        if (!ID_PATTERN.matcher(value).matches()) {
          throw new IllegalArgumentException("Invalid identifier: " + value);
        }
        replacement = value;
      } else {
        replacement = URLEncoder.encode(value, StandardCharsets.UTF_8);
      }
      matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
    }
    matcher.appendTail(sb);
    return sb.toString();
  }
}
