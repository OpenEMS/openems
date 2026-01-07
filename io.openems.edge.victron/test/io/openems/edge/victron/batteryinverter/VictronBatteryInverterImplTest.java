package io.openems.edge.victron.batteryinverter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.type.Phase.SingleOrAllPhase;
import io.openems.edge.ess.test.DummyPower;
import io.openems.edge.victron.enums.DeviceType;

/**
 * Tests for {@link VictronBatteryInverterImpl}.
 */
public class VictronBatteryInverterImplTest {

	private static final String INVERTER_ID = "batteryInverter0";
	private static final String MODBUS_ID = "modbus0";
	private static final String ESS_ID = "ess0";

	private static final ChannelAddress ACTIVE_POWER = new ChannelAddress(INVERTER_ID, "ActivePower");
	private static final ChannelAddress BATTERY_SOC = new ChannelAddress(INVERTER_ID, "BatterySoc");
	private static final ChannelAddress DC_BATTERY_VOLTAGE = new ChannelAddress(INVERTER_ID, "DcBatteryVoltage");
	private static final ChannelAddress DC_BATTERY_CURRENT = new ChannelAddress(INVERTER_ID, "DcBatteryCurrent");
	private static final ChannelAddress AC_CONSUMPTION_POWER_L1 = new ChannelAddress(INVERTER_ID,
			"AcConsumptionPowerL1");
	private static final ChannelAddress GRID_POWER_L1 = new ChannelAddress(INVERTER_ID, "GridPowerL1");

	@Test
	public void test() throws Exception {
		new ComponentTest(new VictronBatteryInverterImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("power", new DummyPower()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID) //
						.withRegisters(800,
								// SERIAL_NUMBER (registers 800-805) - 6 words
								0x4142, 0x4344, 0x4546, 0x4748, 0x494A, 0x4B00,
								// CCGX_RELAY1_STATE (register 806)
								0,
								// CCGX_RELAY2_STATE (register 807)
								0,
								// AC_PV_ON_OUTPUT_POWER_L1 (register 808)
								500,
								// AC_PV_ON_OUTPUT_POWER_L2 (register 809)
								600,
								// AC_PV_ON_OUTPUT_POWER_L3 (register 810)
								400,
								// AC_PV_ON_INPUT_POWER_L1 (register 811)
								0,
								// AC_PV_ON_INPUT_POWER_L2 (register 812)
								0,
								// AC_PV_ON_INPUT_POWER_L3 (register 813)
								0,
								// DUMMY (registers 814-816)
								0, 0, 0,
								// AC_CONSUMPTION_POWER_L1 (register 817)
								1000,
								// AC_CONSUMPTION_POWER_L2 (register 818)
								800,
								// AC_CONSUMPTION_POWER_L3 (register 819)
								600,
								// GRID_POWER_L1 (register 820)
								500,
								// GRID_POWER_L2 (register 821)
								400,
								// GRID_POWER_L3 (register 822)
								300,
								// AC_GENSET_POWER_L1 (register 823)
								0,
								// AC_GENSET_POWER_L2 (register 824)
								0,
								// AC_GENSET_POWER_L3 (register 825)
								0,
								// ACTIVE_INPUT_SOURCE (register 826)
								1)
						.withRegisters(840,
								// DC_BATTERY_VOLTAGE (register 840) - 520 = 52.0V
								520,
								// DC_BATTERY_CURRENT (register 841) - 100 = 10.0A
								100,
								// ACTIVE_POWER (register 842) - 2000W (DC Power)
								2000,
								// BATTERY_SOC (register 843) - 75%
								75,
								// BATTERY_STATE (register 844)
								2,
								// BATTERY_CONSUMED_AMPHOURS (register 845)
								50,
								// BATTERY_TIME_TO_GO (register 846)
								120)
						.withRegisters(850,
								// DC_PV_POWER (register 850)
								1500,
								// DC_PV_CURRENT (register 851)
								30)
						.withRegisters(855,
								// CHARGER_POWER (register 855)
								1000)
						.withRegisters(860,
								// DC_SYSTEM_POWER (register 860)
								200)
						.withRegisters(865,
								// VE_BUS_CHARGE_CURRENT (register 865)
								50,
								// VE_BUS_CHARGE_POWER (register 866)
								2500)
						.withRegisters(2700,
								// ESS_CONTROL_LOOP_SETPOINT (register 2700)
								0,
								// ESS_MAX_CHARGE_CURRENT_PERCENTAGE (register 2701)
								100,
								// ESS_MAX_DISCHARGE_CURRENT_PERCENTAGE (register 2702)
								100,
								// ESS_CONTROL_LOOP_SETPOINT_SCALE_FACTOR_2 (register 2703)
								0,
								// ESS_MAX_DISCHARGE_POWER (register 2704)
								5000,
								// SYSTEM_MAX_CHARGE_CURRENT (register 2705)
								50,
								// MAX_FEED_IN_POWER (register 2706)
								0,
								// FEED_EXCESS_DC (register 2707)
								0,
								// DONT_FEED_EXCESS_AC (register 2708)
								0,
								// PV_POWER_LIMITER_ACTIVE (register 2709)
								0,
								// MAX_CHARGE_VOLTAGE (register 2710) - 560 = 56.0V
								// Note: This is a READ_WRITE channel, so we can't verify it as output
								560)) //
				.activate(MyConfig.create() //
						.setId(INVERTER_ID) //
						.setAlias("Victron Battery Inverter") //
						.setEnabled(true) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(100) //
						.setPhase(SingleOrAllPhase.ALL) //
						.setStartStop(StartStopConfig.START) //
						.setDeviceType(DeviceType.Multiplus2GX3kVa) //
						.setDcFeedInThreshold(100) //
						.setMaxChargePower(2000) //
						.setMaxDischargePower(2000) //
						.setDebugMode(false) //
						.setReadOnlyMode(false) //
						.setEssId(ESS_ID) //
						.build()) //
				.next(new TestCase()) // First cycle to read registers
				.next(new TestCase() //
						.output(ACTIVE_POWER, -2000) // Inverted: 2000W charging
						.output(BATTERY_SOC, 75) //
						.output(DC_BATTERY_VOLTAGE, 52) // 520 / 10 (SCALE_FACTOR_MINUS_1) = 52V
						.output(DC_BATTERY_CURRENT, 10) // 100 / 10 (SCALE_FACTOR_MINUS_1) = 10A
						.output(AC_CONSUMPTION_POWER_L1, 1000) //
						.output(GRID_POWER_L1, 500)); // Note: MAX_CHARGE_VOLTAGE not tested (READ_WRITE channel)
	}

	@Test
	public void testChannelIdCount() {
		var channelIds = VictronBatteryInverter.ChannelId.values();
		assertNotNull(channelIds);
		assertEquals(true, channelIds.length > 0);
	}

	@Test
	public void testChannelIdDoc() {
		for (var channelId : VictronBatteryInverter.ChannelId.values()) {
			assertNotNull("ChannelId " + channelId.name() + " should have a doc", channelId.doc());
		}
	}

	@Test
	public void testConfigBuilder() {
		var config = MyConfig.create() //
				.setId(INVERTER_ID) //
				.setAlias("Victron Battery Inverter") //
				.setEnabled(true) //
				.setModbusId(MODBUS_ID) //
				.setModbusUnitId(100) //
				.setPhase(SingleOrAllPhase.ALL) //
				.setStartStop(StartStopConfig.AUTO) //
				.setDeviceType(DeviceType.Multiplus2GX3kVa) //
				.setDcFeedInThreshold(100) //
				.setMaxChargePower(2000) //
				.setMaxDischargePower(2000) //
				.setDebugMode(false) //
				.setReadOnlyMode(false) //
				.setEssId(ESS_ID) //
				.build();

		assertEquals(INVERTER_ID, config.id());
		assertEquals("Victron Battery Inverter", config.alias());
		assertEquals(true, config.enabled());
		assertEquals(MODBUS_ID, config.modbus_id());
		assertEquals(100, config.modbusUnitId());
		assertEquals(SingleOrAllPhase.ALL, config.phase());
		assertEquals(StartStopConfig.AUTO, config.startStop());
		assertEquals(DeviceType.Multiplus2GX3kVa, config.DeviceType());
		assertEquals(100, config.dcFeedInThreshold());
		assertEquals(2000, config.maxChargePower());
		assertEquals(2000, config.maxDischargePower());
		assertEquals(false, config.debugMode());
		assertEquals(false, config.readOnlyMode());
		assertEquals(ESS_ID, config.ess_id());
	}

	@Test
	public void testConstructor() throws OpenemsNamedException {
		var inverter = new VictronBatteryInverterImpl();
		assertNotNull(inverter);
	}

	@Test
	public void testGetPowerPrecision() throws OpenemsNamedException {
		var inverter = new VictronBatteryInverterImpl();
		assertEquals(100, inverter.getPowerPrecision());
	}

	@Test
	public void testDeviceTypes() {
		for (var deviceType : DeviceType.values()) {
			assertNotNull(deviceType.getApparentPowerLimit());
		}
	}

}
