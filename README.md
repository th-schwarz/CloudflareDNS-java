# CloudflareDNS-java

![GitLab Pipeline Status](https://gitlab.com/th-schwarz/CloudflareDNS-java/badges/develop/pipeline.svg)
![GitLab License](https://img.shields.io/gitlab/license/th-schwarz%2FCloudflareDNS-java)

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=thischwa_CloudflareDNS-java&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=thischwa_CloudflareDNS-java)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=thischwa_CloudflareDNS-java&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=thischwa_CloudflareDNS-java)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=thischwa_CloudflareDNS-java&metric=coverage)](https://sonarcloud.io/summary/new_code?id=thischwa_CloudflareDNS-java)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=thischwa_CloudflareDNS-java&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=thischwa_CloudflareDNS-java)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=thischwa_CloudflareDNS-java&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=thischwa_CloudflareDNS-java)

## Preface

This project provides a java client for minimalistic access to the Cloudflare API version 4, which is mainly used for
managing DNS settings such as creating, updating and deleting DNS records.

If you encounter any bugs or find missing features, feel free to report them on
the [GitLab Issues page](https://gitlab.com/th-schwarz/CloudflareDNS-java/-/issues).

---

## Disclaimer

This guide comes without any warranty. Use at your own risk. The author is not responsible for potential data loss, hardware damage or keyboard mishaps!

---

## State of the Project

BETA

---

## Get It

The project has its own maven repository. It can be added to the `pom.xml`:

```xml
<repositories>
  <repository>
    <id>gitlab-cloudflare</id>
    <url>https://gitlab.com/api/v4/projects/68509751/packages/maven</url>
  </repository>
</repositories>
```

The dependency is:
```xml
<dependency>
  <groupId>codes.thischwa</groupId>
  <artifactId>cloudflaredns</artifactId>
  <version>0.1.0-beta.3</version>
</dependency>
```

## Changelog

- 0.1.0-beta.3:
  - fixed json deserialization
  - added logging of api errors
- 0.1.0-beta.1: 1st runnable version

## Methods Overview

The methods can be categorized as follows:

- `Zone`: list, info
- `Record`: list, info, create, update, delete

The following text focuses on the basic methods. For further information, take a look at
the [javadoc of the CfDnsClient](https://cloudflaredns-java-f4ee3a.gitlab.io/apidocs/codes/thischwa/cf/CfDnsClient.html).

### Instantiation of `CfDnsClient`

```java
CfDnsClient cfDnsClient = new CfDnsClient(
     "email@example.com", "yourApiKey"
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