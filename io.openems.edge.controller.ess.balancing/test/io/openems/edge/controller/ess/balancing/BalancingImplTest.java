package io.openems.edge.controller.ess.balancing;

import static io.openems.edge.ess.api.ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyPower;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.test.DummyElectricityMeter;

public class BalancingImplTest {

	@Test
	public void test() throws Exception {
		new ControllerTest(new ControllerEssBalancingImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", new DummyManagedSymmetricEss("ess0") //
						.setPower(new DummyPower(0.3, 0.3, 0.1))) //
				.addReference("meter", new DummyElectricityMeter("meter0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setMeterId("meter0") //
						.setTargetGridSetpoint(0) //
						.build())
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 0) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20000) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 6000)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 0) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20000) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 12000)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 3793) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20000 - 3793) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 16483)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 8981) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20000 - 8981) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 19649)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 13723) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20000 - 13723) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 21577)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 17469) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20000 - 17469) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 22436)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 20066) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20000 - 20066) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 22531)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 21564) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20000 - 21564) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 22171)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 22175) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20000 - 22175) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 21608)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 22173) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20000 - 22173) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 21017)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 21816) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20000 - 21816) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 20508)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 21311) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20000 - 21311) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 20129)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 20803) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20000 - 20803) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 19889)) //
				.next(new TestCase() //
						.input("ess0", SymmetricEss.ChannelId.ACTIVE_POWER, 20377) //
						.input("meter0", ElectricityMeter.ChannelId.ACTIVE_POWER, 20000 - 20377) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 19767)) //
				.deactivate();
	}

}
