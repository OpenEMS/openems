package io.openems.edge.victron.meter.grid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

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
	private static final ChannelAddress ACTIVE_POWER = new ChannelAddress(METER_ID, "ActivePower");
	private static final ChannelAddress VOLTAGE_L1 = new ChannelAddress(METER_ID, "VoltageL1");
	private static final ChannelAddress CURRENT_L1 = new ChannelAddress(METER_ID, "CurrentL1");
	private static final ChannelAddress ACTIVE_PRODUCTION_ENERGY_L1 = new ChannelAddress(METER_ID,
			"ActiveProductionEnergyL1");
	private static final ChannelAddress ACTIVE_PRODUCTION_ENERGY = new ChannelAddress(METER_ID,
			"ActiveProductionEnergy");
	private static final ChannelAddress ACTIVE_CONSUMPTION_ENERGY_L1 = new ChannelAddress(METER_ID,
			"ActiveConsumptionEnergyL1");
	private static final ChannelAddress ACTIVE_CONSUMPTION_ENERGY = new ChannelAddress(METER_ID,
			"ActiveConsumptionEnergy");
	private static final ChannelAddress FREQUENCY = new ChannelAddress(METER_ID, "Frequency");

	@Test
	public void test() throws Exception {
		new ComponentTest(new VictronMeterImpl()) //
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
								// VOLTAGE_L1 (register 2616) - 2300 = 230.0V
								2300,
								// CURRENT_L1 (register 2617) - 50 = 5.0A
								50,
								// VOLTAGE_L2 (register 2618) - 2310 = 231.0V
								2310,
								// CURRENT_L2 (register 2619) - 52 = 5.2A
								52,
								// VOLTAGE_L3 (register 2620) - 2290 = 229.0V
								2290,
								// CURRENT_L3 (register 2621) - 35 = 3.5A
								35,
								// Energy/Forward L1 = bought (registers 2622-2623) - 1000 = 10.00 kWh
								0, 1000,
								// Energy/Forward L2 = bought (registers 2624-2625) - 1200 = 12.00 kWh
								0, 1200,
								// Energy/Forward L3 = bought (registers 2626-2627) - 800 = 8.00 kWh
								0, 800,
								// Energy/Reverse L1 = sold (registers 2628-2629) - 500 = 5.00 kWh
								0, 500,
								// Energy/Reverse L2 = sold (registers 2630-2631) - 600 = 6.00 kWh
								0, 600,
								// Energy/Reverse L3 = sold (registers 2632-2633) - 400 = 4.00 kWh
								0, 400)
						.withRegisters(2644,
								// FREQUENCY (register 2644) - 5002 = 50.02 Hz
								5002)) //
				.activate(MyConfig.create() //
						.setId(METER_ID) //
						.setAlias("Victron Grid Meter") //
						.setEnabled(true) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(30) //
						.build()) //
				.next(new TestCase()) //
				.next(new TestCase() //
						.output(ACTIVE_POWER_L1, 1000) //
						.output(ACTIVE_POWER_L2, 1200) //
						.output(ACTIVE_POWER_L3, 800) //
						.output(ACTIVE_POWER, 3000) //
						.output(VOLTAGE_L1, 230_000) //
						.output(CURRENT_L1, 5_000) //
						// Bought energy is Active Production Energy for a grid meter
						.output(ACTIVE_PRODUCTION_ENERGY_L1, 10_000L) //
						.output(ACTIVE_PRODUCTION_ENERGY, 30_000L) //
						// Sold energy is Active Consumption Energy for a grid meter
						.output(ACTIVE_CONSUMPTION_ENERGY_L1, 5_000L) //
						.output(ACTIVE_CONSUMPTION_ENERGY, 15_000L) //
						.output(FREQUENCY, 50_020));
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
