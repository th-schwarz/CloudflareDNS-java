# CloudflareDNS-java

[![Build and Analyse](https://git.mein-gateway.de/thischwa/CloudflareDNS-java/actions/workflows/build-and-analyse.yml/badge.svg?branch=develop)](https://git.mein-gateway.de/thischwa/CloudflareDNS-java/actions?workflow=build-and-analyse.yml)

[Test report](reports/test.md)

## Preface

This project provides a java client for minimalistic access to the Cloudflare API version 4, which is mainly used for
managing DNS settings such as creating, updating and deleting DNS records.

If you encounter any bugs or find missing features, feel free to report them on
the [GitHub Issues page](https://github.com/th-schwarz/CloudflareDNS-java/issues).

---

## Disclaimer

This guide comes without any warranty. Use at your own risk. The author is not responsible for potential data loss, hardware damage or keyboard mishaps!

---

## Get It

Java 17 or higher is required.

The project has its own maven repository. Follow the instructions on the latest [package](https://git.mein-gateway.de/thischwa/-/packages/maven/codes.thischwa:cloudflaredns) to add the repository to your project.

## Changelog

See [changelog](changelog.md)

## Methods Overview

The methods can be categorized as follows:

- `Zone`: list, info
- `Record`: list, info, create, update, delete, batch

The API provides two styles for working with DNS records:

1. **Traditional API**: Direct method calls with explicit parameters
2. **Fluent API**: Chainable method calls for more readable code

The following text focuses on the basic methods. For further information, take a look at
the \
[![javadoc](https://javadoc.mein-gateway.de/badge/CloudflareDNS-java)](https://javadoc.mein-gateway.de/CloudflareDNS-java/index.html)

### Instantiation of `CfDnsClient`

#### With API Token (recommended):
```java
CfDnsClient cfDnsClient = new CfDnsClientBuilder()
    .withApiTokenAuth("your-api-token")
    .build();
```

#### With Email/Key (legacy):
```java
CfDnsClient cfDnsClient = new CfDnsClientBuilder()
    .withEmailKeyAuth("email@example.com", "yourApiKey")
    .build();
```

#### Further options:
```java
CfDnsClient cfDnsClient = new CfDnsClientBuilder()
    .withApiTokenAuth("your-api-token")
    .withBaseUrl("https://custom-api.example.com/client/v4") // HTTPS required; HTTP allowed only for loopback hosts
    .withConnectTimeout(Duration.ofSeconds(5))               // default: 10 s
    .withResponseTimeout(Duration.ofSeconds(15))             // default: 30 s
    .withEmptyResultThrowsException(true)                    // default: false
    .build();
```

`build()` throws an `IllegalStateException` if no authentication is configured.

The client is thread-safe and should be reused, it holds a pool of HTTP connections.
`CfDnsClient` implements `AutoCloseable`, call `close()` (or use try-with-resources) to release the connections:

```java
try (CfDnsClient cfDnsClient = new CfDnsClientBuilder().withApiTokenAuth("your-api-token").build()) {
  // ...
}
```

### `zoneList`

Retrieve all zones within the Cloudflare account.

- **Returns**: A list of `ZoneEntity` objects.

```java
List<ZoneEntity> zones = cfDnsClient.zoneList();
zones.forEach(zone -> System.out.println("Zone: " + zone.getName()));
```

Without a `PagingRequest`, `zoneList()` fetches zones in pages of 50, requesting further pages as needed.
Use a `PagingRequest` to retrieve a specific page:

```java
List<ZoneEntity> zones = cfDnsClient.zoneList(PagingRequest.forZones(1, 50)); // page 1, 50 zones per page
```

Page numbers start at 1. `PagingRequest` is immutable; use `forRecords(page, perPage)` for DNS records and
`forZones(page, perPage)` for zones. Cloudflare accepts page sizes from 5 to 50 for zones and from
1 to 5,000,000 for DNS records. `PagingRequest.defaultPaging()` requests page 1 with 50 items; it
does not fetch additional pages. Use `next()` or `withPage(page)` to create another page request.

---

### `zoneGet`

Get detailed information about a specific zone by its name.

- **Parameters**:
    - `String name` - The zone name (e.g., "example.com").
- **Returns**: A `ZoneEntity` object.

```java
ZoneEntity zone = cfDnsClient.zoneGet("example.com");
System.out.println("Zone ID: " + zone.getId());
```

---

### `recordList`

Retrieve records for a given zone. There are two variants:

#### List by subdomain (`recordListBySubdomain`)
Retrieve all records for a specific subdomain under a given zone, optionally filtered by record types.

- **Parameters**:
    - `ZoneEntity zone` - The zone object.
    - `String subdomain` - The subdomain relative to the zone (e.g., "www" in "www.example.com"
      or "my.sub" in "my.sub.example.com"). Use `CfDnsClient.APEX` (`"@"`) for the zone apex.
    - `RecordType... types` - Optional record types to filter by. If not specified, returns all record types.
- **Returns**: A list of `RecordEntity` objects.

```java
// Get all records for a specific subdomain
List<RecordEntity> wwwRecords = cfDnsClient.recordListBySubdomain(zone, "www");

// Get only A records for a specific subdomain
List<RecordEntity> wwwARecords = cfDnsClient.recordListBySubdomain(zone, "www", RecordType.A);

// Get the records of the zone apex (e.g. "example.com")
List<RecordEntity> apexRecords = cfDnsClient.recordListBySubdomain(zone, CfDnsClient.APEX);
```

#### List by type (`recordListByType`)
Retrieve all records of a zone, optionally filtered by record types.

```java
// Get only A and AAAA records of a zone
List<RecordEntity> ipZoneRecords = cfDnsClient.recordListByType(zone, RecordType.A, RecordType.AAAA);
```

If exactly one record type is given, the filtering is done by Cloudflare, otherwise by the client.

#### List all records of a zone (`recordList`)

```java
// Get all records of a zone
List<RecordEntity> allZoneRecords = cfDnsClient.recordList(zone);

// Get the records of a zone page by page (page 2, 100 records per page)
List<RecordEntity> pagedRecords = cfDnsClient.recordList(zone, PagingRequest.forRecords(2, 100));
```

Without a `PagingRequest`, all pages are fetched using 1,000 records per page and combined into one result.
This large page size can consume substantial memory. If paging remains inconsistent after three attempts, the client logs a warning and may return an incomplete list. Use a smaller `PagingRequest` to retrieve one page at a time; further pages must be requested explicitly.
With a `PagingRequest`, exactly the requested page is returned.

`RecordEntity#getSubdomain()` returns the record name relative to its zone, e.g. `my.sub` for
`my.sub.example.com` and `"@"` for the zone apex. The timestamps `getCreatedOn()` and `getModifiedOn()`
are of type `java.time.Instant` (UTC).

#### Group by FQDN

The static helper `CfDnsClient.groupRecordsByFqdn` groups a list of records by their fully qualified domain name:

```java
Map<String, List<RecordEntity>> byFqdn = CfDnsClient.groupRecordsByFqdn(cfDnsClient.recordList(zone));
```

---

### `recordCreateSubdomain` / `recordCreate`

Create a new DNS record in a specific zone.

- **Parameters**:
  - `ZoneEntity zone` - The DNS zone object.
  - `String subdomain` - The subdomain of the new record relative to the zone (e.g., "api" in "api.example.com").
  - `int ttl` - The time-to-live in seconds of the new record.
  - `RecordType type` - The record type (e.g., A, AAAA, CNAME).
  - `String content` - The content of the record (e.g., the IP address of an A record).
- **Returns**: The created `RecordEntity`.

```java
RecordEntity created = cfDnsClient.recordCreateSubdomain(zone, "api", 60, RecordType.A, "192.168.1.1");
System.out.println("Created Record ID: " + created.getId());
```

Alternatively, the record can be created with its fully qualified name or from a `RecordEntity`:

```java
cfDnsClient.recordCreate(zone, "api.example.com", 60, RecordType.A, "192.168.1.1");
cfDnsClient.recordCreate(zone, RecordEntity.build("api.example.com", RecordType.A, 60, "192.168.1.1"));
```

Records which need more than a content, like MX, SRV or CAA records, can be created from a `RecordEntity`.
Besides `priority` and `data`, the attributes `proxied`, `comment` and `tags` are supported:

```java
// MX record with priority
RecordEntity mx = RecordEntity.build("example.com", RecordType.MX, 3600, "mail.example.com");
mx.setPriority(10);
cfDnsClient.recordCreate(zone, mx);

// SRV record with structured data (keys as in the Cloudflare API), the priority is part of the data
RecordEntity srv = RecordEntity.build("_sip._tcp.example.com", RecordType.SRV, 3600, null);
srv.setData(Map.of("priority", 10, "weight", 5, "port", 5060, "target", "sip.example.com"));
cfDnsClient.recordCreate(zone, srv);
```

---

### `recordUpdate`

Update an existing DNS record (partial update: attributes which are `null` remain unchanged).
Read-only attributes like timestamps are ignored, the given `RecordEntity` isn't modified.

- **Parameters**:
    - `ZoneEntity zone` - The zone that contains the record.
    - `RecordEntity rec` - Updated record data.

```java
record.setContent("192.168.1.2");
RecordEntity updated = cfDnsClient.recordUpdate(zone, record);
System.out.println("Updated Record: " + updated.getContent());
```

---

### `recordDelete`

Delete a DNS record from a zone.

- **Parameters**:
    - `ZoneEntity zone` - The parent zone.
    - `RecordEntity rec` - Record to delete.

```java
boolean isDeleted = cfDnsClient.recordDelete(zone, record);
System.out.println(isDeleted ? "Deletion successful." : "Deletion failed.");
```

If only the ID is known, the record can be deleted by its ID:

```java
cfDnsClient.recordDelete(zone, record.getId());
```

---

### `recordDeleteTypeIfExists`

Delete the DNS records of the given types if they exist.

- **Parameters**:
    - `ZoneEntity zone` - Target zone.
    - `String subdomain` - The subdomain relative to the zone.
    - `RecordType... types` - One or more record types.

```java
cfDnsClient.recordDeleteTypeIfExists(zone, "api", RecordType.A, RecordType.AAAA);
System.out.println("Deletion attempt completed.");
```

If no record types are specified, records of all types for the subdomain are selected. Each record deletion is attempted independently: a `CloudflareApiException` from one deletion is logged and later records are still processed. No per-record result is returned, so a normal return does not guarantee that every selected record was deleted. Errors while retrieving the records are still reported; no matching records are treated as a no-op.

### Batch Operations

Process multiple DNS record operations (POST, PUT, PATCH, DELETE) in a single batch request.

- **Parameters**:
    - `ZoneEntity zone` - The target zone.
  - `List<RecordEntity> postRecords` - Records to create (nullable). Can be built without IDs.
  - `List<RecordEntity> putRecords` - Records to fully replace (nullable). **Requires record IDs**.
  - `List<RecordEntity> patchRecords` - Records to partially update (nullable). **Requires record IDs**.
  - `List<RecordEntity> deleteRecords` - Records to delete (nullable). **Requires record IDs**.
- **Returns**: A `BatchEntry` object containing the processed records.

**Important**: For UPDATE (PATCH), REPLACE (PUT), and DELETE operations, you must first retrieve the existing records to obtain their IDs.

#### Batch Create (POST)

Create new records, IDs are not required:

```java
List<RecordEntity> newRecords = Arrays.asList(
    RecordEntity.build("cdn." + zone.getName(), RecordType.A, 60, "192.168.1.11"),
    RecordEntity.build("mail." + zone.getName(), RecordType.A, 60, "192.168.1.12")
);

BatchEntry result = cfDnsClient.recordBatch(zone, newRecords, null, null, null);
System.out.println("Created "+result.getPosts().size() +" records.");
```

#### Batch Update (PATCH)

Partially update existing records. **Record IDs are required** - fetch them first:

```java
// Step 1: Fetch existing records to get their IDs
List<RecordEntity> recordsToUpdate = new ArrayList<>();
recordsToUpdate.add(cfDnsClient.recordListBySubdomain(zone, "api",RecordType.A).get(0));
recordsToUpdate.add(cfDnsClient.recordListBySubdomain(zone, "cdn",RecordType.A).get(0));

// Step 2: Modify only the fields you want to update
recordsToUpdate.forEach(record ->record.setContent("192.168.2.10"));

// Step 3: Send batch update request
BatchEntry result = cfDnsClient.recordBatch(zone, null, null, recordsToUpdate, null);
System.out.println("Updated "+result.getPatches().size() +" records.");
```

#### Batch Replace (PUT)

Fully replace existing records. **Record IDs are required** - fetch them first:

```java
// Step 1: Fetch existing records to get their IDs
List<RecordEntity> recordsToReplace = new ArrayList<>();
recordsToReplace.add(cfDnsClient.recordListBySubdomain(zone, "mail",RecordType.A).get(0));
recordsToReplace.add(cfDnsClient.recordListBySubdomain(zone, "cdn",RecordType.A).get(0));

// Step 2: Modify all fields as needed (full replacement)
recordsToReplace.get(0).setContent("192.168.3.10");
recordsToReplace.get(0).setTtl(120);
recordsToReplace.get(1).setContent("192.168.3.11");
recordsToReplace.get(1).setTtl(120);

// Step 3: Send batch replace request
BatchEntry result = cfDnsClient.recordBatch(zone, null, recordsToReplace, null, null);
System.out.println("Replaced "+result.getPuts().size() +" records.");
```

#### Batch Delete

Delete existing records. **Record IDs are required** - fetch them first:

```java
// Step 1: Fetch existing records to get their IDs
List<RecordEntity> recordsToDelete = new ArrayList<>();
recordsToDelete.add(cfDnsClient.recordListBySubdomain(zone, "cdn",RecordType.A).get(0));
recordsToDelete.add(cfDnsClient.recordListBySubdomain(zone, "mail",RecordType.A).get(0));

// Step 2: Send batch delete request
BatchEntry result = cfDnsClient.recordBatch(zone, null, null, null, recordsToDelete);
System.out.println("Deleted "+recordsToDelete.size() +" records.");
```

#### Combined Batch Operations

Combine multiple operations in a single batch request:

```java
// Create new records (no IDs needed)
List<RecordEntity> newRecords = Arrays.asList(
        RecordEntity.build("new-api." + zone.getName(), RecordType.A, 60, "192.168.1.100")
    );

// Fetch existing records for update/delete (IDs required)
List<RecordEntity> recordsToUpdate = Arrays.asList(
    cfDnsClient.recordListBySubdomain(zone, "existing-api", RecordType.A).get(0)
);
recordsToUpdate.get(0).setContent("192.168.1.200");

List<RecordEntity> recordsToDelete = Arrays.asList(
    cfDnsClient.recordListBySubdomain(zone, "old-api", RecordType.A).get(0)
);

// Execute all operations in a single batch request
BatchEntry result = cfDnsClient.recordBatch(
    zone,
    newRecords,        // POST - create new records
    null,              // PUT - not used in this example
    recordsToUpdate,   // PATCH - update existing records
    recordsToDelete    // DELETE - remove records
);

System.out.println("Batch completed:");
System.out.println("  Created: "+result.getPosts().size());
System.out.println("  Updated: "+result.getPatches().size());
System.out.println("  Deleted: "+result.getDeletes().size());
```

---

## Fluent API

The fluent API provides a chainable, readable interface for DNS operations. It's an alternative to the traditional API
that reduces verbosity and improves code readability.

### Basic Usage

```java
// Create a DNS record
client.zone("example.com")
    .getRecord("api")
    .create(RecordType.A, "192.168.1.1",60);

// Get DNS records of a subdomain
List<RecordEntity> records = client.zone("example.com")
    .getRecord("www", RecordType.A)
    .get();

// Get all DNS records of a zone
List<RecordEntity> zoneRecords = client.zone("example.com")
    .list(RecordType.A, RecordType.AAAA);

// Update a DNS record
RecordEntity updated = client.zone("example.com")
    .getRecord("api", RecordType.A)
    .update("192.168.1.2");

// Delete DNS records
client.zone("example.com") 
    .getRecord("old-service")
    .delete(RecordType.A, RecordType.AAAA);
```

The types passed to `delete` determine which record types are selected; a type filter passed to `getRecord` does not restrict deletion. Calling `delete()` without type arguments selects all record types for that subdomain. A `CloudflareApiException` from an individual deletion is logged and does not stop later attempts; no per-record result is returned.

### Advantages of Fluent API

- **More Readable**: The chain of method calls reads like natural language
- **Less Verbose**: No need to pass zone objects between method calls
- **Type Safe**: Full compile-time type checking
- **IDE Friendly**: Excellent autocomplete support

### Complete Example

```java
CfDnsClient client = new CfDnsClientBuilder()
    .withApiTokenAuth("your-api-token")
    .build();

// Create a new record
client.zone("example.com")
    .getRecord("api")
    .create(RecordType.A, "192.168.100.1",60);

// Retrieve and verify
List<RecordEntity> records = client.zone("example.com")
    .getRecord("api", RecordType.A)
    .get();
System.out.println("IP: "+records.get(0).getContent());

// Update the record
client.zone("example.com")
     .getRecord("api",RecordType.A)
     .update("192.168.100.2");

// Clean up
client.zone("example.com")
    .getRecord("api")
    .delete(RecordType.A);
```

---

# Notes on Error Handling

The `CfDnsClient` provides internal error-handling mechanisms through exceptions. For example:
- `CloudflareApiException` is thrown for errors during API communication or invalid responses.
  `getStatusCode()` returns the HTTP status code (or `CloudflareApiException.NO_STATUS_CODE` if there was no response)
  and `getErrors()` the error codes and messages reported by Cloudflare.
- `CloudflareNotFoundException` is thrown if Cloudflare responds with HTTP status 404 (e.g. deleting a non-existing record).
- `CloudflareNotFoundException` is also thrown when the requested resource (single or multiple) is not found, if enabled via the `emptyResultThrowsException` flag during initialization. **Default is `false`**, meaning empty results will be returned without throwing an exception.

To enable exception throwing for empty results:

```java
CfDnsClient client = new CfDnsClientBuilder()
    .withApiTokenAuth("your-api-token")
    .withEmptyResultThrowsException(true)
    .build();
```

## Example:

```java
try {
  List<RecordEntity> records = cfDnsClient.recordListBySubdomain(zone, "www", RecordType.A);
  System.out.println("Record IP: "+records.get(0).getContent());
} catch (CloudflareApiException e) {
  if (e instanceof CloudflareNotFoundException) {
    log.warn("Subdomain not found: www");
  } else {
    log.error("Error while getting records of subdomain www (HTTP status {}, errors {})",
        e.getStatusCode(), e.getErrors(), e);
    throw e;
  }
}
```

---
 