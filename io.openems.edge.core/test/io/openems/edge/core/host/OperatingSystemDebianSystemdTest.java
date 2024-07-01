package io.openems.edge.core.host;

import static io.openems.common.utils.JsonUtils.prettyToString;
import static io.openems.edge.core.host.OperatingSystemDebianSystemd.parseSystemdNetworkdConfigurationFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.google.common.collect.Lists;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

public class OperatingSystemDebianSystemdTest {

	@Test
	public void test() throws OpenemsNamedException {
		var lines = Lists.newArrayList(//
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

		var n = parseSystemdNetworkdConfigurationFile(lines, null);

		assertEquals("eth0", n.getName());
		assertEquals(true, n.getDhcp().getValue());
		assertEquals(true, n.getLinkLocalAddressing().getValue());
		assertEquals("192.168.100.100/24", n.getAddresses().getValue().toArray()[0].toString());

		assertEquals("""
				{
				  "dhcp": true,
				  "linkLocalAddressing": true,
				  "addresses": [
				    {
				      "label": "normal",
				      "address": "192.168.100.100",
				      "subnetmask": "255.255.255.0"
				    }
				  ]
				}""", prettyToString(n.toJson()));

		var json = n.toJson();
		assertEquals(json, NetworkInterface.from("eth0", json).toJson());
	}

	@Test
	public void testMultipleAddresses() throws OpenemsNamedException {
		var lines = Lists.newArrayList(//
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

		var n = parseSystemdNetworkdConfigurationFile(lines, null);

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

		var json = n.toJson();
		assertEquals(json, NetworkInterface.from("eth0", json).toJson());
	}

	@Test
	public void testLabelBefore() throws OpenemsNamedException {
		var lines = Lists.newArrayList(//
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

		var n = parseSystemdNetworkdConfigurationFile(lines, null);
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

		var json = n.toJson();
		assertEquals(json, NetworkInterface.from("eth0", json).toJson());
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

		var n = parseSystemdNetworkdConfigurationFile(lines, null);

		assertEquals("enx*", n.getName());
		assertEquals(true, n.getDhcp().getValue());

		var json = n.toJson();
		assertEquals(json, NetworkInterface.from("eth0", json).toJson());
	}

	@Test
	public void test3() throws OpenemsNamedException {
		var lines = """
				[Match]
				Name=eth0

				[Network]
				DHCP=no
				LinkLocalAddressing=yes
				Address=192.168.100.100/24
				Address=10.4.0.1/24

				[Route]
				Gateway=10.4.0.2
				""".lines().toList();

		var n = parseSystemdNetworkdConfigurationFile(lines, null);

		assertEquals("eth0", n.getName());
		assertEquals(false, n.getDhcp().getValue());
		assertEquals(true, n.getLinkLocalAddressing().getValue());
		assertEquals("192.168.100.100/24", n.getAddresses().getValue().toArray()[0].toString());
		assertEquals("10.4.0.1/24", n.getAddresses().getValue().toArray()[1].toString());

		assertEquals("10.4.0.2", n.getGateway().getValue().getHostName());
		assertEquals(null, n.getMetric().getValue());

		var json = n.toJson();
		assertEquals(json, NetworkInterface.from("eth0", json).toJson());
	}

	@Test
	public void test4() throws OpenemsNamedException {
		var lines = """
				[Match]
				Name=eth0

				[Network]
				DHCP=no
				LinkLocalAddressing=yes
				Address=192.168.100.100/24
				Address=10.4.0.1/24
				Gateway=10.4.0.2
				""".lines().toList();

		var n = parseSystemdNetworkdConfigurationFile(lines, null);

		assertEquals("eth0", n.getName());
		assertEquals(false, n.getDhcp().getValue());
		assertEquals(true, n.getLinkLocalAddressing().getValue());
		assertEquals("192.168.100.100/24", n.getAddresses().getValue().toArray()[0].toString());
		assertEquals("10.4.0.1/24", n.getAddresses().getValue().toArray()[1].toString());
		assertEquals("10.4.0.2", n.getGateway().getValue().getHostName());

		var json = n.toJson();
		assertEquals(json, NetworkInterface.from("eth0", json).toJson());
	}

	@Test
	public void test5() throws OpenemsNamedException {
		var lines = """
				[Match]
				Name=eth0

				[Network]
				DHCP=yes

				[DHCP]
				RouteMetric=216
				""".lines().toList();

		var n = parseSystemdNetworkdConfigurationFile(lines, null);

		assertEquals("eth0", n.getName());
		assertEquals(true, n.getDhcp().getValue());
		assertEquals(216, n.getMetric().getValue().intValue());

		lines = """
				[Network]
				DHCP=no
				DNS=10.0.0.1
				LinkLocalAddressing=yes

				[Route]
				Gateway=10.0.10.10
				Metric=520

				[Address]
				Address=10.4.0.1/16
				""".lines().toList();

		n = parseSystemdNetworkdConfigurationFile(lines, null);

		assertEquals(false, n.getDhcp().getValue());
		assertEquals(520, n.getMetric().getValue().intValue());

		var json = n.toJson();
		assertEquals(json, NetworkInterface.from("eth0", json).toJson());
	}

	@Test
	public void testUpdate() throws OpenemsNamedException {
		var n1 = parseSystemdNetworkdConfigurationFile(Lists.newArrayList(//
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
		), null);

		assertTrue(n1.getDhcp().getValue());

		var n2 = parseSystemdNetworkdConfigurationFile(Lists.newArrayList(//
				"[Match]", //
				"Name=eth0", //
				"", //
				"[Network]", //
				"DHCP=no", //
				"LinkLocalAddressing=yes", //
				"", //
				"[Address]", //
				"Address=192.168.100.100/24", //
				"Label=normal" //
		), null);
		n1.updateFrom(n2);

		assertFalse(n1.getDhcp().getValue());
	}
}
