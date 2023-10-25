package io.openems.edge.controller.ess.standby;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.Before;
import org.junit.Test;

import io.openems.common.test.TimeLeapClock;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.ess.standby.statemachine.StateMachine.State;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;

public class ControllerEssStandbyImplTest {

	private static final int MAX_APPARENT_POWER = 50_000; // [W]

	private static final String CTRL_ID = "ctrlEssStandby0";
	private static final ChannelAddress STATE_MACHINE = new ChannelAddress(CTRL_ID, "StateMachine");

	private static final String SUM_ID = Sum.SINGLETON_COMPONENT_ID;
	private static final ChannelAddress SUM_GRID_ACTIVE_POWER = new ChannelAddress(SUM_ID, "GridActivePower");
	private static final ChannelAddress SUM_PRODUCTION_ACTIVE_POWER = new ChannelAddress(SUM_ID,
			"ProductionActivePower");
	private static final ChannelAddress SUM_CONSUMPTION_ACTIVE_POWER = new ChannelAddress(SUM_ID,
			"ConsumptionActivePower");

	private static final String ESS_ID = "ess0";
	private static final ChannelAddress ESS_ACTIVE_POWER = new ChannelAddress(ESS_ID, "ActivePower");
	private static final ChannelAddress ESS_SET_ACTIVE_POWER_EQUALS = new ChannelAddress(ESS_ID,
			"SetActivePowerEquals");
	private static final ChannelAddress ESS_ALLOWED_CHARGE_POWER = new ChannelAddress(ESS_ID, "AllowedChargePower");

	// Initialize mocked Clock
	private static TimeLeapClock clock;

	@Before
	public void initialize() {
		clock = new TimeLeapClock(ZonedDateTime.parse("2020-01-31T23:49:59Z").toInstant(), ZoneId.of("UTC"));
	}

	private static ControllerTest tillDischarge() throws Exception {
		// Initialize ESS
		final var ess = new DummyManagedSymmetricEss(ESS_ID) //
				.setGridMode(GridMode.ON_GRID) //
				.setMaxApparentPower(MAX_APPARENT_POWER) //
				.setSoc(70);

		return new ControllerTest(new ControllerEssStandbyImpl()) //
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
				.next(new TestCase("1 second before midnight (friday) before 01.02.2020") //
						.timeleap(clock, 10, ChronoUnit.MINUTES) //
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
						.input(SUM_GRID_ACTIVE_POWER, 10_000 /* buy from grid */) //
						.input(ESS_ACTIVE_POWER, 100 /* discharge */) //
						.output(STATE_MACHINE, State.DISCHARGE) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 10_100)) //
				.next(new TestCase("discharge > 70 % of maxApparentPower") //
						.timeleap(clock, 30, ChronoUnit.MINUTES) //
						.input(SUM_GRID_ACTIVE_POWER, 29_900 /* buy from grid */) //
						.input(ESS_ACTIVE_POWER, 10_100 /* discharge */) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 40_000)) //
				.next(new TestCase("discharge > 70 % of maxApparentPower - 9 minutes") //
						.timeleap(clock, 9, ChronoUnit.MINUTES) //
						.input(SUM_GRID_ACTIVE_POWER, 0 /* buy from grid */) //
						.input(ESS_ACTIVE_POWER, 40_000 /* discharge */) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 40_000)) //
				.next(new TestCase("discharge > 70 % of maxApparentPower - 10 minutes: reduce to 50 %") //
						.timeleap(clock, 1, ChronoUnit.MINUTES) //
						.input(SUM_GRID_ACTIVE_POWER, 0 /* buy from grid */) //
						.input(ESS_ACTIVE_POWER, 40_000 /* discharge */) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, (int) (MAX_APPARENT_POWER * 0.5)))
				.next(new TestCase("do not charge") //
						.timeleap(clock, 1, ChronoUnit.MINUTES) //
						.input(SUM_GRID_ACTIVE_POWER, -100 /* buy from grid */) //
						.input(ESS_ACTIVE_POWER, 0 /* discharge */) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 0));
	}

	private static ControllerTest tillSlowCharge1_1() throws Exception {
		return tillDischarge() //
				.next(new TestCase("production > consumption") //
						.input(SUM_GRID_ACTIVE_POWER, 0 /* buy from grid */) //
						.input(SUM_PRODUCTION_ACTIVE_POWER, 1000) //
						.input(SUM_CONSUMPTION_ACTIVE_POWER, 999) //
						.input(ESS_ACTIVE_POWER, 10_000 /* discharge */) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, 10_000)) //
				.next(new TestCase("production > consumption: more than 1 minute -> SLOW_CHARGE") //
						.timeleap(clock, 1, ChronoUnit.MINUTES) //
						.input(SUM_GRID_ACTIVE_POWER, 0 /* buy from grid */) //
						.input(SUM_PRODUCTION_ACTIVE_POWER, 1000) //
						.input(SUM_CONSUMPTION_ACTIVE_POWER, 999)) //
				/*
				 * SLOW_CHARGE
				 */
				.next(new TestCase("SLOW_CHARGE") //
						.output(STATE_MACHINE, State.SLOW_CHARGE_1)); //
	}

	private static ControllerTest tillSlowCharge1_2() throws Exception {
		return tillDischarge() //
				.next(new TestCase("latest at 12 -> SLOW_CHARGE") //
						.timeleap(clock, 12, ChronoUnit.HOURS)) //
				/*
				 * SLOW_CHARGE
				 */
				.next(new TestCase("SLOW_CHARGE") //
						.output(STATE_MACHINE, State.SLOW_CHARGE_1)); //
	}

	private static ControllerTest tillSlowCharge2_1() throws Exception {
		return tillSlowCharge1_2() //
				.next(new TestCase("") //
						.input(ESS_ACTIVE_POWER, 0) //
						.input(SUM_GRID_ACTIVE_POWER, -20_000 /* sell to grid */) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, -20_000)) //
				.next(new TestCase("Charge with minimum 20 %") //
						.input(ESS_ACTIVE_POWER, 0) //
						.input(SUM_GRID_ACTIVE_POWER, (int) (MAX_APPARENT_POWER * -0.19) /* sell to grid */) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, (int) (MAX_APPARENT_POWER * -0.20))) //
				.next(new TestCase("Charge with maximum 50 %") //
						.input(ESS_ACTIVE_POWER, 0) //
						.input(SUM_GRID_ACTIVE_POWER, (int) (MAX_APPARENT_POWER * -0.51) /* sell to grid */) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, (int) (MAX_APPARENT_POWER * -0.50))) //
				.next(new TestCase("after 30 minutes -> FAST_CHARGE") //
						.timeleap(clock, 30, ChronoUnit.MINUTES)) //
				/*
				 * FAST_CHARGE
				 */
				.next(new TestCase("FAST_CHARGE with max power") //
						.output(STATE_MACHINE, State.FAST_CHARGE) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, MAX_APPARENT_POWER * -1)) //
				.next(new TestCase("after 10 minutes -> SLOW_CHARGE_2") //
						.timeleap(clock, 10, ChronoUnit.MINUTES)) //
				/*
				 * SLOW_CHARGE_2
				 */
				.next(new TestCase("SLOW_CHARGE_2") //
						.input(ESS_ACTIVE_POWER, 0) //
						.input(ESS_ALLOWED_CHARGE_POWER, -60_000) //
						.input(SUM_GRID_ACTIVE_POWER, -20_000 /* sell to grid */) //
						.output(STATE_MACHINE, State.SLOW_CHARGE_2) //
						.output(ESS_SET_ACTIVE_POWER_EQUALS, -20_000)) //
				/*
				 * FINISHED
				 */
				.next(new TestCase("no more charging allowed -> FINISHED") //
						.input(ESS_ALLOWED_CHARGE_POWER, 0)) //
				.next(new TestCase("FINISHED") //
						.output(STATE_MACHINE, State.FINISHED)) //
				/*
				 * UNDEFINED
				 */
				.next(new TestCase("on day change -> UNDEFINED") //
						.timeleap(clock, 11, ChronoUnit.HOURS)) //
				.next(new TestCase("UNDEFINED") //
						.output(STATE_MACHINE, State.UNDEFINED)) //
				.next(new TestCase("After test period") //
						.timeleap(clock, 30, ChronoUnit.DAYS) //
						.output(STATE_MACHINE, State.UNDEFINED)); //
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
