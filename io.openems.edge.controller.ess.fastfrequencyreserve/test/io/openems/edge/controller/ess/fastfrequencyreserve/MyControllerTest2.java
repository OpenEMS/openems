package io.openems.edge.controller.ess.fastfrequencyreserve;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.test.TimeLeapClock;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.ess.fastfrequencyreserve.enums.ActivationTime;
import io.openems.edge.controller.ess.fastfrequencyreserve.enums.ControlMode;
import io.openems.edge.controller.ess.fastfrequencyreserve.enums.SupportDuration;
import io.openems.edge.controller.ess.fastfrequencyreserve.statemachine.StateMachine.State;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.meter.test.DummyElectricityMeter;

public class MyControllerTest2 {

	private static final String CTRL_ID = "ctrl0";
	private static final String ESS_ID = "ess0";
	private static final String METER_ID = "meter0";

	private static final DummyManagedSymmetricEss ESS = new DummyManagedSymmetricEss(ESS_ID);
	private static final DummyElectricityMeter METER = new DummyElectricityMeter(METER_ID);

	private static final ChannelAddress METER_FREQUENCY = new ChannelAddress(METER_ID, "Frequency");
	private static final ChannelAddress STATE_MACHINE = new ChannelAddress(CTRL_ID, "StateMachine");
	private static final ChannelAddress ESS_ACTIVE_POWER = new ChannelAddress(ESS_ID, "SetActivePowerEquals");

	@Test
	public void test1() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2023-07-13T08:45:00.00Z"), ZoneOffset.UTC);
		final var cm = new DummyComponentManager(clock);

		new ControllerTest(new ControllerFastFrequencyReserveImpl()) //
				.addReference("componentManager", cm) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", ESS //
						.withGridMode(GridMode.ON_GRID)//
						.withMaxApparentPower(92000)//
						.withAllowedChargePower(-92000)//
						.withAllowedDischargePower(92000)) //
				.addReference("meter", METER) //
				.activate(MyConfig.create() //
						.setEssId(ESS_ID) //
						.setId(CTRL_ID) //
						.setMeterId(METER_ID) //
						.setMode(ControlMode.MANUAL_ON) //
						.setPreActivationTime(15)//
						.setactivationScheduleJson(JsonUtils.buildJsonArray() //
								.add(JsonUtils.buildJsonObject() //
										.addProperty("startTimestamp", 1689242400) // Thu Jul 13 2023 10:00:00 GMT+0000
										.addProperty("duration", 86400) //
										.addProperty("dischargePowerSetPoint", 92000) //
										.addProperty("frequencyLimit", 49500) //
										.addProperty("activationRunTime", ActivationTime.LONG_ACTIVATION_RUN) //
										.addProperty("supportDuration", SupportDuration.LONG_SUPPORT_DURATION) //
										.build())
								.add(JsonUtils.buildJsonObject() //
										.addProperty("startTimestamp", 1689331500) // Fri Jul 14 2023 10:45:00 GMT+0000
										.addProperty("duration", 86400) //
										.addProperty("dischargePowerSetPoint", 92000) //
										.addProperty("frequencyLimit", 49500) //
										.addProperty("activationRunTime", ActivationTime.LONG_ACTIVATION_RUN) //
										.addProperty("supportDuration", SupportDuration.LONG_SUPPORT_DURATION) //
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
				.next(new TestCase("4"))//
				.next(new TestCase("5") //
						.output(STATE_MACHINE, State.UNDEFINED))
				.next(new TestCase("6") //
						.timeleap(clock, 10, ChronoUnit.MINUTES)//
						.output(STATE_MACHINE, State.PRE_ACTIVATION_STATE))
				.next(new TestCase("7") //
						.timeleap(clock, 10, ChronoUnit.MINUTES))
				.next(new TestCase("8") //
						.output(STATE_MACHINE, State.ACTIVATION_TIME)//
						.output(ESS_ACTIVE_POWER, 0))
				.next(new TestCase("9") //
						.input(METER_FREQUENCY, 50000)//
						.output(STATE_MACHINE, State.ACTIVATION_TIME)//
						.output(ESS_ACTIVE_POWER, 0))
				.next(new TestCase("10") //
						.input(METER_FREQUENCY, 49400)//
						.output(STATE_MACHINE, State.ACTIVATION_TIME)//
						.output(ESS_ACTIVE_POWER, 92000))
				.next(new TestCase("11") //
						.timeleap(clock, ActivationTime.LONG_ACTIVATION_RUN.getValue(), ChronoUnit.MILLIS)) //
				.next(new TestCase("12") //
						.output(STATE_MACHINE, State.SUPPORT_DURATION)//
						.output(ESS_ACTIVE_POWER, 92000)) //
				.next(new TestCase("13") //
						.timeleap(clock, SupportDuration.LONG_SUPPORT_DURATION.getValue(), ChronoUnit.SECONDS) //
						.output(STATE_MACHINE, State.SUPPORT_DURATION))
				.next(new TestCase("14") //
						.output(STATE_MACHINE, State.DEACTIVATION_TIME).output(ESS_ACTIVE_POWER, 0))
				.next(new TestCase("15") //
						.timeleap(clock, ActivationTime.LONG_ACTIVATION_RUN.getValue(), ChronoUnit.MILLIS)) //
				.next(new TestCase("16") //
						.output(STATE_MACHINE, State.BUFFERED_TIME_BEFORE_RECOVERY) //
						.output(ESS_ACTIVE_POWER, 0))
				.next(new TestCase("17") //
						.timeleap(clock, 16, ChronoUnit.SECONDS) //
						.output(STATE_MACHINE, State.BUFFERED_TIME_BEFORE_RECOVERY))
				.next(new TestCase("18") //
						.timeleap(clock, 12, ChronoUnit.MINUTES)) //
				.next(new TestCase("19") //
						.output(STATE_MACHINE, State.RECOVERY_TIME) //
						.output(ESS_ACTIVE_POWER, -16560))
				.next(new TestCase("20") //
						.output(STATE_MACHINE, State.RECOVERY_TIME))
				.next(new TestCase("21") //
						.output(STATE_MACHINE, State.RECOVERY_TIME))
				.next(new TestCase("22") //
						.timeleap(clock, SupportDuration.LONG_SUPPORT_DURATION.getValue(), ChronoUnit.MINUTES))
				.next(new TestCase("23") //
						.input(METER_FREQUENCY, 50000)//
						.output(STATE_MACHINE, State.ACTIVATION_TIME))
				.next(new TestCase("24") //
						.timeleap(clock, 1, ChronoUnit.DAYS)) //
				.next(new TestCase("25") //
						.output(STATE_MACHINE, State.ACTIVATION_TIME))
				.next(new TestCase("26") //
						.timeleap(clock, 4, ChronoUnit.HOURS)) //
				.next(new TestCase("27") //
						.output(STATE_MACHINE, State.ACTIVATION_TIME))
				.next(new TestCase("28")//
						.input(METER_FREQUENCY, 49400))
				.next(new TestCase("29") //
						.timeleap(clock, ActivationTime.LONG_ACTIVATION_RUN.getValue(), ChronoUnit.MILLIS)) //
				.next(new TestCase("30") //
						.output(STATE_MACHINE, State.SUPPORT_DURATION)//
						.output(ESS_ACTIVE_POWER, 92000)) //
				.next(new TestCase("31") //
						.timeleap(clock, ActivationTime.LONG_ACTIVATION_RUN.getValue(), ChronoUnit.MILLIS)) //
				.next(new TestCase("32") //
						.output(STATE_MACHINE, State.SUPPORT_DURATION)//
						.output(ESS_ACTIVE_POWER, 92000)) //
				.next(new TestCase("33") //
						.timeleap(clock, SupportDuration.LONG_SUPPORT_DURATION.getValue(), ChronoUnit.SECONDS) //
						.output(STATE_MACHINE, State.SUPPORT_DURATION))
				.next(new TestCase("34") //
						.output(STATE_MACHINE, State.DEACTIVATION_TIME).output(ESS_ACTIVE_POWER, 0))
				.next(new TestCase("35") //
						.timeleap(clock, ActivationTime.LONG_ACTIVATION_RUN.getValue(), ChronoUnit.MILLIS)) //
				.next(new TestCase("36") //
						.output(STATE_MACHINE, State.BUFFERED_TIME_BEFORE_RECOVERY) //
						.output(ESS_ACTIVE_POWER, 0))
				.next(new TestCase("37") //
						.output(STATE_MACHINE, State.BUFFERED_TIME_BEFORE_RECOVERY))
				.next(new TestCase("38") //
						.output(STATE_MACHINE, State.BUFFERED_TIME_BEFORE_RECOVERY))
				.next(new TestCase("39") //
						.timeleap(clock, SupportDuration.LONG_SUPPORT_DURATION.getValue(), ChronoUnit.MINUTES))
				.next(new TestCase("40") //
						.input(METER_FREQUENCY, 50000)//
						.output(STATE_MACHINE, State.BUFFERED_TIME_BEFORE_RECOVERY))
				.next(new TestCase("41") //
						.input(METER_FREQUENCY, 50000)//
						.output(STATE_MACHINE, State.RECOVERY_TIME))
				.next(new TestCase("42") //
						.timeleap(clock, 1, ChronoUnit.DAYS));
	}

}
