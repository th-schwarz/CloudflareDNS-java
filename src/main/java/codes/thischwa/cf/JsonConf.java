package codes.thischwa.cf;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

/**
 * The JsonConf class provides a utility method for initializing and configuring a shared
 * {@link ObjectMapper} instance for JSON serialization and deserialization.
 */
class JsonConf {

  private JsonConf() {
  }

  static ObjectMapper initObjectMapper() {
    return JsonMapper.builder()
        .changeDefaultPropertyInclusion(
            incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
        .build();
  }
}
