package io.openems.edge.victron.meter.acin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

/**
 * Tests for {@link VictronAcInPowerMeterImpl}.
 */
public class VictronAcInPowerMeterImplTest {

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

	@Test
	public void test() throws Exception {
		new ComponentTest(new VictronAcInPowerMeterImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID) //
						.withRegisters(3,
								// VOLTAGE_L1 (register 3) - 23000 = 230.00V
								23000,
								// VOLTAGE_L2 (register 4) - 23100 = 231.00V
								23100,
								// VOLTAGE_L3 (register 5) - 22900 = 229.00V
								22900,
								// CURRENT_L1 (register 6) - 500 = 5.00A
								500,
								// CURRENT_L2 (register 7) - 520 = 5.20A
								520,
								// CURRENT_L3 (register 8) - 350 = 3.50A
								350,
								// FREQUENCY (register 9) - 5000 = 50.00Hz
								5000,
								// DUMMY (registers 10-11)
								0, 0,
								// ACTIVE_POWER_L1 (register 12) - 1000W
								1000,
								// ACTIVE_POWER_L2 (register 13) - 1200W
								1200,
								// ACTIVE_POWER_L3 (register 14) - 800W
								800)) //
				.activate(MyConfig.create() //
						.setId(METER_ID) //
						.setAlias("Victron AC In") //
						.setEnabled(true) //
						.setType(MeterType.GRID) //
						.setInvert(false) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(246) //
						.build()) //
				.next(new TestCase()) // First cycle to read registers
				.next(new TestCase() //
						.output(VOLTAGE_L1, 2300000) // 23000 * 100 (SCALE_FACTOR_2) = 2300000mV
						.output(VOLTAGE_L2, 2310000) //
						.output(VOLTAGE_L3, 2290000) //
						.output(CURRENT_L1, 50000) // 500 * 100 (SCALE_FACTOR_2) = 50000mA
						.output(CURRENT_L2, 52000) //
						.output(CURRENT_L3, 35000) //
						.output(FREQUENCY, 50000) // 5000 * 10 (SCALE_FACTOR_1) = 50000mHz
						.output(ACTIVE_POWER_L1, 10000) // 1000 * 10 (SCALE_FACTOR_1) = 10000
						.output(ACTIVE_POWER_L2, 12000) //
						.output(ACTIVE_POWER_L3, 8000));
	}

	@Test
	public void testInvertedPower() throws Exception {
		new ComponentTest(new VictronAcInPowerMeterImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID) //
						.withRegisters(3,
								// VOLTAGE_L1-L3
								23000, 23100, 22900,
								// CURRENT_L1-L3
								500, 520, 350,
								// FREQUENCY
								5000,
								// DUMMY
								0, 0,
								// ACTIVE_POWER_L1-L3
								1000, 1200, 800)) //
				.activate(MyConfig.create() //
						.setId(METER_ID) //
						.setAlias("Victron AC In Inverted") //
						.setEnabled(true) //
						.setType(MeterType.GRID) //
						.setInvert(true) // Invert power values
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(246) //
						.build()) //
				.next(new TestCase()) //
				.next(new TestCase() //
						.output(ACTIVE_POWER_L1, -10000) // Inverted: 1000 * 10 * -1
						.output(ACTIVE_POWER_L2, -12000) //
						.output(ACTIVE_POWER_L3, -8000));
	}

	@Test
	public void testMeterType() {
		var meter = new VictronAcInPowerMeterImpl();
		assertNotNull(meter);
	}

	@Test
	public void testConfigBuilder() {
		var config = MyConfig.create() //
				.setId(METER_ID) //
				.setAlias("Victron AC In") //
				.setEnabled(true) //
				.setType(MeterType.GRID) //
				.setInvert(false) //
				.setModbusId(MODBUS_ID) //
				.setModbusUnitId(246) //
				.build();

		assertEquals(METER_ID, config.id());
		assertEquals("Victron AC In", config.alias());
		assertEquals(true, config.enabled());
		assertEquals(MeterType.GRID, config.type());
		assertEquals(false, config.invert());
		assertEquals(MODBUS_ID, config.modbus_id());
		assertEquals(246, config.modbusUnitId());
	}

	@Test
	public void testConstructor() {
		var meter = new VictronAcInPowerMeterImpl();
		assertNotNull(meter);
	}

}
