package io.openems.edge.controller.io.heatingelement;

import static io.openems.edge.common.sum.Sum.ChannelId.GRID_ACTIVE_POWER;
import static io.openems.edge.controller.io.heatingelement.ControllerIoHeatingElement.ChannelId.LEVEL;
import static java.time.temporal.ChronoUnit.SECONDS;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.io.heatingelement.enums.Level;
import io.openems.edge.controller.io.heatingelement.enums.Mode;
import io.openems.edge.controller.io.heatingelement.enums.NumberOfHeatingElements;
import io.openems.edge.controller.io.heatingelement.enums.WorkMode;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.io.test.DummyInputOutput;

/**
 * Focused tests for the symmetric SURPLUS switching hysteresis in
 * {@code applyHysteresis()}.
 *
 * <p>
 * Each level change in either direction is limited to one step per
 * {@code minimumSwitchingTime}. OFF and FAST_HEAT continue to bypass
 * hysteresis.
 *
 * <p>
 * The SURPLUS tests use equal 2000 W Steps and no meter, so
 * {@code getRequiredLevelFromPower()} derives the target Level from the nominal
 * Step power.
 */
class ControllerIoHeatingElementHysteresisTest {

	private static final int POWER_PER_PHASE = 2000;
	private static final int MINIMUM_SWITCHING_TIME = 60;
	private static TimeLeapClock clock;

	private static MyConfig.Builder createConfig(Mode mode, NumberOfHeatingElements numberOfHeatingElements,
			Level defaultLevel, int minimumSwitchingTime) {
		return MyConfig.create() //
				.setId("ctrl0") //
				.setOutputChannelPhaseL1("io0/InputOutput0") //
				.setOutputChannelPhaseL2("io0/InputOutput1") //
				.setOutputChannelPhaseL3("io0/InputOutput2") //
				.setPowerOfPhase(POWER_PER_PHASE) //
				.setMode(mode) //
				.setDefaultLevel(defaultLevel) //
				.setEndTime("23:59:00") //
				.setWorkMode(WorkMode.NONE) //
				.setMinTime(1) //
				.setMinimumSwitchingTime(minimumSwitchingTime) //
				.setMinEnergylimit(5000) //
				.setEndTimeWithMeter("00:00") //
				.setMeterid("") //
				.setScheduler("") //
				.setNumberOfHeatingElements(numberOfHeatingElements) //
				.setPowerPerPhaseHeatingElementB(POWER_PER_PHASE) //
				.setOutputChannelPhaseL1HeatingElementB("io0/InputOutput3") //
				.setOutputChannelPhaseL2HeatingElementB("io0/InputOutput4") //
				.setOutputChannelPhaseL3HeatingElementB("io0/InputOutput5");
	}

	private static ControllerTest prepareTest(TimeLeapClock clock, NumberOfHeatingElements numberOfHeatingElements,
			Mode mode, Level defaultLevel, int minimumSwitchingTime) throws Exception {
		return new ControllerTest(new ControllerIoHeatingElementImpl()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("sum", new DummySum()) //
				.addComponent(new DummyInputOutput("io0")) //
				.activate(createConfig(mode, numberOfHeatingElements, defaultLevel, minimumSwitchingTime).build());
	}

	private static TimeLeapClock newClock() {
		return new TimeLeapClock(Instant.ofEpochSecond(1577836800) /* 1. January 2020 00:00:00 */, ZoneOffset.UTC);
	}

	private static Stream<Arguments> heatingElementCounts() {
		return Stream.of(//
				Arguments.of(NumberOfHeatingElements.ONE, 3), //
				Arguments.of(NumberOfHeatingElements.TWO, 6));
	}

	@BeforeEach
	void setup() {
		clock = newClock();
	}

	@ParameterizedTest
	@MethodSource("heatingElementCounts")
	void surplusMovesOneStepPerIntervalInBothDirections(NumberOfHeatingElements numberOfHeatingElements, int maxLevel)
			throws Exception {
		final var maxPower = maxLevel * POWER_PER_PHASE;
		final var test = prepareTest(clock, numberOfHeatingElements, Mode.AUTOMATIC, Level.LEVEL_1,
				MINIMUM_SWITCHING_TIME);
		test.next(new TestCase() //
				.input(GRID_ACTIVE_POWER, 0) //
				.output(LEVEL, Level.LEVEL_0));

		for (var level = 1; level <= maxLevel; level++) {
			test.next(new TestCase() //
					.timeleap(clock, level == 1 ? 1 : MINIMUM_SWITCHING_TIME, SECONDS) //
					.input(GRID_ACTIVE_POWER, -maxPower) //
					.output(LEVEL, Level.fromValue(level)));
		}

		for (var level = maxLevel - 1; level >= 0; level--) {
			test.next(new TestCase() //
					.timeleap(clock, MINIMUM_SWITCHING_TIME, SECONDS) //
					.input(GRID_ACTIVE_POWER, maxPower) //
					.output(LEVEL, Level.fromValue(level)));
		}
		test.deactivate();
	}

	@Test
	void surplusReevaluatesTargetWhenDirectionChangesDuringWaiting() throws Exception {
		prepareTest(clock, NumberOfHeatingElements.TWO, Mode.AUTOMATIC, Level.LEVEL_1, MINIMUM_SWITCHING_TIME) //
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, 0) //
						.output(LEVEL, Level.LEVEL_0)) //
				.next(new TestCase() // target rises to 3; first change is allowed
						.timeleap(clock, 1, SECONDS) //
						.input(GRID_ACTIVE_POWER, -6000) //
						.output(LEVEL, Level.LEVEL_1)) //
				.next(new TestCase() // target reverses to 0 while the interval is still running
						.timeleap(clock, 10, SECONDS) //
						.input(GRID_ACTIVE_POWER, 12000) //
						.output(LEVEL, Level.LEVEL_1)) //
				.next(new TestCase() // at the boundary, move down toward the latest target
						.timeleap(clock, 50, SECONDS) //
						.input(GRID_ACTIVE_POWER, 12000) //
						.output(LEVEL, Level.LEVEL_0)) //
				.next(new TestCase() // target reverses up; the downward change restarted the interval
						.timeleap(clock, 1, SECONDS) //
						.input(GRID_ACTIVE_POWER, -12000) //
						.output(LEVEL, Level.LEVEL_0)) //
				.next(new TestCase() // at the next boundary, move up toward the latest target
						.timeleap(clock, 59, SECONDS) //
						.input(GRID_ACTIVE_POWER, -12000) //
						.output(LEVEL, Level.LEVEL_1)) //
				.deactivate();
	}

	@Test
	void switchingTimeBoundaryAllowsChangeAtExactlyTheMinimum() throws Exception {
		prepareTest(clock, NumberOfHeatingElements.ONE, Mode.AUTOMATIC, Level.LEVEL_1, MINIMUM_SWITCHING_TIME) //
				.next(new TestCase() //
						.input(GRID_ACTIVE_POWER, 0) //
						.output(LEVEL, Level.LEVEL_0)) //
				.next(new TestCase() //
						.timeleap(clock, 1, SECONDS) //
						.input(GRID_ACTIVE_POWER, -4000) //
						.output(LEVEL, Level.LEVEL_1)) //
				.next(new TestCase() // 59 seconds after the previous change is still too early
						.timeleap(clock, 59, SECONDS) //
						.input(GRID_ACTIVE_POWER, -4000) //
						.output(LEVEL, Level.LEVEL_1)) //
				.next(new TestCase() // exactly 60 seconds after the previous change is allowed
						.timeleap(clock, 1, SECONDS) //
						.input(GRID_ACTIVE_POWER, -4000) //
						.output(LEVEL, Level.LEVEL_2)) //
				.deactivate();
	}

	@Test
	void zeroSecondSwitchingTimeAllowsOneStepPerCycle() throws Exception {
		final var test = prepareTest(clock, NumberOfHeatingElements.TWO, Mode.AUTOMATIC, Level.LEVEL_1, 0);
		test.next(new TestCase() //
				.input(GRID_ACTIVE_POWER, 0) //
				.output(LEVEL, Level.LEVEL_0));
		for (var level = 1; level <= 6; level++) {
			test.next(new TestCase() //
					.input(GRID_ACTIVE_POWER, -12000) //
					.output(LEVEL, Level.fromValue(level)));
		}
		test.deactivate();
	}

	@Test
	void offSwitchesFromLevelFourToZeroImmediately() throws Exception {
		final var test = prepareTest(clock, NumberOfHeatingElements.TWO, Mode.MANUAL_ON, Level.LEVEL_4,
				MINIMUM_SWITCHING_TIME);
		test.next(new TestCase().output(LEVEL, Level.LEVEL_4));
		test.modified(createConfig(Mode.MANUAL_OFF, NumberOfHeatingElements.TWO, Level.LEVEL_4, MINIMUM_SWITCHING_TIME)
				.build()) //
				.next(new TestCase().output(LEVEL, Level.LEVEL_0)) //
				.deactivate();
	}

	@Test
	void fastHeatImmediatelySwitchesToMaximumLevel() throws Exception {
		prepareTest(clock, NumberOfHeatingElements.TWO, Mode.FAST_HEAT, Level.LEVEL_0, MINIMUM_SWITCHING_TIME) //
				.next(new TestCase().output(LEVEL, Level.LEVEL_6)) //
				.deactivate();
	}
}
