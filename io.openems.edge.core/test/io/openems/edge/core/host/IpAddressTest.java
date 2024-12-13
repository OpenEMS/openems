package io.openems.edge.core.host;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

public class IpAddressTest {

	@Test
	public void test() throws OpenemsNamedException {
		String jsonInput = "{\"stdout\":[\"[{\\\"ifindex\\\":1,\\\"ifname\\\":\\\"lo\\\",\\\"flags\\\":[\\\"LOOPBACK\\\",\\\"UP\\\",\\\"LOWER_UP\\\"],\\\"mtu\\\":65536,\\\"qdisc\\\":\\\"noqueue\\\",\\\"operstate\\\":\\\"UNKNOWN\\\",\\\"group\\\":\\\"default\\\",\\\"txqlen\\\":1000,\\\"addr_info\\\":[{\\\"family\\\":\\\"inet\\\",\\\"local\\\":\\\"127.0.0.1\\\",\\\"prefixlen\\\":8,\\\"scope\\\":\\\"host\\\",\\\"label\\\":\\\"lo\\\",\\\"valid_life_time\\\":4294967295,\\\"preferred_life_time\\\":4294967295}]},{\\\"ifindex\\\":2,\\\"ifname\\\":\\\"eth0\\\",\\\"flags\\\":[\\\"BROADCAST\\\",\\\"MULTICAST\\\",\\\"UP\\\",\\\"LOWER_UP\\\"],\\\"mtu\\\":1500,\\\"qdisc\\\":\\\"pfifo_fast\\\",\\\"operstate\\\":\\\"UP\\\",\\\"group\\\":\\\"default\\\",\\\"txqlen\\\":1000,\\\"addr_info\\\":[{\\\"family\\\":\\\"inet\\\",\\\"local\\\":\\\"169.254.97.140\\\",\\\"prefixlen\\\":16,\\\"broadcast\\\":\\\"169.254.255.255\\\",\\\"scope\\\":\\\"link\\\",\\\"label\\\":\\\"eth0\\\",\\\"valid_life_time\\\":4294967295,\\\"preferred_life_time\\\":4294967295},{\\\"family\\\":\\\"inet\\\",\\\"local\\\":\\\"192.168.100.100\\\",\\\"prefixlen\\\":24,\\\"broadcast\\\":\\\"192.168.100.255\\\",\\\"scope\\\":\\\"global\\\",\\\"label\\\":\\\"eth0\\\",\\\"valid_life_time\\\":4294967295,\\\"preferred_life_time\\\":4294967295},{\\\"family\\\":\\\"inet\\\",\\\"local\\\":\\\"192.168.25.10\\\",\\\"prefixlen\\\":24,\\\"broadcast\\\":\\\"192.168.25.255\\\",\\\"scope\\\":\\\"global\\\",\\\"label\\\":\\\"Evcs\\\",\\\"valid_life_time\\\":4294967295,\\\"preferred_life_time\\\":4294967295},{\\\"family\\\":\\\"inet\\\",\\\"local\\\":\\\"10.0.3.217\\\",\\\"prefixlen\\\":16,\\\"broadcast\\\":\\\"10.0.255.255\\\",\\\"scope\\\":\\\"global\\\",\\\"dynamic\\\":true,\\\"label\\\":\\\"eth0\\\",\\\"valid_life_time\\\":21474407,\\\"preferred_life_time\\\":21474407}]}]\"],\"stderr\":[],\"exitcode\":0}\n";
		assertEquals("5 Ip Addresses", 5, OperatingSystemDebianSystemd.parseIpJson(jsonInput).size());
		String emptyInput = "{" //
				+ "    \"stdout\": [" //
				+ "        \"[]\"" //
				+ "    ]," //
				+ "    \"stderr\": []," //
				+ "    \"exitcode\": 0" //
				+ "}"; //
		assertEquals("0 Ip Addresses", 0, OperatingSystemDebianSystemd.parseIpJson(emptyInput).size());
		String failedInput = "{" //
				+ "    \"stdout\": [" //
				+ "        \"Device eth4 does not exist\"" //
				+ "    ]," //
				+ "    \"stderr\": []," //
				+ "    \"exitcode\": 0" //
				+ "}"; //
		assertEquals("0 Ip Addresses", 0, OperatingSystemDebianSystemd.parseIpJson(failedInput).size());
	}
}
