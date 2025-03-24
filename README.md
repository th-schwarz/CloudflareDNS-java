# CloudflareDNS-java

[![Build and Test](https://github.com/th-schwarz/CloudflareDNS-java/actions/workflows/build-and-test.yml/badge.svg)](https://github.com/th-schwarz/CloudflareDNS-java/actions/workflows/build-and-test.yml)  ![GitHub License](https://img.shields.io/github/license/th-schwarz/CloudflareDNS-java)

Give it a try ![GitHub Release](https://img.shields.io/github/v/release/th-schwarz/CloudflareDNS-java?include_prereleases)

## Preface

This project offers minimalistic access to the Cloudflare API, focused on managing DNS settings like creating, updating,
and deleting DNS records. Supported types include A, CNAME, MX, TXT, and more.

If you encounter any bugs or find missing features, feel free to report them on
the [GitHub Issues page](https://github.com/th-schwarz/CloudflareDNS-java/issues).

---

## Disclaimer

This guide comes without any warranty. Use at your own risk. The author is not responsible for potential data loss,
hardware damage, or keyboard mishaps!

---

## State of the Project

BETA

---

## Methods Overview

The following text focuses on the basic methods. For further information take a look at the [javadoc of the CfDnsClient](https://th-schwarz.github.io/CloudflareDNS-java/apidocs/codes/thischwa/cf/CfDnsClient.html).

### Instantiation of `CfDnsClient`

```java
CfDnsClient cfDnsClient = new CfDnsClient(
     "email@example.com", "yourApiKey", "yourApiToken"
 );
```

### `zoneListAll`

Retrieve all zones within the Cloudflare account.

- **Returns**: A list of `ZoneEntity` objects.

```java
List<ZoneEntity> zones = cfDnsClient.zoneListAll();
zones.forEach(zone -> System.out.println("Zone: " + zone.getName()));
```

---

### `zoneInfo`

Get detailed information about a specific zone by its name.

- **Parameters**:
    - `String name` - The zone name (e.g., "example.com").
- **Returns**: A `ZoneEntity` object.

```java
ZoneEntity zone = cfDnsClient.zoneInfo("example.com");
System.out.println("Zone ID: " + zone.getId());
```

---

### `sldListAll`

Retrieve all records for a specific second-level domain (SLD) under a given zone.

- **Parameters**:
    - `ZoneEntity zone` - The zone object.
    - `String sld` - Second-level domain (e.g., "www" in "www.example.com").
- **Returns**: A list of `RecordEntity` objects.

```java
List<RecordEntity> records = cfDnsClient.sldListAll(zone, "sld");
records.forEach(record -> 
    System.out.println("Record Type: " + record.getType() + ", Value: " + record.getContent())
);
```

---

### `sldInfo`

Retrieve DNS record details for a specific SLD, zone, and record type.

- **Parameters**:
    - `ZoneEntity zone` - The zone object.
    - `String sld` - The second-level domain.
    - `RecordType type` - Record type (e.g., A, CNAME).

```java
RecordEntity record = cfDnsClient.sldInfo(zone, "www", RecordType.A);
System.out.println("Record IP: " + record.getContent());
```

---

### `recordCreate`

Create a new DNS record in a specific zone.

- **Parameters**:
    - `ZoneEntity zone` - DNS zone object.
    - `RecordEntity rec` - Details of the new record (name, type, content).

```java
RecordEntity newRecord = new RecordEntity("api.example.com", RecordType.A, "192.168.1.1");
RecordEntity created = cfDnsClient.recordCreate(zone, newRecord);
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

---

### Notes on Error Handling

The `CfDnsClient` provides internal error-handling mechanisms through exceptions. For example:
- `CloudflareApiException` is thrown for errors during API communication or invalid responses.
- `CloudflareNotFoundException` is thrown when the requested single resource is not found, if enabled via the `emptyResultThrowsException` flag during initialization.

#### Example:

```java
try {
    RecordEntity record = cfDnsClient.sldInfo(zone, "www", RecordType.A);
    System.out.println("Record IP: " + record.getContent());
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

### Summary

`CfDnsClient` offers a simple interface for managing DNS entries via Cloudflare's public API, allowing seamless CRUD
operations and automation-friendly workflows.