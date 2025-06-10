package io.openems.edge.controller.ess.standby;

import static io.openems.edge.common.sum.Sum.ChannelId.CONSUMPTION_ACTIVE_POWER;
import static io.openems.edge.common.sum.Sum.ChannelId.GRID_ACTIVE_POWER;
import static io.openems.edge.common.sum.Sum.ChannelId.PRODUCTION_ACTIVE_POWER;
import static io.openems.edge.controller.ess.standby.ControllerEssStandby.ChannelId.STATE_MACHINE;
import static io.openems.edge.ess.api.ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER;
import static io.openems.edge.ess.api.ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS;
import static io.openems.edge.ess.api.SymmetricEss.ChannelId.ACTIVE_POWER;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.Before;
import org.junit.Test;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.ess.standby.statemachine.StateMachine.State;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;

public class ControllerEssStandbyImplTest {

	private static final int MAX_APPARENT_POWER = 50_000; // [W]

	private static TimeLeapClock clock;

	@Before
	public void initialize() {
		clock = new TimeLeapClock(ZonedDateTime.parse("2020-01-31T23:49:59Z").toInstant(), ZoneId.of("UTC"));
	}

	private static ControllerTest tillDischarge() throws Exception {
		// Initialize ESS
		final var ess = new DummyManagedSymmetricEss("ess0") //
				.withGridMode(GridMode.ON_GRID) //
				.withMaxApparentPower(MAX_APPARENT_POWER) //
				.withSoc(70);

		return new ControllerTest(new ControllerEssStandbyImpl()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("sum", new DummySum()) //
				.addComponent(ess) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setStartDate("01.02.2020") //
						.setEndDate("01.03.2020") //
						.setDayOfWeek(DayOfWeek.SUNDAY) //
						.build()) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.UNDEFINED)) //
				.next(new TestCase("1 second before midnight (friday) before 01.02.2020") //
						.timeleap(clock, 10, MINUTES) //
						.output(STATE_MACHINE, State.UNDEFINED)) //
				.next(new TestCase("midnight (saturday)") //
						.timeleap(clock, 1, ChronoUnit.SECONDS) //
						.output(STATE_MACHINE, State.UNDEFINED)) //
				.next(new TestCase("sunday") //
						.timeleap(clock, 1, ChronoUnit.DAYS) //
						.output(STATE_MACHINE, State.UNDEFINED)) //
				/*
				 * DISCHARGE
				 */
				.next(new TestCase("sunday -> switch to DISCHARGE") //
						.input(GRID_ACTIVE_POWER, 10_000 /* buy from grid */) //
						.input("ess0", ACTIVE_POWER, 100 /* discharge */) //
						.output(STATE_MACHINE, State.DISCHARGE) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 10_100)) //
				.next(new TestCase("discharge > 70 % of maxApparentPower") //
						.timeleap(clock, 30, MINUTES) //
						.input(GRID_ACTIVE_POWER, 29_900 /* buy from grid */) //
						.input("ess0", ACTIVE_POWER, 10_100 /* discharge */) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 40_000)) //
				.next(new TestCase("discharge > 70 % of maxApparentPower - 9 minutes") //
						.timeleap(clock, 9, MINUTES) //
						.input(GRID_ACTIVE_POWER, 0 /* buy from grid */) //
						.input("ess0", ACTIVE_POWER, 40_000 /* discharge */) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 40_000)) //
				.next(new TestCase("discharge > 70 % of maxApparentPower - 10 minutes: reduce to 50 %") //
						.timeleap(clock, 1, MINUTES) //
						.input(GRID_ACTIVE_POWER, 0 /* buy from grid */) //
						.input("ess0", ACTIVE_POWER, 40_000 /* discharge */) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, (int) (MAX_APPARENT_POWER * 0.5)))
				.next(new TestCase("do not charge") //
						.timeleap(clock, 1, MINUTES) //
						.input(GRID_ACTIVE_POWER, -100 /* buy from grid */) //
						.input("ess0", ACTIVE_POWER, 0 /* discharge */) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 0)) //
				.deactivate();
	}

	private static ControllerTest tillSlowCharge1_1() throws Exception {
		return tillDischarge() //
				.next(new TestCase("production > consumption") //
						.input(GRID_ACTIVE_POWER, 0 /* buy from grid */) //
						.input(PRODUCTION_ACTIVE_POWER, 1000) //
						.input(CONSUMPTION_ACTIVE_POWER, 999) //
						.input("ess0", ACTIVE_POWER, 10_000 /* discharge */) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 10_000)) //
				.next(new TestCase("production > consumption: more than 1 minute -> SLOW_CHARGE") //
						.timeleap(clock, 1, MINUTES) //
						.input(GRID_ACTIVE_POWER, 0 /* buy from grid */) //
						.input(PRODUCTION_ACTIVE_POWER, 1000) //
						.input(CONSUMPTION_ACTIVE_POWER, 999)) //
				/*
				 * SLOW_CHARGE
				 */
				.next(new TestCase("SLOW_CHARGE") //
						.output(STATE_MACHINE, State.SLOW_CHARGE_1)) //
				.deactivate();
	}

	private static ControllerTest tillSlowCharge1_2() throws Exception {
		return tillDischarge() //
				.next(new TestCase("latest at 12 -> SLOW_CHARGE") //
						.timeleap(clock, 12, HOURS)) //
				/*
				 * SLOW_CHARGE
				 */
				.next(new TestCase("SLOW_CHARGE") //
						.output(STATE_MACHINE, State.SLOW_CHARGE_1)) //
				.deactivate();
	}

	private static ControllerTest tillSlowCharge2_1() throws Exception {
		return tillSlowCharge1_2() //
				.next(new TestCase("") //
						.input("ess0", ACTIVE_POWER, 0) //
						.input(GRID_ACTIVE_POWER, -20_000 /* sell to grid */) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -20_000)) //
				.next(new TestCase("Charge with minimum 20 %") //
						.input("ess0", ACTIVE_POWER, 0) //
						.input(GRID_ACTIVE_POWER, (int) (MAX_APPARENT_POWER * -0.19)) // sell to grid
						.output("ess0", SET_ACTIVE_POWER_EQUALS, (int) (MAX_APPARENT_POWER * -0.20))) //
				.next(new TestCase("Charge with maximum 50 %") //
						.input("ess0", ACTIVE_POWER, 0) //
						.input(GRID_ACTIVE_POWER, (int) (MAX_APPARENT_POWER * -0.51)) // sell to grid
						.output("ess0", SET_ACTIVE_POWER_EQUALS, (int) (MAX_APPARENT_POWER * -0.50))) //
				.next(new TestCase("after 30 minutes -> FAST_CHARGE") //
						.timeleap(clock, 30, MINUTES)) //
				/*
				 * FAST_CHARGE
				 */
				.next(new TestCase("FAST_CHARGE with max power") //
						.output(STATE_MACHINE, State.FAST_CHARGE) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, MAX_APPARENT_POWER * -1)) //
				.next(new TestCase("after 10 minutes -> SLOW_CHARGE_2") //
						.timeleap(clock, 10, MINUTES)) //
				/*
				 * SLOW_CHARGE_2
				 */
				.next(new TestCase("SLOW_CHARGE_2") //
						.input("ess0", ACTIVE_POWER, 0) //
						.input("ess0", ALLOWED_CHARGE_POWER, -60_000) //
						.input(GRID_ACTIVE_POWER, -20_000 /* sell to grid */) //
						.output(STATE_MACHINE, State.SLOW_CHARGE_2) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -20_000)) //
				/*
				 * FINISHED
				 */
				.next(new TestCase("no more charging allowed -> FINISHED") //
						.input("ess0", ALLOWED_CHARGE_POWER, 0)) //
				.next(new TestCase("FINISHED") //
						.output(STATE_MACHINE, State.FINISHED)) //
				/*
				 * UNDEFINED
				 */
				.next(new TestCase("on day change -> UNDEFINED") //
						.timeleap(clock, 11, HOURS)) //
				.next(new TestCase("UNDEFINED") //
						.output(STATE_MACHINE, State.UNDEFINED)) //
				.next(new TestCase("After test period") //
						.timeleap(clock, 30, ChronoUnit.DAYS) //
						.output(STATE_MACHINE, State.UNDEFINED)) //
				.deactivate();
	}

	@Test
	public void test1() throws Exception {
		tillSlowCharge2_1();
	}

	@Test
	public void test2() throws Exception {
		tillSlowCharge1_1();
	}

}
