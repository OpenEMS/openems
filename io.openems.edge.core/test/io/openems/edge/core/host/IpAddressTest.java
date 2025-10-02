package io.openems.edge.core.host;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

public class IpAddressTest {

	private final String jsonInput = "{\"stdout\":[\"[{\\\"ifindex\\\":1,\\\"ifname\\\":\\\"lo\\\",\\\"flags\\\":[\\\"LOOPBACK\\\",\\\"UP\\\",\\\"LOWER_UP\\\"],\\\"mtu\\\":65536,\\\"qdisc\\\":\\\"noqueue\\\",\\\"operstate\\\":\\\"UNKNOWN\\\",\\\"group\\\":\\\"default\\\",\\\"txqlen\\\":1000,\\\"addr_info\\\":[{\\\"family\\\":\\\"inet\\\",\\\"local\\\":\\\"127.0.0.1\\\",\\\"prefixlen\\\":8,\\\"scope\\\":\\\"host\\\",\\\"label\\\":\\\"lo\\\",\\\"valid_life_time\\\":4294967295,\\\"preferred_life_time\\\":4294967295}]},{\\\"ifindex\\\":2,\\\"ifname\\\":\\\"eth0\\\",\\\"flags\\\":[\\\"BROADCAST\\\",\\\"MULTICAST\\\",\\\"UP\\\",\\\"LOWER_UP\\\"],\\\"mtu\\\":1500,\\\"qdisc\\\":\\\"pfifo_fast\\\",\\\"operstate\\\":\\\"UP\\\",\\\"group\\\":\\\"default\\\",\\\"txqlen\\\":1000,\\\"addr_info\\\":[{\\\"family\\\":\\\"inet\\\",\\\"local\\\":\\\"169.254.97.140\\\",\\\"prefixlen\\\":16,\\\"broadcast\\\":\\\"169.254.255.255\\\",\\\"scope\\\":\\\"link\\\",\\\"label\\\":\\\"eth0\\\",\\\"valid_life_time\\\":4294967295,\\\"preferred_life_time\\\":4294967295},{\\\"family\\\":\\\"inet\\\",\\\"local\\\":\\\"192.168.100.100\\\",\\\"prefixlen\\\":24,\\\"broadcast\\\":\\\"192.168.100.255\\\",\\\"scope\\\":\\\"global\\\",\\\"label\\\":\\\"eth0\\\",\\\"valid_life_time\\\":4294967295,\\\"preferred_life_time\\\":4294967295},{\\\"family\\\":\\\"inet\\\",\\\"local\\\":\\\"192.168.25.10\\\",\\\"prefixlen\\\":24,\\\"broadcast\\\":\\\"192.168.25.255\\\",\\\"scope\\\":\\\"global\\\",\\\"label\\\":\\\"Evcs\\\",\\\"valid_life_time\\\":4294967295,\\\"preferred_life_time\\\":4294967295},{\\\"family\\\":\\\"inet\\\",\\\"local\\\":\\\"10.0.3.217\\\",\\\"prefixlen\\\":16,\\\"broadcast\\\":\\\"10.0.255.255\\\",\\\"scope\\\":\\\"global\\\",\\\"dynamic\\\":true,\\\"label\\\":\\\"eth0\\\",\\\"valid_life_time\\\":21474407,\\\"preferred_life_time\\\":21474407}]}]\"],\"stderr\":[],\"exitcode\":0}\n";
	private final String routeInput = " {\"stdout\":[\"[{\\\"dst\\\":\\\"default\\\",\\\"gateway\\\":\\\"10.0.0.1\\\",\\\"dev\\\":\\\"eth0\\\",\\\"protocol\\\":\\\"dhcp\\\",\\\"prefsrc\\\":\\\"10.0.3.217\\\",\\\"metric\\\":1024,\\\"flags\\\":[]},{\\\"dst\\\":\\\"10.0.0.0/16\\\",\\\"dev\\\":\\\"eth0\\\",\\\"protocol\\\":\\\"kernel\\\",\\\"scope\\\":\\\"link\\\",\\\"prefsrc\\\":\\\"10.0.3.217\\\",\\\"flags\\\":[]},{\\\"dst\\\":\\\"10.0.0.1\\\",\\\"dev\\\":\\\"eth0\\\",\\\"protocol\\\":\\\"dhcp\\\",\\\"scope\\\":\\\"link\\\",\\\"prefsrc\\\":\\\"10.0.3.217\\\",\\\"metric\\\":1024,\\\"flags\\\":[]},{\\\"dst\\\":\\\"169.254.0.0/16\\\",\\\"dev\\\":\\\"eth0\\\",\\\"protocol\\\":\\\"kernel\\\",\\\"scope\\\":\\\"link\\\",\\\"prefsrc\\\":\\\"169.254.97.140\\\",\\\"flags\\\":[]},{\\\"dst\\\":\\\"192.168.25.0/24\\\",\\\"dev\\\":\\\"eth0\\\",\\\"protocol\\\":\\\"kernel\\\",\\\"scope\\\":\\\"link\\\",\\\"prefsrc\\\":\\\"192.168.25.10\\\",\\\"flags\\\":[]},{\\\"dst\\\":\\\"192.168.100.0/24\\\",\\\"dev\\\":\\\"eth0\\\",\\\"protocol\\\":\\\"kernel\\\",\\\"scope\\\":\\\"link\\\",\\\"prefsrc\\\":\\\"192.168.100.100\\\",\\\"flags\\\":[]}]\"],\"stderr\":[],\"exitcode\":0}";
	private final String emptyInput = "{" //
			+ "    \"stdout\": [" //
			+ "        \"[]\"" //
			+ "    ]," //
			+ "    \"stderr\": []," //
			+ "    \"exitcode\": 0" //
			+ "}"; //
	private final String failedInput = "{" //
			+ "    \"stdout\": [" //
			+ "        \"Device eth4 does not exist\"" //
			+ "    ]," //
			+ "    \"stderr\": []," //
			+ "    \"exitcode\": 0" //
			+ "}"; //

	// "ip -j" exists since iproute2 version 4.10.0 starting with Debian 10 (Buster)
	private final String stderr = "{" //
			+ "    \"stdout\": []," //
			+ "    \"stderr\": [" //
			+ "        \"ip: invalid argument -j\"" //
			+ "    ]," //
			+ "    \"exitcode\": 0" //
			+ "}"; //

	@Test
	public void testShow() throws OpenemsNamedException {
		assertEquals("2 NetworkInterfaces", 2, OperatingSystemDebianSystemd.parseShowJson(this.jsonInput).size());

		assertEquals("0 NetworkInterfaces", 0, OperatingSystemDebianSystemd.parseShowJson(this.emptyInput).size());

		assertEquals("0 NetworkInterfaces", 0, OperatingSystemDebianSystemd.parseShowJson(this.failedInput).size());

		assertEquals("0 NetworkInterfaces", 0, OperatingSystemDebianSystemd.parseShowJson(this.stderr).size());
	}

	@Test
	public void testRoute() throws OpenemsNamedException {
		assertEquals("6 Routing Infos", 6, OperatingSystemDebianSystemd.parseRouteJson(this.routeInput).size());
	}
}
