# Changelog

- 0.6.0-SNAPSHOT:
  - updated dependencies: jackson 3.2.4, httpclient5 5.6.4, lombok 1.18.48, logback-classic 1.6.6, mockito-junit-jupiter 5.24.0
  - updated maven plugins: maven-compiler-plugin 3.16.0, maven-surefire-plugin 3.6.0, maven-jar-plugin 3.5.1
  - README: fixed fluent API examples, documented paging and further methods
  - fixed "getRecord" wording in javadoc, log and exception messages (e.g. "Invalid record type")
- 0.5.0:
  - moved the project to git.mein-gateway.de
  - replaced sonarqube with own actions
  - migrate to jackson 3.x
- 0.4.0:
    - fixed some paging issues
    - **Breaking Change**: renamed `client.zone().record()` to `client.zone().getRecord()`
    - Code quality improvements: Increasing test coverage
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
    - Code quality improvements: removed duplication in batch operations, improved type safety in HTTP methods,
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