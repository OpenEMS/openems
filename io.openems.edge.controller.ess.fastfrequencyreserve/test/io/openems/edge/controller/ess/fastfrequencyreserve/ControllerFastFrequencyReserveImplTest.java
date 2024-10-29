package io.openems.edge.controller.ess.fastfrequencyreserve;

import static io.openems.edge.common.test.TestUtils.createDummyClock;
import static io.openems.edge.controller.ess.fastfrequencyreserve.ControllerFastFrequencyReserve.ChannelId.STATE_MACHINE;
import static io.openems.edge.controller.ess.fastfrequencyreserve.enums.ActivationTime.LONG_ACTIVATION_RUN;
import static io.openems.edge.controller.ess.fastfrequencyreserve.enums.SupportDuration.LONG_SUPPORT_DURATION;
import static io.openems.edge.ess.api.ManagedSymmetricEss.ChannelId.SET_ACTIVE_POWER_EQUALS;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.FREQUENCY;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.MILLIS;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.test.TimeLeapClock;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.ess.fastfrequencyreserve.enums.ControlMode;
import io.openems.edge.controller.ess.fastfrequencyreserve.jsonrpc.SetActivateFastFreqReserveRequest.ActivateFastFreqReserveSchedule;
import io.openems.edge.controller.ess.fastfrequencyreserve.statemachine.StateMachine.State;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.meter.test.DummyElectricityMeter;

public class ControllerFastFrequencyReserveImplTest {

	@Test
	public void testFfrController() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2023-07-13T08:45:00.00Z"), ZoneOffset.UTC);
		new ControllerTest(new ControllerFastFrequencyReserveImpl()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", new DummyManagedSymmetricEss("ess0") //
						.withGridMode(GridMode.ON_GRID)//
						.withMaxApparentPower(92000)//
						.withAllowedChargePower(-92000)//
						.withAllowedDischargePower(92000)) //
				.addReference("meter", new DummyElectricityMeter("meter0")) //
				.activate(MyConfig.create() //
						.setEssId("ess0") //
						.setId("ctrl0") //
						.setMeterId("meter0") //
						.setMode(ControlMode.MANUAL_ON) //
						.setPreActivationTime(15)//
						.setactivationScheduleJson(JsonUtils.buildJsonArray() //
								.add(JsonUtils.buildJsonObject() //
										.addProperty("startTimestamp", 1689242400) // Thu Jul 13 2023 10:00:00 GMT+0000
										.addProperty("duration", 86400) //
										.addProperty("dischargePowerSetPoint", 92000) //
										.addProperty("frequencyLimit", 49500) //
										.addProperty("activationRunTime", LONG_ACTIVATION_RUN) //
										.addProperty("supportDuration", LONG_SUPPORT_DURATION) //
										.build())
								.add(JsonUtils.buildJsonObject() //
										.addProperty("startTimestamp", 1689331500) // Fri Jul 14 2023 10:45:00 GMT+0000
										.addProperty("duration", 86400) //
										.addProperty("dischargePowerSetPoint", 92000) //
										.addProperty("frequencyLimit", 49500) //
										.addProperty("activationRunTime", LONG_ACTIVATION_RUN) //
										.addProperty("supportDuration", LONG_SUPPORT_DURATION) //
										.build())
								.build()//
								.toString())//
						.build())
				.next(new TestCase("1") //
						.input("meter0", FREQUENCY, 50000)//
						.output(STATE_MACHINE, State.UNDEFINED) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, null))//
				.next(new TestCase("2") //
						.input("meter0", FREQUENCY, 50000)//
						.output(STATE_MACHINE, State.UNDEFINED) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, null))//
				.next(new TestCase("3") //
						.timeleap(clock, 1, HOURS) //
						.input("meter0", FREQUENCY, 50000))//
				.next(new TestCase("4"))//
				.next(new TestCase("5") //
						.output(STATE_MACHINE, State.UNDEFINED))
				.next(new TestCase("6") //
						.timeleap(clock, 10, MINUTES)//
						.output(STATE_MACHINE, State.PRE_ACTIVATION_STATE))
				.next(new TestCase("7") //
						.timeleap(clock, 10, MINUTES))
				.next(new TestCase("8") //
						.output(STATE_MACHINE, State.ACTIVATION_TIME)//
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 0))
				.next(new TestCase("9") //
						.input("meter0", FREQUENCY, 50000)//
						.output(STATE_MACHINE, State.ACTIVATION_TIME)//
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 0))
				.next(new TestCase("10") //
						.input("meter0", FREQUENCY, 49400)//
						.output(STATE_MACHINE, State.ACTIVATION_TIME)//
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 92000))
				.next(new TestCase("11") //
						.timeleap(clock, LONG_ACTIVATION_RUN.getValue(), MILLIS)) //
				.next(new TestCase("12") //
						.output(STATE_MACHINE, State.SUPPORT_DURATION)//
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 92000)) //
				.next(new TestCase("13") //
						.timeleap(clock, LONG_SUPPORT_DURATION.getValue(), SECONDS) //
						.output(STATE_MACHINE, State.SUPPORT_DURATION))
				.next(new TestCase("14") //
						.output(STATE_MACHINE, State.DEACTIVATION_TIME) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 0))
				.next(new TestCase("15") //
						.timeleap(clock, LONG_ACTIVATION_RUN.getValue(), MILLIS)) //
				.next(new TestCase("16") //
						.output(STATE_MACHINE, State.BUFFERED_TIME_BEFORE_RECOVERY) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 0))
				.next(new TestCase("17") //
						.timeleap(clock, 16, SECONDS) //
						.output(STATE_MACHINE, State.BUFFERED_TIME_BEFORE_RECOVERY))
				.next(new TestCase("18") //
						.timeleap(clock, 12, MINUTES)) //
				.next(new TestCase("19") //
						.output(STATE_MACHINE, State.RECOVERY_TIME) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, -16560))
				.next(new TestCase("20") //
						.output(STATE_MACHINE, State.RECOVERY_TIME))
				.next(new TestCase("21") //
						.output(STATE_MACHINE, State.RECOVERY_TIME))
				.next(new TestCase("22") //
						.timeleap(clock, LONG_SUPPORT_DURATION.getValue(), MINUTES))
				.next(new TestCase("23") //
						.input("meter0", FREQUENCY, 50000)//
						.output(STATE_MACHINE, State.ACTIVATION_TIME))
				.next(new TestCase("24") //
						.timeleap(clock, 1, DAYS)) //
				.next(new TestCase("25") //
						.output(STATE_MACHINE, State.ACTIVATION_TIME))
				.next(new TestCase("26") //
						.timeleap(clock, 4, HOURS)) //
				.next(new TestCase("27") //
						.output(STATE_MACHINE, State.ACTIVATION_TIME))
				.next(new TestCase("28")//
						.input("meter0", FREQUENCY, 49400))
				.next(new TestCase("29") //
						.timeleap(clock, LONG_ACTIVATION_RUN.getValue(), MILLIS)) //
				.next(new TestCase("30") //
						.output(STATE_MACHINE, State.SUPPORT_DURATION)//
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 92000)) //
				.next(new TestCase("31") //
						.timeleap(clock, LONG_ACTIVATION_RUN.getValue(), MILLIS)) //
				.next(new TestCase("32") //
						.output(STATE_MACHINE, State.SUPPORT_DURATION)//
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 92000)) //
				.next(new TestCase("33") //
						.timeleap(clock, LONG_SUPPORT_DURATION.getValue(), SECONDS) //
						.output(STATE_MACHINE, State.SUPPORT_DURATION))
				.next(new TestCase("34") //
						.output(STATE_MACHINE, State.DEACTIVATION_TIME).output("ess0", SET_ACTIVE_POWER_EQUALS, 0))
				.next(new TestCase("35") //
						.timeleap(clock, LONG_ACTIVATION_RUN.getValue(), MILLIS)) //
				.next(new TestCase("36") //
						.output(STATE_MACHINE, State.BUFFERED_TIME_BEFORE_RECOVERY) //
						.output("ess0", SET_ACTIVE_POWER_EQUALS, 0))
				.next(new TestCase("37") //
						.output(STATE_MACHINE, State.BUFFERED_TIME_BEFORE_RECOVERY))
				.next(new TestCase("38") //
						.output(STATE_MACHINE, State.BUFFERED_TIME_BEFORE_RECOVERY))
				.next(new TestCase("39") //
						.timeleap(clock, LONG_SUPPORT_DURATION.getValue(), MINUTES))
				.next(new TestCase("40") //
						.input("meter0", FREQUENCY, 50000)//
						.output(STATE_MACHINE, State.BUFFERED_TIME_BEFORE_RECOVERY))
				.next(new TestCase("41") //
						.input("meter0", FREQUENCY, 50000)//
						.output(STATE_MACHINE, State.RECOVERY_TIME))
				.next(new TestCase("42") //
						.timeleap(clock, 1, DAYS)) //
				.deactivate();
	}

	@Test
	public void testInvalidJsonSchedule() throws Exception {
		final var clock = createDummyClock();
		new ControllerTest(new ControllerFastFrequencyReserveImpl()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", new DummyManagedSymmetricEss("ess0") //
						.withGridMode(GridMode.ON_GRID)//
						.withMaxApparentPower(92000)//
						.withAllowedChargePower(-92000)//
						.withAllowedDischargePower(92000)) //
				.addReference("meter", new DummyElectricityMeter("meter0")) //
				.activate(MyConfig.create() //
						.setEssId("ess0") //
						.setId("ctrl0") //
						.setMeterId("meter0") //
						.setMode(ControlMode.MANUAL_ON) //
						.setPreActivationTime(15)//
						.setactivationScheduleJson("foo")//
						.build()) //
				.deactivate();
	}

	@Test
	public void testInvalidJsonSchedule1() throws Exception {
		final var clock = createDummyClock();
		new ControllerTest(new ControllerFastFrequencyReserveImpl()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", new DummyManagedSymmetricEss("ess0") //
						.withGridMode(GridMode.ON_GRID)//
						.withMaxApparentPower(92000)//
						.withAllowedChargePower(-92000)//
						.withAllowedDischargePower(92000)) //
				.addReference("meter", new DummyElectricityMeter("meter0")) //
				.activate(MyConfig.create() //
						.setEssId("ess0") //
						.setId("ctrl0") //
						.setMeterId("meter0") //
						.setMode(ControlMode.MANUAL_ON) //
						.setPreActivationTime(15)//
						.setactivationScheduleJson("[foo]")//
						.build()) //
				.deactivate();
	}

	public static final String MY_JSON = """
			[
			   {
			      "startTimestamp":"1701738000",
			      "duration":"10800",
			      "dischargePowerSetPoint":"92000",
			      "frequencyLimit":"50000",
			      "activationRunTime":"LONG_ACTIVATION_RUN",
			      "supportDuration":"LONG_SUPPORT_DURATION"
			   },
			   {
			      "startTimestamp":"1701738000",
			      "duration":"10800",
			      "dischargePowerSetPoint":"92000",
			      "frequencyLimit":"50000",
			      "activationRunTime":"LONG_ACTIVATION_RUN",
			      "supportDuration":"LONG_SUPPORT_DURATION"
			   },
			   {
			      "startTimestamp":"1701752400",
			      "duration":"10800",
			      "dischargePowerSetPoint":"92000",
			      "frequencyLimit":"50000",
			      "activationRunTime":"LONG_ACTIVATION_RUN",
			      "supportDuration":"LONG_SUPPORT_DURATION"
			   },
			   {
			      "startTimestamp":"1701777600",
			      "duration":"10800",
			      "dischargePowerSetPoint":"92000",
			      "frequencyLimit":"50000",
			      "activationRunTime":"LONG_ACTIVATION_RUN",
			      "supportDuration":"LONG_SUPPORT_DURATION"
			   }
			]
			""";

	@Test
	public void testFromMethod() throws OpenemsNamedException {
		var scheduleArray = JsonUtils.parseToJsonArray(MY_JSON);
		try {
			List<ActivateFastFreqReserveSchedule> scheduleList = ActivateFastFreqReserveSchedule.from(scheduleArray);

			assertEquals(3, scheduleList.size());
			assertTrue(scheduleList.get(0).startTimestamp() <= scheduleList.get(1).startTimestamp());
			assertTrue(scheduleList.get(1).startTimestamp() <= scheduleList.get(2).startTimestamp());

		} catch (OpenemsNamedException e) {
			e.printStackTrace();
		}
	}
}