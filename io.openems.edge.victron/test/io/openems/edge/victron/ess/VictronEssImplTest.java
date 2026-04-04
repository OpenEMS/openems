package io.openems.edge.victron.ess;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.type.Phase.SingleOrAllPhase;
import io.openems.edge.ess.test.DummyPower;
import io.openems.edge.timedata.test.DummyTimedata;
import io.openems.edge.victron.battery.VictronBatteryImpl;
import io.openems.edge.victron.batteryinverter.VictronBatteryInverterImpl;

/**
 * Tests for {@link VictronEssImpl}.
 */
public class VictronEssImplTest {

	private static final String ESS_ID = "ess0";
	private static final String BATTERY_INVERTER_ID = "batteryInverter0";
	private static final String BATTERY_ID = "battery0";
	private static final String MODBUS_ID = "modbus0";
	private static final String TIMEDATA_ID = "timedata0";

	private static final ChannelAddress ESS_SOC = new ChannelAddress(ESS_ID, "Soc");
	private static final ChannelAddress ESS_VOLTAGE_INPUT_L1 = new ChannelAddress(ESS_ID, "VoltageInputL1");
	private static final ChannelAddress ESS_CURRENT_INPUT_L1 = new ChannelAddress(ESS_ID, "CurrentInputL1");
	private static final ChannelAddress ESS_ACTIVE_POWER_INPUT_L1 = new ChannelAddress(ESS_ID, "ActivePowerInputL1");
	private static final ChannelAddress ESS_ACTIVE_POWER_OUTPUT_L1 = new ChannelAddress(ESS_ID, "ActivePowerOutputL1");
	private static final ChannelAddress ESS_BATTERY_VOLTAGE = new ChannelAddress(ESS_ID, "BatteryVoltage");
	private static final ChannelAddress ESS_BATTERY_CURRENT = new ChannelAddress(ESS_ID, "BatteryCurrent");
	private static final ChannelAddress ESS_PHASE_COUNT = new ChannelAddress(ESS_ID, "PhaseCount");

	@Test
	public void test() throws Exception {
		new ComponentTest(new VictronEssImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("power", new DummyPower()) //
				.addReference("timedata", new DummyTimedata(TIMEDATA_ID)) //
				.addReference("batteryInverter", new VictronBatteryInverterImpl()) //
				.addReference("battery", new VictronBatteryImpl()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID) //
						.withRegisters(3,
								// VOLTAGE_INPUT_L1 (register 3) - 2300 = 230.00V (SCALE_FACTOR_2)
								2300,
								// VOLTAGE_INPUT_L2 (register 4) - 2310 = 231.00V
								2310,
								// VOLTAGE_INPUT_L3 (register 5) - 2290 = 229.00V
								2290,
								// CURRENT_INPUT_L1 (register 6) - 100 = 10.00A (SCALE_FACTOR_2, signed)
								100,
								// CURRENT_INPUT_L2 (register 7) - 90 = 9.00A
								90,
								// CURRENT_INPUT_L3 (register 8) - 80 = 8.00A
								80,
								// FREQUENCY_INPUT_L1 (register 9) - 500 = 50.0Hz (SCALE_FACTOR_1)
								500,
								// FREQUENCY_INPUT_L2 (register 10)
								500,
								// FREQUENCY_INPUT_L3 (register 11)
								500,
								// ACTIVE_POWER_INPUT_L1 (register 12) - 1000W (SCALE_FACTOR_1_AND_INVERT)
								1000,
								// ACTIVE_POWER_INPUT_L2 (register 13)
								800,
								// ACTIVE_POWER_INPUT_L3 (register 14)
								600,
								// VOLTAGE_OUTPUT_L1 (register 15) - 2300 = 230.00V (SCALE_FACTOR_2)
								2300,
								// VOLTAGE_OUTPUT_L2 (register 16)
								2310,
								// VOLTAGE_OUTPUT_L3 (register 17)
								2290,
								// CURRENT_OUTPUT_L1 (register 18) - 50 = 5.00A (SCALE_FACTOR_2, signed)
								50,
								// CURRENT_OUTPUT_L2 (register 19)
								40,
								// CURRENT_OUTPUT_L3 (register 20)
								30,
								// FREQUENCY_OUTPUT (register 21) - 500 = 50.0Hz
								500,
								// CURRENT_INPUT_LIMIT (register 22) - 160 = 16.0A (SCALE_FACTOR_MINUS_1)
								160,
								// ACTIVE_POWER_OUTPUT_L1 (register 23) - 500W
								500,
								// ACTIVE_POWER_OUTPUT_L2 (register 24) - 400W
								400,
								// ACTIVE_POWER_OUTPUT_L3 (register 25) - 300W
								300,
								// BATTERY_VOLTAGE (register 26) - 5200 = 52.00V (SCALE_FACTOR_MINUS_2)
								5200,
								// BATTERY_CURRENT (register 27) - 100 = 10.0A (SCALE_FACTOR_MINUS_1, signed)
								100,
								// PHASE_COUNT (register 28)
								3,
								// ACTIVE_INPUT (register 29)
								1,
								// SOC (register 30) - 850 = 85.0% (SCALE_FACTOR_MINUS_1)
								850,
								// VE_BUS_STATE (register 31) - 9 = Inverting
								9,
								// VE_BUS_ERROR (register 32)
								0,
								// SWITCH_POSITION (register 33)
								3,
								// TEMPERATURE_ALARM (register 34)
								0,
								// LOW_BATTERY_ALARM (register 35)
								0,
								// OVERLOAD_ALARM (register 36)
								0,
								// ESS_POWER_SETPOINT_PHASE_1 (register 37)
								0,
								// ESS_DISABLE_CHARGE_FLAG (register 38)
								0,
								// ESS_DISABLE_FEEDBACK_FLAG (register 39)
								0,
								// ESS_POWER_SETPOINT_PHASE_2 (register 40)
								0,
								// ESS_POWER_SETPOINT_PHASE_3 (register 41)
								0,
								// TEMPERATURE_SENSOR_ALARM (register 42)
								0,
								// VOLTAGE_SENSOR_ALARM (register 43)
								0,
								// TEMPERATURE_ALARM_L1 (register 44)
								0,
								// LOW_BATTERY_ALARM_L1 (register 45)
								0,
								// OVERLOAD_ALARM_L1 (register 46)
								0,
								// RIPPLE_ALARM_L1 (register 47)
								0,
								// TEMPERATURE_ALARM_L2 (register 48)
								0,
								// LOW_BATTERY_ALARM_L2 (register 49)
								0,
								// OVERLOAD_ALARM_L2 (register 50)
								0,
								// RIPPLE_ALARM_L2 (register 51)
								0,
								// TEMPERATURE_ALARM_L3 (register 52)
								0,
								// LOW_BATTERY_ALARM_L3 (register 53)
								0,
								// OVERLOAD_ALARM_L3 (register 54)
								0,
								// RIPPLE_ALARM_L3 (register 55)
								0,
								// DISABLE_PV_INVERTER (register 56)
								0,
								// VE_BUS_BMS_ALLOW_BATTERY_CHARGE (register 57) - 1 = Allowed
								1,
								// VE_BUS_BMS_ALLOW_BATTERY_DISCHARGE (register 58) - 1 = Allowed
								1,
								// VE_BUS_BMS_EXPECTED (register 59)
								1,
								// VE_BUS_BMS_ERROR (register 60)
								0,
								// BATTERY_TEMPERATURE (register 61) - 250 = 25.0°C (SCALE_FACTOR_MINUS_1)
								250,
								// VE_BUS_RESET (register 62)
								0,
								// PHASE_ROTATION_WARNING (register 63)
								0,
								// GRID_LOST_ALARM (register 64)
								0,
								// FEED_DC_OVERVOLTAGE_TO_GRID (register 65)
								0,
								// MAX_DC_OVERVOLTAGE_POWER_TO_GRID_L1 (register 66)
								0,
								// MAX_DC_OVERVOLTAGE_POWER_TO_GRID_L2 (register 67)
								0,
								// MAX_DC_OVERVOLTAGE_POWER_TO_GRID_L3 (register 68)
								0,
								// AC_INPUT1_IGNORED (register 69)
								0,
								// AC_INPUT2_IGNORED (register 70)
								0,
								// AC_POWER_SETPOINT_AS_FEED_IN_LIMIT (register 71)
								0,
								// SOLAR_OFFSET_VOLTAGE (register 72)
								0,
								// SUSTAIN_ACTIVE (register 73)
								0,
								// ENERGY_FROM_AC_IN_1_TO_AC_OUT (register 74-75) - double word
								0, 0,
								// ENERGY_FROM_AC_IN_1_TO_BATTERY (register 76-77)
								0, 0,
								// ENERGY_FROM_AC_IN_2_TO_AC_OUT (register 78-79)
								0, 0,
								// ENERGY_FROM_AC_IN_2_TO_BATTERY (register 80-81)
								0, 0,
								// ENERGY_FROM_AC_OUT_TO_AC_IN_1 (register 82-83)
								0, 0,
								// ENERGY_FROM_AC_OUT_TO_AC_IN_2 (register 84-85)
								0, 0,
								// ENERGY_FROM_BATTERY_TO_AC_IN_1 (register 86-87)
								0, 0,
								// ENERGY_FROM_BATTERY_TO_AC_IN_2 (register 88-89)
								0, 0,
								// ENERGY_FROM_BATTERY_TO_AC_OUT (register 90-91)
								0, 0,
								// ENERGY_FROM_AC_OUT_TO_BATTERY (register 92-93)
								0, 0,
								// LOW_CELL_VOLTAGE_IMMINENT (register 94)
								0,
								// CHARGE_STATE (register 95)
								1,
								// INT32_ESS_POWER_SETPOINT_PHASE_1 (register 96-97)
								0, 0,
								// INT32_ESS_POWER_SETPOINT_PHASE_2 (register 98-99)
								0, 0,
								// INT32_ESS_POWER_SETPOINT_PHASE_3 (register 100-101)
								0, 0,
								// PREFER_RENEWABLE_ENERGY (register 102)
								0,
								// SELECT_REMOTE_GENERATOR (register 103)
								0,
								// REMOTE_GENERATOR_SELECTED (register 104)
								0)) //
				.activate(MyConfig.create() //
						.setId(ESS_ID) //
						.setAlias("Victron ESS") //
						.setEnabled(true) //
						.setBatteryInverterId(BATTERY_INVERTER_ID) //
						.setBatteryId(BATTERY_ID) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(227) //
						.setPhase(SingleOrAllPhase.ALL) //
						.setDebugMode(false) //
						.setReadOnlyMode(false) //
						.setCapacity(10000) //
						.setMaxApparentPower(5000) //
						.build()) //
				.next(new TestCase()) // First cycle to read registers
				.next(new TestCase() //
						.output(ESS_SOC, 85) // 850 / 10 (SCALE_FACTOR_MINUS_1) = 85%
						.output(ESS_VOLTAGE_INPUT_L1, 230000) // 2300 * 100 (SCALE_FACTOR_2) = 230000mV
						.output(ESS_CURRENT_INPUT_L1, 10000) // 100 * 100 (SCALE_FACTOR_2) = 10000mA
						.output(ESS_ACTIVE_POWER_INPUT_L1, -10000) // 1000 * -10 (SCALE_FACTOR_1_AND_INVERT) = -10000
						.output(ESS_ACTIVE_POWER_OUTPUT_L1, 5000) // 500 * 10 (SCALE_FACTOR_1) = 5000W
						.output(ESS_BATTERY_VOLTAGE, 52) // 5200 / 100 (SCALE_FACTOR_MINUS_2) = 52V
						.output(ESS_BATTERY_CURRENT, 10) // 100 / 10 (SCALE_FACTOR_MINUS_1) = 10A
						.output(ESS_PHASE_COUNT, 3)); // 3 phases
	}

	@Test
	public void testChannelIdCount() {
		// Verify that all ChannelIds are defined
		var channelIds = VictronEss.ChannelId.values();
		assertNotNull(channelIds);
		// Should have many channels defined
		assertEquals(true, channelIds.length > 50);
	}

	@Test
	public void testChannelIdDoc() {
		// Verify that all ChannelIds have a doc
		for (var channelId : VictronEss.ChannelId.values()) {
			assertNotNull("ChannelId " + channelId.name() + " should have a doc", channelId.doc());
		}
	}

	@Test
	public void testConfigBuilder() {
		var config = MyConfig.create() //
				.setId(ESS_ID) //
				.setAlias("Victron ESS") //
				.setEnabled(true) //
				.setBatteryInverterId(BATTERY_INVERTER_ID) //
				.setBatteryId(BATTERY_ID) //
				.setModbusId(MODBUS_ID) //
				.setModbusUnitId(227) //
				.setPhase(SingleOrAllPhase.ALL) //
				.setDebugMode(false) //
				.setReadOnlyMode(false) //
				.setCapacity(10000) //
				.setMaxApparentPower(5000) //
				.build();

		assertEquals(ESS_ID, config.id());
		assertEquals("Victron ESS", config.alias());
		assertEquals(true, config.enabled());
		assertEquals(BATTERY_INVERTER_ID, config.batteryInverter_id());
		assertEquals(BATTERY_ID, config.battery_id());
		assertEquals(MODBUS_ID, config.modbus_id());
		assertEquals(227, config.modbusUnitId());
		assertEquals(SingleOrAllPhase.ALL, config.phase());
		assertEquals(false, config.debugMode());
		assertEquals(false, config.readOnlyMode());
		assertEquals(10000, config.capacity());
		assertEquals(5000, config.maxApparentPower());
	}

	@Test
	public void testConfigSinglePhaseL1() {
		var config = MyConfig.create() //
				.setId(ESS_ID) //
				.setPhase(SingleOrAllPhase.L1) //
				.build();

		assertEquals(SingleOrAllPhase.L1, config.phase());
	}

	@Test
	public void testConfigSinglePhaseL2() {
		var config = MyConfig.create() //
				.setId(ESS_ID) //
				.setPhase(SingleOrAllPhase.L2) //
				.build();

		assertEquals(SingleOrAllPhase.L2, config.phase());
	}

	@Test
	public void testConfigSinglePhaseL3() {
		var config = MyConfig.create() //
				.setId(ESS_ID) //
				.setPhase(SingleOrAllPhase.L3) //
				.build();

		assertEquals(SingleOrAllPhase.L3, config.phase());
	}

	@Test
	public void testConfigReadOnlyMode() {
		var config = MyConfig.create() //
				.setId(ESS_ID) //
				.setReadOnlyMode(true) //
				.build();

		assertEquals(true, config.readOnlyMode());
	}

	@Test
	public void testVictronEssConstructor() {
		var victronEss = new VictronEssImpl();
		assertNotNull(victronEss);
	}

	@Test
	public void testGetPowerPrecision() {
		var victronEss = new VictronEssImpl();
		assertEquals(100, victronEss.getPowerPrecision());
	}

	@Test
	public void testCalculateAcInSetpoint_chargeWithAcOutLoad() {
		// Issue #3573: Charge request of 3kW with 5kW AC-out load
		// Battery should charge at 3kW, so AC-in must be 8kW
		var result = VictronEssImpl.calculateAcInSetpoint(-3000, 5000, 3000, 3000);
		assertEquals(-8000, result);
	}

	@Test
	public void testCalculateAcInSetpoint_chargeExceedsMaxWithAcOut() {
		// Charge request of 5kW exceeds maxChargePower of 3kW, with 2kW AC-out
		// Should clamp to -3kW charge, then subtract 2kW AC-out => -5kW
		var result = VictronEssImpl.calculateAcInSetpoint(-5000, 2000, 3000, 3000);
		assertEquals(-5000, result);
	}

	@Test
	public void testCalculateAcInSetpoint_chargeWithinLimitsNoAcOut() {
		// Charge request of 2kW, no AC-out, maxCharge 3kW
		var result = VictronEssImpl.calculateAcInSetpoint(-2000, 0, 3000, 3000);
		assertEquals(-2000, result);
	}

	@Test
	public void testCalculateAcInSetpoint_dischargeWithinLimits() {
		// Discharge request of 2kW, maxDischarge 3kW
		var result = VictronEssImpl.calculateAcInSetpoint(2000, 1000, 3000, 3000);
		assertEquals(2000, result);
	}

	@Test
	public void testCalculateAcInSetpoint_dischargeExceedsMax() {
		// Discharge request of 5kW, maxDischarge 3kW => clamped to 3kW
		var result = VictronEssImpl.calculateAcInSetpoint(5000, 0, 3000, 3000);
		assertEquals(3000, result);
	}

	@Test
	public void testCalculateAcInSetpoint_zeroPower() {
		// Zero power target, should remain zero
		var result = VictronEssImpl.calculateAcInSetpoint(0, 5000, 3000, 3000);
		assertEquals(0, result);
	}

}
