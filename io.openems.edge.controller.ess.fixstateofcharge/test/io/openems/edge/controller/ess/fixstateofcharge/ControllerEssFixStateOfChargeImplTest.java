package io.openems.edge.controller.ess.fixstateofcharge;

import static io.openems.edge.controller.ess.fixstateofcharge.api.AbstractFixStateOfCharge.DEFAULT_POWER_FACTOR;
import static io.openems.edge.controller.ess.fixstateofcharge.api.EndCondition.CAPACITY_CHANGED;
import static io.openems.edge.controller.ess.fixstateofcharge.api.FixStateOfCharge.ChannelId.DEBUG_SET_ACTIVE_POWER;
import static io.openems.edge.controller.ess.fixstateofcharge.api.FixStateOfCharge.ChannelId.DEBUG_SET_ACTIVE_POWER_RAW;
import static io.openems.edge.controller.ess.fixstateofcharge.api.FixStateOfCharge.ChannelId.STATE_MACHINE;
import static io.openems.edge.ess.api.ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS;
import static io.openems.edge.ess.api.SymmetricEss.ChannelId.CAPACITY;
import static io.openems.edge.ess.api.SymmetricEss.ChannelId.MAX_APPARENT_POWER;
import static io.openems.edge.ess.api.SymmetricEss.ChannelId.SOC;
import static java.lang.Math.min;
import static java.lang.Math.round;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MINUTES;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.Test;

import io.openems.common.test.TimeLeapClock;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.ess.fixstateofcharge.statemachine.StateMachine.State;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.timedata.test.DummyTimedata;

public class ControllerEssFixStateOfChargeImplTest {

	private static final DummyManagedSymmetricEss ESS = new DummyManagedSymmetricEss("ess0") //
			.withMaxApparentPower(10_000);
	private static final String DEFAULT_TARGET_TIME = "2022-10-27T10:30:00+01:00";
	private static final ChannelAddress CTRL_ESS_CAPACITY = new ChannelAddress("ctrl0", "EssCapacity");

	@Test
	public void testNotRunning() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2022-01-01T08:00:00.00Z"), ZoneOffset.UTC);
		new ControllerTest(new ControllerEssFixStateOfChargeImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.addReference("sum", new DummySum()) //
				.addReference("ess", ESS) //
				.activate(FixStateOfChargeConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setRunning(false) //
						.setTargetSoc(30) //
						.setSpecifyTargetTime(true) //
						.setTargetTime(DEFAULT_TARGET_TIME) //
						.setSelfTermination(false) //
						.setTerminationBuffer(720) //
						.setConditionalTermination(false) //
						.setEndCondition(CAPACITY_CHANGED) //
						.build())

				.next(new TestCase()) //
				.next(new TestCase() //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, null) //
						.output(DEBUG_SET_ACTIVE_POWER, null) //
						.output(DEBUG_SET_ACTIVE_POWER_RAW, null) //
						.output(STATE_MACHINE, State.IDLE)) //
				.deactivate();
	}

	@Test
	public void testAllStates() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2023-01-01T08:00:00.00Z"), ZoneOffset.UTC);
		new ControllerTest(new ControllerEssFixStateOfChargeImpl()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.addReference("ess", new DummyManagedSymmetricEss("ess0")) //
				.activate(FixStateOfChargeConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setRunning(true) //
						.setTargetSoc(30) //
						.setSpecifyTargetTime(true) //
						.setTargetTime(DEFAULT_TARGET_TIME) //
						.setSelfTermination(false) //
						.setTerminationBuffer(720) //
						.setConditionalTermination(true) //
						.setEndCondition(CAPACITY_CHANGED) //
						.build())
				.next(new TestCase() //
						.input("ess0", SOC, 22) //
						.input("ess0", MAX_APPARENT_POWER, 10000) //
						.output(STATE_MACHINE, State.IDLE)) //
				.next(new TestCase() //
						.input("ess0", SOC, 22) //
						.input("ess0", MAX_APPARENT_POWER, 10000) //
						.output(STATE_MACHINE, State.IDLE)) //
				.next(new TestCase() //
						.input("ess0", SOC, 21) //
						.output(STATE_MACHINE, State.NOT_STARTED)) //
				.next(new TestCase() //
						.input("ess0", SOC, 20) //
						.output(STATE_MACHINE, State.BELOW_TARGET_SOC)) //
				.next(new TestCase() //
						.input("ess0", SOC, 25) //
						.output(STATE_MACHINE, State.BELOW_TARGET_SOC)) //
				.next(new TestCase() //
						.input("ess0", SOC, 30)) //
				.next(new TestCase() //
						.input("ess0", SOC, 30) ///
						.output(STATE_MACHINE, State.AT_TARGET_SOC)) //
				.next(new TestCase() //
						.input("ess0", SOC, 30) //
						.output(STATE_MACHINE, State.AT_TARGET_SOC)) //
				.deactivate();
	}

	@Test
	public void testCapacityCondition() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2023-01-01T08:00:00.00Z"), ZoneOffset.UTC);
		var timedata = new DummyTimedata("timedata0");

		var start = ZonedDateTime.of(2022, 05, 05, 0, 0, 0, 0, ZoneId.of("UTC"));
		timedata.add(start.plusMinutes(30), CTRL_ESS_CAPACITY, 8_000);
		timedata.add(start.plusMinutes(60), CTRL_ESS_CAPACITY, 8_000);
		timedata.add(start.plusMinutes(90), CTRL_ESS_CAPACITY, 8_000);

		new ControllerTest(new ControllerEssFixStateOfChargeImpl()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("timedata", timedata) //
				.addReference("ess", new DummyManagedSymmetricEss("ess0")) //
				.activate(FixStateOfChargeConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setRunning(true) //
						.setTargetSoc(30) //
						.setSpecifyTargetTime(true) //
						.setTargetTime(DEFAULT_TARGET_TIME) //
						.setSelfTermination(false) //
						.setTerminationBuffer(720) //
						.setConditionalTermination(true) //
						.setEndCondition(CAPACITY_CHANGED) //
						.build())
				.next(new TestCase() //
						.input("ess0", SOC, 22) //
						.input("ess0", MAX_APPARENT_POWER, 10000) //
						.output(STATE_MACHINE, State.IDLE)) //
				.next(new TestCase() //
						.input("ess0", SOC, 22) //
						.input("ess0", MAX_APPARENT_POWER, 10000) //
						.output(STATE_MACHINE, State.IDLE)) //
				.next(new TestCase() //
						.input("ess0", SOC, 21) //
						.output(STATE_MACHINE, State.NOT_STARTED)) //
				.next(new TestCase() //
						.input("ess0", SOC, 20) //
						.output(STATE_MACHINE, State.BELOW_TARGET_SOC)) //
				.next(new TestCase() //
						.input("ess0", SOC, 25) //
						.input(CTRL_ESS_CAPACITY, 8_000) //
						.input("ess0", CAPACITY, 8_000) //
						.output(CTRL_ESS_CAPACITY, 8_000) //
						.output(STATE_MACHINE, State.BELOW_TARGET_SOC)) //
				.next(new TestCase() //
						.input("ess0", CAPACITY, 8_000) //
						.input("ess0", SOC, 30) //
						.output(CTRL_ESS_CAPACITY, 8_000)) //
				.next(new TestCase() //
						.input("ess0", SOC, 30) ///
						.input("ess0", CAPACITY, 8_000) //
						.output(CTRL_ESS_CAPACITY, 8_000) //
						.output(STATE_MACHINE, State.AT_TARGET_SOC)) //

				.next(new TestCase("EMS restart") //
						.input("ess0", CAPACITY, null) //
						.input(CTRL_ESS_CAPACITY, null) //
						.output(CTRL_ESS_CAPACITY, null)) //

				.next(new TestCase("New Ess.Capacity (Ctrl is taking the last one from timedata)") //
						.input("ess0", CAPACITY, 10_000) //
						.input(CTRL_ESS_CAPACITY, null) //
						.output(CTRL_ESS_CAPACITY, 8_000)) //

				.next(new TestCase() //
						.output(STATE_MACHINE, State.IDLE)) //

				.deactivate();
	}

	@Test
	public void testAboveLimit() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2022-01-01T08:00:00.00Z"), ZoneOffset.UTC);
		new ControllerTest(new ControllerEssFixStateOfChargeImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("sum", new DummySum()) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.addReference("ess", ESS) //
				.activate(FixStateOfChargeConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setRunning(true) //
						.setTargetSoc(30) //
						.setSpecifyTargetTime(false) //
						.setTargetTime(DEFAULT_TARGET_TIME) //
						.setSelfTermination(false) //
						.setTerminationBuffer(720) //
						.setConditionalTermination(false) //
						.setEndCondition(CAPACITY_CHANGED) //
						.build())
				.next(new TestCase() //
						.input("ess0", SOC, 50) //
						.input("ess0", MAX_APPARENT_POWER, 10_000) //
						.output(STATE_MACHINE, State.IDLE)) //
				.next(new TestCase() //
						.input("ess0", SOC, 50) //
						.input("ess0", MAX_APPARENT_POWER, 10_000) //
						.output(STATE_MACHINE, State.IDLE)) //
				.next(new TestCase() //
						.input("ess0", SOC, 50) //
						.input("ess0", MAX_APPARENT_POWER, 10_000) //
						.output(STATE_MACHINE, State.NOT_STARTED)) //
				.next(new TestCase() //
						.input("ess0", SOC, 50) //
						.input("ess0", MAX_APPARENT_POWER, 10_000) //
						.output(STATE_MACHINE, State.ABOVE_TARGET_SOC) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 500) //
						.output(DEBUG_SET_ACTIVE_POWER_RAW, 500) //
						.output(DEBUG_SET_ACTIVE_POWER, 500)) // Would increase till 10_000
				.deactivate();
	}

	@Test
	public void testBelowLimit() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2022-01-01T08:00:00.00Z"), ZoneOffset.UTC);
		new ControllerTest(new ControllerEssFixStateOfChargeImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("sum", new DummySum()) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.addReference("ess", ESS) //
				.activate(FixStateOfChargeConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setRunning(true) //
						.setTargetSoc(30) //
						.setSpecifyTargetTime(false) //
						.setTargetTime(DEFAULT_TARGET_TIME) //
						.setSelfTermination(false) //
						.setTerminationBuffer(720) //
						.setConditionalTermination(false) //
						.setEndCondition(CAPACITY_CHANGED) //
						.build())
				.next(new TestCase() //
						.input("ess0", SOC, 10) //
						.input("ess0", MAX_APPARENT_POWER, 10_000) //
						.output(STATE_MACHINE, State.IDLE)) //
				.next(new TestCase() //
						.input("ess0", SOC, 10) //
						.input("ess0", MAX_APPARENT_POWER, 10_000) //
						.output(STATE_MACHINE, State.IDLE)) //
				.next(new TestCase() //
						.input("ess0", SOC, 10) //
						.input("ess0", MAX_APPARENT_POWER, 10_000) //
						.output(STATE_MACHINE, State.NOT_STARTED)) //
				.next(new TestCase() //
						.input("ess0", SOC, 10) //
						.input("ess0", MAX_APPARENT_POWER, 10_000) //
						.output(STATE_MACHINE, State.BELOW_TARGET_SOC) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -500) //
						.output(DEBUG_SET_ACTIVE_POWER_RAW, -500) //
						.output(DEBUG_SET_ACTIVE_POWER, -500)) // Would increase till 10_000
				.deactivate();
	}

	@Test
	public void testAtLimit() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2022-10-27T08:00:00.00Z"), ZoneOffset.UTC);
		new ControllerTest(new ControllerEssFixStateOfChargeImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("sum", new DummySum()) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.addReference("ess", ESS) //
				.activate(FixStateOfChargeConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setRunning(true) //
						.setTargetSoc(30) //
						.setSpecifyTargetTime(false) //
						.setTargetTime(DEFAULT_TARGET_TIME) //
						.setSelfTermination(false) //
						.setTerminationBuffer(720) //
						.setConditionalTermination(false) //
						.setEndCondition(CAPACITY_CHANGED) //
						.build())
				.next(new TestCase() //
						.input("ess0", SOC, 10) //
						.input("ess0", MAX_APPARENT_POWER, 10_000) //
						.output(STATE_MACHINE, State.IDLE)) //
				.next(new TestCase() //
						.input("ess0", SOC, 10) //
						.input("ess0", MAX_APPARENT_POWER, 10_000) //
						.output(STATE_MACHINE, State.IDLE)) //
				.next(new TestCase() //
						.input("ess0", SOC, 10) //
						.input("ess0", MAX_APPARENT_POWER, 10_000) //
						.output(STATE_MACHINE, State.NOT_STARTED)) //
				.next(new TestCase() //
						.input("ess0", SOC, 10) //
						.input("ess0", MAX_APPARENT_POWER, 10_000) //
						.output(STATE_MACHINE, State.BELOW_TARGET_SOC) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -500) //
						.output(DEBUG_SET_ACTIVE_POWER_RAW, -500) //
						.output(DEBUG_SET_ACTIVE_POWER, -500)) //
				.next(new TestCase() //
						.input("ess0", SOC, 10) //
						.input("ess0", MAX_APPARENT_POWER, 10_000) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -1000)) //
				.next(new TestCase() //
						.input("ess0", SOC, 10) //
						.input("ess0", MAX_APPARENT_POWER, 10_000) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -1500)) //
				.next(new TestCase() //
						.input("ess0", SOC, 10) //
						.input("ess0", MAX_APPARENT_POWER, 10_000) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -2000)) //
				.next(new TestCase() //
						.input("ess0", SOC, 10) //
						.input("ess0", MAX_APPARENT_POWER, 10_000) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -2500)) //
				.next(new TestCase() //
						.input("ess0", SOC, 10) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -3000)) //
				.next(new TestCase() //
						.input("ess0", SOC, 10) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -3500)) //
				.next(new TestCase() //
						.input("ess0", SOC, 10) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -4000)) //
				.next(new TestCase()) //
				.next(new TestCase() //
						.input("ess0", SOC, 10) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -5000)) //
				.next(new TestCase()) //
				.next(new TestCase() //
						.input("ess0", SOC, 10) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -6000)) //
				.next(new TestCase()) //
				.next(new TestCase() //
						.input("ess0", SOC, 10) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -7000)) //
				.next(new TestCase()) //
				.next(new TestCase() //
						.input("ess0", SOC, 10) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -8000)) //
				.next(new TestCase()) //
				.next(new TestCase() //
						.input("ess0", SOC, 10) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -9000)) //
				.next(new TestCase()) //
				.next(new TestCase() //
						.input("ess0", SOC, 10) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -10000)) //
				.next(new TestCase() //
						.input("ess0", SOC, 30) //
						.input("ess0", MAX_APPARENT_POWER, 10_000)) //
				.next(new TestCase() //
						.input("ess0", SOC, 30) //
						.output(STATE_MACHINE, State.AT_TARGET_SOC) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -9000)) //
				.next(new TestCase() //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -8000)) //
				.next(new TestCase()) //
				.next(new TestCase()) //
				.next(new TestCase()) //
				.next(new TestCase()) //
				.next(new TestCase()) //
				.next(new TestCase()) //
				.next(new TestCase() //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -1000)) //
				.next(new TestCase() //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 0)) //
				.deactivate();
	}

	@Test
	public void testAtLimitDeadBand() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2022-10-27T08:00:00.00Z"), ZoneOffset.UTC);
		new ControllerTest(new ControllerEssFixStateOfChargeImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("sum", new DummySum()) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.addReference("ess", ESS) //
				.activate(FixStateOfChargeConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setRunning(true) //
						.setTargetSoc(30) //
						.setSpecifyTargetTime(false) //
						.setTargetTime(DEFAULT_TARGET_TIME) //
						.setSelfTermination(false) //
						.setTerminationBuffer(720) //
						.setConditionalTermination(false) //
						.setEndCondition(CAPACITY_CHANGED) //
						.build())
				.next(new TestCase() //
						.input("ess0", SOC, 30) //
						.input("ess0", MAX_APPARENT_POWER, 10_000) //
						.input("ess0", CAPACITY, 10_000) //
						.output(STATE_MACHINE, State.IDLE)) //
				.next(new TestCase() //
						.input("ess0", SOC, 30) //
						.input("ess0", MAX_APPARENT_POWER, 10_000) //
						.output(STATE_MACHINE, State.IDLE)) //
				.next(new TestCase() //
						.input("ess0", SOC, 30) //
						.input("ess0", MAX_APPARENT_POWER, 10_000) //
						.output(STATE_MACHINE, State.NOT_STARTED)) //
				.next(new TestCase() //
						.input("ess0", SOC, 30) //
						.input("ess0", MAX_APPARENT_POWER, 10_000) //
						.output(STATE_MACHINE, State.AT_TARGET_SOC) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 0) //
						.output(DEBUG_SET_ACTIVE_POWER_RAW, 0) //
						.output(DEBUG_SET_ACTIVE_POWER, 0)) //
				.next(new TestCase() //
						.input("ess0", SOC, 30) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 0)) //
				.next(new TestCase() //
						.input("ess0", SOC, 31) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 0)) //
				.next(new TestCase() //
						.input("ess0", SOC, 29) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 0)) //
				.next(new TestCase() //
						.input("ess0", SOC, 28)) //
				.next(new TestCase() //
						.input("ess0", SOC, 28) //
						.output(STATE_MACHINE, State.WITHIN_LOWER_TARGET_SOC_BOUNDARIES) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -500)) //
				.next(new TestCase() //
						.input("ess0", SOC, 28) //
						.output(STATE_MACHINE, State.WITHIN_LOWER_TARGET_SOC_BOUNDARIES) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -1000)) //
				.next(new TestCase() //
						.input("ess0", SOC, 28) //
						.output(STATE_MACHINE, State.WITHIN_LOWER_TARGET_SOC_BOUNDARIES) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -1500)) //
				.next(new TestCase() //
						.input("ess0", SOC, 28) //
						.output(STATE_MACHINE, State.WITHIN_LOWER_TARGET_SOC_BOUNDARIES) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -1 * round(//
								min(10_000/* maxApparentPower */ * DEFAULT_POWER_FACTOR,
										10_000 /* capacity */ * (1f / 6f))) // 1467
						)) //
				.deactivate();
	}

	@Test
	public void testBoundaries() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2022-10-27T08:00:00.00Z"), ZoneOffset.UTC);
		/*
		 * Below target SoC
		 */
		new ControllerTest(new ControllerEssFixStateOfChargeImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("sum", new DummySum()) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.addReference("ess", ESS) //
				.activate(FixStateOfChargeConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setRunning(true) //
						.setTargetSoc(30) //
						.setSpecifyTargetTime(false) //
						.setTargetTime(DEFAULT_TARGET_TIME) //
						.setSelfTermination(false) //
						.setTerminationBuffer(720) //
						.setConditionalTermination(false) //
						.setEndCondition(CAPACITY_CHANGED) //
						.build())
				.next(new TestCase() //
						.input("ess0", SOC, 20) //
						.input("ess0", MAX_APPARENT_POWER, 10_000) //
						.output(STATE_MACHINE, State.IDLE)) //
				.next(new TestCase() //
						.input("ess0", SOC, 20) //
						.input("ess0", MAX_APPARENT_POWER, 10_000) //
						.output(STATE_MACHINE, State.IDLE)) //
				.next(new TestCase() //
						.input("ess0", SOC, 20) //
						.input("ess0", MAX_APPARENT_POWER, 10_000) //
						.output(STATE_MACHINE, State.NOT_STARTED)) //
				.next(new TestCase() //
						.input("ess0", SOC, 20) //
						.input("ess0", MAX_APPARENT_POWER, 10_000) //
						.input("ess0", CAPACITY, 10_000) //
						.output(STATE_MACHINE, State.BELOW_TARGET_SOC) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -500) //
						.output(DEBUG_SET_ACTIVE_POWER_RAW, -500) //
						.output(DEBUG_SET_ACTIVE_POWER, -500)) //
				.next(new TestCase() //
						.input("ess0", SOC, 22) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -1000)) //
				// Skip Ramp
				.next(new TestCase(), 17) //
				.next(new TestCase() //
						.input("ess0", SOC, 27) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -10000)) //
				.next(new TestCase() //
						.input("ess0", SOC, 28)) //
				.next(new TestCase() //
						.input("ess0", SOC, 28) //
						.output(STATE_MACHINE, State.WITHIN_LOWER_TARGET_SOC_BOUNDARIES) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -9500)) //
				.next(new TestCase() //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -9000)) //
				// Skip ramp
				.next(new TestCase(), 13) //
				.next(new TestCase() //
						.input("ess0", SOC, 29) //
						.output(STATE_MACHINE, State.WITHIN_LOWER_TARGET_SOC_BOUNDARIES) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -2000)) //
				.next(new TestCase() //
						.input("ess0", SOC, 29) //
						.output(STATE_MACHINE, State.WITHIN_LOWER_TARGET_SOC_BOUNDARIES) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -1 * round(//
								min(10_000/* maxApparentPower */ * DEFAULT_POWER_FACTOR,
										10_000 /* capacity */ * (1f / 6f))) //
						))// 1667
				.next(new TestCase() //
						.input("ess0", SOC, 30)) //
				.next(new TestCase() //
						.input("ess0", SOC, 30) //
						.output(STATE_MACHINE, State.AT_TARGET_SOC) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -667)) //
				.next(new TestCase() //
						.input("ess0", SOC, 30) //
						.output(STATE_MACHINE, State.AT_TARGET_SOC) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 0)) //
		;

		/*
		 * Above target SoC
		 */
		new ControllerTest(new ControllerEssFixStateOfChargeImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("sum", new DummySum()) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.addReference("ess", ESS) //
				.activate(FixStateOfChargeConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setRunning(true) //
						.setTargetSoc(30) //
						.setSpecifyTargetTime(false) //
						.setTargetTime(DEFAULT_TARGET_TIME) //
						.setSelfTermination(false) //
						.setTerminationBuffer(720) //
						.setConditionalTermination(false) //
						.setEndCondition(CAPACITY_CHANGED) //
						.build())
				.next(new TestCase() //
						.input("ess0", SOC, 40) //
						.input("ess0", MAX_APPARENT_POWER, 10_000) //
						.output(STATE_MACHINE, State.IDLE)) //
				.next(new TestCase() //
						.input("ess0", SOC, 40) //
						.input("ess0", MAX_APPARENT_POWER, 10_000) //
						.output(STATE_MACHINE, State.IDLE)) //
				.next(new TestCase() //
						.input("ess0", SOC, 40) //
						.input("ess0", MAX_APPARENT_POWER, 10_000) //
						.output(STATE_MACHINE, State.NOT_STARTED)) //
				.next(new TestCase() //
						.input("ess0", SOC, 40) //
						.input("ess0", MAX_APPARENT_POWER, 10_000) //
						.output(STATE_MACHINE, State.ABOVE_TARGET_SOC) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 500) //
						.output(DEBUG_SET_ACTIVE_POWER_RAW, 500) //
						.output(DEBUG_SET_ACTIVE_POWER, 500)) //
				.next(new TestCase() //
						.input("ess0", SOC, 40) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 1000)) //
				// Skip ramp
				.next(new TestCase(), 18) //
				.next(new TestCase() //
						.input("ess0", SOC, 33)) //
				.next(new TestCase() //
						.input("ess0", SOC, 32)) //
				.next(new TestCase() //
						.input("ess0", SOC, 32) //
						.output(STATE_MACHINE, State.WITHIN_UPPER_TARGET_SOC_BOUNDARIES) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 9500)) //
				.next(new TestCase() //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 9000)) //
				// Skip ramp
				.next(new TestCase(), 13) //
				.next(new TestCase() //
						.input("ess0", SOC, 31) //
						.output(STATE_MACHINE, State.WITHIN_UPPER_TARGET_SOC_BOUNDARIES) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 2000)) //
				.next(new TestCase() //
						.input("ess0", SOC, 31) //
						.output(STATE_MACHINE, State.WITHIN_UPPER_TARGET_SOC_BOUNDARIES) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, round(//
								min(10_000/* maxApparentPower */ * DEFAULT_POWER_FACTOR,
										10_000 /* capacity */ * (1f / 6f))) //
						))// 1667
				.next(new TestCase() //
						.input("ess0", SOC, 30)) //
				.next(new TestCase() //
						.input("ess0", SOC, 30) //
						.output(STATE_MACHINE, State.AT_TARGET_SOC) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 667)) //
				.next(new TestCase() //
						.input("ess0", SOC, 30) //
						.output(STATE_MACHINE, State.AT_TARGET_SOC) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 0)) //
				.deactivate();
	}

	@Test
	public void testLimitWithSpecifiedTimeBelowLimit() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2022-10-27T08:00:00.00Z"), ZoneOffset.ofHours(1));
		new TimeLeapClock(Instant.parse("2022-10-27T09:00:00.00Z"), ZoneOffset.ofHours(1));
		final var componentManager = new DummyComponentManager(clock);

		new ControllerTest(new ControllerEssFixStateOfChargeImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", componentManager) //
				.addReference("sum", new DummySum()) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.addReference("ess", ESS) //
				.activate(FixStateOfChargeConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setRunning(true) //
						.setTargetSoc(30) //
						.setSpecifyTargetTime(true) //
						.setTargetTime(DEFAULT_TARGET_TIME) //
						.setSelfTermination(false) //
						.setTerminationBuffer(720) //
						.setConditionalTermination(false) //
						.setEndCondition(CAPACITY_CHANGED) //
						.build())
				.next(new TestCase() //
						.input("ess0", SOC, 10) //
						.input("ess0", MAX_APPARENT_POWER, 10_000) //
						.output(STATE_MACHINE, State.IDLE)) //
				.next(new TestCase() //
						.input("ess0", SOC, 10) //
						.input("ess0", MAX_APPARENT_POWER, 10_000) //
						.output(STATE_MACHINE, State.IDLE)) //
				.next(new TestCase() //
						.input("ess0", SOC, 10) //
						.input("ess0", CAPACITY, 30_000) //
						.input("ess0", MAX_APPARENT_POWER, 10_000) //
						.output(STATE_MACHINE, State.NOT_STARTED)) //

				// Start time = 2022-10-27T09:14:24+01:00, Current: 2022-10-27T09:00:00+01:00
				.next(new TestCase() //
						.input("ess0", SOC, 10) //
						.input("ess0", MAX_APPARENT_POWER, 10_000) //
						.input("ess0", CAPACITY, 30_000) //
						.output(STATE_MACHINE, State.NOT_STARTED) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, null) //
						.output(DEBUG_SET_ACTIVE_POWER_RAW, null) //
						.output(DEBUG_SET_ACTIVE_POWER, null)) //
				.next(new TestCase() //
						.timeleap(clock, 15, MINUTES))//
				.next(new TestCase() //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -500)) //
				.next(new TestCase()) //
				.next(new TestCase()) //
				.next(new TestCase()) //
				.next(new TestCase()) //
				.next(new TestCase()) //
				.next(new TestCase()) //
				.next(new TestCase()) //
				.next(new TestCase()) //
				.next(new TestCase() //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -5000)) //
				.next(new TestCase() //
						.input("ess0", SOC, 10) //
						.input("ess0", MAX_APPARENT_POWER, 10_000) //
						.input("ess0", CAPACITY, 30_000) //
						.output(STATE_MACHINE, State.BELOW_TARGET_SOC) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -5040) //
						.output(DEBUG_SET_ACTIVE_POWER_RAW, -5040) //
						.output(DEBUG_SET_ACTIVE_POWER, -5040)) //
				.next(new TestCase() //
						.input("ess0", SOC, 30) //
						.input("ess0", MAX_APPARENT_POWER, 10_000)) //
				.next(new TestCase() //
						.input("ess0", SOC, 30) //
						.output(STATE_MACHINE, State.AT_TARGET_SOC) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -4040)) //
				.next(new TestCase() //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -3040)) //
				.next(new TestCase() //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -2040)) //
				.next(new TestCase() //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -1040)) //
				.next(new TestCase() //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -40)) //
				.next(new TestCase() //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 0)) //
				.deactivate();
	}

	@Test
	public void testLimitWithSpecifiedTimeAboveLimit() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2022-10-26T22:00:00.00Z"), ZoneOffset.ofHours(1));
		final var componentManager = new DummyComponentManager(clock);

		new ControllerTest(new ControllerEssFixStateOfChargeImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", componentManager) //
				.addReference("sum", new DummySum()) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.addReference("ess", ESS) //
				.activate(FixStateOfChargeConfig.create() //
						.setId("ctrl0") //
						.setEssId("ess0") //
						.setRunning(true) //
						.setTargetSoc(30) //
						.setSpecifyTargetTime(true) //
						.setTargetTime(DEFAULT_TARGET_TIME) //
						.setTargetTimeBuffer(60) //
						.setSelfTermination(false) //
						.setTerminationBuffer(720) //
						.setConditionalTermination(false) //
						.setEndCondition(CAPACITY_CHANGED) //
						.build())
				.next(new TestCase() //
						.input("ess0", SOC, 80) //
						.input("ess0", CAPACITY, 30_000) //
						.input("ess0", MAX_APPARENT_POWER, 10_000) //
						.output(STATE_MACHINE, State.IDLE)) //
				.next(new TestCase() //
						.input("ess0", SOC, 80) //
						.input("ess0", CAPACITY, 30_000) //
						.input("ess0", MAX_APPARENT_POWER, 10_000) //
						.output(STATE_MACHINE, State.IDLE)) //
				.next(new TestCase() //
						.input("ess0", SOC, 80) //
						.input("ess0", CAPACITY, 30_000) //
						.input("ess0", MAX_APPARENT_POWER, 10_000) //
						.output(STATE_MACHINE, State.NOT_STARTED)) //

				// Start time = 2022-10-27T06:26:24, Current: 2022-10-26T23:00
				.next(new TestCase() //
						.input("ess0", SOC, 80) //
						.input("ess0", CAPACITY, 30_000) //
						.input("ess0", MAX_APPARENT_POWER, 10_000) //
						.output(STATE_MACHINE, State.NOT_STARTED) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, null) //
						.output(DEBUG_SET_ACTIVE_POWER_RAW, null) //
						.output(DEBUG_SET_ACTIVE_POWER, null)) //
				.next(new TestCase() //
						.timeleap(clock, 7, HOURS)) //
				.next(new TestCase() //
						.timeleap(clock, 31, MINUTES)) //
				.next(new TestCase() //
						.input("ess0", SOC, 80) //
						.input("ess0", CAPACITY, 30_000) //
						.input("ess0", MAX_APPARENT_POWER, 10_000) //
						.output(STATE_MACHINE, State.ABOVE_TARGET_SOC) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 500) //
						.output(DEBUG_SET_ACTIVE_POWER_RAW, 500) //
						.output(DEBUG_SET_ACTIVE_POWER, 500)) //
				.next(new TestCase() //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 1000)) //
				.next(new TestCase()) //
				.next(new TestCase() //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 2000)) //
				.next(new TestCase()) //
				.next(new TestCase() //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 3000)) //
				.next(new TestCase()) //
				.next(new TestCase() //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 4000)) //
				.next(new TestCase()) //
				.next(new TestCase() //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 5000)) //
				.next(new TestCase()) //
				.next(new TestCase() //
						.input("ess0", SOC, 80) //
						.input("ess0", MAX_APPARENT_POWER, 10_000) //
						.input("ess0", CAPACITY, 30_000) //
						.output(STATE_MACHINE, State.ABOVE_TARGET_SOC) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 5128) //
						.output(DEBUG_SET_ACTIVE_POWER_RAW, 5128) //
						.output(DEBUG_SET_ACTIVE_POWER, 5128)) //
				.next(new TestCase() //
						.input("ess0", SOC, 30) //
						.input("ess0", MAX_APPARENT_POWER, 10_000)) //
				.next(new TestCase() //
						.input("ess0", SOC, 30) //
						.output(STATE_MACHINE, State.AT_TARGET_SOC) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 4128)) //
				.next(new TestCase() //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 3128)) //
				.next(new TestCase() //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 2128)) //
				.next(new TestCase() //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 1128)) //
				.next(new TestCase() //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 128)) //
				.next(new TestCase() //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 0)) //
				.deactivate();
	}
}
