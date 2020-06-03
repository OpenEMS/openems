package io.openems.edge.controller.ess.standby;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.OpenemsConstants;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.controller.ess.standby.statemachine.State;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.ess.test.DummyPower;

public class StandbyControllerImplTest {

	private final static String CTRL_ID = "ctrlEssStandby0";
	private final static ChannelAddress STATE_MACHINE = new ChannelAddress(CTRL_ID, "StateMachine");

	private final static String SUM_ID = OpenemsConstants.SUM_ID;
	private final static ChannelAddress SUM_GRID_ACTIVE_POWER = new ChannelAddress(SUM_ID, "GridActivePower");

	private final static String ESS_ID = "ess0";
	private final static ChannelAddress ESS_ACTIVE_POWER = new ChannelAddress(ESS_ID, "ActivePower");
	private final static ChannelAddress ESS_SET_ACTIVE_POWER_EQUALS = new ChannelAddress(ESS_ID,
			"SetActivePowerEquals");

	@Test
	public void testStart() throws Exception {
		// Initialize mocked Clock
		final TimeLeapClock clock = new TimeLeapClock(ZonedDateTime.parse("2020-01-31T23:49:59Z").toInstant(),
				ZoneId.of("UTC"));

		// Initialize ESS
		final DummyPower power = new DummyPower(1, 0, 0);
		final DummyManagedSymmetricEss ess = new DummyManagedSymmetricEss(ESS_ID, power) //
				.setGridMode(GridMode.ON_GRID) //
				.setMaxApparentPower(50_000);

		new ControllerTest(new StandbyControllerImpl()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("sum", new DummySum()) //
				.addComponent(ess) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setStartDate("01.02.2020") //
						.setEndDate("01.03.2020") //
						.setDayOfWeek(DayOfWeek.SUNDAY) //
						.build()) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.UNDEFINED)) //
				.next(new TestCase("1 second before midnight (friday)") //
						.timeleap(clock, 10, ChronoUnit.MINUTES) //
						.output(STATE_MACHINE, State.UNDEFINED)) //
				.next(new TestCase("midnight (saturday)") //
						.timeleap(clock, 1, ChronoUnit.SECONDS) //
						.output(STATE_MACHINE, State.UNDEFINED)) //
				.next(new TestCase("sunday") //
						.timeleap(clock, 1, ChronoUnit.DAYS) //
						.output(STATE_MACHINE, State.UNDEFINED)) //
				.next(new TestCase("sunday -> switch to DISCHARGE") //
						.input(SUM_GRID_ACTIVE_POWER, 10_000 /* buy from grid */) //
						.input(ESS_ACTIVE_POWER, 100 /* discharge */) //
						.output(STATE_MACHINE, State.DISCHARGE) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 10_000)) //
				.next(new TestCase("clock: 12:01") //
						.timeleap(clock, 12 * 60 + 1, ChronoUnit.MINUTES)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.SLOW_CHARGE_1)) //
		;
	}

}
