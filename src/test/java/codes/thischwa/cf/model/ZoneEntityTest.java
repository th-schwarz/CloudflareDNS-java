package codes.thischwa.cf.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class ZoneEntityTest {

  @Test
  void testZoneEntity() {
    ZoneEntity zone = new ZoneEntity();
    zone.setId("zone-id");
    zone.setName("example.com");
    zone.setDevelopmentMode(7200);
    Set<String> ns = Set.of("ns1.cloudflare.com", "ns2.cloudflare.com");
    zone.setNameServers(ns);
    zone.setOriginalNameServers(ns);
    LocalDateTime now = LocalDateTime.now();
    zone.setCreatedOn(now);
    zone.setModifiedOn(now);
    zone.setActivatedOn(now);
    zone.setStatus("active");
    zone.setPaused(false);
    zone.setType("full");

    assertEquals("zone-id", zone.getId());
    assertEquals("example.com", zone.getName());
    assertEquals(7200, zone.getDevelopmentMode());
    assertEquals(ns, zone.getNameServers());
    assertEquals(ns, zone.getOriginalNameServers());
    assertEquals(now, zone.getCreatedOn());
    assertEquals(now, zone.getModifiedOn());
    assertEquals(now, zone.getActivatedOn());
    assertEquals("active", zone.getStatus());
    assertFalse(zone.getPaused());
    assertEquals("full", zone.getType());
  }
}
