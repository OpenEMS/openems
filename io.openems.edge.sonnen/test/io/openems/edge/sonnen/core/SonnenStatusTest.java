package io.openems.edge.sonnen.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;

public class SonnenStatusTest {

	/**
	 * Example response from the sonnen API documentation.
	 */
	private static final String EXAMPLE_JSON = """
			{
				"Apparent_output": 96,
				"BackupBuffer": "0",
				"BatteryCharging": false,
				"BatteryDischarging": false,
				"Consumption_W": 0,
				"Fac": 50.0239143371582,
				"FlowConsumptionBattery": false,
				"FlowConsumptionGrid": false,
				"FlowConsumptionProduction": false,
				"FlowGridBattery": false,
				"FlowProductionBattery": false,
				"FlowProductionGrid": false,
				"GridFeedIn_W": -3,
				"IsSystemInstalled": 1,
				"OperatingMode": "2",
				"Pac_total_W": 7,
				"Production_W": 0,
				"RSOC": 21,
				"Sac1": 96,
				"Sac2": null,
				"Sac3": null,
				"SystemStatus": "OnGrid",
				"Timestamp": "2020-03-26 17:10:06",
				"USOC": 12,
				"Uac": 238,
				"Ubat": 209,
				"dischargeNotAllowed": false,
				"generator_autostart": false
			}""";

	@Test
	public void testFrom() throws OpenemsNamedException {
		var status = SonnenStatus.from(JsonUtils.parse(EXAMPLE_JSON));

		// ESS
		assertEquals(7, status.pacTotalW());
		assertEquals(12, status.usoc());
		assertEquals(21, status.rsoc());
		assertFalse(status.batteryCharging());
		assertFalse(status.batteryDischarging());
		assertFalse(status.dischargeNotAllowed());
		assertEquals(OperatingMode.SELF_CONSUMPTION, status.operatingMode());

		// PV and grid
		assertEquals(0, status.productionW());
		assertEquals(-3, status.gridFeedInW());
		assertEquals(0, status.consumptionW());

		// General
		assertEquals(50.0239143371582F, status.fac());
		assertEquals(238, status.uac());
		assertEquals(209, status.ubat());

		// RemainingCapacity_Wh is not part of the example
		assertNull(status.remainingCapacityWh());
		assertNull(status.consumptionAvgW());
	}

	@Test
	public void testOperatingModeAsNumber() throws OpenemsNamedException {
		var status = SonnenStatus.from(JsonUtils.parse("{\"OperatingMode\": 1}"));
		assertEquals(OperatingMode.MANUAL, status.operatingMode());
	}

	@Test
	public void testUnknownOperatingMode() throws OpenemsNamedException {
		var status = SonnenStatus.from(JsonUtils.parse("{\"OperatingMode\": 99}"));
		assertEquals(OperatingMode.UNDEFINED, status.operatingMode());
	}

	@Test
	public void testIsOffGrid() throws OpenemsNamedException {
		assertTrue(SonnenStatus.from(JsonUtils.parse("{\"SystemStatus\": \"OffGrid\"}")).isOffGrid());
		assertFalse(SonnenStatus.from(JsonUtils.parse("{\"SystemStatus\": \"OnGrid\"}")).isOffGrid());
		assertNull(SonnenStatus.from(JsonUtils.parse("{}")).isOffGrid());
	}
}
