package io.openems.edge.controller.ess.fastfrequencyreserve;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.controller.ess.fastfrequencyreserve.enums.ActivationTime;
import io.openems.edge.controller.ess.fastfrequencyreserve.enums.Mode;
import io.openems.edge.controller.ess.fastfrequencyreserve.enums.SupportDuration;
import io.openems.edge.controller.ess.fastfrequencyreserve.statemachine.StateMachine.State;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.meter.test.DummyElectricityMeter;

public class MyControllerTest {

	private static final String CTRL_ID = "ctrl0";
	private static final String ESS_ID = "ess0";
	private static final String METER_ID = "meter0";

	private static final DummyManagedSymmetricEss ESS = new DummyManagedSymmetricEss(ESS_ID);
	// private static final DummyAsymmetricMeter METER = new
	// DummyAsymmetricMeter(METER_ID);
	private static final DummyElectricityMeter METER = new DummyElectricityMeter(METER_ID);

	private static final ChannelAddress METER_FREQUENCY = new ChannelAddress(METER_ID, "Frequency");
	private static final ChannelAddress STATE_MACHINE = new ChannelAddress(CTRL_ID, "StateMachine");
	private static final ChannelAddress ESS_ACTIVE_POWER = new ChannelAddress(ESS_ID, "SetActivePowerEquals");

	@Test
	public void test1() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2023-07-13T08:45:00.00Z"), ZoneOffset.UTC);

		final var cm = new DummyComponentManager(clock);

		new ControllerTest(new FastFrequencyReserveImpl()) //

				.addReference("componentManager", cm) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", ESS //
						.withGridMode(GridMode.ON_GRID)//
						.withAllowedChargePower(-92000)//
						.withAllowedDischargePower(92000)) //
				.addReference("meter", METER) //
				.activate(MyConfig.create() //
						.setEssId(ESS_ID) //
						.setId(CTRL_ID) //
						.setMeterId(METER_ID) //
						.setMode(Mode.MANUAL_ON) //
						.setactivationScheduleJson(JsonUtils.buildJsonArray() //
								.add(JsonUtils.buildJsonObject() //
										.addProperty("startTimestamp", 1689242400) // Thu Jul 13 2023 10:00:00 GMT+0000
										.addProperty("duration", 86400) //
										.addProperty("dischargePowerSetPoint", 92000) //
										.addProperty("frequencyLimit", 49500) //
										.addProperty("activationRunTime", "LONG_ACTIVATION_RUN") //
										.addProperty("supportDuration", "LONG_SUPPORT_DURATION") //
										.build())
								.add(JsonUtils.buildJsonObject() //
										.addProperty("startTimestamp", 1689854400) // Fri Jul 14 2023 12:00:00 GMT+0000
										.addProperty("duration", 86400) //
										.addProperty("dischargePowerSetPoint", 92000) //
										.addProperty("frequencyLimit", 49500) //
										.addProperty("activationRunTime", "LONG_ACTIVATION_RUN") //
										.addProperty("supportDuration", "LONG_SUPPORT_DURATION") //
										.build())
								.build()//
								.toString())//
						.build())
				.next(new TestCase("1") //
						.input(METER_FREQUENCY, 50000)//
						.output(STATE_MACHINE, State.UNDEFINED) //
						.output(ESS_ACTIVE_POWER, null))//
				.next(new TestCase("2") //
						.input(METER_FREQUENCY, 50000)//
						.output(STATE_MACHINE, State.UNDEFINED) //
						.output(ESS_ACTIVE_POWER, null))//
				.next(new TestCase("3") //
						.timeleap(clock, 1, ChronoUnit.HOURS) //
						.input(METER_FREQUENCY, 50000))//
				.next(new TestCase("4") //
						.output(STATE_MACHINE, State.PRE_ACTIVATIOM_STATE)//
						.output(ESS_ACTIVE_POWER, -16560))
				.next(new TestCase("5") //
						.timeleap(clock, 16, ChronoUnit.MINUTES))
				.next(new TestCase("6") //
						.input(METER_FREQUENCY, 50000)//
						.output(STATE_MACHINE, State.ACTIVATION_TIME) //
						.output(ESS_ACTIVE_POWER, 0)) //
				.next(new TestCase("7") //
						.input(METER_FREQUENCY, 49500)//
						.output(STATE_MACHINE, State.ACTIVATION_TIME) //
						.output(ESS_ACTIVE_POWER, 92000)) //
				.next(new TestCase("8") //
						.timeleap(clock, ActivationTime.LONG_ACTIVATION_RUN.getValue(), ChronoUnit.MILLIS)) //
				.next(new TestCase("9") //
						.output(STATE_MACHINE, State.SUPPORT_DURATION)//
						.output(ESS_ACTIVE_POWER, 92000)) //
				.next(new TestCase("10") //
						.timeleap(clock, SupportDuration.LONG_SUPPORT_DURATION.getValue(), ChronoUnit.SECONDS) //
						.output(STATE_MACHINE, State.SUPPORT_DURATION))
				.next(new TestCase("11") //
						.output(STATE_MACHINE, State.DEACTIVATION_TIME) //
						.output(ESS_ACTIVE_POWER, 0))
				.next(new TestCase("12") //
						.timeleap(clock, ActivationTime.LONG_ACTIVATION_RUN.getValue(), ChronoUnit.MILLIS)) //
				.next(new TestCase("13") //
						.output(STATE_MACHINE, State.BUFFERED_TIME_BEFORE_RECOVERY) //
						.output(ESS_ACTIVE_POWER, -16560))
				.next(new TestCase("14") //
						.output(STATE_MACHINE, State.BUFFERED_TIME_BEFORE_RECOVERY))
				.next(new TestCase("15") //
						.output(STATE_MACHINE, State.RECOVERY_TIME))
				.next(new TestCase("16") //
						.timeleap(clock, SupportDuration.LONG_SUPPORT_DURATION.getValue(), ChronoUnit.MINUTES))
				.next(new TestCase("17") //
						.input(METER_FREQUENCY, 50000)//
						.output(STATE_MACHINE, State.RECOVERY_TIME))
				.next(new TestCase("18") //
						.timeleap(clock, 1, ChronoUnit.DAYS)) //
				.next(new TestCase("19") //
						.output(STATE_MACHINE, State.RECOVERY_TIME))
				.next(new TestCase("20") //
						.timeleap(clock, 1, ChronoUnit.DAYS)) //
				.next(new TestCase("21") //
						.output(STATE_MACHINE, State.RECOVERY_TIME));
	}

	@Test
	public void test2() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2023-07-13T08:45:00.00Z"), ZoneOffset.UTC);

		final var cm = new DummyComponentManager(clock);

		new ControllerTest(new FastFrequencyReserveImpl()) //

				.addReference("componentManager", cm) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", ESS //
						.withGridMode(GridMode.ON_GRID)//
						.withAllowedChargePower(-92000)//
						.withAllowedDischargePower(92000)) //
				.addReference("meter", METER) //
				.activate(MyConfig.create() //
						.setEssId(ESS_ID) //
						.setId(CTRL_ID) //
						.setMeterId(METER_ID) //
						.setMode(Mode.MANUAL_ON) //
						.setactivationScheduleJson(JsonUtils.buildJsonArray() //
								.add(JsonUtils.buildJsonObject() //
										.addProperty("startTimestamp", 1689242400) // Thu Jul 13 2023 10:00:00 GMT+0000
										.addProperty("duration", 86400) //
										.addProperty("dischargePowerSetPoint", 92000) //
										.addProperty("frequencyLimit", 49500) //
										.addProperty("activationRunTime", "LONG_ACTIVATION_RUN") //
										.addProperty("supportDuration", "LONG_SUPPORT_DURATION") //
										.build())
								.add(JsonUtils.buildJsonObject() //
										.addProperty("startTimestamp", 1689854400) // Fri Jul 14 2023 12:00:00 GMT+0000
										.addProperty("duration", 86400) //
										.addProperty("dischargePowerSetPoint", 92000) //
										.addProperty("frequencyLimit", 49500) //
										.addProperty("activationRunTime", "LONG_ACTIVATION_RUN") //
										.addProperty("supportDuration", "LONG_SUPPORT_DURATION") //
										.build())
								.build()//
								.toString())//
						.build())

				.next(new TestCase("one")) //
				.next(new TestCase("two") //
						.timeleap(clock, 80, ChronoUnit.MINUTES)) //
				.next(new TestCase("three") //
						.timeleap(clock, 16, ChronoUnit.MINUTES)) //
				.next(new TestCase("four") //
						.timeleap(clock, ActivationTime.LONG_ACTIVATION_RUN.getValue(), ChronoUnit.MILLIS)) //
				.next(new TestCase("five") //
						.timeleap(clock, SupportDuration.LONG_SUPPORT_DURATION.getValue(), ChronoUnit.SECONDS)) //
				.next(new TestCase("six") //
						.timeleap(clock, ActivationTime.LONG_ACTIVATION_RUN.getValue(), ChronoUnit.MILLIS)) //
				.next(new TestCase("seven") //
						.timeleap(clock, SupportDuration.LONG_SUPPORT_DURATION.getValue(), ChronoUnit.MINUTES)) //
				.next(new TestCase("eight") //
						.timeleap(clock, 1, ChronoUnit.DAYS)//
						.output(STATE_MACHINE, State.ACTIVATION_TIME)) //

		;

	}
}