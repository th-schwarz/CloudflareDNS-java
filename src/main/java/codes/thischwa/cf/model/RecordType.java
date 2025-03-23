package codes.thischwa.cf.model;

import lombok.Getter;

/**
 * Enum representing various DNS record types.
 *
 * <p>Each constant in this enum corresponds to a specific DNS record type, such as "A", "AAAA",
 * "CNAME", or "TXT". This enum provides a means to standardize the representation of these record
 * types throughout the application while allowing easy retrieval of their string representation.
 */
@Getter
public enum RecordType {
  /**
   * Represents the DNS A record type.
   *
   * <p>The "A" record type is used to map a domain name to an IPv4 address.
   */
  A("A"),
  /**
   * Represents the DNS AAAA record type.
   *
   * <p>The "AAAA" record type maps a domain name to an IPv6 address.
   */
  AAAA("AAAA"),
  /**
   * Represents the DNS CAA (Certificate Authority Authorization) record type.
   *
   * <p>The "CAA" record type is used to specify which certificate authorities (CAs) are allowed to
   * issue SSL/TLS certificates for a domain.
   */
  CAA("CAA"),
  /**
   * Represents the DNS CERT record type.
   *
   * <p>The "CERT" record type is used to store certificates and related certificate revocation
   * lists or certificate authority data in DNS zones.
   */
  CERT("CERT"),
  /**
   * Represents the DNS CNAME (Canonical Name) record type.
   *
   * <p>The "CNAME" record type is used to alias one domain name to another.
   */
  CNAME("CNAME"),
  /**
   * Represents the DNSKEY record type.
   *
   * <p>The "DNSKEY" record type is used for storing public keys in DNS, as part of the DNS Security
   * Extensions (DNSSEC). It helps in verifying the authenticity of DNS responses through digital
   * signatures.
   */
  DNSKEY("DNSKEY"),
  /**
   * Represents the DNS DS (Delegation Signer) record type.
   *
   * <p>The "DS" record type is used in the DNSSEC (Domain Name System Security Extensions)
   * protocol. It contains a hash of a DNSKEY record which is utilized in establishing a chain of
   * trust from a parent zone to a child zone.
   */
  DS("DS"),
  /**
   * Represents the DNS HTTPS (HTTP Service) record type.
   *
   * <p>The "HTTPS" record type is used to specify information about the HTTP/3 and related services
   * offered by a domain.
   */
  HTTPS("HTTPS"),
  /**
   * Represents the DNS LOC (Location) record type.
   *
   * <p>The "LOC" record type is used to store geographical location information for a domain,
   * including latitude, longitude, altitude, and other details. It enables associating domains with
   * physical locations.
   */
  LOC("LOC"),
  /**
   * Represents the DNS MX (Mail Exchange) record type.
   *
   * <p>The "MX" record type is used to specify the mail servers responsible for receiving email
   * messages on behalf of a domain.
   */
  MX("MX"),
  /**
   * Represents the NAPTR record type for DNS configurations.
   *
   * <p>This constant is used to specify NAPTR (Naming Authority Pointer) DNS records, which allow
   * for service discovery through flexible DNS-based mechanisms. NAPTR records are commonly used in
   * applications such as VoIP and ENUM (Telephone Number Mapping) for resolving information about
   * available services.
   */
  NAPTR("NAPTR"),
  /**
   * Represents the namespace or identifier `"NS"` within the domain model.
   *
   * <p>This variable typically designates elements related to name server operations or
   * configurations in the context of the Cloudflare DNS system. It may be used as an identifier or
   * constant throughout the application for operations involving name servers.
   */
  NS("NS"),
  /**
   * Represents the "OPENPGPKEY" DNS record type.
   *
   * <p>This constant is primarily used to identify DNS records of the type "OPENPGPKEY". It may be
   * utilized within the system for operations such as creating, retrieving, updating, or managing
   * DNS records specific to the "OPENPGPKEY" type.
   */
  OPENPGPKEY("OPENPGPKEY"),
  /**
   * Represents a DNS record type.
   *
   * <p>The `PTR` value specifically refers to a "pointer record" in the DNS system, which is
   * typically used for reverse DNS lookups.
   */
  PTR("PTR"),
  /**
   * Represents the SMIMEA DNS record type.
   *
   * <p>The SMIMEA resource record is used to associate a user's certificate information for email
   * message signing or encryption. This type of DNS record is part of the DNS-based Authentication
   * of Named Entities (DANE) protocol.
   *
   * <p>SMIMEA records provide a mechanism for utilizing certificates in email communication
   * securely by publishing their information in DNS. They ensure integrity and authenticity of
   * encrypted or signed email exchanges.
   *
   * <p>Key features include:
   * <ul>
   *   <li>Use in Secure/Multipurpose Internet Mail Extensions (S/MIME)-based messaging.
   *   <li>Facilitating secure email communications by publishing certificates in DNS.
   *   <li>Enhancing security by eliminating dependency on third-party certificate authorities.
   * </ul>
   */
  SMIMEA("SMIMEA"),
  /**
   * Represents a service record (SRV) type in the DNS configuration model.
   *
   * <p>This constant may be used to identify and work with SRV record types in various DNS-related
   * operations or integrations.
   */
  SRV("SRV"),
  /**
   * Represents the DNS record type "SSHFP" (SSH Fingerprint), used in DNS to store cryptographic
   * fingerprints associated with SSH host keys.
   *
   * <p>This DNS record type provides a mechanism for verifying the authenticity of an SSH server
   * before initiating a connection, enhancing the security of SSH communications.
   */
  SSHFP("SSHFP"),
  /**
   * Represents the Service Binding (SVCB) DNS record type.
   *
   * <p>The SVCB record is a DNS resource record used to indicate alternative endpoints or specific
   * configuration details for services. It is commonly applied in service discovery and
   * protocol-specific configurations.
   */
  SVCB("SVCB"),
  /**
   * Represents a constant for the DNS-based Authentication of Named Entities (DANE) TLSA record
   * type.
   *
   * <p>The TLSA record is used to associate a TLS server certificate or public key with the domain
   * name (e.g., via DNSSEC). It enables cryptographically secured connections by attaching
   * certificate and key constraints to the specific domain.
   */
  TLSA("TLSA"),
  /**
   * Represents the TXT DNS record type.
   *
   * <p>The TXT DNS record type is commonly used to store text-based information for various
   * verification and configuration purposes, such as domain ownership verification or email
   * authentication protocols (e.g., SPF, DKIM).
   */
  TXT("TXT"),
  /**
   * Represents a Uniform Resource Identifier (URI).
   *
   * <p>This variable is used to define and manage URIs, which are string identifiers commonly
   * utilized to specify the location of resources in various network-based systems.
   *
   * <p>Features and usage:
   *
   * <ul>
   *   <li>Provides a standard way of identifying resources via URI syntax.
   *   <li>Can encapsulate support for schemes such as HTTP, HTTPS, FTP, etc.
   * </ul>
   *
   * <p>This variable serves as a critical component for resource identification and communication
   * operations within the application.
   */
  URI("URI");

  private final String type;

  RecordType(String type) {
    this.type = type;
  }

  @Override
  public String toString() {
    return getType();
  }
}
