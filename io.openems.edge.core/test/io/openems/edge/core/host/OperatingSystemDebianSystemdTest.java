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
				"Address=192.168.100.100/24" //
		);

		NetworkInterface<Object> n = OperatingSystemDebianSystemd.parseSystemdNetworkdConfigurationFile(lines, null);

		assertEquals("eth0", n.getName());
		assertEquals(true, n.getDhcp().getValue());
		assertEquals(true, n.getLinkLocalAddressing().getValue());
		assertEquals("192.168.100.100/24", n.getAddresses().getValue().toArray()[0].toString());
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
