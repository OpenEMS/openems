package io.openems.edge.controller.ess.limittotalcharge;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.controller.ess.limittotalcharge.ControllerEssLimitTotalCharge.ChannelId.AWAITING_HYSTERESIS;
import static io.openems.edge.ess.api.ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_GREATER_OR_EQUALS;
import static io.openems.edge.ess.api.SymmetricEss.ChannelId.SOC;

import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;

public class ControllerEssLimitTotalChargeImplTest {

	@Test
	public void testMaxSoc() throws Exception {
		final var clock = createDummyClock();
		new ControllerTest(new ControllerEssLimitTotalChargeImpl()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addComponent(new DummyManagedSymmetricEss("ess0") //
						.withSoc(80) //
						.withCapacity(9000)) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setMaxSoc(85) //
						.setForceDischargeSoc(90) //
						.setForceDischargePower(1000) //
						.build())
				.next(new TestCase() //
						.input("ess0", SOC, 80) //
						.output("ess0", SET_ACTIVE_POWER_GREATER_OR_EQUALS, null)//
						.output(AWAITING_HYSTERESIS, false)) //
				.next(new TestCase() //
						.input("ess0", SOC, 85) //
						.output("ess0", SET_ACTIVE_POWER_GREATER_OR_EQUALS, 0) //
						.output(AWAITING_HYSTERESIS, false)) //
				.next(new TestCase() //
						.input("ess0", SOC, 84) //
						.output("ess0", SET_ACTIVE_POWER_GREATER_OR_EQUALS, 0) //
						.output(AWAITING_HYSTERESIS, true)) //
				.next(new TestCase() //
						.timeleap(clock, 6, ChronoUnit.MINUTES) //
						.input("ess0", SOC, 84) //
						.output("ess0", SET_ACTIVE_POWER_GREATER_OR_EQUALS, null) //
						.output(AWAITING_HYSTERESIS, false)) //
				.deactivate();
	}

	@Test
	public void testForceDischargeSoc() throws Exception {
		final var clock = createDummyClock();
		new ControllerTest(new ControllerEssLimitTotalChargeImpl()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addComponent(new DummyManagedSymmetricEss("ess0") //
						.withSoc(92) //
						.withCapacity(9000)) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setMaxSoc(85) //
						.setForceDischargeSoc(90) //
						.setForceDischargePower(1000) //
						.build())
				.next(new TestCase() //
						.input("ess0", SOC, 92) //
						.output("ess0", SET_ACTIVE_POWER_GREATER_OR_EQUALS, 1000) //
						.output(AWAITING_HYSTERESIS, false)) //
				.next(new TestCase() //
						.input("ess0", SOC, 89) //
						.output("ess0", SET_ACTIVE_POWER_GREATER_OR_EQUALS, 1000) //
						.output(AWAITING_HYSTERESIS, true)) //
				.next(new TestCase() //
						.timeleap(clock, 6, ChronoUnit.MINUTES) //
						.input("ess0", SOC, 89) //
						.output("ess0", SET_ACTIVE_POWER_GREATER_OR_EQUALS, 0) //
						.output(AWAITING_HYSTERESIS, false)) //
				.deactivate();
	}

}
