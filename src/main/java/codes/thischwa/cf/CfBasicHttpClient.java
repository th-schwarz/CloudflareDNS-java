package codes.thischwa.cf;

import codes.thischwa.cf.model.AbstractResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

  private final ObjectMapper objectMapper;

  CfBasicHttpClient(String baseUrl, String authEmail, String authKey)
      throws IllegalArgumentException {
    if (authEmail == null || authEmail.isBlank()) {
      throw new IllegalArgumentException("Authentication email must not be null or blank!");
    }
    if (authKey == null || authKey.isBlank()) {
      throw new IllegalArgumentException("Authentication key must not be null or blank!");
    }
    this.baseUrl = baseUrl;
    this.authEmail = authEmail;
    this.authKey = authKey;
    this.objectMapper = JsonConf.initObjectMapper();
  }

  private CloseableHttpClient createHttpClient() {
    return HttpClients.custom().addRequestInterceptorFirst((request, context, execChain) -> {
      request.addHeader(HttpHeaders.ACCEPT_CHARSET, "UTF-8");
      request.addHeader(HttpHeaders.ACCEPT_ENCODING, "gzip");
      request.addHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
      request.addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
      request.addHeader("X-Auth-Email", authEmail);
      request.addHeader("X-Auth-Key", authKey);
    }).build();
  }

  private <T extends AbstractResponse> T executeRequest(ClassicHttpRequest request,
                                                        Class<T> responseType)
      throws CloudflareApiException {
    String logUri = null;
    try (CloseableHttpClient client = createHttpClient()) {
      ResultWrapper result = client.execute(request,
          (ClassicHttpResponse response) -> new ResultWrapper(response.getCode(),
              EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8)));


      T respObj = objectMapper.readValue(result.responseBody, responseType);
      if (!respObj.getResponseResultInfo().isSuccess()) {
        log.error("API error.");
        respObj.getResponseResultInfo().getErrors().forEach(e -> log.error("  - {}", e.toString()));
        throw new CloudflareApiException("API error.");
      }
      logUri = request.getRequestUri();
      if (result.statusCode >= 200 && result.statusCode < 300) {
        return respObj;
      } else {
        log.error("{} request failed for URL {}: Status {}", request.getMethod(), request.getUri(),
            result.statusCode);
        throw new CloudflareApiException(
            request.getMethod() + " request failed with status code: " + result.statusCode);
      }
    } catch (JsonProcessingException e) {
      log.error("JSON parsing error for request to {}", logUri, e);
      throw new CloudflareApiException("Error processing JSON response", e);
    } catch (Exception e) {
      throw new CloudflareApiException("Server error!", e);
    }
  }

  /**
   * Sends a GET request to the given endpoint and maps the response.
   */
  <T extends AbstractResponse> T getRequest(String endpoint, Class<T> responseType)
      throws CloudflareApiException {
    HttpGet request = new HttpGet(buildUrl(endpoint));
    return executeRequest(request, responseType);
  }

  /**
   * Sends a DELETE request to the given endpoint and maps the response.
   *
   * @param endpoint     the API endpoint path
   * @param responseType the expected response type class
   * @param <T>          the response type extending AbstractResponse
   * @return the parsed response object
   * @throws CloudflareApiException if an error occurs during the request
   */
  <T extends AbstractResponse> T deleteRequest(String endpoint, Class<T> responseType) throws CloudflareApiException {
    HttpDelete request = new HttpDelete(buildUrl(endpoint));
    return executeRequest(request, responseType);
  }


  /**
   * Sends a POST request with a payload to the given endpoint and maps the response.
   */
  <T extends AbstractResponse> T postRequest(String endpoint,
                                             Object requestPayload,
                                             Class<T> responseType)
      throws CloudflareApiException {
    HttpPost request = new HttpPost(buildUrl(endpoint));
    setRequestPayload(request, requestPayload);
    return executeRequest(request, responseType);
  }

  /**
   * Sends a PUT request with a payload to the given endpoint and maps the response.
   */
  <T extends AbstractResponse> T putRequest(String endpoint,
                                            Object requestPayload,
                                            Class<T> responseType)
      throws CloudflareApiException {
    HttpPut request = new HttpPut(buildUrl(endpoint));
    setRequestPayload(request, requestPayload);
    return executeRequest(request, responseType);
  }

  /**
   * Sends a PATCH request with a payload to the given endpoint and maps the response.
   *
   * @param endpoint       the API endpoint path
   * @param requestPayload the payload to send
   * @param responseType   the expected response type class
   * @param <T>            the response type extending AbstractResponse
   * @return the parsed response object
   * @throws CloudflareApiException if an error occurs during the request
   */
  <T extends AbstractResponse> T patchRequest(String endpoint,
                                              Object requestPayload,
                                              Class<T> responseType)
      throws CloudflareApiException {
    HttpPatch request = new HttpPatch(buildUrl(endpoint));
    setRequestPayload(request, requestPayload);
    return executeRequest(request, responseType);
  }

  /**
   * Sets the JSON payload for a request.
   */
  private void setRequestPayload(BasicClassicHttpRequest request,
                                 Object requestPayload)
      throws CloudflareApiException {
    try {
      String jsonPayload = objectMapper.writeValueAsString(requestPayload);
      log.trace("Request methode [{}] payload: {}", request.getMethod(), jsonPayload);
      request.setEntity(new StringEntity(jsonPayload,
          ContentType.APPLICATION_JSON));
    } catch (JsonProcessingException e) {
      throw new CloudflareApiException("Error serializing JSON payload", e);
    }
  }

  private String buildUrl(String endpoint) {
    return baseUrl + endpoint;
  }

  private record ResultWrapper(int statusCode, String responseBody) {
  }
}
