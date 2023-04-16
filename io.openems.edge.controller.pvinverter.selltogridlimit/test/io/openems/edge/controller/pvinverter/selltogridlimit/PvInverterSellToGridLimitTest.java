package io.openems.edge.controller.pvinverter.selltogridlimit;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.meter.test.DummyElectricityMeter;
import io.openems.edge.pvinverter.test.DummyManagedSymmetricPvInverter;

public class PvInverterSellToGridLimitTest {

	private static final String CTRL_ID = "ctrl0";

	private static final String METER_ID = "meter0";
	private static final ChannelAddress GRID_ACTIVE_POWER = new ChannelAddress(METER_ID, "ActivePower");
	private static final ChannelAddress GRID_ACTIVE_POWER_L1 = new ChannelAddress(METER_ID, "ActivePowerL1");
	private static final ChannelAddress GRID_ACTIVE_POWER_L2 = new ChannelAddress(METER_ID, "ActivePowerL2");
	private static final ChannelAddress GRID_ACTIVE_POWER_L3 = new ChannelAddress(METER_ID, "ActivePowerL3");

	private static final String PV_INVERTER = "pvInverter0";
	private static final ChannelAddress PV_INVERTER_ACTIVE_POWER = new ChannelAddress(PV_INVERTER, "ActivePower");
	private static final ChannelAddress PV_INVERTER_SET_ACTIVE_POWER_EQUALS = new ChannelAddress(PV_INVERTER,
			"ActivePowerLimit");

	private static final double ADJUST_RATE = PvInverterSellToGridLimit.DEFAULT_MAX_ADJUSTMENT_RATE;

	@Test
	public void symmetricMeterTest() throws Exception {
		new ControllerTest(new PvInverterSellToGridLimit()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addComponent(new DummyElectricityMeter(METER_ID)) //
				.addComponent(new DummyManagedSymmetricPvInverter(PV_INVERTER)).activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setMeterId(METER_ID) //
						.setAsymmetricMode(false) //
						.setMaximumSellToGridPower(10_000) //
						.setPvInverterId(PV_INVERTER) //
						.build())
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, -15000) //
						.input(PV_INVERTER_ACTIVE_POWER, 15000) //
						.output(PV_INVERTER_SET_ACTIVE_POWER_EQUALS, 10000)) //
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, -15000) //
						.input(PV_INVERTER_ACTIVE_POWER, 10000) //
						.output(PV_INVERTER_SET_ACTIVE_POWER_EQUALS,
								TypeUtils.getAsType(OpenemsType.INTEGER, 10000 - 10000 * ADJUST_RATE))) // 5000 -> 8000
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, -13000) //
						.input(PV_INVERTER_ACTIVE_POWER, 8000) //
						.output(PV_INVERTER_SET_ACTIVE_POWER_EQUALS,
								TypeUtils.getAsType(OpenemsType.INTEGER, 8000 - 8000 * ADJUST_RATE))) // 5000 -> 6400
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, -11400) //
						.input(PV_INVERTER_ACTIVE_POWER, 6400) //
						.output(PV_INVERTER_SET_ACTIVE_POWER_EQUALS,
								TypeUtils.getAsType(OpenemsType.INTEGER, 6400 - 6400 * ADJUST_RATE))) // 5000 -> 5120
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, -10120) //
						.input(PV_INVERTER_ACTIVE_POWER, 5120) //
						.output(PV_INVERTER_SET_ACTIVE_POWER_EQUALS, 5000)) //
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, -9000) //
						.input(PV_INVERTER_ACTIVE_POWER, 5000) //
						.output(PV_INVERTER_SET_ACTIVE_POWER_EQUALS, 6000)) //
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, 0) //
						.input(PV_INVERTER_ACTIVE_POWER, 6000) //
						.output(PV_INVERTER_SET_ACTIVE_POWER_EQUALS,
								TypeUtils.getAsType(OpenemsType.INTEGER, 6000 + 6000 * ADJUST_RATE))); // 16000 -> 7200
	}

	@Test
	public void asymmetricMeterTest() throws Exception {
		new ControllerTest(new PvInverterSellToGridLimit()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addComponent(new DummyElectricityMeter(METER_ID)) //
				.addComponent(new DummyManagedSymmetricPvInverter(PV_INVERTER)).activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setMeterId(METER_ID) //
						.setAsymmetricMode(true) //
						.setMaximumSellToGridPower(4_000) // 12_000 in total
						.setPvInverterId(PV_INVERTER) //
						.build())
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER_L1, -2000) //
						.input(GRID_ACTIVE_POWER_L2, -4000) //
						.input(GRID_ACTIVE_POWER_L3, -3000) //
						.input(PV_INVERTER_ACTIVE_POWER, 12000) //
						.output(PV_INVERTER_SET_ACTIVE_POWER_EQUALS, 12000)) //
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER_L1, -2000) //
						.input(GRID_ACTIVE_POWER_L2, -5000) //
						.input(GRID_ACTIVE_POWER_L3, -2000) //
						.input(PV_INVERTER_ACTIVE_POWER, 12000) //
						.output(PV_INVERTER_SET_ACTIVE_POWER_EQUALS,
								TypeUtils.getAsType(OpenemsType.INTEGER, 12000 - 12000 * ADJUST_RATE))) // 9000 -> 9600
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER_L1, -1200) //
						.input(GRID_ACTIVE_POWER_L2, -4200) //
						.input(GRID_ACTIVE_POWER_L3, -1200) //
						.input(PV_INVERTER_ACTIVE_POWER, 9600) //
						.output(PV_INVERTER_SET_ACTIVE_POWER_EQUALS, 9000)) //
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER_L1, -1000) //
						.input(GRID_ACTIVE_POWER_L2, -4000) //
						.input(GRID_ACTIVE_POWER_L3, -1000) //
						.input(PV_INVERTER_ACTIVE_POWER, 9000) //
						.output(PV_INVERTER_SET_ACTIVE_POWER_EQUALS, 9000)) //
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER_L1, -1000) //
						.input(GRID_ACTIVE_POWER_L2, -3700) //
						.input(GRID_ACTIVE_POWER_L3, -1000) //
						.input(PV_INVERTER_ACTIVE_POWER, 9000) //
						.output(PV_INVERTER_SET_ACTIVE_POWER_EQUALS, 9900)) //
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER_L1, -2000) //
						.input(GRID_ACTIVE_POWER_L2, -5000) //
						.input(GRID_ACTIVE_POWER_L3, -2000) //
						.input(PV_INVERTER_ACTIVE_POWER, 9900) //
						.output(PV_INVERTER_SET_ACTIVE_POWER_EQUALS,
								TypeUtils.getAsType(OpenemsType.INTEGER, 9900 - 9900 * ADJUST_RATE))) // 6900 -> 7920
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER_L1, -2000) //
						.input(GRID_ACTIVE_POWER_L2, -5000) //
						.input(GRID_ACTIVE_POWER_L3, -2000) //
						.input(PV_INVERTER_ACTIVE_POWER, 7920) //
						.output(PV_INVERTER_SET_ACTIVE_POWER_EQUALS,
								TypeUtils.getAsType(OpenemsType.INTEGER, 7920 - 7920 * ADJUST_RATE))); // 4920 -> 6336

		new ControllerTest(new PvInverterSellToGridLimit()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addComponent(new DummyElectricityMeter(METER_ID)) //
				.addComponent(new DummyManagedSymmetricPvInverter(PV_INVERTER)).activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setMeterId(METER_ID) //
						.setAsymmetricMode(true) //
						.setMaximumSellToGridPower(4_000) // 12_000 in total
						.setPvInverterId(PV_INVERTER) //
						.build())
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER_L1, 1000) //
						.input(GRID_ACTIVE_POWER_L2, 2000) //
						.input(GRID_ACTIVE_POWER_L3, 3000) //
						.input(PV_INVERTER_ACTIVE_POWER, 1000) //
						.output(PV_INVERTER_SET_ACTIVE_POWER_EQUALS, 16000)) //
		;
	}
}
