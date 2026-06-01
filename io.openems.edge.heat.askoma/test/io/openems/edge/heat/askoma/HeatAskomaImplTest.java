package io.openems.edge.heat.askoma;

import static io.openems.common.test.TestUtils.createDummyClock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.modbus.api.ChannelMetaInfo;
import io.openems.edge.bridge.modbus.api.ChannelMetaInfoBit;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.heat.api.Heat;
import io.openems.edge.heat.api.ManagedHeatElement;
import io.openems.edge.heat.askoma.statemachine.StateMachine.State;
import io.openems.edge.meter.api.ElectricityMeter;

class HeatAskomaImplTest {

	private static final int MAX_HEAT_POWER = 10_000;

	@Test
	void testModeOff() throws Exception {
		new ControllerTest(new HeatAskomaImpl()) //
				.addReference("configurationAdmin", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager())//
				.activate(MyConfig.create() //
						.setId("component0") //
						.setModbusId("modbus0") //
						.setReadOnly(false) //
						.setMode(Mode.OFF) //
						.setMaxHeatPower(MAX_HEAT_POWER) //
						.build()) //
				.next(new TestCase() //
						.output(ManagedHeatElement.ChannelId.TARGET_GRID_ACTIVE_POWER, 0) //
						.output(HeatAskoma.ChannelId.STATE_MACHINE, State.OFF)) //
				.deactivate();
	}

	@Test
	void testSchedulerActiveTaskOverridesConfigMode() throws Exception {
		var clock = createDummyClock();
		new ControllerTest(new HeatAskomaImpl()) //
				.addReference("configurationAdmin", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.activate(MyConfig.create() //
						.setId("component0") //
						.setModbusId("modbus0") //
						.setReadOnly(false) //
						.setMode(Mode.OFF) //
						.setJsCalendar(this.jsCalendarTask("00:00:00", "PT2H", Mode.FAST_HEAT)) //
						.setMaxHeatPower(MAX_HEAT_POWER) //
						.build()) //
				.next(new TestCase("Task active at start: FAST_HEAT overrides config") //
						.output(ManagedHeatElement.ChannelId.TARGET_GRID_ACTIVE_POWER, -10_050) //
						.output(HeatAskoma.ChannelId.MODE, ChannelMode.FAST_HEAT) //
						.output(HeatAskoma.ChannelId.STATE_MACHINE, State.FAST_HEAT)) //
				.next(new TestCase("After task duration: fallback to configured OFF") //
						.timeleap(clock, 2, ChronoUnit.HOURS) //
						.output(ManagedHeatElement.ChannelId.TARGET_GRID_ACTIVE_POWER, 0) //
						.output(HeatAskoma.ChannelId.MODE, ChannelMode.OFF) //
						.output(HeatAskoma.ChannelId.STATE_MACHINE, State.OFF)) //
				.deactivate();
	}

	@Test
	void testSchedulerInactiveTaskFallsBackToConfigMode() throws Exception {
		var clock = createDummyClock();
		new ControllerTest(new HeatAskomaImpl()) //
				.addReference("configurationAdmin", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.activate(MyConfig.create() //
						.setId("component0") //
						.setModbusId("modbus0") //
						.setReadOnly(false) //
						.setMode(Mode.OFF) //
						.setJsCalendar(this.jsCalendarTask("01:00:00", "PT1H", Mode.FAST_HEAT)) //
						.setMaxHeatPower(MAX_HEAT_POWER) //
						.build()) //
				.next(new TestCase("Before task start: configured OFF is active") //
						.output(ManagedHeatElement.ChannelId.TARGET_GRID_ACTIVE_POWER, 0) //
						.output(HeatAskoma.ChannelId.MODE, ChannelMode.OFF) //
						.output(HeatAskoma.ChannelId.STATE_MACHINE, State.OFF)) //
				.next(new TestCase("Task start reached: switch to FAST_HEAT") //
						.timeleap(clock, 1, ChronoUnit.HOURS) //
						.output(ManagedHeatElement.ChannelId.TARGET_GRID_ACTIVE_POWER, -10_050) //
						.output(HeatAskoma.ChannelId.MODE, ChannelMode.FAST_HEAT) //
						.output(HeatAskoma.ChannelId.STATE_MACHINE, State.FAST_HEAT)) //
				.next(new TestCase("Task ended: switch back to configured OFF") //
						.timeleap(clock, 1, ChronoUnit.HOURS) //
						.output(ManagedHeatElement.ChannelId.TARGET_GRID_ACTIVE_POWER, 0) //
						.output(HeatAskoma.ChannelId.MODE, ChannelMode.OFF) //
						.output(HeatAskoma.ChannelId.STATE_MACHINE, State.OFF)) //
				.deactivate();
	}

	@Test
	void testModeFastHeatContinueWhenTargetTemperatureReached() throws Exception {
		final var configurationAdmin = new DummyConfigurationAdmin();
		new ControllerTest(new HeatAskomaImpl()) //
				.addReference("configurationAdmin", configurationAdmin) //
				.addReference("componentManager", new DummyComponentManager(createDummyClock())) //
				.activate(MyConfig.create() //
						.setId("component0") //
						.setModbusId("modbus0") //
						.setReadOnly(false) //
						.setMode(Mode.FAST_HEAT) //
						.setMaxHeatPower(MAX_HEAT_POWER) //
						.build()) //
				// Actual (50.0 °C) < Target (60.0 °C) → keep heating at maximum power
				.next(new TestCase("Actual below target: keep heating") //
						.input(Heat.ChannelId.TEMPERATURE, 500) // 50.0 °C in deci-degree
						.input(HeatAskoma.ChannelId.TEMPERATURE_SETPOINT, 600) // 60.0 °C in deci-degree
						.output(ManagedHeatElement.ChannelId.TARGET_GRID_ACTIVE_POWER, -10_050) //
						.output(HeatAskoma.ChannelId.STATE_MACHINE, State.FAST_HEAT)) //
				// Actual (60.0 °C) >= Target (60.0 °C) → continue heating
				.next(new TestCase("Actual reaches target: continue heating") //
						.input(Heat.ChannelId.TEMPERATURE, 600) // 60.0 °C in deci-degree
						.input(HeatAskoma.ChannelId.TEMPERATURE_SETPOINT, 600) // 60.0 °C in deci-degree
						.output(HeatAskoma.ChannelId.STATE_MACHINE, State.FAST_HEAT) //
						.onAfterControllersCallbacks(() -> { //
							var config = configurationAdmin.getOrCreateEmptyConfiguration("component0"); //
							assertEquals(Mode.FAST_HEAT.name(), config.getProperties().get("mode").toString()); //
						})) //
				.deactivate(); //
	}

	@Test
	void testReadOnlyModeFastHeat() throws Exception {
		var clock = createDummyClock();
		new ControllerTest(new HeatAskomaImpl()) //
				.addReference("configurationAdmin", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.activate(MyConfig.create() //
						.setId("component0") //
						.setModbusId("modbus0") //
						.setReadOnly(true) //
						.setMode(Mode.FAST_HEAT) //
						.setJsCalendar(this.jsCalendarTask("00:00:00", "PT12H", Mode.OFF)) //
						.setMaxHeatPower(MAX_HEAT_POWER) //
						.build()) //
				.next(new TestCase("read-only: no control, MODE reflects configured mode, scheduler ignored") //
						.output(ManagedHeatElement.ChannelId.TARGET_GRID_ACTIVE_POWER, null) //
						.output(ManagedHeatElement.ChannelId.CONTROL_NOT_ALLOWED, true) //
						.output(HeatAskoma.ChannelId.MODE, ChannelMode.FAST_HEAT)) //
				.deactivate();
	}

	@Test
	void testFastHeatStopsWhenExpired() throws Exception {
		var clock = createDummyClock();
		final var configurationAdmin = new DummyConfigurationAdmin();
		new ControllerTest(new HeatAskomaImpl()) //
				.addReference("configurationAdmin", configurationAdmin) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.activate(MyConfig.create() //
						.setId("component0") //
						.setModbusId("modbus0") //
						.setReadOnly(false) //
						.setMode(Mode.FAST_HEAT) //
						.setMaxHeatPower(MAX_HEAT_POWER) //
						.build()) //
				// Actual (50.0 °C) < target (60.0 °C) → keep heating at maximum power
				.next(new TestCase("Actual below target: keep heating at max power") //
						.input(Heat.ChannelId.TEMPERATURE, 500) // 50.0 °C in deci-degree
						.input(HeatAskoma.ChannelId.TEMPERATURE_SETPOINT, 600) // 60.0 °C in deci-degree
						.output(ManagedHeatElement.ChannelId.TARGET_GRID_ACTIVE_POWER, -10_050) //
						.output(HeatAskoma.ChannelId.STATE_MACHINE, State.FAST_HEAT)) //
				// Advance clock past FAST_HEAT_DURATION (10 hours) → fast heat expires, mode
				.next(new TestCase("fast heat expired: enter safety lockout and stop heating") //
						.timeleap(clock, 10, ChronoUnit.HOURS) //
						.input(Heat.ChannelId.TEMPERATURE, 500) // still below target
						.input(HeatAskoma.ChannelId.TEMPERATURE_SETPOINT, 600) //
						.output(ManagedHeatElement.ChannelId.TARGET_GRID_ACTIVE_POWER, 0) //
						.output(HeatAskoma.ChannelId.STATE_MACHINE, State.FAST_HEAT_PAUSE) //
						.onAfterControllersCallbacks(() -> { //
							var config = configurationAdmin.getOrCreateEmptyConfiguration("component0"); //
							assertEquals(Mode.FAST_HEAT.name(), config.getProperties().get("mode").toString()); //
						})) //
				.next(new TestCase("within pause: heating stays off") //
						.input(Heat.ChannelId.TEMPERATURE, 500) //
						.output(ManagedHeatElement.ChannelId.TARGET_GRID_ACTIVE_POWER, 0) //
						.output(HeatAskoma.ChannelId.STATE_MACHINE, State.FAST_HEAT_PAUSE)) //
				.next(new TestCase("pause expired: heating restarts") //
						.timeleap(clock, 1, ChronoUnit.HOURS) //
						.input(Heat.ChannelId.TEMPERATURE, 500) //
						.output(ManagedHeatElement.ChannelId.TARGET_GRID_ACTIVE_POWER, -10_050) //
						.output(HeatAskoma.ChannelId.STATE_MACHINE, State.FAST_HEAT)) //
				// Second cycle: another 10h heat window expires and enters pause again
				.next(new TestCase("second cycle: heat window expires again") //
						.timeleap(clock, 10, ChronoUnit.HOURS) //
						.input(Heat.ChannelId.TEMPERATURE, 500) //
						.output(ManagedHeatElement.ChannelId.TARGET_GRID_ACTIVE_POWER, 0) //
						.output(HeatAskoma.ChannelId.STATE_MACHINE, State.FAST_HEAT_PAUSE)) //
				.next(new TestCase("second cycle: within pause, heating stays off") //
						.input(Heat.ChannelId.TEMPERATURE, 500) //
						.output(ManagedHeatElement.ChannelId.TARGET_GRID_ACTIVE_POWER, 0) //
						.output(HeatAskoma.ChannelId.STATE_MACHINE, State.FAST_HEAT_PAUSE)) //
				.next(new TestCase("second cycle: pause expired, heating restarts again") //
						.timeleap(clock, 1, ChronoUnit.HOURS) //
						.input(Heat.ChannelId.TEMPERATURE, 500) //
						.output(ManagedHeatElement.ChannelId.TARGET_GRID_ACTIVE_POWER, -10_050) //
						.output(HeatAskoma.ChannelId.STATE_MACHINE, State.FAST_HEAT)) //
				.deactivate(); //
	}

	@Test
	void testFastHeatPowerNotAppliedWarningDelayAndReset() throws Exception {
		var clock = createDummyClock();
		new ControllerTest(new HeatAskomaImpl()) //
				.addReference("configurationAdmin", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.activate(MyConfig.create() //
						.setId("component0") //
						.setModbusId("modbus0") //
						.setReadOnly(false) //
						.setMode(Mode.OFF) //
						.setJsCalendar(this.jsCalendarTask("00:00:00", "PT10M", Mode.FAST_HEAT)) //
						.setMaxHeatPower(MAX_HEAT_POWER) //
						.build()) //
				.next(new TestCase("FAST_HEAT started: warning is initially false") //
						.output(HeatAskoma.ChannelId.MODE, ChannelMode.FAST_HEAT) //
						.output(HeatAskoma.ChannelId.FAST_HEAT_POWER_NOT_APPLIED, false)) //
				.next(new TestCase("before 5 minutes elapsed: warning stays false") //
						.timeleap(clock, 4, ChronoUnit.MINUTES) //
						.output(HeatAskoma.ChannelId.MODE, ChannelMode.FAST_HEAT) //
						.output(HeatAskoma.ChannelId.FAST_HEAT_POWER_NOT_APPLIED, false)) //
				.next(new TestCase("after 5 minutes without response: warning turns true") //
						.timeleap(clock, 1, ChronoUnit.MINUTES) //
						.output(HeatAskoma.ChannelId.MODE, ChannelMode.FAST_HEAT) //
						.output(HeatAskoma.ChannelId.FAST_HEAT_POWER_NOT_APPLIED, true)) //
				.next(new TestCase("active power response available: warning resets immediately") //
						.input(ElectricityMeter.ChannelId.ACTIVE_POWER, 1000) //
						.output(HeatAskoma.ChannelId.MODE, ChannelMode.FAST_HEAT) //
						.output(HeatAskoma.ChannelId.FAST_HEAT_POWER_NOT_APPLIED, false)) //
				.next(new TestCase("task ended -> OFF: warning stays reset") //
						.timeleap(clock, 6, ChronoUnit.MINUTES) //
						.output(HeatAskoma.ChannelId.MODE, ChannelMode.OFF) //
						.output(HeatAskoma.ChannelId.STATE_MACHINE, State.OFF) //
						.output(HeatAskoma.ChannelId.FAST_HEAT_POWER_NOT_APPLIED, false)) //
				.deactivate();
	}

	@Test
	void testDefineModbusProtocolReadOnly() throws Exception {
		var sut = new HeatAskomaImpl();
		new ComponentTest(sut) //
				.addReference("configurationAdmin", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager())//
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("component0") //
						.setModbusId("modbus0") //
						.setReadOnly(true) //
						.build());

		var tasks = sut.defineModbusProtocol().getTaskManager().getTasks();

		assertEquals(3, tasks.size());
		assertTrue(tasks.stream().anyMatch(t -> t instanceof FC4ReadInputRegistersTask && t.getStartAddress() == 109));
		assertTrue(tasks.stream().anyMatch(t -> t instanceof FC3ReadRegistersTask && t.getStartAddress() == 597));
		assertTrue(tasks.stream().anyMatch(t -> t instanceof FC4ReadInputRegistersTask && t.getStartAddress() == 638));
		assertFalse(tasks.stream().anyMatch(t -> t instanceof FC3ReadRegistersTask && t.getStartAddress() == 202));
		assertFalse(tasks.stream().anyMatch(t -> t instanceof FC6WriteRegisterTask && t.getStartAddress() == 202));

		assertEquals(//
				new ChannelMetaInfo(597), //
				sut.channel(HeatAskoma.ChannelId.TEMPERATURE_SETPOINT).getMetaInfo());
		assertEquals(//
				new ChannelMetaInfo(638), //
				sut.channel(Heat.ChannelId.TEMPERATURE).getMetaInfo());
		assertNull(sut.channel(ManagedHeatElement.ChannelId.TARGET_GRID_ACTIVE_POWER).getMetaInfo());
		this.assertBitsRegister109Mapping(sut);
	}

	@Test
	void testDefineModbusProtocolNotReadOnly() throws Exception {
		var sut = new HeatAskomaImpl();
		new ComponentTest(sut) //
				.addReference("configurationAdmin", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager())//
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("component0") //
						.setModbusId("modbus0") //
						.setReadOnly(false) //
						.build());

		var tasks = sut.defineModbusProtocol().getTaskManager().getTasks();

		assertEquals(5, tasks.size());
		assertTrue(tasks.stream().anyMatch(t -> t instanceof FC4ReadInputRegistersTask && t.getStartAddress() == 109));
		assertTrue(tasks.stream().anyMatch(t -> t instanceof FC3ReadRegistersTask && t.getStartAddress() == 597));
		assertTrue(tasks.stream().anyMatch(t -> t instanceof FC4ReadInputRegistersTask && t.getStartAddress() == 638));
		assertTrue(tasks.stream().anyMatch(t -> t instanceof FC3ReadRegistersTask && t.getStartAddress() == 202));
		assertTrue(tasks.stream().anyMatch(t -> t instanceof FC6WriteRegisterTask && t.getStartAddress() == 202));

		assertEquals(//
				new ChannelMetaInfo(597), //
				sut.channel(HeatAskoma.ChannelId.TEMPERATURE_SETPOINT).getMetaInfo());
		assertEquals(//
				new ChannelMetaInfo(638), //
				sut.channel(Heat.ChannelId.TEMPERATURE).getMetaInfo());
		assertEquals(//
				new ChannelMetaInfo(202), //
				sut.channel(ManagedHeatElement.ChannelId.TARGET_GRID_ACTIVE_POWER).getMetaInfo());
		this.assertBitsRegister109Mapping(sut);
	}

	private void assertBitsRegister109Mapping(HeatAskomaImpl sut) {
		assertEquals(//
				new ChannelMetaInfoBit(109, 0), //
				sut.channel(HeatAskoma.ChannelId.HEATER1_ACTIVE).getMetaInfo());
		assertEquals(//
				new ChannelMetaInfoBit(109, 1), //
				sut.channel(HeatAskoma.ChannelId.HEATER2_ACTIVE).getMetaInfo());
		assertEquals(//
				new ChannelMetaInfoBit(109, 2), //
				sut.channel(HeatAskoma.ChannelId.HEATER3_ACTIVE).getMetaInfo());
		assertEquals(//
				new ChannelMetaInfoBit(109, 3), //
				sut.channel(HeatAskoma.ChannelId.PUMP_ACTIVE).getMetaInfo());
		assertEquals(//
				new ChannelMetaInfoBit(109, 4), //
				sut.channel(HeatAskoma.ChannelId.RELAYBOARD_IS_CONNECTED).getMetaInfo());
		assertEquals(//
				new ChannelMetaInfoBit(109, 5), //
				sut.channel(HeatAskoma.ChannelId.HEATER_1_2_3_CURRENT_FLOW).getMetaInfo());
		assertEquals(//
				new ChannelMetaInfoBit(109, 6), //
				sut.channel(HeatAskoma.ChannelId.HEAT_PUMP_REQUEST_ACTIVE).getMetaInfo());
		assertEquals(//
				new ChannelMetaInfoBit(109, 7), //
				sut.channel(HeatAskoma.ChannelId.EMERGENCY_MODE_ACTIVE).getMetaInfo());
		assertEquals(//
				new ChannelMetaInfoBit(109, 8), //
				sut.channel(HeatAskoma.ChannelId.LEGIONELLA_PROTECTION_ACTIVE).getMetaInfo());
		assertEquals(//
				new ChannelMetaInfoBit(109, 9), //
				sut.channel(HeatAskoma.ChannelId.ANALOG_INPUT_ACTIVE).getMetaInfo());
		assertEquals(//
				new ChannelMetaInfoBit(109, 10), //
				sut.channel(HeatAskoma.ChannelId.LOAD_SETPOINT_ACTIVE).getMetaInfo());
		assertEquals(//
				new ChannelMetaInfoBit(109, 11), //
				sut.channel(HeatAskoma.ChannelId.LOAD_FEEDIN_ACTIVE).getMetaInfo());
		assertEquals(//
				new ChannelMetaInfoBit(109, 12), //
				sut.channel(HeatAskoma.ChannelId.AUTO_HEATER_OFF_ACTIVE).getMetaInfo());
		assertEquals(//
				new ChannelMetaInfoBit(109, 13), //
				sut.channel(HeatAskoma.ChannelId.PUMP_RELAY_FOLLOW_UP_ACTIVE).getMetaInfo());
		assertEquals(//
				new ChannelMetaInfoBit(109, 14), //
				sut.channel(HeatAskoma.ChannelId.TEMPERATURE_LIMIT_REACHED).getMetaInfo());
		assertEquals(//
				new ChannelMetaInfoBit(109, 15), //
				sut.channel(HeatAskoma.ChannelId.ANY_ERROR_OCCURRED).getMetaInfo());
	}

	private String jsCalendarTask(String start, String duration, Mode mode) {
		return """
				[
					{
						"@type":"Task",
						"start":"%s",
						"duration":"%s",
						"recurrenceRules":[{"frequency":"daily"}],
						"openems.io:payload":{
							"mode":"%s"
						}
					}
				]
				""".formatted(start, duration, mode.name());
	}

}
