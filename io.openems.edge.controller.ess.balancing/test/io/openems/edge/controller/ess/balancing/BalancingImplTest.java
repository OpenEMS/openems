package io.openems.edge.controller.ess.balancing;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyPower;
import io.openems.edge.meter.test.DummySymmetricMeter;

public class BalancingImplTest {

	private static final String CTRL_ID = "ctrl0";

	private static final String ESS_ID = "ess0";
	private static final ChannelAddress ESS_ACTIVE_POWER = new ChannelAddress(ESS_ID, "ActivePower");
	private static final ChannelAddress ESS_SET_ACTIVE_POWER_EQUALS = new ChannelAddress(ESS_ID,
			"SetActivePowerEquals");

	private static final String METER_ID = "meter0";
	private static final ChannelAddress METER_ACTIVE_POWER = new ChannelAddress(METER_ID, "ActivePower");

	@Test
	public void test() throws Exception {
		new ControllerTest(new BalancingImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID, new DummyPower(0.3, 0.3, 0.1))) //
				.addReference("meter", new DummySymmetricMeter(METER_ID)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setMeterId(METER_ID) //
						.setTargetGridSetpoint(0) //
						.build())
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 0) //
						.input(METER_ACTIVE_POWER, 20000) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 6000)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 0) //
						.input(METER_ACTIVE_POWER, 20000) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 12000)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 3793) //
						.input(METER_ACTIVE_POWER, 20000 - 3793) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 16483)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 8981) //
						.input(METER_ACTIVE_POWER, 20000 - 8981) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 19649)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 13723) //
						.input(METER_ACTIVE_POWER, 20000 - 13723) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 21577)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 17469) //
						.input(METER_ACTIVE_POWER, 20000 - 17469) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 22436)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 20066) //
						.input(METER_ACTIVE_POWER, 20000 - 20066) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 22531)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 21564) //
						.input(METER_ACTIVE_POWER, 20000 - 21564) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 22171)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 22175) //
						.input(METER_ACTIVE_POWER, 20000 - 22175) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 21608)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 22173) //
						.input(METER_ACTIVE_POWER, 20000 - 22173) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 21017)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 21816) //
						.input(METER_ACTIVE_POWER, 20000 - 21816) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 20508)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 21311) //
						.input(METER_ACTIVE_POWER, 20000 - 21311) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 20129)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 20803) //
						.input(METER_ACTIVE_POWER, 20000 - 20803) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 19889)) //
				.next(new TestCase() //
						.input(ESS_ACTIVE_POWER, 20377) //
						.input(METER_ACTIVE_POWER, 20000 - 20377) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 19767));
	}

}
