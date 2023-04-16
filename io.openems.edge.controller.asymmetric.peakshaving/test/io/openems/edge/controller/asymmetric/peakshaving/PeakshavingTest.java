package io.openems.edge.controller.asymmetric.peakshaving;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyPower;
import io.openems.edge.meter.test.DummyElectricityMeter;

public class PeakshavingTest {

	private static final String CTRL_ID = "ctrl0";

	private static final String METER_ID = "meter0";
	private static final ChannelAddress GRID_ACTIVE_POWER = new ChannelAddress(METER_ID, "ActivePower");
	private static final ChannelAddress GRID_ACTIVE_POWER_L1 = new ChannelAddress(METER_ID, "ActivePowerL1");
	private static final ChannelAddress GRID_ACTIVE_POWER_L2 = new ChannelAddress(METER_ID, "ActivePowerL2");
	private static final ChannelAddress GRID_ACTIVE_POWER_L3 = new ChannelAddress(METER_ID, "ActivePowerL3");

	private static final String ESS_ID = "ess0";
	private static final ChannelAddress ESS_ACTIVE_POWER = new ChannelAddress(ESS_ID, "ActivePower");
	private static final ChannelAddress ESS_SET_ACTIVE_POWER_EQUALS = new ChannelAddress(ESS_ID,
			"SetActivePowerEquals");

	@Test
	public void symmetricMeterTest() throws Exception {
		new ControllerTest(new PeakShaving()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addComponent(new DummyElectricityMeter(METER_ID)) //
				.addComponent(new DummyManagedSymmetricEss(ESS_ID, new DummyPower(0.3, 0.3, 0.1))) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setMeterId(METER_ID) //
						.setEssId(ESS_ID) //
						.setPeakShavingPower(33333) //
						.setRechargePower(16666) //
						.build())
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 0) //
						.input(GRID_ACTIVE_POWER, 120000) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 6000)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 0) //
						.input(GRID_ACTIVE_POWER, 120000) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 12001)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 3793) //
						.input(GRID_ACTIVE_POWER, 120000 - 3793) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 16484)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 8981) //
						.input(GRID_ACTIVE_POWER, 120000 - 8981) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 19650)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 13723) //
						.input(GRID_ACTIVE_POWER, 120000 - 13723) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 21578)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 17469) //
						.input(GRID_ACTIVE_POWER, 120000 - 17469) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 22437)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 20066) //
						.input(GRID_ACTIVE_POWER, 120000 - 20066) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 22533)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 21564) //
						.input(GRID_ACTIVE_POWER, 120000 - 21564) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 22174)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 22175) //
						.input(GRID_ACTIVE_POWER, 120000 - 22175) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 21610)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 22173) //
						.input(GRID_ACTIVE_POWER, 120000 - 22173) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 21020)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 21816) //
						.input(GRID_ACTIVE_POWER, 120000 - 21816) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 20511)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 21311) //
						.input(GRID_ACTIVE_POWER, 120000 - 21311) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 20133)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 20803) //
						.input(GRID_ACTIVE_POWER, 120000 - 20803) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 19893)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 20377) //
						.input(GRID_ACTIVE_POWER, 120000 - 20377) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 19772)); //
	}

	@Test
	public void asymmetricMeterTest() throws Exception {
		new ControllerTest(new PeakShaving()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addComponent(new DummyElectricityMeter(METER_ID)) //
				.addComponent(new DummyManagedSymmetricEss(ESS_ID, new DummyPower(0.3, 0.3, 0.1))) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setMeterId(METER_ID) //
						.setEssId(ESS_ID) //
						.setPeakShavingPower(33333) //
						.setRechargePower(16666) //
						.build())
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 0) //
						.input(GRID_ACTIVE_POWER_L1, 20000) //
						.input(GRID_ACTIVE_POWER_L2, 40000) //
						.input(GRID_ACTIVE_POWER_L3, 10000) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 6000)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 0) //
						.input(GRID_ACTIVE_POWER_L1, 20000) //
						.input(GRID_ACTIVE_POWER_L2, 40000) //
						.input(GRID_ACTIVE_POWER_L3, 10000) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 12001)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 3793) //
						.input(GRID_ACTIVE_POWER_L1, 20000 - 3793 / 3) //
						.input(GRID_ACTIVE_POWER_L2, 40000 - 3793 / 3) //
						.input(GRID_ACTIVE_POWER_L3, 10000 - 3793 / 3) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 16484)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 8981) //
						.input(GRID_ACTIVE_POWER_L1, 20000 - 8981 / 3) //
						.input(GRID_ACTIVE_POWER_L2, 40000 - 8981 / 3) //
						.input(GRID_ACTIVE_POWER_L3, 10000 - 8981 / 3) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 19651)); //
	}
}
