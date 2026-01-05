/**
 * Fluent API interfaces and implementations for chainable DNS operations.
 *
 * <p>This package provides a fluent, chainable interface for interacting with Cloudflare DNS
 * records, making code more readable and concise.
 *
 * <p>Example usage:
 * <pre><code>
 * // Create a DNS record
 * client.zone("example.com")
 *       .record("api")
 *       .create(RecordType.A, "192.168.1.1", 60);
 *
 * // Get DNS records
 * List&lt;RecordEntity&gt; records = client.zone("example.com")
 *                                      .record("www", RecordType.A)
 *                                      .get();
 *
 * // Update a DNS record
 * client.zone("example.com")
 *       .record("api", RecordType.A)
 *       .update("192.168.1.2");
 *
 * // Delete DNS records
 * client.zone("example.com")
 *       .record("old-service")
 *       .delete(RecordType.A, RecordType.AAAA);
 * </code></pre>
 */

package codes.thischwa.cf.fluent;
