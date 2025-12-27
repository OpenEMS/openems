package io.openems.edge.battery.victron;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.victron.battery.VictronBattery;
import io.openems.edge.victron.battery.VictronBatteryImpl;
import io.openems.edge.common.test.ComponentTest;

public class VictronCanBusBatteryImplTest {

	private static final String BATTERY_ID = "battery0";
	private static final String MODBUS_ID = "modbus0";
	private static final String ESS_ID = "ess0";

	private static final ChannelAddress BATTERY_SOC = new ChannelAddress(BATTERY_ID, "Soc");
	private static final ChannelAddress BATTERY_VOLTAGE = new ChannelAddress(BATTERY_ID, "Voltage");
	private static final ChannelAddress BATTERY_CURRENT = new ChannelAddress(BATTERY_ID, "Current");

	@Test
	public void test() throws Exception {
		new ComponentTest(new VictronBatteryImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID) //
						.withRegisters(259,
								// VOLTAGE (register 259) - 5200 = 52.00V
								5200,
								// STARTER_BATTERY_VOLTAGE (register 260) - 1200 = 12.00V
								1200,
								// CURRENT (register 261) - 100 = 10.0A (signed)
								100,
								// TEMPERATURE (register 262) - 250 = 25.0°C
								250,
								// MID_VOLTAGE (register 263) - 2600 = 26.00V
								2600,
								// MID_VOLTAGE_DEVIATION (register 264) - 10 = 0.10V
								10,
								// CONSUMED_AMPHOURS (register 265) - 100 = 10.0Ah
								100,
								// SOC (register 266) - 850 = 85.0%
								850,
								// ALARM (register 267)
								0,
								// LOW_VOLTAGE_ALARM (register 268)
								0,
								// HIGH_VOLTAGE_ALARM (register 269)
								0,
								// LOW_STARTER_VOLTAGE_ALARM (register 270)
								0,
								// HIGH_STARTER_VOLTAGE_ALARM (register 271)
								0,
								// LOW_STATE_OF_CHARGE_ALARM (register 272)
								0,
								// LOW_TEMPERATURE_ALARM (register 273)
								0,
								// HIGH_TEMPERATURE_ALARM (register 274)
								0,
								// MID_VOLTAGE_ALARM (register 275)
								0,
								// LOW_FUSED_VOLTAGE_ALARM (register 276)
								0,
								// HIGH_FUSED_VOLTAGE_ALARM (register 277)
								0,
								// FUSE_BLOWN_ALARM (register 278)
								0,
								// HIGH_INTERNAL_TEMPERATURE_ALARM (register 279)
								0,
								// RELAY_STATUS (register 280)
								0,
								// DEEPEST_DISCHARGE (register 281)
								500,
								// LAST_DISCHARGE (register 282)
								100,
								// AVERAGE_DISCHARGE (register 283)
								200,
								// CHARGE_CYCLES (register 284)
								50,
								// FULL_DISCHARGES (register 285)
								5,
								// TOTAL_AMPHOURS_DRAWN (register 286)
								1000,
								// HISTORY_MIN_VOLTAGE (register 287)
								4800,
								// HISTORY_MAX_VOLTAGE (register 288)
								5600,
								// TIME_SINCE_LAST_FULL_CHARGE (register 289)
								100,
								// AUTOMATIC_SYNCS (register 290)
								10,
								// LOW_VOLTAGE_ALARMS (register 291)
								0,
								// HIGH_VOLTAGE_ALARMS (register 292)
								0,
								// LOW_STARTER_VOLTAGE_ALARMS (register 293)
								0,
								// HIGH_STARTER_VOLTAGE_ALARMS (register 294)
								0,
								// MIN_STARTER_VOLTAGE (register 295)
								1100,
								// MAX_STARTER_VOLTAGE (register 296)
								1400,
								// LOW_FUSED_VOLTAGE_ALARMS (register 297)
								0,
								// HIGH_FUSED_VOLTAGE_ALARMS (register 298)
								0,
								// MIN_FUSED_VOLTAGE (register 299)
								4700,
								// MAX_FUSED_VOLTAGE (register 300)
								5700,
								// DC_DISCHARGED_ENERGY (register 301)
								100,
								// DC_CHARGED_ENERGY (register 302)
								120,
								// TIME_TO_GO (register 303)
								500,
								// SOH (register 304) - 980 = 98.0%
								980,
								// CHARGE_MAX_VOLTAGE (register 305) - 580 = 58.0V
								580,
								// DISCHARGE_MIN_VOLTAGE (register 306) - 440 = 44.0V
								440,
								// CHARGE_MAX_CURRENT (register 307) - 500 = 50.0A
								500,
								// DISCHARGE_MAX_CURRENT (register 308) - 500 = 50.0A
								500,
								// CAPACITY_IN_AMPHOURS (register 309) - 1000 = 100.0Ah
								1000,
								// TIMESTAMP_1ST_LAST_ERROR (register 310-311)
								0, 0,
								// TIMESTAMP_2ND_LAST_ERROR (register 312-313)
								0, 0,
								// TIMESTAMP_3RD_LAST_ERROR (register 314-315)
								0, 0,
								// TIMESTAMP_4TH_LAST_ERROR (register 316-317)
								0, 0,
								// MIN_CELL_TEMPERATURE (register 318) - 200 = 20.0°C
								200,
								// MAX_CELL_TEMPERATURE (register 319) - 300 = 30.0°C
								300,
								// HIGH_CHARGE_CURRENT_ALARM (register 320)
								0,
								// HIGH_DISCHARGE_CURRENT_ALARM (register 321)
								0,
								// CELL_IMBALANCE_ALARM (register 322)
								0,
								// INTERNAL_FAILURE_ALARM (register 323)
								0,
								// HIGH_CHARGE_TEMPERATURE_ALARM (register 324)
								0,
								// LOW_CHARGE_TEMPERATURE_ALARM (register 325)
								0,
								// LOW_CELL_VOLTAGE_ALARM (register 326)
								0)
						.withRegisters(1282,
								// VICTRON_STATE (register 1282)
								0,
								// ERROR (register 1283)
								0,
								// SYSTEM_SWITCH (register 1284)
								1,
								// BALANCING (register 1285)
								0,
								// NUMBER_OF_BATTERIES (register 1286)
								1,
								// BATTERIES_PARALLEL (register 1287)
								1,
								// BATTERIES_SERIES (register 1288)
								1,
								// NUMBER_OF_CELLS_PER_BATTERY (register 1289)
								16,
								// MIN_CELL_VOLTAGE (register 1290) - 320 = 3.20V
								320,
								// MAX_CELL_VOLTAGE (register 1291) - 340 = 3.40V
								340,
								// SHUTDOWNS_DUE_ERROR (register 1292)
								0,
								// DIAGNOSTICS_1ST_LAST_ERROR (register 1293)
								0,
								// DIAGNOSTICS_2ND_LAST_ERROR (register 1294)
								0,
								// DIAGNOSTICS_3RD_LAST_ERROR (register 1295)
								0,
								// DIAGNOSTICS_4TH_LAST_ERROR (register 1296)
								0,
								// ALLOW_TO_CHARGE (register 1297)
								1,
								// ALLOW_TO_DISCHARGE (register 1298)
								1,
								// EXTERNAL_RELAY (register 1299)
								0,
								// HISTORY_MIN_CELL_VOLTAGE (register 1300)
								300,
								// HISTORY_MAX_CELL_VOLTAGE (register 1301)
								360)) //
				.activate(MyConfig.create() //
						.setId(BATTERY_ID) //
						.setAlias("Victron Battery") //
						.setEnabled(true) //
						.setModbusId(MODBUS_ID) //
						.setEssId(ESS_ID) //
						.setDebugMode(false) //
						.build()) //
				.next(new TestCase()) // First cycle to read registers
				.next(new TestCase() //
						.output(BATTERY_SOC, 85) //
						.output(BATTERY_VOLTAGE, 52) // 5200 / 100 (SCALE_FACTOR_MINUS_2) = 52V
						.output(BATTERY_CURRENT, 10)); // 100 / 10 (SCALE_FACTOR_MINUS_1) = 10A
	}

	@Test
	public void testChannelIds() {
		var channelIds = VictronBattery.ChannelId.values();
		for (var channelId : channelIds) {
			org.junit.Assert.assertNotNull("ChannelId " + channelId.name() + " should have a doc", channelId.doc());
		}
	}

	@Test
	public void testConstants() {
		org.junit.Assert.assertEquals(225, VictronBatteryImpl.DEFAULT_UNIT_ID);
		org.junit.Assert.assertEquals(48, VictronBatteryImpl.BATTERY_VOLTAGE);
	}

}
