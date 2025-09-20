package io.openems.edge.controller.ess.linearpowerband;

import static io.openems.edge.controller.ess.linearpowerband.ControllerEssLinearPowerBand.ChannelId.STATE_MACHINE;
import static io.openems.edge.ess.api.ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;

public class ControllerEssLinearPowerBandImplTest {

	@Test
	public void test() throws Exception {
		new ControllerTest(new ControllerEssLinearPowerBandImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", new DummyManagedSymmetricEss("ess0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setMinPower(-1000) //
						.setMaxPower(1000) //
						.setAdjustPower(300) //
						.setStartDirection(StartDirection.CHARGE) //
						.build()) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.DOWNWARDS) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -300)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.DOWNWARDS) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -600)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.DOWNWARDS) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -900)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.DOWNWARDS) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -1000)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.UPWARDS) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -700)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.UPWARDS) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -400)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.UPWARDS) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -100)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.UPWARDS) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 200)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.UPWARDS) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 500)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.UPWARDS) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 800)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.UPWARDS) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 1000)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.DOWNWARDS) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 700)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.DOWNWARDS) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 400)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.DOWNWARDS) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 100)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.DOWNWARDS) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -200)) //
				.deactivate();
	}
}
