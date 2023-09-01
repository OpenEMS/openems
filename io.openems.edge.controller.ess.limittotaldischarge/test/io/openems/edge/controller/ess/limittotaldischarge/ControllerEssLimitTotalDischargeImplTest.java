package io.openems.edge.controller.ess.limittotaldischarge;

import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.test.TimeLeapClock;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;

public class ControllerEssLimitTotalDischargeImplTest {

	private static final String CTRL_ID = "ctrl0";
	private static final ChannelAddress CTRL_AWAITING_HYSTERESIS = new ChannelAddress(CTRL_ID, "AwaitingHysteresis");

	private static final String ESS_ID = "ess0";
	private static final ChannelAddress ESS_SOC = new ChannelAddress(ESS_ID, "Soc");
	private static final ChannelAddress ESS_SET_ACTIVE_POWER_LESS_OR_EQUALS = new ChannelAddress(ESS_ID,
			"SetActivePowerLessOrEquals");

	@Test
	public void test() throws Exception {
		// Initialize mocked Clock
		final var clock = new TimeLeapClock();
		new ControllerTest(new ControllerEssLimitTotalDischargeImpl()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addComponent(new DummyManagedSymmetricEss(ESS_ID) //
						.withSoc(20) //
						.withCapacity(9000)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setMinSoc(15) //
						.setForceChargeSoc(10) //
						.setForceChargePower(1000) //
						.build())
				.next(new TestCase() //
						.input(ESS_SOC, 20) //
						.output(ESS_SET_ACTIVE_POWER_LESS_OR_EQUALS, null)//
						.output(CTRL_AWAITING_HYSTERESIS, false)) //
				.next(new TestCase() //
						.input(ESS_SOC, 15) //
						.output(ESS_SET_ACTIVE_POWER_LESS_OR_EQUALS, 0) //
						.output(CTRL_AWAITING_HYSTERESIS, false)) //
				.next(new TestCase() //
						.input(ESS_SOC, 16) //
						.output(ESS_SET_ACTIVE_POWER_LESS_OR_EQUALS, 0) //
						.output(CTRL_AWAITING_HYSTERESIS, true)) //
				.next(new TestCase() //
						.timeleap(clock, 6, ChronoUnit.MINUTES) //
						.input(ESS_SOC, 16) //
						.output(ESS_SET_ACTIVE_POWER_LESS_OR_EQUALS, null) //
						.output(CTRL_AWAITING_HYSTERESIS, false));
	}

}
