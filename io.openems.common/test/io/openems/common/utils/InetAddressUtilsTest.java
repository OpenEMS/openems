package io.openems.common.utils;

import static org.junit.Assert.assertEquals;

import java.net.Inet4Address;
import java.net.UnknownHostException;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;

public class InetAddressUtilsTest {

	private static final Inet4Address IP;
	static {
		Inet4Address ip = null;
		try {
			ip = (Inet4Address) Inet4Address.getByName("192.168.1.2");
		} catch (UnknownHostException uhe) {
			// Handle exception.
		}
		IP = ip;
	}

	@Test
	public void testParse() throws UnknownHostException {
		assertEquals(null, InetAddressUtils.parseOrNull(null));
		assertEquals(null, InetAddressUtils.parseOrNull(""));
		assertEquals(null, InetAddressUtils.parseOrNull("256.256.256.0"));
		assertEquals(IP, InetAddressUtils.parseOrNull("192.168.1.2"));
	}

	@Test(expected = OpenemsException.class)
	public void testParseOrError1() throws OpenemsException {
		InetAddressUtils.parseOrError(null);
	}

	@Test(expected = OpenemsException.class)
	public void testParseOrError2() throws OpenemsException {
		InetAddressUtils.parseOrError("");
	}

	@Test(expected = OpenemsException.class)
	public void testParseOrError3() throws OpenemsException {
		InetAddressUtils.parseOrError("256.256.256.0");
	}

	@Test
	public void testParseOrError4() throws OpenemsException {
		assertEquals(IP, InetAddressUtils.parseOrError("192.168.1.2"));
	}

}
