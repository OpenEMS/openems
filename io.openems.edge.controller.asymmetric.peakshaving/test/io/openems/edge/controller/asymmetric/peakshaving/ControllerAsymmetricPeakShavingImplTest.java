package io.openems.edge.controller.asymmetric.peakshaving;

import static io.openems.edge.ess.api.ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_POWER;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_POWER_L1;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_POWER_L2;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_POWER_L3;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyPower;
import io.openems.edge.meter.test.DummyElectricityMeter;

public class ControllerAsymmetricPeakShavingImplTest {

	@Test
	public void symmetricMeterTest() throws Exception {
		new ControllerTest(new ControllerAsymmetricPeakShavingImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addComponent(new DummyElectricityMeter("meter0")) //
				.addComponent(new DummyManagedSymmetricEss("ess0") //
						.setPower(new DummyPower(0.3, 0.3, 0.1))) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setMeterId("meter0") //
						.setEssId("ess0") //
						.setPeakShavingPower(33333) //
						.setRechargePower(16666) //
						.build())
				.next(new TestCase() //
						.input("ess0", ACTIVE_POWER, 0) //
						.input("meter0", ACTIVE_POWER, 120000) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 6000)) //
				.next(new TestCase() //
						.input("ess0", ACTIVE_POWER, 0) //
						.input("meter0", ACTIVE_POWER, 120000) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 12001)) //
				.next(new TestCase() //
						.input("ess0", ACTIVE_POWER, 3793) //
						.input("meter0", ACTIVE_POWER, 120000 - 3793) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 16484)) //
				.next(new TestCase() //
						.input("ess0", ACTIVE_POWER, 8981) //
						.input("meter0", ACTIVE_POWER, 120000 - 8981) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 19650)) //
				.next(new TestCase() //
						.input("ess0", ACTIVE_POWER, 13723) //
						.input("meter0", ACTIVE_POWER, 120000 - 13723) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 21578)) //
				.next(new TestCase() //
						.input("ess0", ACTIVE_POWER, 17469) //
						.input("meter0", ACTIVE_POWER, 120000 - 17469) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 22437)) //
				.next(new TestCase() //
						.input("ess0", ACTIVE_POWER, 20066) //
						.input("meter0", ACTIVE_POWER, 120000 - 20066) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 22533)) //
				.next(new TestCase() //
						.input("ess0", ACTIVE_POWER, 21564) //
						.input("meter0", ACTIVE_POWER, 120000 - 21564) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 22174)) //
				.next(new TestCase() //
						.input("ess0", ACTIVE_POWER, 22175) //
						.input("meter0", ACTIVE_POWER, 120000 - 22175) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 21610)) //
				.next(new TestCase() //
						.input("ess0", ACTIVE_POWER, 22173) //
						.input("meter0", ACTIVE_POWER, 120000 - 22173) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 21020)) //
				.next(new TestCase() //
						.input("ess0", ACTIVE_POWER, 21816) //
						.input("meter0", ACTIVE_POWER, 120000 - 21816) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 20511)) //
				.next(new TestCase() //
						.input("ess0", ACTIVE_POWER, 21311) //
						.input("meter0", ACTIVE_POWER, 120000 - 21311) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 20133)) //
				.next(new TestCase() //
						.input("ess0", ACTIVE_POWER, 20803) //
						.input("meter0", ACTIVE_POWER, 120000 - 20803) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 19893)) //
				.next(new TestCase() //
						.input("ess0", ACTIVE_POWER, 20377) //
						.input("meter0", ACTIVE_POWER, 120000 - 20377) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 19772)) //
				.deactivate();
	}

	@Test
	public void asymmetricMeterTest() throws Exception {
		new ControllerTest(new ControllerAsymmetricPeakShavingImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addComponent(new DummyElectricityMeter("meter0")) //
				.addComponent(new DummyManagedSymmetricEss("ess0") //
						.setPower(new DummyPower(0.3, 0.3, 0.1))) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setMeterId("meter0") //
						.setEssId("ess0") //
						.setPeakShavingPower(33333) //
						.setRechargePower(16666) //
						.build())
				.next(new TestCase() //
						.input("ess0", ACTIVE_POWER, 0) //
						.input("meter0", ACTIVE_POWER_L1, 20000) //
						.input("meter0", ACTIVE_POWER_L2, 40000) //
						.input("meter0", ACTIVE_POWER_L3, 10000) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 6000)) //
				.next(new TestCase() //
						.input("ess0", ACTIVE_POWER, 0) //
						.input("meter0", ACTIVE_POWER_L1, 20000) //
						.input("meter0", ACTIVE_POWER_L2, 40000) //
						.input("meter0", ACTIVE_POWER_L3, 10000) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 12001)) //
				.next(new TestCase() //
						.input("ess0", ACTIVE_POWER, 3793) //
						.input("meter0", ACTIVE_POWER_L1, 20000 - 3793 / 3) //
						.input("meter0", ACTIVE_POWER_L2, 40000 - 3793 / 3) //
						.input("meter0", ACTIVE_POWER_L3, 10000 - 3793 / 3) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 16484)) //
				.next(new TestCase() //
						.input("ess0", ACTIVE_POWER, 8981) //
						.input("meter0", ACTIVE_POWER_L1, 20000 - 8981 / 3) //
						.input("meter0", ACTIVE_POWER_L2, 40000 - 8981 / 3) //
						.input("meter0", ACTIVE_POWER_L3, 10000 - 8981 / 3) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 19651)) //
				.deactivate();
	}
}
