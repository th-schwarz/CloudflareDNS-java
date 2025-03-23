package codes.thischwa.cf;

import codes.thischwa.cf.model.AbstractEntity;
import codes.thischwa.cf.model.AbstractResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPatch;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpRequest;

/**
 * Abstract base class for creating HTTP clients to interact with the Cloudflare API. Provides
 * methods for handling GET and POST requests and includes utilities for constructing HTTP clients,
 * managing authentication, and handling JSON serialization.
 */
@Slf4j
abstract class CfBasicHttpClient {
  private final String baseUrl;
  private final String authEmail;
  private final String authKey;
  private final String authToken;

  private final ObjectMapper objectMapper;

  CfBasicHttpClient(String baseUrl, String authEmail, String authKey, String authToken) {
    this.baseUrl = baseUrl;
    this.authEmail = authEmail;
    this.authKey = authKey;
    this.authToken = authToken;
    this.objectMapper = initObjectMapper();
  }

  private ObjectMapper initObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    return objectMapper;
  }

  private CloseableHttpClient createHttpClient() {
    return HttpClients.custom()
        .addRequestInterceptorFirst(
            (request, context, execChain) -> {
              request.addHeader(HttpHeaders.ACCEPT_CHARSET, "UTF-8");
              request.addHeader(HttpHeaders.ACCEPT_ENCODING, "gzip");
              request.addHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
              request.addHeader(
                  HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
              request.addHeader("X-Auth-Email", authEmail);
              request.addHeader("X-Auth-Key", authKey);
              request.addHeader("X-Auth-Token", authToken);
            })
        .build();
  }

  private <T extends AbstractResponse> T executeRequest(
      ClassicHttpRequest request, Class<T> responseType) throws CloudflareApiException {
    String logUri = null;
    try (CloseableHttpClient client = createHttpClient()) {
      ResultWrapper result =
          client.execute(
              request,
              (ClassicHttpResponse response) ->
                  new ResultWrapper(
                      response.getCode(),
                      EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8)));

      logUri = request.getRequestUri();
      if (result.statusCode >= 200 && result.statusCode < 300) {
        return objectMapper.readValue(result.responseBody, responseType);
      } else {
        log.error(
            "{} request failed for URL {}: Status {}",
            request.getMethod(),
            request.getUri(),
            result.statusCode);
        throw new CloudflareApiException(
            request.getMethod() + " request failed with status code: " + result.statusCode);
      }
    } catch (JsonProcessingException e) {
      log.error("JSON parsing error for request to {}", logUri, e);
      throw new CloudflareApiException("Error processing JSON response", e);
    } catch (Exception e) {
      log.error("Error during request execution", e);
      throw new CloudflareApiException("Request failed", e);
    }
  }

  /** Sends a GET request to the given endpoint and maps the response. */
  <T extends AbstractResponse> T getRequest(String endpoint, Class<T> responseType)
      throws CloudflareApiException {
    HttpGet request = new HttpGet(buildUrl(endpoint));
    return executeRequest(request, responseType);
  }

  /** Sends a DELETE request to the given endpoint and maps the response. */
  <T extends AbstractResponse> T deleteRequest(String endpoint)
      throws CloudflareApiException {
    HttpDelete request = new HttpDelete(buildUrl(endpoint));
    return executeRequest(request, (Class<T>) codes.thischwa.cf.model.RecordSingleResponse.class);
  }

  /** Sends a POST request with a payload to the given endpoint and maps the response. */
  <T extends AbstractResponse, R extends AbstractEntity> T postRequest(
      String endpoint, R requestPayload) throws CloudflareApiException {
    HttpPost request = new HttpPost(buildUrl(endpoint));
    setRequestPayload(request, requestPayload);
    return executeRequest(request, (Class<T>) codes.thischwa.cf.model.RecordSingleResponse.class);
  }

  /** Sends a PUT request with a payload to the given endpoint and maps the response. */
  <T extends AbstractResponse, R extends AbstractEntity> T putRequest(
      String endpoint, R requestPayload, Class<T> responseType) throws CloudflareApiException {
    HttpPut request = new HttpPut(buildUrl(endpoint));
    setRequestPayload(request, requestPayload);
    return executeRequest(request, responseType);
  }

  /** Sends a PATCH request with a payload to the given endpoint and maps the response. */
  <T extends AbstractResponse, R extends AbstractEntity> T patchRequest(
      String endpoint, R requestPayload) throws CloudflareApiException {
    HttpPatch request = new HttpPatch(buildUrl(endpoint));
    setRequestPayload(request, requestPayload);
    return executeRequest(request, (Class<T>) codes.thischwa.cf.model.RecordSingleResponse.class);
  }

  /** Sets the JSON payload for a request. */
  private <R extends AbstractEntity> void setRequestPayload(
      BasicClassicHttpRequest request, R requestPayload) throws CloudflareApiException {
    try {
      request.setEntity(
          new StringEntity(
              objectMapper.writeValueAsString(requestPayload), ContentType.APPLICATION_JSON));
    } catch (JsonProcessingException e) {
      throw new CloudflareApiException("Error serializing JSON payload", e);
    }
  }

  private String buildUrl(String endpoint) {
    return baseUrl + endpoint;
  }

  private record ResultWrapper(int statusCode, String responseBody) {}
}
