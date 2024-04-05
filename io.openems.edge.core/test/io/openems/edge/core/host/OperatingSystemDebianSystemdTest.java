package io.openems.edge.core.host;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import com.google.common.collect.Lists;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

public class OperatingSystemDebianSystemdTest {

	@Test
	public void test() throws OpenemsNamedException {
		List<String> lines = Lists.newArrayList(//
				"[Match]", //
				"Name=eth0", //
				"", //
				"[Network]", //
				"DHCP=yes", //
				"LinkLocalAddressing=yes", //
				"", //
				"[Address]", //
				"Address=192.168.100.100/24", //
				"Label=normal" //
		);

		NetworkInterface<Object> n = OperatingSystemDebianSystemd.parseSystemdNetworkdConfigurationFile(lines, null);

		assertEquals("eth0", n.getName());
		assertEquals(true, n.getDhcp().getValue());
		assertEquals(true, n.getLinkLocalAddressing().getValue());
		assertEquals("192.168.100.100/24", n.getAddresses().getValue().toArray()[0].toString());
	}

	@Test
	public void testMultipleAddresses() throws OpenemsNamedException {
		List<String> lines = Lists.newArrayList(//
				"[Match]", //
				"Name=eth0", //
				"", //
				"[Network]", //
				"DHCP=yes", //
				"LinkLocalAddressing=yes", //
				"", //
				"[Address]", //
				"Address=192.168.100.100/24", //
				"Label=normal", //
				"", //
				"[Address]", //
				"Address=192.168.123.123/24", //
				"Label=" //
		);

		NetworkInterface<Object> n = OperatingSystemDebianSystemd.parseSystemdNetworkdConfigurationFile(lines, null);

		assertEquals("eth0", n.getName());
		assertEquals(true, n.getDhcp().getValue());
		assertEquals(true, n.getLinkLocalAddressing().getValue());
		{
			var address = (Inet4AddressWithSubnetmask) n.getAddresses().getValue().toArray()[0];
			assertEquals("192.168.100.100/24", address.toString());
			assertEquals("normal", address.getLabel());
		}
		{
			var address = (Inet4AddressWithSubnetmask) n.getAddresses().getValue().toArray()[1];
			assertEquals("192.168.123.123/24", address.toString());
			assertEquals("", address.getLabel());
		}
	}

	@Test
	public void testLabelBefore() throws OpenemsNamedException {
		List<String> lines = Lists.newArrayList(//
				"[Match]", //
				"Name=eth0", //
				"", //
				"[Network]", //
				"DHCP=yes", //
				"LinkLocalAddressing=yes", //
				"", //
				"[Address]", //
				"Address=192.168.100.100/24", //
				"Label=fallback", //
				"", //
				"[Address]", //
				"Label=foo", //
				"Address=192.168.123.123/24" //
		);

		NetworkInterface<Object> n = OperatingSystemDebianSystemd.parseSystemdNetworkdConfigurationFile(lines, null);
		{
			var address = (Inet4AddressWithSubnetmask) n.getAddresses().getValue().toArray()[0];
			assertEquals("192.168.100.100/24", address.toString());
			assertEquals("fallback", address.getLabel());
		}
		{
			var address = (Inet4AddressWithSubnetmask) n.getAddresses().getValue().toArray()[1];
			assertEquals("192.168.123.123/24", address.toString());
			assertEquals("", address.getLabel()); // NOTE: if Label is before Address, it is ignored
		}
	}

	@Test
	public void test2() throws OpenemsNamedException {
		List<String> lines = Lists.newArrayList(//
				"[Match]", //
				"Name=enx*", //
				"", //
				"[Network]", //
				"DHCP=yes" //
		);

		NetworkInterface<Object> n = OperatingSystemDebianSystemd.parseSystemdNetworkdConfigurationFile(lines, null);

		assertEquals("enx*", n.getName());
		assertEquals(true, n.getDhcp().getValue());
	}

}
