package io.openems.edge.pytes.ess;

import static io.openems.edge.pytes.ess.AllowedChargeDischargeHandler.minWithInverterLimit;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AllowedChargeDischargeHandlerTest {

	@Test
	public void inverterLimitOnlyLowersTheLimit() {
		assertEquals(40, minWithInverterLimit(40, 50_000)); // BMS/config 40 A, inverter 50 A
		assertEquals(40, minWithInverterLimit(50, 40_000)); // inverter 40 A
		assertEquals(48, minWithInverterLimit(50, 48_300)); // 48.3 A rounded down
	}

	@Test
	public void placeholdersAndMissingValuesAreIgnored() {
		assertEquals(40, minWithInverterLimit(40, null)); // not read yet
		assertEquals(40, minWithInverterLimit(40, 0)); // not set
		assertEquals(40, minWithInverterLimit(40, 999_000)); // 999.0 A placeholder (regs 43012/43013)
		assertEquals(40, minWithInverterLimit(40, 150_001)); // above the datasheet range
	}
}
