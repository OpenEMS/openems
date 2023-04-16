package io.openems.edge.controller.ess.delayedselltogrid;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.meter.test.DummyElectricityMeter;

public class DelayedSellToGridImplTest {

	private static final String CTRL_ID = "ctrlDelayedSellToGrid0";
	private static final String ESS_ID = "ess0";
	private static final String METER_ID = "meter0";
	private static final ChannelAddress ESS_ACTIVE_POWER = new ChannelAddress(ESS_ID, "ActivePower");
	private static final ChannelAddress ESS_SET_ACTIVE_POWER_EQUALS = new ChannelAddress(ESS_ID,
			"SetActivePowerEquals");
	private static final ChannelAddress METER_ACTIVE_POWER = new ChannelAddress(METER_ID, "ActivePower");

	@Test
	public void test() throws Exception {
		new ControllerTest(new DelayedSellToGridImpl())//
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("meter", new DummyElectricityMeter(METER_ID)) //
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID)) //
				.activate(MyConfig.create()//
						.setId(CTRL_ID)//
						.setEssId(ESS_ID)//
						.setMeterId(METER_ID)//
						.setSellToGridPowerLimit(12_500_000)//
						.setContinuousSellToGridPower(500_000).build())//
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 0) //
						.input(METER_ACTIVE_POWER, 0) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 500_000)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 0) //
						.input(METER_ACTIVE_POWER, -30_000)//
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 470_000)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 500_000) //
						.input(METER_ACTIVE_POWER, -500_000)//
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 500_000)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 50_000) //
						.input(METER_ACTIVE_POWER, -500_000)//
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 50_000)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, -50_000) //
						.input(METER_ACTIVE_POWER, -500_000)//
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 0)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 150_000) //
						.input(METER_ACTIVE_POWER, -500_000)//
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 150_000)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 0) //
						.input(METER_ACTIVE_POWER, -1_500_000)//
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 0)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, -100_000) //
						.input(METER_ACTIVE_POWER, -15_000_000)//
						.output(ESS_SET_ACTIVE_POWER_EQUALS, -2_600_000)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, -1_000_000) //
						.input(METER_ACTIVE_POWER, -16_000_000)//
						.output(ESS_SET_ACTIVE_POWER_EQUALS, -4_500_000)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 0) //
						.input(METER_ACTIVE_POWER, -16_000_000)//
						.output(ESS_SET_ACTIVE_POWER_EQUALS, -3_500_000)) //
		;
	}
}
