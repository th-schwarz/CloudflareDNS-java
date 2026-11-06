package codes.thischwa.cf;

import codes.thischwa.cf.model.AbstractResponse;
import codes.thischwa.cf.model.ResponseResultInfo;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPatch;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpRequest;
import org.apache.hc.core5.util.Timeout;
import org.jetbrains.annotations.NotNull;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Abstract base class for creating HTTP clients to interact with the Cloudflare API. Provides
 * methods for handling GET and POST requests and includes utilities for constructing HTTP clients,
 * managing authentication, and handling JSON serialization.
 *
 * <p>One underlying HTTP client (with connection pooling) is shared for all requests. It is
 * released by {@link #close()}.
 */
@Slf4j
abstract class CfBasicHttpClient implements AutoCloseable {

  private final String baseUrl;
  private final CfDnsClientBuilder.CfAuth auth;
  private final ObjectMapper objectMapper;
  private final CloseableHttpClient httpClient;

  private record ResultWrapper(int statusCode, String responseBody) {
  }

  /**
   * Creates a new Cloudflare HTTP client with the specified base URL and authentication.
   *
   * @param baseUrl the base URL for the Cloudflare API
   * @param auth    the authentication mechanism to use
   */
  CfBasicHttpClient(@NotNull String baseUrl, @NotNull CfDnsClientBuilder.CfAuth auth) {
    this(baseUrl, auth, CfDnsClientBuilder.DEFAULT_CONNECT_TIMEOUT,
        CfDnsClientBuilder.DEFAULT_RESPONSE_TIMEOUT);
  }

  /**
   * Creates a new Cloudflare HTTP client with the specified base URL, authentication and timeouts.
   *
   * @param baseUrl         the base URL for the Cloudflare API
   * @param auth            the authentication mechanism to use
   * @param connectTimeout  the timeout for establishing a connection
   * @param responseTimeout the timeout for waiting for the response data
   */
  CfBasicHttpClient(@NotNull String baseUrl, @NotNull CfDnsClientBuilder.CfAuth auth,
                    @NotNull Duration connectTimeout, @NotNull Duration responseTimeout) {
    this.baseUrl = baseUrl;
    this.auth = auth;
    this.objectMapper = JsonConf.initObjectMapper();
    this.httpClient = createHttpClient(connectTimeout, responseTimeout);
  }

  /**
   * Releases the underlying HTTP client and its connections. The client must not be used anymore
   * afterwards.
   *
   * @throws IOException if an I/O error occurs while closing the HTTP client
   */
  @Override
  public void close() throws IOException {
    httpClient.close();
  }

  private CloseableHttpClient createHttpClient(Duration connectTimeout, Duration responseTimeout) {
    Timeout connect = Timeout.ofMilliseconds(connectTimeout.toMillis());
    Timeout response = Timeout.ofMilliseconds(responseTimeout.toMillis());
    ConnectionConfig connectionConfig = ConnectionConfig.custom()
        .setConnectTimeout(connect)
        .setSocketTimeout(response)
        .build();
    RequestConfig requestConfig = RequestConfig.custom()
        .setConnectionRequestTimeout(connect)
        .setResponseTimeout(response)
        .build();
    return HttpClients.custom()
        .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
            .setDefaultConnectionConfig(connectionConfig)
            .build())
        .setDefaultRequestConfig(requestConfig)
        .addRequestInterceptorFirst((request, context, execChain) -> {
          request.addHeader(HttpHeaders.ACCEPT_CHARSET, "UTF-8");
          request.addHeader(HttpHeaders.ACCEPT_ENCODING, "gzip");
          request.addHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
          request.addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
          if (request instanceof ClassicHttpRequest classicRequest) {
            auth.applyAuth(classicRequest);
          }
        })
        .build();
  }

  private <T extends AbstractResponse> T executeRequest(ClassicHttpRequest request,
                                                        Class<T> responseType)
      throws CloudflareApiException {
    String requestInfo = request.getMethod() + " " + request.getRequestUri();
    ResultWrapper result;
    try {
      result = httpClient.execute(request, (ClassicHttpResponse response) -> new ResultWrapper(
          response.getCode(),
          response.getEntity() == null ? ""
              : EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8)));
    } catch (IOException e) {
      throw new CloudflareApiException("Request failed: " + requestInfo, e);
    }

    boolean httpSuccess = result.statusCode >= 200 && result.statusCode < 300;
    T respObj;
    try {
      respObj = objectMapper.readValue(result.responseBody, responseType);
    } catch (JacksonException e) {
      if (!httpSuccess) {
        // e.g. an HTML error page of a proxy
        throw buildException(requestInfo + " failed with status code " + result.statusCode,
            result.statusCode, null);
      }
      throw new CloudflareApiException("Error processing JSON response of " + requestInfo, e);
    }

    ResponseResultInfo resultInfo = respObj.getResponseResultInfo();
    boolean apiSuccess = resultInfo != null && resultInfo.isSuccess();
    if (!httpSuccess || !apiSuccess) {
      List<ResponseResultInfo.Error> errors = resultInfo == null ? null : resultInfo.getErrors();
      String message = "API error";
      if (errors != null && !errors.isEmpty()) {
        message += ": " + errors.stream().map(Object::toString).collect(Collectors.joining("; "));
      }
      log.debug("{} failed with status code {}: {}", requestInfo, result.statusCode, message);
      throw buildException(message, result.statusCode, errors);
    }
    return respObj;
  }

  private static CloudflareApiException buildException(String message, int statusCode,
                                                       List<ResponseResultInfo.Error> errors) {
    if (statusCode == HttpStatus.SC_NOT_FOUND) {
      return new CloudflareNotFoundException(message, statusCode, errors);
    }
    return new CloudflareApiException(message, statusCode, errors);
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
    } catch (JacksonException e) {
      throw new CloudflareApiException("Error serializing JSON payload", e);
    }
  }

  private String buildUrl(String endpoint) {
    return baseUrl + endpoint;
  }
}
