package io.openems.edge.core.host;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.Inet4Address;
import java.net.InetAddress;

import org.junit.Before;
import org.junit.Test;

public class NetworkInterfaceTest {

	private static Inet4AddressWithSubnetmask inet4AddressWithNetmask;

	@Before
	public void beforeEach() throws Exception {
		inet4AddressWithNetmask = Inet4AddressWithSubnetmask.fromString("", "192.168.178.1/24");
	}

	@Test
	public void testGetNetmaskAsString() throws Exception {
		assertEquals("255.0.0.0", Inet4AddressWithSubnetmask.fromString("", "192.168.178.1/8").getSubnetmaskAsString());
		assertEquals("255.127.0.0",
				Inet4AddressWithSubnetmask.fromString("", "192.168.178.1/9").getSubnetmaskAsString());
		assertEquals("255.192.0.0",
				Inet4AddressWithSubnetmask.fromString("", "192.168.178.1/10").getSubnetmaskAsString());
		assertEquals("255.224.0.0",
				Inet4AddressWithSubnetmask.fromString("", "192.168.178.1/11").getSubnetmaskAsString());
		assertEquals("255.240.0.0",
				Inet4AddressWithSubnetmask.fromString("", "192.168.178.1/12").getSubnetmaskAsString());
		assertEquals("255.248.0.0",
				Inet4AddressWithSubnetmask.fromString("", "192.168.178.1/13").getSubnetmaskAsString());
		assertEquals("255.252.0.0",
				Inet4AddressWithSubnetmask.fromString("", "192.168.178.1/14").getSubnetmaskAsString());
		assertEquals("255.254.0.0",
				Inet4AddressWithSubnetmask.fromString("", "192.168.178.1/15").getSubnetmaskAsString());
		assertEquals("255.255.0.0",
				Inet4AddressWithSubnetmask.fromString("", "192.168.178.1/16").getSubnetmaskAsString());
		assertEquals("255.255.255.0", inet4AddressWithNetmask.getSubnetmaskAsString());
	}

	@Test
	public void testGetCidrFromSubnetmask() throws Exception {
		assertEquals(0, //
				Inet4AddressWithSubnetmask.getCidrFromSubnetmask((Inet4Address) InetAddress.getByName("0.0.0.0")));
		assertEquals(1, //
				Inet4AddressWithSubnetmask.getCidrFromSubnetmask((Inet4Address) InetAddress.getByName("128.0.0.0")));
		assertEquals(3, //
				Inet4AddressWithSubnetmask.getCidrFromSubnetmask((Inet4Address) InetAddress.getByName("224.0.0.0")));
		assertEquals(8, //
				Inet4AddressWithSubnetmask.getCidrFromSubnetmask((Inet4Address) InetAddress.getByName("255.0.0.0")));
		assertEquals(24, Inet4AddressWithSubnetmask
				.getCidrFromSubnetmask((Inet4Address) InetAddress.getByName("255.255.255.0")));
		assertEquals(25, Inet4AddressWithSubnetmask
				.getCidrFromSubnetmask((Inet4Address) InetAddress.getByName("255.255.255.128")));
		assertEquals(26, Inet4AddressWithSubnetmask
				.getCidrFromSubnetmask((Inet4Address) InetAddress.getByName("255.255.255.192")));
		assertEquals(27, Inet4AddressWithSubnetmask
				.getCidrFromSubnetmask((Inet4Address) InetAddress.getByName("255.255.255.224")));
	}

	@Test
	public void testIsInSameNetwork() throws Exception {
		assertTrue(
				inet4AddressWithNetmask.isInSameNetwork(Inet4AddressWithSubnetmask.fromString("", "192.168.178.2/24")));
		assertFalse(
				inet4AddressWithNetmask.isInSameNetwork(Inet4AddressWithSubnetmask.fromString("", "192.168.179.2/24")));
	}

}
