package io.openems.edge.controller.io.heatingelement;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.common.sum.Sum.ChannelId.GRID_ACTIVE_POWER;
import static io.openems.edge.controller.io.heatingelement.ControllerIoHeatingElement.ChannelId.FORCE_START_AT_SECONDS_OF_DAY;
import static io.openems.edge.controller.io.heatingelement.ControllerIoHeatingElement.ChannelId.LEVEL;
import static io.openems.edge.controller.io.heatingelement.ControllerIoHeatingElement.ChannelId.LEVEL6_TIME;
import static io.openems.edge.controller.io.heatingelement.ControllerIoHeatingElement.ChannelId.STATUS;
import static io.openems.edge.controller.io.heatingelement.ControllerIoHeatingElement.ChannelId.TOTAL_PHASE_TIME;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_POWER;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.SECONDS;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.io.heatingelement.enums.Distribution;
import io.openems.edge.controller.io.heatingelement.enums.Level;
import io.openems.edge.controller.io.heatingelement.enums.Mode;
import io.openems.edge.controller.io.heatingelement.enums.NumberOfHeatingElements;
import io.openems.edge.controller.io.heatingelement.enums.Status;
import io.openems.edge.controller.io.heatingelement.enums.WorkMode;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.io.test.DummyInputOutput;
import io.openems.edge.meter.test.DummyElectricityMeter;

class HeatingElementRegressionTest {

	private final TimeLeapClock clock = createDummyClock();
	private final ControllerIoHeatingElementImpl sut = new ControllerIoHeatingElementImpl();

	private static MyConfig.Builder config(Distribution distribution) {
		return MyConfig.create() //
				.setId("ctrl0") //
				.setOutputChannelPhaseL1("io0/InputOutput0") //
				.setOutputChannelPhaseL2("io0/InputOutput1") //
				.setOutputChannelPhaseL3("io0/InputOutput2") //
				.setOutputChannelPhaseL1HeatingElementB("io0/InputOutput3") //
				.setOutputChannelPhaseL2HeatingElementB("io0/InputOutput4") //
				.setOutputChannelPhaseL3HeatingElementB("io0/InputOutput5") //
				.setNumberOfHeatingElements(NumberOfHeatingElements.TWO) //
				.setDistribution(distribution) //
				.setPowerOfPhase(2000) //
				.setPowerPerPhaseHeatingElementB(1000) //
				.setMode(Mode.AUTOMATIC) //
				.setWorkMode(WorkMode.NONE) //
				.setDefaultLevel(Level.LEVEL_6) //
				.setMinTime(1) //
				.setEndTime("14:00") //
				.setEndTimeWithMeter("14:00") //
				.setMinEnergylimit(8000) //
				.setMinimumSwitchingTime(60) //
				.setMeterid("") //
				.setScheduler("");
	}

	private ControllerTest prepare(MyConfig config, boolean withMeter) throws Exception {
		var test = new ControllerTest(this.sut) //
				.addReference("componentManager", new DummyComponentManager(this.clock)) //
				.addReference("sum", new DummySum()) //
				.addComponent(new DummyInputOutput("io0"));
		if (withMeter) {
			test.addReference("meter", new DummyElectricityMeter("meter0"));
		}
		return test.activate(config);
	}

	@ParameterizedTest
	@EnumSource(Distribution.class)
	void removedRelaysAreSwitchedOffEvenWithPendingOn(Distribution distribution) throws Exception {
		var test = this.prepare(config(distribution).setMode(Mode.MANUAL_OFF).build(), false);
		test.next(new TestCase() //
				.input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT0, false) //
				.input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT1, false) //
				.input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT2, false) //
				.input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT3, false) //
				.input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT4, false) //
				.input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT5, false) //
				.onBeforeControllersCallbacks(() -> {
					this.sut.applyLevel(Level.LEVEL_6);
					test.modified(config(distribution) //
							.setNumberOfHeatingElements(NumberOfHeatingElements.ONE) //
							.setMode(Mode.MANUAL_OFF).build());
				}) //
				.output(LEVEL, Level.LEVEL_0) //
				.output("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT0, false) //
				.output("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT1, false) //
				.output("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT2, false) //
				.output("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT3, false) //
				.output("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT4, false) //
				.output("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT5, false));
	}

	@ParameterizedTest
	@CsvSource({ //
			"SEQUENTIAL,missing/InputOutput0", //
			"SEQUENTIAL,io0/MissingOutput", //
			"PARALLEL,missing/InputOutput0", //
			"PARALLEL,io0/MissingOutput" })
	void missingRemovedRelayDoesNotPreventOtherRelaysFromSwitchingOffOrConfigUpdate(Distribution distribution,
			String missingOutput) throws Exception {
		var test = this.prepare(config(distribution) //
				.setOutputChannelPhaseL1HeatingElementB(missingOutput) //
				.setMode(Mode.MANUAL_OFF).build(), false);
		test.next(new TestCase() //
				.input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT4, true) //
				.input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT5, true) //
				.onBeforeControllersCallbacks(() -> test.modified(config(distribution) //
						.setNumberOfHeatingElements(NumberOfHeatingElements.ONE) //
						.setMode(Mode.FAST_HEAT).build())) //
				.output(LEVEL, Level.LEVEL_3) //
				.output("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT0, true) //
				.output("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT1, true) //
				.output("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT2, true) //
				.output("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT4, false) //
				.output("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT5, false));
	}

	@ParameterizedTest
	@EnumSource(Distribution.class)
	void liveTwoToOneSwitchesOffLatchedRelays(Distribution distribution) throws Exception {
		var test = this.prepare(config(distribution).setMode(Mode.FAST_HEAT).build(), false);
		test.next(new TestCase().output(LEVEL, Level.LEVEL_6)) //
				.next(new TestCase() //
						.input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT3, true) //
						.input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT4, true) //
						.input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT5, true) //
						.onBeforeControllersCallbacks(() -> test.modified(config(distribution) //
								.setNumberOfHeatingElements(NumberOfHeatingElements.ONE) //
								.setMode(Mode.MANUAL_OFF).build())) //
						.output(LEVEL, Level.LEVEL_0) //
						.output("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT3, false) //
						.output("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT4, false) //
						.output("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT5, false));
	}

	@ParameterizedTest
	@EnumSource(Distribution.class)
	void fastHeatTwoToOneKeepsHeatingElementAAtLevelThree(Distribution distribution) throws Exception {
		var test = this.prepare(config(distribution).setMode(Mode.FAST_HEAT).build(), false);
		test//
				.next(new TestCase() //
						.output(LEVEL, Level.LEVEL_6) //
						.output("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT0, true) //
						.output("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT1, true) //
						.output("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT2, true) //
						.output("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT3, true) //
						.output("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT4, true) //
						.output("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT5, true)) //
				.next(new TestCase() //
						.input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT0, true) //
						.input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT1, true) //
						.input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT2, true) //
						.input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT3, true) //
						.input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT4, true) //
						.input("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT5, true) //
						.onBeforeControllersCallbacks(() -> test.modified(config(distribution) //
								.setNumberOfHeatingElements(NumberOfHeatingElements.ONE) //
								.setMode(Mode.FAST_HEAT).build())) //
						.output(LEVEL, Level.LEVEL_3) //
						.output("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT0, true) //
						.output("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT1, true) //
						.output("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT2, true) //
						.output("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT3, false) //
						.output("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT4, false) //
						.output("io0", DummyInputOutput.ChannelId.INPUT_OUTPUT5, false));
	}

	@ParameterizedTest
	@EnumSource(NumberOfHeatingElements.class)
	void fastHeatBypassesDelayWhileManualOnKeepsDefaultLevel(NumberOfHeatingElements number) throws Exception {
		this.prepare(config(Distribution.SEQUENTIAL).setNumberOfHeatingElements(number) //
				.setMode(Mode.MANUAL_ON).setDefaultLevel(Level.LEVEL_2).build(), false) //
				.next(new TestCase().output(LEVEL, Level.LEVEL_2)) //
				.modified(config(Distribution.SEQUENTIAL).setNumberOfHeatingElements(number) //
						.setMode(Mode.FAST_HEAT).setDefaultLevel(Level.LEVEL_1).build()) //
				.next(new TestCase().output(LEVEL,
						number == NumberOfHeatingElements.ONE ? Level.LEVEL_3 : Level.LEVEL_6)) //
				.modified(config(Distribution.SEQUENTIAL).setNumberOfHeatingElements(number) //
						.setMode(Mode.MANUAL_OFF).build()) //
				.next(new TestCase().output(LEVEL, Level.LEVEL_0));
	}

	@ParameterizedTest
	@EnumSource(Distribution.class)
	void timeGuaranteeCreditsEarlierSurplusHeating(Distribution distribution) throws Exception {
		var test = this.prepare(config(distribution).setWorkMode(WorkMode.TIME) //
				.setMinimumSwitchingTime(0).build(), false);
		test.next(new TestCase().timeleap(this.clock, 10, HOURS).input(GRID_ACTIVE_POWER, -9000));
		for (var level = 2; level <= 6; level++) {
			test.next(new TestCase().timeleap(this.clock, 1, SECONDS).output(LEVEL, Level.fromValue(level)));
		}
		test.next(new TestCase().timeleap(this.clock, 1, HOURS) //
				.input(GRID_ACTIVE_POWER, 9000) //
				.output(LEVEL, Level.LEVEL_0) //
				.output(TOTAL_PHASE_TIME, 21615) //
				.output(FORCE_START_AT_SECONDS_OF_DAY, null)) //
				.next(new TestCase().timeleap(this.clock, 9000, SECONDS) //
						.input(GRID_ACTIVE_POWER, 0) //
						.output(LEVEL, Level.LEVEL_0) //
						.output(FORCE_START_AT_SECONDS_OF_DAY, null));
	}

	@ParameterizedTest
	@CsvSource({ //
			"SEQUENTIAL,MANUAL_ON,3600,47400", //
			"PARALLEL,MANUAL_ON,3600,47400", //
			"SEQUENTIAL,MANUAL_OFF,0,46800", //
			"PARALLEL,MANUAL_OFF,0,46800" })
	void timeGuaranteeUsesCurrentDayBeforeSwitching(Distribution distribution, Mode previousMode, int totalPhaseTime,
			int forceStartAtSecondsOfDay) throws Exception {
		// Today's target is six step-hours by 14:00. At 00:10, previously active
		// steps contribute 6 * 600 = 3600 s (force start 13:10 = 47400 s of day).
		// Previously inactive steps contribute 0 s (force start 13:00 = 46800 s of
		// day).
		this.prepare(config(distribution).setMode(Mode.MANUAL_ON).build(), false) //
				.next(new TestCase("Day 1, 10:00: switch on all six steps")//
						.timeleap(this.clock, 10, HOURS)//
						.output(LEVEL, Level.LEVEL_6)) //
				.next(new TestCase("Day 1, 11:00: six steps heated for one hour each (6 * 3600 s)")//
						.timeleap(this.clock, 1, HOURS)//
						.output(TOTAL_PHASE_TIME, 21600)) //
				.modified(config(distribution)//
						.setMode(previousMode)//
						.build()) //
				.next(new TestCase("Day 1, 11:00: keep all steps on or switch them off, depending on previousMode")) //
				.modified(config(distribution)//
						.setWorkMode(WorkMode.TIME)//
						.setMinimumSwitchingTime(0)//
						.build()) //
				.next(new TestCase("Day 2, 00:10: use today's duration for TIME before switching off without surplus")//
						// From 11:00 to 00:10 next day: 13 hours + 10 minutes = 47400 s.
						.timeleap(this.clock, 47400, SECONDS) //
						.input(GRID_ACTIVE_POWER, 9000) //
						.output(LEVEL, Level.LEVEL_0) //
						.output(TOTAL_PHASE_TIME, totalPhaseTime) //
						.output(FORCE_START_AT_SECONDS_OF_DAY, forceStartAtSecondsOfDay)) //
				.next(new TestCase("Day 2, 00:11: durations and force start stay unchanged while all steps are off")//
						// Advance time to detect steps incorrectly continuing to accumulate duration.
						.timeleap(this.clock, 60, SECONDS) //
						.output(LEVEL, Level.LEVEL_0) //
						.output(TOTAL_PHASE_TIME, totalPhaseTime) //
						.output(FORCE_START_AT_SECONDS_OF_DAY, forceStartAtSecondsOfDay));
	}

	@ParameterizedTest
	@EnumSource(Distribution.class)
	void dailyCountersCoverEveryLevelAndResetAtMidnight(Distribution distribution) throws Exception {
		var test = this.prepare(config(distribution).setMode(Mode.MANUAL_OFF).build(), false);
		for (var level = 1; level <= 6; level++) {
			test.modified(config(distribution).setMode(Mode.MANUAL_ON) //
					.setDefaultLevel(Level.fromValue(level)).build()) //
					.next(new TestCase()) //
					.next(new TestCase().timeleap(this.clock, 60, SECONDS) //
							.output(ControllerIoHeatingElement.ChannelId.valueOf("LEVEL" + level + "_TIME"), 60));
		}
		test.next(new TestCase().output(TOTAL_PHASE_TIME, 1260)) //
				.next(new TestCase().timeleap(this.clock, 86400 - 360, SECONDS) //
						.output(TOTAL_PHASE_TIME, 0).output(LEVEL6_TIME, 0)) //
				.next(new TestCase().timeleap(this.clock, 60, SECONDS) //
						.output(TOTAL_PHASE_TIME, 360).output(LEVEL6_TIME, 60));
	}

	@ParameterizedTest
	@CsvSource({ "SEQUENTIAL,5000,LEVEL_3,ACTIVE_FORCED_LIMIT", "PARALLEL,5000,LEVEL_4,ACTIVE_FORCED_LIMIT",
			"SEQUENTIAL,5500,LEVEL_4,ACTIVE_FORCED_LIMIT", "PARALLEL,5500,LEVEL_5,ACTIVE_FORCED_LIMIT",
			"SEQUENTIAL,8000,LEVEL_6,ACTIVE_FORCED_LIMIT", "PARALLEL,8000,LEVEL_6,ACTIVE_FORCED_LIMIT",
			"SEQUENTIAL,10000,LEVEL_6,UNREACHABLE", "PARALLEL,10000,LEVEL_6,UNREACHABLE" })
	void energyUsesConfiguredCapacity(Distribution distribution, int energy, Level level, Status status)
			throws Exception {
		this.prepare(config(distribution).setWorkMode(WorkMode.ENERGY).setMinEnergylimit(energy) //
				.setMeterid("meter0").build(), true) //
				.next(new TestCase().timeleap(this.clock, 13, HOURS) //
						.input(GRID_ACTIVE_POWER, 0) //
						.input("meter0", ACTIVE_POWER, 0) //
						.input("meter0", ACTIVE_PRODUCTION_ENERGY, 0L) //
						.output(LEVEL, level).output(STATUS, status));
	}

	@ParameterizedTest
	@EnumSource(Distribution.class)
	void scheduledEnergyUsesTaskTargetAndSixStepCapacity(Distribution distribution) throws Exception {
		this.prepare(config(distribution).setWorkMode(WorkMode.ENERGY).setMinEnergylimit(1000) //
				.setMeterid("meter0").setScheduler("""
						[{
						    "@type": "Task",
						    "start": "13:00:00",
						    "duration": "PT1H",
						    "recurrenceRules": [{"frequency": "daily"}],
						    "openems.io:payload": {"sessionEnergy": 8000}
						}]
						""").build(), true) //
				.next(new TestCase().timeleap(this.clock, 13, HOURS) //
						.input(GRID_ACTIVE_POWER, 0) //
						.input("meter0", ACTIVE_POWER, 0) //
						.input("meter0", ACTIVE_PRODUCTION_ENERGY, 0L) //
						.output(LEVEL, Level.LEVEL_6).output(STATUS, Status.ACTIVE_FORCED_LIMIT)) //
				.next(new TestCase().timeleap(this.clock, 1, HOURS) //
						.output(LEVEL, Level.LEVEL_0).output(STATUS, Status.INACTIVE));
	}
}
