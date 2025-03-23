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
  A("A"),
  AAAA("AAAA"),
  CAA("CAA"),
  CERT("CERT"),
  CNAME("CNAME"),
  DNSKEY("DNSKEY"),
  DS("DS"),
  HTTPS("HTTPS"),
  LOC("LOC"),
  MX("MX"),
  NAPTR("NAPTR"),
  NS("NS"),
  OPENPGPKEY("OPENPGPKEY"),
  PTR("PTR"),
  SMIMEA("SMIMEA"),
  SRV("SRV"),
  SSHFP("SSHFP"),
  SVCB("SVCB"),
  TLSA("TLSA"),
  TXT("TXT"),
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
