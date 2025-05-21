package io.openems.edge.controller.ess.delayedselltogrid;

import static io.openems.edge.ess.api.ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.meter.test.DummyElectricityMeter;

public class ControllerEssDelayedSellToGridImplTest {

	@Test
	public void test() throws Exception {
		new ControllerTest(new ControllerEssDelayedSellToGridImpl())//
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("meter", new DummyElectricityMeter("meter0")) //
				.addReference("ess", new DummyManagedSymmetricEss("ess0")) //
				.activate(MyConfig.create()//
						.setId("ctrlDelayedSellToGrid0")//
						.setEssId("ess0")//
						.setMeterId("meter0")//
						.setSellToGridPowerLimit(12_500_000)//
						.setContinuousSellToGridPower(500_000).build())//
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 0) //
						.input("meter0", SymmetricEss.ChannelId.ACTIVE_POWER, 0) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 500_000)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 0) //
						.input("meter0", SymmetricEss.ChannelId.ACTIVE_POWER, -30_000)//
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 470_000)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 500_000) //
						.input("meter0", SymmetricEss.ChannelId.ACTIVE_POWER, -500_000)//
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 500_000)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 50_000) //
						.input("meter0", SymmetricEss.ChannelId.ACTIVE_POWER, -500_000)//
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 50_000)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, -50_000) //
						.input("meter0", SymmetricEss.ChannelId.ACTIVE_POWER, -500_000)//
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 0)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 150_000) //
						.input("meter0", SymmetricEss.ChannelId.ACTIVE_POWER, -500_000)//
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 150_000)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 0) //
						.input("meter0", SymmetricEss.ChannelId.ACTIVE_POWER, -1_500_000)//
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 0)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, -100_000) //
						.input("meter0", SymmetricEss.ChannelId.ACTIVE_POWER, -15_000_000)//
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -2_600_000)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, -1_000_000) //
						.input("meter0", SymmetricEss.ChannelId.ACTIVE_POWER, -16_000_000)//
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -4_500_000)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 0) //
						.input("meter0", SymmetricEss.ChannelId.ACTIVE_POWER, -16_000_000)//
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -3_500_000)) //
				.deactivate();
	}
}
