package io.openems.edge.controller.io.heatingelement;

import static io.openems.edge.common.sum.Sum.ChannelId.ESS_DISCHARGE_POWER;
import static io.openems.edge.common.sum.Sum.ChannelId.GRID_ACTIVE_POWER;
import static io.openems.edge.controller.io.heatingelement.ControllerIoHeatingElement.ChannelId.LEVEL;
import static java.time.temporal.ChronoUnit.SECONDS;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.TestUtils;
import io.openems.edge.controller.io.heatingelement.enums.Level;
import io.openems.edge.controller.io.heatingelement.enums.Mode;
import io.openems.edge.controller.io.heatingelement.enums.WorkMode;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.io.test.DummyInputOutput;

public class ControllerHeatingElementImplTest4 {

	private static TimeLeapClock clock;

	private static ControllerTest prepareTest(Mode mode, Level level) throws OpenemsNamedException, Exception {
		clock = TestUtils.createDummyClock();
		return new ControllerTest(new ControllerIoHeatingElementImpl()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("sum", new DummySum()) //
				.addComponent(new DummyInputOutput("io0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setOutputChannelPhaseL1("io0/InputOutput0") //
						.setOutputChannelPhaseL2("io0/InputOutput1") //
						.setOutputChannelPhaseL3("io0/InputOutput2") //
						.setEndTime("15:45:00") //
						.setPowerOfPhase(2000) //
						.setMode(mode) //
						.setDefaultLevel(level) //
						.setWorkMode(WorkMode.NONE) //
						.setMinTime(1) //
						.setMinimumSwitchingTime(180) //
						.build()); //
	}

	@Test
	public void testDischargeTakeIntoAccount() throws OpenemsNamedException, Exception {
		prepareTest(Mode.AUTOMATIC, Level.LEVEL_3)//
				.next(new TestCase()//
						.input(GRID_ACTIVE_POWER, -2500)//
						.output(LEVEL, Level.LEVEL_1)) //
				.next(new TestCase() //
						.timeleap(clock, 181, SECONDS)//
						.input(GRID_ACTIVE_POWER, -2500)//
						.output(LEVEL, Level.LEVEL_2))//
				// Grid power reducing because of 2kW heating power
				.next(new TestCase()//
						.timeleap(clock, 181, SECONDS)//
						.input(GRID_ACTIVE_POWER, -500) //
						.output(LEVEL, Level.LEVEL_2)) //
				.next(new TestCase() //
						.timeleap(clock, 181, SECONDS)//
						.input(GRID_ACTIVE_POWER, -500) //
						.input(ESS_DISCHARGE_POWER, 2300) //
						.output(LEVEL, Level.LEVEL_1)) //
				.deactivate();
	}

	@Test
	public void realDataTest() throws OpenemsNamedException, Exception {
		prepareTest(Mode.AUTOMATIC, Level.LEVEL_3)//
				// ensure level 3
				.next(new TestCase()//
						.input(GRID_ACTIVE_POWER, -6000)//
						.output(LEVEL, Level.LEVEL_3)) //
				.next(new TestCase()//
						.timeleap(clock, 181, SECONDS)//
						.input(GRID_ACTIVE_POWER, 0)//
						.input(ESS_DISCHARGE_POWER, 2280)//
						.output(LEVEL, Level.LEVEL_1)) //
				.deactivate();
	}
}
