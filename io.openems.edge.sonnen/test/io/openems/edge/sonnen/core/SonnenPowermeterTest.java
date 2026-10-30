package io.openems.edge.sonnen.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;

public class SonnenPowermeterTest {

	/**
	 * Example entry of the 'consumption' meter from the sonnen API
	 * documentation.
	 */
	private static final String CONSUMPTION_JSON = """
			{
				"a_l1": 1.6,
				"a_l2": 2.1,
				"a_l3": 0.9,
				"channel": 2,
				"deviceid": 4,
				"direction": "consumption",
				"error": -1,
				"kwh_exported": 1000.5,
				"kwh_imported": 2000.7,
				"v_l1_n": 229.5,
				"v_l2_n": 230.1,
				"v_l3_n": 228.9,
				"va_total": 1500.2,
				"var_total": -123.4,
				"w_l1": 366.7,
				"w_l2": 483.6,
				"w_l3": 205.1,
				"w_total": 1055.4
			}""";

	private static final String PRODUCTION_JSON = """
			{
				"channel": 1,
				"deviceid": 4,
				"direction": "production",
				"w_l1": -100.4,
				"w_total": -301.2
			}""";

	@Test
	public void testFrom() throws OpenemsNamedException {
		var powermeter = SonnenPowermeter.from(JsonUtils.parse(CONSUMPTION_JSON));

		assertEquals("consumption", powermeter.direction());
		assertEquals(1055, powermeter.wTotal());
		assertEquals(367, powermeter.wL1());
		assertEquals(484, powermeter.wL2());
		assertEquals(205, powermeter.wL3());
		assertEquals(-123, powermeter.varTotal());
		assertEquals(1.6F, powermeter.currentL1());
		assertEquals(229.5F, powermeter.voltageL1N());
		assertEquals(2000L, powermeter.kwhImported());
		assertEquals(1000L, powermeter.kwhExported());
		assertTrue(powermeter.isConsumption());
	}

	@Test
	public void testProductionEntry() throws OpenemsNamedException {
		var powermeter = SonnenPowermeter.from(JsonUtils.parse(PRODUCTION_JSON));

		assertEquals("production", powermeter.direction());
		assertEquals(-301, powermeter.wTotal());
		assertEquals(-100, powermeter.wL1());
		assertFalse(powermeter.isConsumption());
	}

	@Test
	public void testEntryWithoutDirectionIsConsumption() throws OpenemsNamedException {
		// Older firmwares provide the grid meter only, without a 'direction' field
		var powermeter = SonnenPowermeter.from(JsonUtils.parse("{\"w_total\": 500}"));
		assertTrue(powermeter.isConsumption());
		assertEquals(500, powermeter.wTotal());
	}
}
