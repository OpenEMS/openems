package io.openems.edge.controller.ess.limitusablecapacity;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyPower;

public class LimitUsableCapacityControllerTest {

	private static final String CTRL_ID = "ctrl0";
	private static final String ESS_ID = "ess0";
	private static final ChannelAddress ESS_SOC = new ChannelAddress(ESS_ID, "Soc");
	private static final ChannelAddress STATE_MACHINE = new ChannelAddress(CTRL_ID, "StateMachine");
	private final static ChannelAddress MAX_APPARENT_POWER = new ChannelAddress(ESS_ID, "MaxApparentPower");

	@Test
	public void test() throws Exception {
		new ControllerTest(new LimitUsableCapacityControllerImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addComponent(new DummyManagedSymmetricEss(ESS_ID, new DummyPower(0.3, 0.3, 0.1)))

				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setStopDischargeSoc(10) //
						.setAllowDischargeSoc(12) //
						.setForceChargeSoc(8) //
						.setStopChargeSoc(90) //
						.setAllowChargeSoc(85) //
						.build())
				.next(new TestCase() //
						.input(ESS_SOC, 90) //
						.output(STATE_MACHINE, State.STOP_CHARGE)) //

				.next(new TestCase() //
						.input(ESS_SOC, 86) //
						.output(STATE_MACHINE, State.STOP_CHARGE)) //

				.next(new TestCase() //
						.input(ESS_SOC, 84) //
						.output(STATE_MACHINE, State.NO_LIMIT)) //

				.next(new TestCase() //
						.input(ESS_SOC, 9) //
						.output(STATE_MACHINE, State.STOP_DISCHARGE)) //
				
				.next(new TestCase() //
						.input(ESS_SOC, 13) //
						.output(STATE_MACHINE, State.NO_LIMIT)) 
				
				.next(new TestCase() //
						.input(ESS_SOC, 9) //
						.output(STATE_MACHINE, State.STOP_DISCHARGE))
				
				.next(new TestCase() //
						.input(ESS_SOC, 5) //
						.input(MAX_APPARENT_POWER, 5000)
						.output(STATE_MACHINE, State.FORCE_CHARGE))
				
				.next(new TestCase() //
						.input(ESS_SOC, 84) //
						.output(STATE_MACHINE, State.NO_LIMIT))
				
				

				.next(new TestCase() //
						.input(ESS_SOC, 84) //
						.output(STATE_MACHINE, State.NO_LIMIT)

				);
	}
}
