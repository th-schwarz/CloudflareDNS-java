# CloudflareDNS-java

[![pipeline-badge](https://ci.codeberg.org/api/badges/16522/status.svg?events=push%2Cmanual%2Cpull_request%2Cpull_request_closed)](https://ci.codeberg.org/repos/16522)

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=th-schwarz_CloudflareDNS-java&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=th-schwarz_CloudflareDNS-java)   
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=th-schwarz_CloudflareDNS-java&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=th-schwarz_CloudflareDNS-java)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=th-schwarz_CloudflareDNS-java&metric=coverage)](https://sonarcloud.io/summary/new_code?id=th-schwarz_CloudflareDNS-java)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=th-schwarz_CloudflareDNS-java&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=th-schwarz_CloudflareDNS-java)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=th-schwarz_CloudflareDNS-java&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=th-schwarz_CloudflareDNS-java)

[![codeberg.png](docs/codeberg.png)](https://codeberg.org/th-schwarz/CloudflareDNS-java)

## Preface

This project provides a java client for minimalistic access to the Cloudflare API version 4, which is mainly used for
managing DNS settings such as creating, updating and deleting DNS records.

If you encounter any bugs or find missing features, feel free to report them on
the [Codeberg Issues page](https://codeberg.org/th-schwarz/CloudflareDNS-java/issues).

---

## Disclaimer

This guide comes without any warranty. Use at your own risk. The author is not responsible for potential data loss, hardware damage or keyboard mishaps!

---

## Get It

The project has its own maven repository. It can be added to the `pom.xml`:

```xml
<repositories>
  <repository>
      <id>gitea</id>
      <url>https://codeberg.org/api/packages/th-schwarz/maven</url>
  </repository>
</repositories>
```

The dependency is:
```xml
<dependency>
  <groupId>codes.thischwa</groupId>
  <artifactId>cloudflaredns</artifactId>
  <version>[version]</version>
</dependency>
```

## Changelog

- 0.3.0:
  - **Breaking Change**:
    - **New Fluent API**: Changed the initialization of the client(`new CfDnsClientBuilder().withApiTokenAuth("your-api-token").build()`)
  - Authentication with API token.
- 0.2.0:
  - **Breaking Change**: `emptyResultThrowsException` default changed from `true` to `false`. Now applies to both
    single and multiple result requests. Empty results will be returned by default without throwing exceptions.
    - API method names refactored for consistency: `zoneListAll` → `zoneList`, `zoneInfo` → `zoneGet`, `sldListAll` →
      `recordList`
    - RecordEntity getter methods renamed for clarity: `getName()` → `getSld()`
    - **New Fluent API**: Changed the initialization of the client(`new CfDnsClientBuilder().withApiTokenAuth("your-api-token").build()`) and added chainable method interface for more readable DNS operations (
      `client.zone().record()...`)
  - Code quality improvements: eliminated duplication in batch operations, improved type safety in HTTP methods,
    optimized string concatenation, removed mutable setters from CfDnsClient
    - Enhanced type validation in `RecordEntity.build()` with better error messages
    - CfClient#recordList must return multiple RecordEntries
    - add a missing source jar
    - ResponseResultInfo#Errors: wrong object structure
    - changing multiple records with put, post, patch and delete for dns-records
- 0.1.0:
    - refactored / extended tests
- 0.1.0-beta.3:
  - fixed json deserialization
  - added logging of api errors
- 0.1.0-beta.1: 1st runnable version

## Methods Overview

The methods can be categorized as follows:

- `Zone`: list, info
- `Record`: list, info, create, update, delete

The API provides two styles for working with DNS records:

1. **Traditional API**: Direct method calls with explicit parameters
2. **Fluent API**: Chainable method calls for more readable code

The following text focuses on the basic methods. For further information, take a look at
the [javadoc of the CfDnsClient](https://cloudflaredns-java-f4ee3a.gitlab.io/apidocs/codes/thischwa/cf/CfDnsClient.html).

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

### `zoneList`

Retrieve all zones within the Cloudflare account.

- **Returns**: A list of `ZoneEntity` objects.

```java
List<ZoneEntity> zones = cfDnsClient.zoneList();
zones.forEach(zone -> System.out.println("Zone: " + zone.getName()));
```

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

#### List by SLD
Retrieve all records for a specific second-level domain (SLD) under a given zone.

- **Parameters**:
    - `ZoneEntity zone` - The zone object.
    - `String sld` - Second-level domain (e.g., "www" in "www.example.com").
- **Returns**: A list of `RecordEntity` objects.

```java
List<RecordEntity> records = cfDnsClient.recordList(zone, "sld");
records.forEach(record ->
    System.out.println("Record Type: "+record.getType() 
        +", Value: "+record.getContent())
);
```

#### List by Zone (optional filtering)
Retrieve DNS record details for a zone or a specific SLD, optionally filtered by record types.

- **Parameters**:
    - `ZoneEntity zone` - The zone object.
    - `String sld` - (Optional) The second-level domain.
    - `RecordType... types` - Optional record types to filter by (e.g., A, CNAME). If not specified, returns all record types.
- **Returns**: A list of `RecordEntity` objects matching the criteria.

```java
// Get all records for a specific SLD
List<RecordEntity> allSldRecords = cfDnsClient.recordList(zone, "www");

// Get only A records for a specific SLD
List<RecordEntity> aSldRecords = cfDnsClient.recordList(zone, "www", RecordType.A);

// Get all records of a zone
List<RecordEntity> allZoneRecords = cfDnsClient.recordList(zone);

// Get only A and AAAA records of a zone
List<RecordEntity> ipZoneRecords = cfDnsClient.recordList(zone, RecordType.A, RecordType.AAAA);
```
---

### `recordCreate`

Create a new DNS record in a specific zone.

- **Parameters**:
  - `ZoneEntity zone` - DNS zone object.
  - `String sld` - The sub-tld of the new record.
  - `int ttl` - The time-to-live in seconds of the new rcord.

```java
RecordEntity created = client.recordCreateSld(zone, "api", 60, RecordType.A, "192.168.1.1");
System.out.println("Created Record ID: " + created.getId());
```

---

### `recordUpdate`

Update an existing DNS record.

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

---

### `recordDeleteTypeIfExists`

Delete a DNS record of a specific type if it exists.

- **Parameters**:
    - `ZoneEntity zone` - Target zone.
    - `String sld` - Second-level domain.
    - `RecordType type` - Record type.

```java
cfDnsClient.recordDeleteTypeIfExists(zone, "api", RecordType.A);
System.out.println("Deletion attempt completed.");
```

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
recordsToUpdate.add(cfDnsClient.recordList(zone, "api",RecordType.A).get(0));
recordsToUpdate.add(cfDnsClient.recordList(zone, "cdn",RecordType.A).get(0));

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
recordsToReplace.add(cfDnsClient.recordList(zone, "mail",RecordType.A).get(0));
recordsToReplace.add(cfDnsClient.recordList(zone, "cdn",RecordType.A).get(0));

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
recordsToDelete.add(cfDnsClient.recordList(zone, "cdn",RecordType.A).get(0));
recordsToDelete.add(cfDnsClient.recordList(zone, "mail",RecordType.A).get(0));

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
    cfDnsClient.recordList(zone, "existing-api", RecordType.A).get(0)
);
recordsToUpdate.get(0).setContent("192.168.1.200");

List<RecordEntity> recordsToDelete = Arrays.asList(
    cfDnsClient.recordList(zone, "old-api", RecordType.A).get(0)
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
// Create a DNS getRecord
client.zone("example.com")
    .record("api")
    .create(RecordType.A, "192.168.1.1",60);

// Get DNS records of a subdomain
List<RecordEntity> records = client.zone("example.com")
    .record("www", RecordType.A)
    .get();

// Get all DNS records of a zone
List<RecordEntity> zoneRecords = client.zone("example.com")
    .list(RecordType.A, RecordType.AAAA);

// Update a DNS getRecord
RecordEntity updated = client.zone("example.com")
    .record("api", RecordType.A)
    .update("192.168.1.2");

// Delete DNS records
client.zone("example.com") 
    .record("old-service")
    .delete(RecordType.A, RecordType.AAAA);
```

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

// Create a new getRecord
client.zone("example.com")
    .record("api")
    .create(RecordType.A, "192.168.100.1",60);

// Retrieve and verify
List<RecordEntity> records = client.zone("example.com")
    .record("api", RecordType.A)
    .get();
System.out.println("IP: "+records.get(0).getContent());

// Update the getRecord
client.zone("example.com")
     .record("api",RecordType.A)
     .update("192.168.100.2");

// Clean up
client.zone("example.com")
    .record("api")
    .delete(RecordType.A);
```

---

# Notes on Error Handling

The `CfDnsClient` provides internal error-handling mechanisms through exceptions. For example:
- `CloudflareApiException` is thrown for errors during API communication or invalid responses.
- `CloudflareNotFoundException` is thrown when the requested resource (single or multiple) is not found, if enabled via the `emptyResultThrowsException` flag during initialization. **Default is `false`**, meaning empty results will be returned without throwing an exception.

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
  List<RecordEntity> records = cfDnsClient.recordList(zone, "www", RecordType.A);
  System.out.println("Record IP: "+records.get(0).getContent());
} catch (CloudflareApiException e) {
  if (e instanceof CloudflareNotFoundException) {
    log.warn("Sld not found: www");
  } else {
    log.error("Error while getting sld info of www", e);
    throw e;
  }
}
```

---

# Summary

`CfDnsClient` offers a simple interface for managing DNS entries via Cloudflare's public API, allowing seamless CRUD operations and automation-friendly workflows. 