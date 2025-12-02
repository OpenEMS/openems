package io.openems.edge.meter.victron.grid;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.ess.test.DummyPower;

public class VictronMeterImplTest {

	private static final String METER_ID = "meter0";
	private static final String MODBUS_ID = "modbus0";

	private static final ChannelAddress ACTIVE_POWER_L1 = new ChannelAddress(METER_ID, "ActivePowerL1");
	private static final ChannelAddress ACTIVE_POWER_L2 = new ChannelAddress(METER_ID, "ActivePowerL2");
	private static final ChannelAddress ACTIVE_POWER_L3 = new ChannelAddress(METER_ID, "ActivePowerL3");
	private static final ChannelAddress VOLTAGE_L1 = new ChannelAddress(METER_ID, "VoltageL1");

	@Test
	public void test() throws Exception {
		new ComponentTest(new VictronMeterImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("power", new DummyPower()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID) //
						.withRegisters(2600,
								// ACTIVE_POWER_L1 (register 2600) - 1000W
								1000,
								// ACTIVE_POWER_L2 (register 2601) - 1200W
								1200,
								// ACTIVE_POWER_L3 (register 2602) - 800W
								800)
						.withRegisters(2609,
								// SERIAL_NUMBER (registers 2609-2615) - 7 words
								0x4142, 0x4344, 0x4546, 0x4748, 0x494A, 0x4B4C, 0x4D00,
								// VOLTAGE_L1 (register 2616) - 23000 = 230.00V
								23000,
								// CURRENT_L1 (register 2617) - 500 = 5.00A
								500,
								// VOLTAGE_L2 (register 2618) - 23100 = 231.00V
								23100,
								// CURRENT_L2 (register 2619) - 520 = 5.20A
								520,
								// VOLTAGE_L3 (register 2620) - 22900 = 229.00V
								22900,
								// CURRENT_L3 (register 2621) - 350 = 3.50A
								350,
								// ACTIVE_CONSUMPTION_ENERGY_L1 (registers 2622-2623)
								0, 1000,
								// ACTIVE_CONSUMPTION_ENERGY_L2 (registers 2624-2625)
								0, 1200,
								// ACTIVE_CONSUMPTION_ENERGY_L3 (registers 2626-2627)
								0, 800,
								// ACTIVE_PRODUCTION_ENERGY_L1 (registers 2628-2629)
								0, 500,
								// ACTIVE_PRODUCTION_ENERGY_L2 (registers 2630-2631)
								0, 600,
								// ACTIVE_PRODUCTION_ENERGY_L3 (registers 2632-2633)
								0, 400)) //
				.activate(MyConfig.create() //
						.setId(METER_ID) //
						.setAlias("Victron Grid Meter") //
						.setEnabled(true) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(30) //
						.build()) //
				.next(new TestCase() //
						.output(ACTIVE_POWER_L1, 1000) //
						.output(ACTIVE_POWER_L2, 1200) //
						.output(ACTIVE_POWER_L3, 800) //
						.output(VOLTAGE_L1, 2300000));
	}

	@Test
	public void testMeterType() throws Exception {
		var meter = new VictronMeterImpl();
		assertEquals(MeterType.GRID, meter.getMeterType());
	}

	@Test
	public void testChannelIds() {
		var channelIds = VictronMeter.ChannelId.values();
		assertNotNull(channelIds);
	}

}
