package io.openems.edge.controller.ess.linearpowerband;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;

public class ControllerEssLinearPowerBandImplTest {

	private static final String CTRL_ID = "ctrl0";
	private static final String ESS_ID = "ess0";

	private static final ChannelAddress STATE_MACHINE = new ChannelAddress(CTRL_ID, "StateMachine");
	private static final ChannelAddress ESS_SET_ACTIVE_POWER_EQUALS = new ChannelAddress(ESS_ID,
			"SetActivePowerEquals");

	@Test
	public void test() throws Exception {
		new ControllerTest(new ControllerEssLinearPowerBandImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setMinPower(-1000) //
						.setMaxPower(1000) //
						.setAdjustPower(300) //
						.setStartDirection(StartDirection.CHARGE) //
						.build()) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.DOWNWARDS) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, -300)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.DOWNWARDS) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, -600)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.DOWNWARDS) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, -900)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.DOWNWARDS) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, -1000)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.UPWARDS) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, -700)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.UPWARDS) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, -400)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.UPWARDS) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, -100)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.UPWARDS) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 200)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.UPWARDS) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 500)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.UPWARDS) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 800)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.UPWARDS) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 1000)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.DOWNWARDS) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 700)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.DOWNWARDS) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 400)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.DOWNWARDS) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 100)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.DOWNWARDS) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, -200)) //
		;
	}
}
