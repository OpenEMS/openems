package io.openems.edge.core.host;

import static io.openems.common.utils.JsonUtils.prettyToString;
import static io.openems.edge.core.host.OperatingSystemDebianSystemd.parseSystemdNetworkdConfigurationFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.core.host.NetworkInterface.IpMasqueradeSetting;

public class OperatingSystemDebianSystemdTest {

	@Test
	public void test() throws OpenemsNamedException {
		final var lines = """
				[Match]
				Name=eth0

				[Network]
				DHCP=yes
				LinkLocalAddressing=yes

				[Address]
				Address=192.168.100.100/24
				Label=normal
				""".lines().toList();

		final var n = parseSystemdNetworkdConfigurationFile(lines, null);

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
		final var lines = """
				[Match]
				Name=eth0

				[Network]
				DHCP=yes
				LinkLocalAddressing=yes

				[Address]
				Address=192.168.100.100/24
				Label=normal

				[Address]
				Address=192.168.123.123/24
				Label=
				""".lines().toList();

		final var n = parseSystemdNetworkdConfigurationFile(lines, null);

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
		final var lines = """
				[Match]
				Name=eth0

				[Network]
				DHCP=yes
				LinkLocalAddressing=yes

				[Address]
				Address=192.168.100.100/24
				Label=fallback

				[Address]
				Label=foo
				Address=192.168.123.123/24
				""".lines().toList();

		final var n = parseSystemdNetworkdConfigurationFile(lines, null);
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
		final var lines = """
				[Match]
				Name=enx*

				[Network]
				DHCP=yes
				""".lines().toList();

		final var n = parseSystemdNetworkdConfigurationFile(lines, null);

		assertEquals("enx*", n.getName());
		assertEquals(true, n.getDhcp().getValue());

		var json = n.toJson();
		assertEquals(json, NetworkInterface.from("eth0", json).toJson());
	}

	@Test
	public void testStaticIpWithRouteGateway() throws OpenemsNamedException {
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

		final var n = parseSystemdNetworkdConfigurationFile(lines, null);

		assertEquals("eth0", n.getName());
		assertEquals(false, n.getDhcp().getValue());
		assertEquals(true, n.getLinkLocalAddressing().getValue());
		assertEquals("192.168.100.100/24", n.getAddresses().getValue().toArray()[0].toString());
		assertEquals("10.4.0.1/24", n.getAddresses().getValue().toArray()[1].toString());
		var currentRoutes = new HashSet<>(n.getRoutes().getValue());
		assertTrue(currentRoutes.stream().anyMatch(t -> Objects.equals(t.getRouteGateway(), "10.4.0.2")));
		assertTrue(currentRoutes.stream().anyMatch(t -> Objects.equals(t.geRouteMetric(), null)));

		var json = n.toJson();
		assertEquals(json, NetworkInterface.from("eth0", json).toJson());
	}

	@Test
	public void testOnlyStaticIp() throws OpenemsNamedException {
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

		final var n = parseSystemdNetworkdConfigurationFile(lines, null);

		assertEquals("eth0", n.getName());
		assertEquals(false, n.getDhcp().getValue());
		assertEquals(true, n.getLinkLocalAddressing().getValue());
		assertEquals("192.168.100.100/24", n.getAddresses().getValue().toArray()[0].toString());
		assertEquals("10.4.0.1/24", n.getAddresses().getValue().toArray()[1].toString());
		assertEquals("10.4.0.2", n.getGateway().getValue().getHostAddress());

		var json = n.toJson();
		assertEquals(json, NetworkInterface.from("eth0", json).toJson());
	}

	@Test
	public void testDhcpRouteMetric() throws OpenemsNamedException {
		var lines = """
				[Match]
				Name=eth0

				[Network]
				DHCP=yes

				[DHCP]
				RouteMetric=216
				""".lines().toList();

		final var n = parseSystemdNetworkdConfigurationFile(lines, null);

		assertEquals("eth0", n.getName());
		assertEquals(true, n.getDhcp().getValue());
		assertEquals(216, n.getDhcpRouteMetric().getValue().intValue());
	}

	@Test
	public void testRouteOnlyWithGatewayAndMetric() throws OpenemsNamedException {
		final var lines = """
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

		final var n = parseSystemdNetworkdConfigurationFile(lines, null);

		assertEquals(false, n.getDhcp().getValue());
		var currentRoutes = new HashSet<>(n.getRoutes().getValue());
		assertTrue(currentRoutes.stream().anyMatch(t -> Objects.equals(t.getRouteGateway(), "10.0.10.10")));
		assertTrue(currentRoutes.stream().anyMatch(t -> Objects.equals(t.geRouteMetric(), 520)));

		var json = n.toJson();
		assertEquals(json, NetworkInterface.from("eth0", json).toJson());
	}

	@Test
	public void testMultipleRouteSections() throws OpenemsNamedException {
		final var lines = """
				[Match]
				Name=eth1

				[Network]
				DHCP=no
				Address=172.23.21.1/24
				Address=192.168.0.11/24

				[Route]
				Gateway=172.23.21.254
				Destination=172.23.22.0/24
				GatewayOnLink=yes

				[Route]
				Gateway=172.23.21.254
				Destination=172.23.23.0/24
				GatewayOnLink=yes

				[Route]
				Gateway=172.23.21.254
				Destination=172.23.24.0/24
				GatewayOnLink=yes
				""".lines().toList();

		final var n = parseSystemdNetworkdConfigurationFile(lines, null);

		assertEquals(false, n.getDhcp().getValue());
		var currentRoutes = new HashSet<>(n.getRoutes().getValue());
		var destinations = currentRoutes.stream()//
				.map(Routes::getRouteDestination)//
				.filter(Objects::nonNull)//
				.collect(Collectors.toSet());
		assertEquals(//
				Set.of("172.23.23.0/24", "172.23.24.0/24", "172.23.22.0/24"), destinations);

		assertTrue(currentRoutes.stream().anyMatch(t -> Objects.equals(t.getRouteGateway(), "172.23.21.254")));
		assertTrue(currentRoutes.stream().anyMatch(t -> Objects.equals(t.isRouteGatewayOnLink(), true)));

		var json = n.toJson();
		assertEquals(json, NetworkInterface.from("eth1", json).toJson());
	}

	@Test
	public void testGatewayDestinationAndGatewayOnLink() throws OpenemsNamedException {
		final var lines = """
				[Match]
				Name=eth0

				[Network]
				DHCP=no
				LinkLocalAddressing=yes
				Address=192.168.100.100/24
				Address=172.23.20.1/24
				Destination=0.0.0.0/0
				GatewayOnLink=yes
				Gateway=172.23.20.254
				""".lines().toList();

		final var n = parseSystemdNetworkdConfigurationFile(lines, null);

		assertEquals(false, n.getDhcp().getValue());
		assertEquals(true, n.getLinkLocalAddressing().getValue());
		assertEquals(//
				Set.of(//
						Inet4AddressWithSubnetmask.fromString("", "0.0.0.0/0")),
				n.getDestination().getValue());
		assertEquals(true, n.getGatewayOnLink().getValue());
		assertEquals("172.23.20.254", n.getGateway().getValue().getHostAddress());

		var json = n.toJson();
		assertEquals(json, NetworkInterface.from("eth0", json).toJson());
	}

	@Test
	public void testDhcpRouteMetricforEth2() throws OpenemsNamedException {
		final var lines = """
				[Match]
				Name=eth2

				[Network]
				DHCP=yes

				[DHCP]
				RouteMetric=1024

				[Address]
				Address=172.25.21.1/24
				""".lines().toList();

		final var n = parseSystemdNetworkdConfigurationFile(lines, null);
		assertEquals(true, n.getDhcp().getValue());
		assertEquals(1024, n.getDhcpRouteMetric().getValue().intValue());

		var json = n.toJson();
		assertEquals(json, NetworkInterface.from("eth2", json).toJson());
	}

	@Test
	public void testParseIpV4Forwarding() throws OpenemsNamedException {
		final var lines = List.of(//
				"[Match]", //
				"Name=eth0", //
				"", //
				"[Network]", //
				"IPv4Forwarding=yes" //
		);

		var n = parseSystemdNetworkdConfigurationFile(lines, null);

		assertEquals("eth0", n.getName());
		assertTrue(n.getIpv4Forwarding().getValue());
	}

	@Test
	public void testParseIpMasquerade() throws OpenemsNamedException {
		final var lines = List.of(//
				"[Match]", //
				"Name=eth0", //
				"", //
				"[Network]", //
				"IPMasquerade=ipv4" //
		);

		var n = parseSystemdNetworkdConfigurationFile(lines, null);

		assertEquals("eth0", n.getName());
		assertEquals(IpMasqueradeSetting.IP_V4, n.getIpMasquerade().getValue());
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
				"IPv4Forwarding=yes", //
				"IPMasquerade=ipv4", //
				"", //
				"[Address]", //
				"Address=192.168.100.100/24", //
				"Label=normal" //
		), null);

		assertTrue(n1.updateFrom(n2));

		assertFalse(n1.getDhcp().getValue());
		assertTrue(n1.getIpv4Forwarding().getValue());
		assertEquals(IpMasqueradeSetting.IP_V4, n1.getIpMasquerade().getValue());
	}
}
