package io.openems.edge.victron.meter.acout;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.timedata.test.DummyTimedata;

/**
 * Tests for {@link VictronAcOutPowerMeterImpl}.
 */
public class VictronAcOutPowerMeterImplTest {

	private static final String METER_ID = "meter0";
	private static final String MODBUS_ID = "modbus0";

	private static final ChannelAddress VOLTAGE_L1 = new ChannelAddress(METER_ID, "VoltageL1");
	private static final ChannelAddress VOLTAGE_L2 = new ChannelAddress(METER_ID, "VoltageL2");
	private static final ChannelAddress VOLTAGE_L3 = new ChannelAddress(METER_ID, "VoltageL3");
	private static final ChannelAddress CURRENT_L1 = new ChannelAddress(METER_ID, "CurrentL1");
	private static final ChannelAddress CURRENT_L2 = new ChannelAddress(METER_ID, "CurrentL2");
	private static final ChannelAddress CURRENT_L3 = new ChannelAddress(METER_ID, "CurrentL3");
	private static final ChannelAddress FREQUENCY = new ChannelAddress(METER_ID, "Frequency");
	private static final ChannelAddress ACTIVE_POWER_L1 = new ChannelAddress(METER_ID, "ActivePowerL1");
	private static final ChannelAddress ACTIVE_POWER_L2 = new ChannelAddress(METER_ID, "ActivePowerL2");
	private static final ChannelAddress ACTIVE_POWER_L3 = new ChannelAddress(METER_ID, "ActivePowerL3");
	private static final ChannelAddress ENERGY_FROM_AC_IN_1_TO_AC_OUT = new ChannelAddress(METER_ID,
			"EnergyFromAcIn1ToAcOut");
	private static final ChannelAddress ENERGY_FROM_BATTERY_TO_AC_OUT = new ChannelAddress(METER_ID,
			"EnergyFromBatteryToAcOut");

	@Test
	public void test() throws Exception {
		new ComponentTest(new VictronAcOutPowerMeterImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("timedata", new DummyTimedata(METER_ID)) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID) //
						.withRegisters(15,
								// VOLTAGE_L1 (register 15) - 2300 = 230.0V
								2300,
								// VOLTAGE_L2 (register 16) - 2310 = 231.0V
								2310,
								// VOLTAGE_L3 (register 17) - 2290 = 229.0V
								2290,
								// CURRENT_L1 (register 18) - 50 = 5.0A
								50,
								// CURRENT_L2 (register 19) - 52 = 5.2A
								52,
								// CURRENT_L3 (register 20) - 35 = 3.5A
								35,
								// FREQUENCY (register 21) - 5000 = 50.00Hz
								5000,
								// DUMMY (register 22)
								0,
								// ACTIVE_POWER_L1 (register 23) - 1000W
								1000,
								// ACTIVE_POWER_L2 (register 24) - 1200W
								1200,
								// ACTIVE_POWER_L3 (register 25) - 800W
								800,
								// DUMMY (registers 26-73)
								0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 26-35
								0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 36-45
								0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 46-55
								0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 56-65
								0, 0, 0, 0, 0, 0, 0, 0, // 66-73
								// ENERGY_FROM_AC_IN_1_TO_AC_OUT (registers 74-75) - 10000Wh
								0, 10000,
								// DUMMY (registers 76-77)
								0, 0,
								// ENERGY_FROM_AC_IN_2_TO_AC_OUT (registers 78-79)
								0, 5000,
								// DUMMY (registers 80-81)
								0, 0,
								// ENERGY_FROM_AC_OUT_TO_AC_IN_1 (registers 82-83)
								0, 2000,
								// ENERGY_FROM_AC_OUT_TO_AC_IN_2 (registers 84-85)
								0, 1000,
								// DUMMY (registers 86-89)
								0, 0, 0, 0,
								// ENERGY_FROM_BATTERY_TO_AC_OUT (registers 90-91)
								0, 8000,
								// ENERGY_FROM_AC_OUT_TO_BATTERY (registers 92-93)
								0, 6000)) //
				.activate(MyConfig.create() //
						.setId(METER_ID) //
						.setAlias("Victron AC Out") //
						.setEnabled(true) //
						.setType(MeterType.CONSUMPTION_METERED) //
						.setInvert(false) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(227) //
						.build()) //
				.next(new TestCase()) // First cycle to read registers
				.next(new TestCase() //
						.output(VOLTAGE_L1, 230) // 2300 / 10 (SCALE_FACTOR_MINUS_1) = 230V (stored as V*10 = 2300)
						.output(VOLTAGE_L2, 231) //
						.output(VOLTAGE_L3, 229) //
						.output(CURRENT_L1, 5) // 50 / 10 (SCALE_FACTOR_MINUS_1) = 5A
						.output(CURRENT_L2, 5) //
						.output(CURRENT_L3, 3) //
						.output(FREQUENCY, 50) // 5000 / 100 (SCALE_FACTOR_MINUS_2) = 50Hz
						.output(ACTIVE_POWER_L1, 10000) // 1000 * 10 (SCALE_FACTOR_1) = 10000
						.output(ACTIVE_POWER_L2, 12000) //
						.output(ACTIVE_POWER_L3, 8000) //
						.output(ENERGY_FROM_AC_IN_1_TO_AC_OUT, 100000L) // 10000 * 10 (SCALE_FACTOR_1)
						.output(ENERGY_FROM_BATTERY_TO_AC_OUT, 80000L));
	}

	@Test
	public void testInvertedPower() throws Exception {
		new ComponentTest(new VictronAcOutPowerMeterImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("timedata", new DummyTimedata(METER_ID)) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID) //
						.withRegisters(15,
								// VOLTAGE_L1-L3
								2300, 2310, 2290,
								// CURRENT_L1-L3
								50, 52, 35,
								// FREQUENCY
								5000,
								// DUMMY
								0,
								// ACTIVE_POWER_L1-L3
								1000, 1200, 800,
								// DUMMY to fill gap
								0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 26-35
								0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 36-45
								0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 46-55
								0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 56-65
								0, 0, 0, 0, 0, 0, 0, 0, // 66-73
								// Energy values
								0, 10000, 0, 0, 0, 5000, 0, 0, 0, 2000, 0, 1000, 0, 0, 0, 0, 0, 8000, 0, 6000)) //
				.activate(MyConfig.create() //
						.setId(METER_ID) //
						.setAlias("Victron AC Out Inverted") //
						.setEnabled(true) //
						.setType(MeterType.CONSUMPTION_METERED) //
						.setInvert(true) // Invert values
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(227) //
						.build()) //
				.next(new TestCase()) //
				.next(new TestCase() //
						.output(ACTIVE_POWER_L1, -10000) // Inverted: 1000 * 10 * -1
						.output(ACTIVE_POWER_L2, -12000) //
						.output(ACTIVE_POWER_L3, -8000) //
						.output(CURRENT_L1, -5) // Inverted: 50 / 10 * -1
						.output(CURRENT_L2, -5) //
						.output(CURRENT_L3, -3));
	}

	@Test
	public void testChannelIdCount() {
		var channelIds = VictronAcOutPowerMeter.ChannelId.values();
		assertNotNull(channelIds);
		assertEquals(true, channelIds.length > 0);
	}

	@Test
	public void testChannelIdDoc() {
		for (var channelId : VictronAcOutPowerMeter.ChannelId.values()) {
			assertNotNull("ChannelId " + channelId.name() + " should have a doc", channelId.doc());
		}
	}

	@Test
	public void testConfigBuilder() {
		var config = MyConfig.create() //
				.setId(METER_ID) //
				.setAlias("Victron AC Out") //
				.setEnabled(true) //
				.setType(MeterType.CONSUMPTION_METERED) //
				.setInvert(false) //
				.setModbusId(MODBUS_ID) //
				.setModbusUnitId(227) //
				.build();

		assertEquals(METER_ID, config.id());
		assertEquals("Victron AC Out", config.alias());
		assertEquals(true, config.enabled());
		assertEquals(MeterType.CONSUMPTION_METERED, config.type());
		assertEquals(false, config.invert());
		assertEquals(MODBUS_ID, config.modbus_id());
		assertEquals(227, config.modbusUnitId());
	}

	@Test
	public void testConstructor() {
		var meter = new VictronAcOutPowerMeterImpl();
		assertNotNull(meter);
	}

}
