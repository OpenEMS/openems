package io.openems.edge.controller.io.heatingelement;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.test.TimeLeapClock;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.io.heatingelement.enums.Level;
import io.openems.edge.controller.io.heatingelement.enums.Mode;
import io.openems.edge.controller.io.heatingelement.enums.WorkMode;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.io.test.DummyInputOutput;

public class ControllerHeatingElementImplTest4 {
	private static final String CTRL_ID = "ctrl0";
	private static final String IO_ID = "io0";

	private static final ChannelAddress IO_OUTPUT1 = new ChannelAddress(IO_ID, "InputOutput1");
	private static final ChannelAddress IO_OUTPUT2 = new ChannelAddress(IO_ID, "InputOutput2");
	private static final ChannelAddress IO_OUTPUT3 = new ChannelAddress(IO_ID, "InputOutput3");
	private static final TimeLeapClock clock = new TimeLeapClock(
			Instant.ofEpochSecond(1577836800) /* starts at 1. January 2020 00:00:00 */, ZoneOffset.UTC);

	private static ControllerTest prepareTest(Mode mode, Level level) throws OpenemsNamedException, Exception {
		return new ControllerTest(new ControllerIoHeatingElementImpl()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("sum", new DummySum()) //
				.addComponent(new DummyInputOutput("io0")) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setOutputChannelPhaseL1(IO_OUTPUT1.toString()) //
						.setOutputChannelPhaseL2(IO_OUTPUT2.toString()) //
						.setOutputChannelPhaseL3(IO_OUTPUT3.toString()) //
						.setEndTime("15:45:00") //
						.setPowerOfPhase(2000) //
						.setMode(mode) //
						.setDefaultLevel(level) //
						.setWorkMode(WorkMode.NONE) //
						.setMinTime(1) //
						.setMinimumSwitchingTime(180) //
						.build()); //
	}

	private static final ChannelAddress ESSO_DISCHARGE_POWER = new ChannelAddress("_sum",
			Sum.ChannelId.ESS_DISCHARGE_POWER.id());
	private static final ChannelAddress LEVEL = new ChannelAddress(CTRL_ID,
			ControllerIoHeatingElement.ChannelId.LEVEL.id());
	private static final ChannelAddress GRID_ACTIVE_POWER = new ChannelAddress("_sum",
			Sum.ChannelId.GRID_ACTIVE_POWER.id());

	@Test
	public void testDischargeTakeIntoAccount() throws OpenemsNamedException, Exception {
		prepareTest(Mode.AUTOMATIC, Level.LEVEL_3)//
				.next(new TestCase()//
						.input(GRID_ACTIVE_POWER, -2500)//
						.output(LEVEL, Level.LEVEL_1)) //
				.next(new TestCase() //
						.timeleap(clock, 181, ChronoUnit.SECONDS)//
						.input(GRID_ACTIVE_POWER, -2500)//
						.output(LEVEL, Level.LEVEL_2))//
				// Grid power reducing because of 2kW heating power
				.next(new TestCase()//
						.timeleap(clock, 181, ChronoUnit.SECONDS)//
						.input(GRID_ACTIVE_POWER, -500) //
						.output(LEVEL, Level.LEVEL_2)//
				).next(new TestCase() //
						.timeleap(clock, 181, ChronoUnit.SECONDS)//
						.input(GRID_ACTIVE_POWER, -500) //
						.input(ESSO_DISCHARGE_POWER, 2300) //
						.output(LEVEL, Level.LEVEL_1)//
				); // ;
	}

	@Test
	public void realDataTest() throws OpenemsNamedException, Exception {
		prepareTest(Mode.AUTOMATIC, Level.LEVEL_3)//
				// ensure level 3
				.next(new TestCase()//
						.input(GRID_ACTIVE_POWER, -6000)//
						.output(LEVEL, Level.LEVEL_3)) //
				.next(new TestCase()//
						.timeleap(clock, 181, ChronoUnit.SECONDS)//
						.input(GRID_ACTIVE_POWER, 0)//
						.input(ESSO_DISCHARGE_POWER, 2280)//
						.output(LEVEL, Level.LEVEL_1)); // ;
	}
}
