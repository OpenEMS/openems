package io.openems.edge.oem.fenecon;

import static io.openems.edge.oem.fenecon.FeneconEdgeOemImpl.getSystemUpdateParams;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FeneconEdgeOemImplTest {

	@Test
	public void testGetSystemUpdateParamsRelease() {
		var p = getSystemUpdateParams(null);
		assertEquals("https://fenecon.de/fems-download/fems-latest.version", p.latestVersionUrl());
		assertEquals("", p.updateScriptParams());
	}

	@Test
	public void testGetSystemUpdateParamsDev() {
		var p = getSystemUpdateParams("feature/foo-bar");
		assertEquals("https://dev.intranet.fenecon.de/feature/foo-bar/fems.version", p.latestVersionUrl());
		assertEquals("-fb \"feature/foo-bar\"", p.updateScriptParams());
	}
}
