package io.openems.common;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

//CHECKSTYLE:OFF
public class OpenemsOEMTest {
	// CHECKSTYLE:ON

	@Test
	public void testGetSystemUpdateLatestVersionUrl() {
		assertEquals("https://fenecon.de/fems-download/fems-latest.version",
				OpenemsOEM.getSystemUpdateLatestVersionUrl(""));
		assertEquals("https://fenecon.de/fems-download/fems-latest.version",
				OpenemsOEM.getSystemUpdateLatestVersionUrl(null));

		assertEquals("https://dev.intranet.fenecon.de/feature/foo-bar/fems.version",
				OpenemsOEM.getSystemUpdateLatestVersionUrl("feature/foo-bar"));
	}

	@Test
	public void testGetSystemUpdateScriptParams() {
		assertEquals("", OpenemsOEM.getSystemUpdateScriptParams(""));
		assertEquals("", OpenemsOEM.getSystemUpdateScriptParams(null));

		assertEquals(" -fb \"feature/foo-bar\"", OpenemsOEM.getSystemUpdateScriptParams("feature/foo-bar"));
	}

}
