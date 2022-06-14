package io.openems.edge.core.host;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import io.openems.edge.core.host.NetworkInterface.Inet4AddressWithNetmask;

public class NetworkInterfaceTest {

	private static Inet4AddressWithNetmask inet4AddressWithNetmask;

	@Before
	public void beforeEach() throws Exception {
		inet4AddressWithNetmask = Inet4AddressWithNetmask.fromString("192.168.178.1/24");
	}

	@Test
	public void testGetNetmaskAsString() throws Exception {
		assertEquals("255.0.0.0", Inet4AddressWithNetmask.fromString("192.168.178.1/8").getNetmaskAsString());
		assertEquals("255.127.0.0", Inet4AddressWithNetmask.fromString("192.168.178.1/9").getNetmaskAsString());
		assertEquals("255.192.0.0", Inet4AddressWithNetmask.fromString("192.168.178.1/10").getNetmaskAsString());
		assertEquals("255.224.0.0", Inet4AddressWithNetmask.fromString("192.168.178.1/11").getNetmaskAsString());
		assertEquals("255.240.0.0", Inet4AddressWithNetmask.fromString("192.168.178.1/12").getNetmaskAsString());
		assertEquals("255.248.0.0", Inet4AddressWithNetmask.fromString("192.168.178.1/13").getNetmaskAsString());
		assertEquals("255.252.0.0", Inet4AddressWithNetmask.fromString("192.168.178.1/14").getNetmaskAsString());
		assertEquals("255.254.0.0", Inet4AddressWithNetmask.fromString("192.168.178.1/15").getNetmaskAsString());
		assertEquals("255.255.0.0", Inet4AddressWithNetmask.fromString("192.168.178.1/16").getNetmaskAsString());
		assertEquals("255.255.255.0", inet4AddressWithNetmask.getNetmaskAsString());
	}

	@Test
	public void testIsInSameNetwork() throws Exception {
		assertTrue(inet4AddressWithNetmask.isInSameNetwork(Inet4AddressWithNetmask.fromString("192.168.178.2/24")));
		assertFalse(inet4AddressWithNetmask.isInSameNetwork(Inet4AddressWithNetmask.fromString("192.168.179.2/24")));
	}

}
