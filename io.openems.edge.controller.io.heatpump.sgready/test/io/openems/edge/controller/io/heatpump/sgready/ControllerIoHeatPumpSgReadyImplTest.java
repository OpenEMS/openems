package io.openems.edge.controller.io.heatpump.sgready;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.common.test.TimeLeapClock;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.io.test.DummyInputOutput;

public class ControllerIoHeatPumpSgReadyImplTest {

	private static final String CTRL_ID = "ctrHeatPump0";
	private static final String IO_ID = "io0";

	private static final String outputChannel1 = "io0/InputOutput0";
	private static final String outputChannel2 = "io0/InputOutput1";

	private static final ChannelAddress STATUS = new ChannelAddress(CTRL_ID, "Status");
	private static final ChannelAddress AWAITING_HYSTERESIS = new ChannelAddress(CTRL_ID, "AwaitingHysteresis");
	private static final ChannelAddress IO_OUTPUT_CHANNEL1 = new ChannelAddress(IO_ID, "InputOutput0");
	private static final ChannelAddress IO_OUTPUT_CHANNEL2 = new ChannelAddress(IO_ID, "InputOutput1");

	private static final ChannelAddress SUM_GRID_ACTIVE_POWER = new ChannelAddress("_sum", "GridActivePower");
	private static final ChannelAddress SUM_ESS_SOC = new ChannelAddress("_sum", "EssSoc");
	private static final ChannelAddress SUM_ESS_DISCHARGE_POWER = new ChannelAddress("_sum", "EssDischargePower");

	@Test
	public void manual_undefined_test() throws Exception {

		final var clock = new TimeLeapClock(Instant.ofEpochSecond(1577836800) /* starts at 1. January 2020 00:00:00 */,
				ZoneOffset.UTC);

		new ControllerTest(new ControllerIoHeatPumpSgReadyImpl()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("sum", new DummySum()) //
				.addComponent(new DummyInputOutput(IO_ID)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setMode(Mode.MANUAL) //
						.setManualState(Status.UNDEFINED) //
						.setOutputChannel1(outputChannel1) //
						.setOutputChannel2(outputChannel2) //
						.build())
				.next(new TestCase() //
						.output(STATUS, Status.REGULAR) //
						.output(IO_OUTPUT_CHANNEL1, false) //
						.output(IO_OUTPUT_CHANNEL2, false));
	}

	@Test
	public void manual_regular_test() throws Exception {

		final var clock = new TimeLeapClock(Instant.ofEpochSecond(1577836800) /* starts at 1. January 2020 00:00:00 */,
				ZoneOffset.UTC);

		new ControllerTest(new ControllerIoHeatPumpSgReadyImpl()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("sum", new DummySum()) //
				.addComponent(new DummyInputOutput(IO_ID)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setMode(Mode.MANUAL) //
						.setManualState(Status.REGULAR) //
						.setOutputChannel1(outputChannel1) //
						.setOutputChannel2(outputChannel2) //
						.build())
				.next(new TestCase() //
						.output(STATUS, Status.REGULAR) //
						.output(IO_OUTPUT_CHANNEL1, false) //
						.output(IO_OUTPUT_CHANNEL2, false));
	}

	@Test
	public void manual_recommendation_test() throws Exception {

		final var clock = new TimeLeapClock(Instant.ofEpochSecond(1577836800) /* starts at 1. January 2020 00:00:00 */,
				ZoneOffset.UTC);

		new ControllerTest(new ControllerIoHeatPumpSgReadyImpl()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("sum", new DummySum()) //
				.addComponent(new DummyInputOutput(IO_ID)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setMode(Mode.MANUAL) //
						.setManualState(Status.RECOMMENDATION) //
						.setOutputChannel1(outputChannel1) //
						.setOutputChannel2(outputChannel2) //
						.build())
				.next(new TestCase() //
						.output(STATUS, Status.RECOMMENDATION) //
						.output(IO_OUTPUT_CHANNEL1, false) //
						.output(IO_OUTPUT_CHANNEL2, true));
	}

	@Test
	public void manual_force_on_test() throws Exception {

		final var clock = new TimeLeapClock(Instant.ofEpochSecond(1577836800) /* starts at 1. January 2020 00:00:00 */,
				ZoneOffset.UTC);

		new ControllerTest(new ControllerIoHeatPumpSgReadyImpl()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("sum", new DummySum()) //
				.addComponent(new DummyInputOutput(IO_ID)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setMode(Mode.MANUAL) //
						.setManualState(Status.FORCE_ON) //
						.setOutputChannel1(outputChannel1) //
						.setOutputChannel2(outputChannel2) //
						.build())
				.next(new TestCase() //
						.output(STATUS, Status.FORCE_ON) //
						.output(IO_OUTPUT_CHANNEL1, true) //
						.output(IO_OUTPUT_CHANNEL2, true));
	}

	@Test
	public void manual_lock_test() throws Exception {

		final var clock = new TimeLeapClock(Instant.ofEpochSecond(1577836800) /* starts at 1. January 2020 00:00:00 */,
				ZoneOffset.UTC);

		new ControllerTest(new ControllerIoHeatPumpSgReadyImpl()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("sum", new DummySum()) //
				.addComponent(new DummyInputOutput(IO_ID)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setMode(Mode.MANUAL) //
						.setManualState(Status.LOCK) //
						.setOutputChannel1(outputChannel1) //
						.setOutputChannel2(outputChannel2) //
						.build())
				.next(new TestCase() //
						.output(STATUS, Status.LOCK) //
						.output(IO_OUTPUT_CHANNEL1, true) //
						.output(IO_OUTPUT_CHANNEL2, false));
	}

	@Test
	public void automatic_regular_test() throws Exception {

		final var clock = new TimeLeapClock(Instant.ofEpochSecond(1577836800) /* starts at 1. January 2020 00:00:00 */,
				ZoneOffset.UTC);

		new ControllerTest(new ControllerIoHeatPumpSgReadyImpl()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("sum", new DummySum()) //
				.addComponent(new DummyInputOutput(IO_ID)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setMode(Mode.AUTOMATIC) //
						.setAutomaticForceOnCtrlEnabled(false) //
						.setAutomaticRecommendationCtrlEnabled(false) //
						.setAutomaticLockCtrlEnabled(false) //
						.setOutputChannel1(outputChannel1) //
						.setOutputChannel2(outputChannel2) //
						.build())
				.next(new TestCase() //
						.output(STATUS, Status.REGULAR) //
						.output(IO_OUTPUT_CHANNEL1, false) //
						.output(IO_OUTPUT_CHANNEL2, false));
	}

	@Test
	public void automatic_normal_config_test() throws Exception {

		final var clock = new TimeLeapClock(Instant.ofEpochSecond(1577836800) /* starts at 1. January 2020 00:00:00 */,
				ZoneOffset.UTC);

		new ControllerTest(new ControllerIoHeatPumpSgReadyImpl()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("sum", new DummySum()) //
				.addComponent(new DummyInputOutput(IO_ID)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setMode(Mode.AUTOMATIC) //
						.setAutomaticRecommendationCtrlEnabled(true) //
						.setAutomaticRecommendationSurplusPower(3000) //
						.setAutomaticForceOnCtrlEnabled(true) //
						.setAutomaticForceOnSurplusPower(5000) //
						.setAutomaticForceOnSoc(90) //
						.setAutomaticLockCtrlEnabled(true) //
						.setAutomaticLockGridBuyPower(5000) //
						.setAutomaticLockSoc(20) //
						.setOutputChannel1(outputChannel1) //
						.setOutputChannel2(outputChannel2) //
						.setMinimumSwitchingTime(0) //
						.build())
				.next(new TestCase() //
						.input(SUM_GRID_ACTIVE_POWER, -4000) //
						.input(SUM_ESS_DISCHARGE_POWER, 0) //
						.input(SUM_ESS_SOC, 95) //
						.output(STATUS, Status.RECOMMENDATION)) //
				.next(new TestCase() //
						.input(SUM_GRID_ACTIVE_POWER, -3000) //
						.input(SUM_ESS_DISCHARGE_POWER, 0) //
						.input(SUM_ESS_SOC, 95) //
						.output(STATUS, Status.FORCE_ON)) //
				.next(new TestCase() //
						.input(SUM_GRID_ACTIVE_POWER, 500) //
						.input(SUM_ESS_DISCHARGE_POWER, 0) //
						.input(SUM_ESS_SOC, 95) //
						.output(STATUS, Status.RECOMMENDATION)) //
				.next(new TestCase() //
						.input(SUM_GRID_ACTIVE_POWER, -2700) //
						.input(SUM_ESS_DISCHARGE_POWER, 0) //
						.input(SUM_ESS_SOC, 95) //
						.output(STATUS, Status.FORCE_ON)) //
				.next(new TestCase() //
						.input(SUM_GRID_ACTIVE_POWER, -150) //
						.input(SUM_ESS_DISCHARGE_POWER, 0) //
						.input(SUM_ESS_SOC, 88) //
						.output(STATUS, Status.RECOMMENDATION)) //
				.next(new TestCase() //
						.input(SUM_GRID_ACTIVE_POWER, 500) //
						.input(SUM_ESS_DISCHARGE_POWER, 0) //
						.input(SUM_ESS_SOC, 88) //
						.output(STATUS, Status.REGULAR))
				.next(new TestCase() //
						.input(SUM_GRID_ACTIVE_POWER, 5500) //
						.input(SUM_ESS_DISCHARGE_POWER, 0) //
						.input(SUM_ESS_SOC, 19) //
						.output(STATUS, Status.LOCK));
	}

	@Test
	public void automatic_switching_time_test() throws Exception {

		final var clock = new TimeLeapClock(Instant.ofEpochSecond(1577836800) /* starts at 1. January 2020 00:00:00 */,
				ZoneOffset.UTC);

		new ControllerTest(new ControllerIoHeatPumpSgReadyImpl()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("sum", new DummySum()) //
				.addComponent(new DummyInputOutput(IO_ID)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setMode(Mode.AUTOMATIC) //
						.setAutomaticRecommendationCtrlEnabled(true) //
						.setAutomaticRecommendationSurplusPower(3000) //
						.setAutomaticForceOnCtrlEnabled(true) //
						.setAutomaticForceOnSurplusPower(5000) //
						.setAutomaticForceOnSoc(90) //
						.setAutomaticLockCtrlEnabled(true) //
						.setAutomaticLockGridBuyPower(5000) //
						.setAutomaticLockSoc(20) //
						.setOutputChannel1(outputChannel1) //
						.setOutputChannel2(outputChannel2) //
						.setMinimumSwitchingTime(60) //
						.build())
				.next(new TestCase("Test 1") //
						.input(SUM_GRID_ACTIVE_POWER, -4000) //
						.input(SUM_ESS_DISCHARGE_POWER, 0) //
						.input(SUM_ESS_SOC, 95) //
						.output(STATUS, Status.RECOMMENDATION)) //
				.next(new TestCase("Test 1") //
						.timeleap(clock, 18, ChronoUnit.SECONDS) //
						.output(AWAITING_HYSTERESIS, true)) //
				.next(new TestCase("Test 2") //
						.timeleap(clock, 18, ChronoUnit.SECONDS) //
						.output(AWAITING_HYSTERESIS, true)) //
				.next(new TestCase("Test 3") //
						.timeleap(clock, 18, ChronoUnit.SECONDS) //
						.output(AWAITING_HYSTERESIS, true)) //
				.next(new TestCase("Test 4") //
						.timeleap(clock, 18, ChronoUnit.SECONDS) //
						.output(AWAITING_HYSTERESIS, false))
				.next(new TestCase("Test 5") //
						.input(SUM_GRID_ACTIVE_POWER, -4000) //
						.input(SUM_ESS_DISCHARGE_POWER, 0) //
						.input(SUM_ESS_SOC, 95) //
						.output(STATUS, Status.FORCE_ON)) //
				.next(new TestCase("Test 6") //
						.timeleap(clock, 30, ChronoUnit.SECONDS) //
						.output(STATUS, Status.FORCE_ON) //
						.output(AWAITING_HYSTERESIS, true)) //
				.next(new TestCase("Test 7") //
						.timeleap(clock, 10, ChronoUnit.SECONDS) //
						.input(SUM_GRID_ACTIVE_POWER, -500) //
						.input(SUM_ESS_DISCHARGE_POWER, 0) //
						.input(SUM_ESS_SOC, 95) //
						.output(STATUS, Status.FORCE_ON) //
						.output(AWAITING_HYSTERESIS, true)) //
				.next(new TestCase("Test 8") //
						.timeleap(clock, 30, ChronoUnit.SECONDS) //
						.input(SUM_GRID_ACTIVE_POWER, 500) //
						.input(SUM_ESS_DISCHARGE_POWER, 0) //
						.input(SUM_ESS_SOC, 95) //
						.output(AWAITING_HYSTERESIS, false) //
						.output(STATUS, Status.RECOMMENDATION) //
				);
	}

	@Test
	public void automatic_switching2_time_test() throws Exception {

		final var clock = new TimeLeapClock(Instant.ofEpochSecond(1577836800), ZoneOffset.UTC);

		new ControllerTest(new ControllerIoHeatPumpSgReadyImpl()).addReference("componentManager", //
				new DummyComponentManager(clock)) //
				.addReference("sum", new DummySum()) //
				.addComponent(new DummyInputOutput(IO_ID)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setMode(Mode.AUTOMATIC) //
						.setAutomaticRecommendationCtrlEnabled(true) //
						.setAutomaticRecommendationSurplusPower(3000) //
						.setAutomaticForceOnCtrlEnabled(true) //
						.setAutomaticForceOnSurplusPower(5000) //
						.setAutomaticForceOnSoc(90) //
						.setAutomaticLockCtrlEnabled(true) //
						.setAutomaticLockGridBuyPower(5000) //
						.setAutomaticLockSoc(20) //
						.setOutputChannel1(outputChannel1) //
						.setOutputChannel2(outputChannel2) //
						.setMinimumSwitchingTime(60) //
						.build())
				.next(new TestCase("Test 1") //
						.input(SUM_GRID_ACTIVE_POWER, -4000) //
						.input(SUM_ESS_DISCHARGE_POWER, 0) //
						.input(SUM_ESS_SOC, 95) //
						.output(STATUS, Status.RECOMMENDATION)) //
				.next(new TestCase("Test 2") //
						.timeleap(clock, 50, ChronoUnit.SECONDS)//
						.input(SUM_GRID_ACTIVE_POWER, -3000) //
						.input(SUM_ESS_DISCHARGE_POWER, 0) //
						.input(SUM_ESS_SOC, 95) //
						.output(STATUS, Status.RECOMMENDATION) //
						.output(AWAITING_HYSTERESIS, true)) //
				.next(new TestCase("Test 3") //
						.timeleap(clock, 15, ChronoUnit.SECONDS)//
						.input(SUM_GRID_ACTIVE_POWER, -3000) //
						.input(SUM_ESS_DISCHARGE_POWER, 0) //
						.input(SUM_ESS_SOC, 95) //
						.output(AWAITING_HYSTERESIS, false)) //
				.next(new TestCase("Test 3 - Results") //
						.output(AWAITING_HYSTERESIS, true) //
						.output(STATUS, Status.FORCE_ON)) //
				.next(new TestCase("Test 4") //
						.timeleap(clock, 65, ChronoUnit.SECONDS)//
						.input(SUM_GRID_ACTIVE_POWER, -3000) //
						.input(SUM_ESS_DISCHARGE_POWER, 0) //
						.input(SUM_ESS_SOC, 95) //
						.output(AWAITING_HYSTERESIS, false) //
						.output(STATUS, Status.FORCE_ON)) //
				.next(new TestCase("Test 5") //
						.timeleap(clock, 15, ChronoUnit.SECONDS)//
						.input(SUM_GRID_ACTIVE_POWER, 500) //
						.input(SUM_ESS_DISCHARGE_POWER, 0) //
						.input(SUM_ESS_SOC, 95) //
						.output(AWAITING_HYSTERESIS, false) //
						.output(STATUS, Status.RECOMMENDATION)) //
				.next(new TestCase("Test 6") //
						.timeleap(clock, 65, ChronoUnit.SECONDS)//
						.input(SUM_GRID_ACTIVE_POWER, 15000) //
						.input(SUM_ESS_DISCHARGE_POWER, 0) //
						.input(SUM_ESS_SOC, 15) //
						.output(AWAITING_HYSTERESIS, false) //
						.output(STATUS, Status.LOCK)) //
				.next(new TestCase("Test 7") //
						.timeleap(clock, 15, ChronoUnit.MINUTES) //
						.input(SUM_GRID_ACTIVE_POWER, -2700) //
						.input(SUM_ESS_DISCHARGE_POWER, 0) //
						.input(SUM_ESS_SOC, 95) //
						.output(STATUS, Status.REGULAR)) //
				.next(new TestCase("Test 8") //
						.timeleap(clock, 15, ChronoUnit.MINUTES)//
						.input(SUM_GRID_ACTIVE_POWER, -15000) //
						.input(SUM_ESS_DISCHARGE_POWER, 0) //
						.input(SUM_ESS_SOC, 88) //
						.output(STATUS, Status.RECOMMENDATION)); //
	}
}
