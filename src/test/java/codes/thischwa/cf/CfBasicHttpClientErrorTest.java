package codes.thischwa.cf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import codes.thischwa.cf.model.ZoneEntity;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the error handling of {@link CfBasicHttpClient} against a local HTTP server.
 */
class CfBasicHttpClientErrorTest {

  private static final String ZONE_JSON = """
      {"success":true,"errors":[],"messages":[],
       "result_info":{"page":1,"per_page":50,"total_pages":1,"count":1,"total_count":1},
       "result":[{"id":"zone1","name":"example.com","created_on":"2025-01-20T08:56:34.736214Z"}]}
      """;

  private HttpServer server;
  private CfDnsClient client;
  private final AtomicInteger requestCount = new AtomicInteger();
  private volatile int status;
  private volatile String body;
  private volatile long delayMillis;

  @BeforeEach
  void setUp() throws IOException {
    server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    server.createContext("/", exchange -> {
      requestCount.incrementAndGet();
      if (delayMillis > 0) {
        try {
          Thread.sleep(delayMillis);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
      byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(status, bytes.length);
      try (OutputStream out = exchange.getResponseBody()) {
        out.write(bytes);
      }
    });
    server.start();
    client = new CfDnsClient(false, "http://localhost:" + server.getAddress().getPort(),
        new CfDnsClientBuilder.ApiTokenAuth("test-token"));
  }

  @AfterEach
  void tearDown() throws IOException {
    client.close();
    server.stop(0);
  }

  private void respond(int status, String body) {
    this.status = status;
    this.body = body;
  }

  @Test
  void apiErrorContainsStatusCodeAndErrors() {
    respond(400, """
        {"success":false,"errors":[{"code":1004,"message":"DNS Validation Error"}],"messages":[],"result":null}
        """);

    CloudflareApiException e = assertThrows(CloudflareApiException.class, () -> client.zoneList());

    assertEquals(CloudflareApiException.class, e.getClass());
    assertEquals(400, e.getStatusCode());
    assertEquals(1, e.getErrors().size());
    assertEquals(1004, e.getErrors().get(0).getCode());
    assertTrue(e.getMessage().contains("DNS Validation Error"), e.getMessage());
  }

  @Test
  void status404ThrowsNotFound() {
    respond(404, """
        {"success":false,"errors":[{"code":81044,"message":"Record does not exist."}],"messages":[],"result":null}
        """);
    ZoneEntity zone = new ZoneEntity();
    zone.setId("zone1");
    zone.setName("example.com");

    CloudflareApiException e = assertThrows(CloudflareApiException.class,
        () -> client.recordDelete(zone, "0123456789abcdef0123456789abcdef"));

    assertInstanceOf(CloudflareNotFoundException.class, e);
    assertEquals(404, e.getStatusCode());
    assertEquals(81044, e.getErrors().get(0).getCode());
  }

  @Test
  void nonJsonErrorResponseKeepsStatusCode() {
    respond(502, "<html><body>Bad Gateway</body></html>");

    CloudflareApiException e = assertThrows(CloudflareApiException.class, () -> client.zoneList());

    assertEquals(502, e.getStatusCode());
    assertTrue(e.getErrors().isEmpty());
    assertTrue(e.getMessage().contains("502"), e.getMessage());
  }

  @Test
  void successWithTimestampsAndReusedClient() throws CloudflareApiException {
    respond(200, ZONE_JSON);

    ZoneEntity zone = client.zoneGet("example.com");
    assertNotNull(zone);
    assertEquals("2025-01-20T08:56:34.736214Z", zone.getCreatedOn().toString());

    // the same client instance can be used for multiple requests
    assertEquals(1, client.zoneList().size());
    assertEquals(2, requestCount.get());
  }

  @Test
  void exceptionWithoutResponseHasNoStatusCode() {
    CloudflareApiException e = new CloudflareApiException("test");
    assertEquals(CloudflareApiException.NO_STATUS_CODE, e.getStatusCode());
    assertTrue(e.getErrors().isEmpty());
  }

  @Test
  void responseTimeoutIsApplied() throws Exception {
    respond(200, ZONE_JSON);
    delayMillis = 2000;
    try (CfDnsClient timeoutClient = new CfDnsClient(false,
        "http://localhost:" + server.getAddress().getPort(),
        new CfDnsClientBuilder.ApiTokenAuth("test-token"), CfDnsClient.RECORDS_PER_PAGE,
        CfDnsClientBuilder.DEFAULT_CONNECT_TIMEOUT, Duration.ofMillis(200))) {
      long start = System.nanoTime();
      CloudflareApiException e = assertThrows(CloudflareApiException.class, timeoutClient::zoneList);
      long elapsedMillis = (System.nanoTime() - start) / 1_000_000;
      assertInstanceOf(SocketTimeoutException.class, e.getCause());
      assertTrue(elapsedMillis < 1500, "timeout not applied, elapsed: " + elapsedMillis);
    } finally {
      delayMillis = 0;
    }
  }
}
