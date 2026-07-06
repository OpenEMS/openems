package io.openems.edge.heat.mypv;

import static io.openems.common.test.TestUtils.createDummyClock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.modbus.api.ChannelMetaInfo;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.heat.api.ManagedHeatElement;
import io.openems.edge.heat.mypv.statemachine.StateMachine.State;
import io.openems.edge.meter.api.ElectricityMeter;

class HeatMyPvImplTest {

	private static final int MAX_HEAT_POWER = 3_000;

	@Test
	void testReadOnlyActivation() throws Exception {
		new ComponentTest(new HeatMyPvImpl()) //
				.addReference("configurationAdmin", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("component0") //
						.setModbusId("modbus0") //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}

	@Test
	void testWritableActivation() throws Exception {
		new ComponentTest(new HeatMyPvImpl()) //
				.addReference("configurationAdmin", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("component0") //
						.setModbusId("modbus0") //
						.setReadOnly(false) //
						.setMaxHeatPower(MAX_HEAT_POWER) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}

	@Test
	void testReadOnlyControlNotAllowed() throws Exception {
		new ControllerTest(new HeatMyPvImpl()) //
				.addReference("configurationAdmin", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId("component0") //
						.setModbusId("modbus0") //
						.setReadOnly(true) //
						.build()) //
				.next(new TestCase() //
						.output(ManagedHeatElement.ChannelId.CONTROL_NOT_ALLOWED, true)) //
				.deactivate();
	}

	@Test
	void testWritableControlAllowed() throws Exception {
		new ControllerTest(new HeatMyPvImpl()) //
				.addReference("configurationAdmin", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId("component0") //
						.setModbusId("modbus0") //
						.setReadOnly(false) //
						.setMaxHeatPower(MAX_HEAT_POWER) //
						.build()) //
				.next(new TestCase() //
						.output(ManagedHeatElement.ChannelId.CONTROL_NOT_ALLOWED, false)) //
				.deactivate();
	}

	@Test
	void testTargetActivePowerClamping() throws Exception {
		var sut = new HeatMyPvImpl();
		new ComponentTest(sut) //
				.addReference("configurationAdmin", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("component0") //
						.setModbusId("modbus0") //
						.setReadOnly(false) //
						.setMaxHeatPower(MAX_HEAT_POWER) //
						.build());

		IntegerWriteChannel channel = sut.channel(ManagedHeatElement.ChannelId.TARGET_ACTIVE_POWER);

		sut.setTargetActivePowerForHeatElement(-1);
		assertEquals(0, channel.getNextWriteValue().get());

		sut.setTargetActivePowerForHeatElement(1_500);
		assertEquals(1_500, channel.getNextWriteValue().get());

		sut.setTargetActivePowerForHeatElement(4_000);
		assertEquals(MAX_HEAT_POWER, channel.getNextWriteValue().get());

		sut.setTargetActivePowerForHeatElement(null);
		assertEquals(0, channel.getNextWriteValue().get());

		sut.deactivate();
	}

	@Test
	void testDefineModbusProtocolReadOnly() throws Exception {
		var sut = new HeatMyPvImpl();
		new ComponentTest(sut) //
				.addReference("configurationAdmin", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("component0") //
						.setModbusId("modbus0") //
						.setReadOnly(true) //
						.build());

		var tasks = sut.defineModbusProtocol().getTaskManager().getTasks();

		assertEquals(1, tasks.size());
		assertTrue(tasks.stream().anyMatch(t -> t instanceof FC3ReadRegistersTask && t.getStartAddress() == 1000));
		assertFalse(tasks.stream().anyMatch(t -> t instanceof FC6WriteRegisterTask && t.getStartAddress() == 1000));
		assertNull(sut.channel(ManagedHeatElement.ChannelId.TARGET_ACTIVE_POWER).getMetaInfo());

		sut.deactivate();
	}

	@Test
	void testDefineModbusProtocolNotReadOnly() throws Exception {
		var sut = new HeatMyPvImpl();
		new ComponentTest(sut) //
				.addReference("configurationAdmin", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("component0") //
						.setModbusId("modbus0") //
						.setReadOnly(false) //
						.setMaxHeatPower(MAX_HEAT_POWER) //
						.build());

		var tasks = sut.defineModbusProtocol().getTaskManager().getTasks();

		assertEquals(2, tasks.size());
		assertTrue(tasks.stream().anyMatch(t -> t instanceof FC3ReadRegistersTask && t.getStartAddress() == 1000));
		assertTrue(tasks.stream().anyMatch(t -> t instanceof FC6WriteRegisterTask && t.getStartAddress() == 1000));
		assertEquals(//
				new ChannelMetaInfo(1000), //
				sut.channel(ManagedHeatElement.ChannelId.TARGET_ACTIVE_POWER).getMetaInfo());

		sut.deactivate();
	}

	@Test
	void testModeOff() throws Exception {
		new ControllerTest(new HeatMyPvImpl()) //
				.addReference("configurationAdmin", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.activate(MyConfig.create() //
						.setId("component0") //
						.setModbusId("modbus0") //
						.setReadOnly(false) //
						.setMode(Mode.OFF) //
						.setMaxHeatPower(MAX_HEAT_POWER) //
						.build()) //
				.next(new TestCase() //
						.output(ManagedHeatElement.ChannelId.TARGET_ACTIVE_POWER, 0) //
						.output(HeatMyPv.ChannelId.STATE_MACHINE, State.OFF)) //
				.deactivate();
	}

	@Test
	void testModeFastHeat() throws Exception {
		new ControllerTest(new HeatMyPvImpl()) //
				.addReference("configurationAdmin", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(createDummyClock())) //
				.activate(MyConfig.create() //
						.setId("component0") //
						.setModbusId("modbus0") //
						.setReadOnly(false) //
						.setMode(Mode.FAST_HEAT) //
						.setMaxHeatPower(MAX_HEAT_POWER) //
						.build()) //
				.next(new TestCase() //
						.output(ManagedHeatElement.ChannelId.TARGET_ACTIVE_POWER, 3_000) //
						.output(HeatMyPv.ChannelId.STATE_MACHINE, State.FAST_HEAT) //
						.output(HeatMyPv.ChannelId.MODE, ChannelMode.FAST_HEAT)) //
				.deactivate();
	}

	@Test
	void testSchedulerActiveTaskOverridesConfigMode() throws Exception {
		var clock = createDummyClock();
		new ControllerTest(new HeatMyPvImpl()) //
				.addReference("configurationAdmin", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.activate(MyConfig.create() //
						.setId("component0") //
						.setModbusId("modbus0") //
						.setReadOnly(false) //
						.setMode(Mode.OFF) //
						.setJsCalendar(jsCalendarTask("00:00:00", "PT2H", Mode.FAST_HEAT)) //
						.setMaxHeatPower(MAX_HEAT_POWER) //
						.build()) //
				.next(new TestCase("Task active at start: FAST_HEAT overrides config") //
						.output(ManagedHeatElement.ChannelId.TARGET_ACTIVE_POWER, 3_000) //
						.output(HeatMyPv.ChannelId.MODE, ChannelMode.FAST_HEAT) //
						.output(HeatMyPv.ChannelId.STATE_MACHINE, State.FAST_HEAT)) //
				.next(new TestCase("After task duration: fallback to configured OFF") //
						.timeleap(clock, 2, ChronoUnit.HOURS) //
						.output(ManagedHeatElement.ChannelId.TARGET_ACTIVE_POWER, 0) //
						.output(HeatMyPv.ChannelId.MODE, ChannelMode.OFF) //
						.output(HeatMyPv.ChannelId.STATE_MACHINE, State.OFF)) //
				.deactivate();
	}

	@Test
	void testSchedulerInactiveTaskFallsBackToConfigMode() throws Exception {
		var clock = createDummyClock();
		new ControllerTest(new HeatMyPvImpl()) //
				.addReference("configurationAdmin", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.activate(MyConfig.create() //
						.setId("component0") //
						.setModbusId("modbus0") //
						.setReadOnly(false) //
						.setMode(Mode.OFF) //
						.setJsCalendar(jsCalendarTask("01:00:00", "PT1H", Mode.FAST_HEAT)) //
						.setMaxHeatPower(MAX_HEAT_POWER) //
						.build()) //
				.next(new TestCase("Before task start: configured OFF is active") //
						.output(ManagedHeatElement.ChannelId.TARGET_ACTIVE_POWER, 0) //
						.output(HeatMyPv.ChannelId.MODE, ChannelMode.OFF) //
						.output(HeatMyPv.ChannelId.STATE_MACHINE, State.OFF)) //
				.next(new TestCase("Task start reached: switch to FAST_HEAT") //
						.timeleap(clock, 1, ChronoUnit.HOURS) //
						.output(ManagedHeatElement.ChannelId.TARGET_ACTIVE_POWER, 3_000) //
						.output(HeatMyPv.ChannelId.MODE, ChannelMode.FAST_HEAT) //
						.output(HeatMyPv.ChannelId.STATE_MACHINE, State.FAST_HEAT)) //
				.next(new TestCase("Task ended: switch back to configured OFF") //
						.timeleap(clock, 1, ChronoUnit.HOURS) //
						.output(ManagedHeatElement.ChannelId.TARGET_ACTIVE_POWER, 0) //
						.output(HeatMyPv.ChannelId.MODE, ChannelMode.OFF) //
						.output(HeatMyPv.ChannelId.STATE_MACHINE, State.OFF)) //
				.deactivate();
	}

	@Test
	void testReadOnlyModeFastHeat() throws Exception {
		var clock = createDummyClock();
		new ControllerTest(new HeatMyPvImpl()) //
				.addReference("configurationAdmin", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.activate(MyConfig.create() //
						.setId("component0") //
						.setModbusId("modbus0") //
						.setReadOnly(true) //
						.setMode(Mode.FAST_HEAT) //
						.setJsCalendar(jsCalendarTask("00:00:00", "PT12H", Mode.OFF)) //
						.setMaxHeatPower(MAX_HEAT_POWER) //
						.build()) //
				.next(new TestCase("read-only: no control, MODE reflects configured mode, scheduler ignored") //
						.output(ManagedHeatElement.ChannelId.TARGET_ACTIVE_POWER, null) //
						.output(ManagedHeatElement.ChannelId.CONTROL_NOT_ALLOWED, true) //
						.output(HeatMyPv.ChannelId.MODE, ChannelMode.FAST_HEAT)) //
				.deactivate();
	}

	@Test
	void testFastHeatStopsWhenExpired() throws Exception {
		var clock = createDummyClock();
		new ControllerTest(new HeatMyPvImpl()) //
				.addReference("configurationAdmin", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.activate(MyConfig.create() //
						.setId("component0") //
						.setModbusId("modbus0") //
						.setReadOnly(false) //
						.setMode(Mode.FAST_HEAT) //
						.setMaxHeatPower(MAX_HEAT_POWER) //
						.build()) //
				.next(new TestCase("Initial: heating at max power") //
						.output(ManagedHeatElement.ChannelId.TARGET_ACTIVE_POWER, 3_000) //
						.output(HeatMyPv.ChannelId.STATE_MACHINE, State.FAST_HEAT)) //
				.next(new TestCase("fast heat expired: enter safety pause and stop heating") //
						.timeleap(clock, 10, ChronoUnit.HOURS) //
						.output(ManagedHeatElement.ChannelId.TARGET_ACTIVE_POWER, 0) //
						.output(HeatMyPv.ChannelId.STATE_MACHINE, State.FAST_HEAT_PROTECTION_PAUSE)) //
				.next(new TestCase("within pause: heating stays off") //
						.output(ManagedHeatElement.ChannelId.TARGET_ACTIVE_POWER, 0) //
						.output(HeatMyPv.ChannelId.STATE_MACHINE, State.FAST_HEAT_PROTECTION_PAUSE)) //
				.next(new TestCase("pause expired: heating restarts") //
						.timeleap(clock, 1, ChronoUnit.HOURS) //
						.output(ManagedHeatElement.ChannelId.TARGET_ACTIVE_POWER, 3_000) //
						.output(HeatMyPv.ChannelId.STATE_MACHINE, State.FAST_HEAT)) //
				.next(new TestCase("second cycle: heat window expires again") //
						.timeleap(clock, 10, ChronoUnit.HOURS) //
						.output(ManagedHeatElement.ChannelId.TARGET_ACTIVE_POWER, 0) //
						.output(HeatMyPv.ChannelId.STATE_MACHINE, State.FAST_HEAT_PROTECTION_PAUSE)) //
				.next(new TestCase("second cycle: within pause, heating stays off") //
						.output(ManagedHeatElement.ChannelId.TARGET_ACTIVE_POWER, 0) //
						.output(HeatMyPv.ChannelId.STATE_MACHINE, State.FAST_HEAT_PROTECTION_PAUSE)) //
				.next(new TestCase("second cycle: pause expired, heating restarts again") //
						.timeleap(clock, 1, ChronoUnit.HOURS) //
						.output(ManagedHeatElement.ChannelId.TARGET_ACTIVE_POWER, 3_000) //
						.output(HeatMyPv.ChannelId.STATE_MACHINE, State.FAST_HEAT)) //
				.deactivate();
	}

	@Test
	void testFastHeatPowerNotAppliedWarningDelayAndReset() throws Exception {
		var clock = createDummyClock();
		new ControllerTest(new HeatMyPvImpl()) //
				.addReference("configurationAdmin", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.activate(MyConfig.create() //
						.setId("component0") //
						.setModbusId("modbus0") //
						.setReadOnly(false) //
						.setMode(Mode.OFF) //
						.setJsCalendar(jsCalendarTask("00:00:00", "PT10M", Mode.FAST_HEAT)) //
						.setMaxHeatPower(MAX_HEAT_POWER) //
						.build()) //
				.next(new TestCase("FAST_HEAT started: warning is initially false") //
						.output(HeatMyPv.ChannelId.MODE, ChannelMode.FAST_HEAT) //
						.output(HeatMyPv.ChannelId.FAST_HEAT_POWER_NOT_APPLIED, false)) //
				.next(new TestCase("before 5 minutes elapsed: warning stays false") //
						.timeleap(clock, 4, ChronoUnit.MINUTES) //
						.output(HeatMyPv.ChannelId.MODE, ChannelMode.FAST_HEAT) //
						.output(HeatMyPv.ChannelId.FAST_HEAT_POWER_NOT_APPLIED, false)) //
				.next(new TestCase("after 5 minutes without response: warning turns true") //
						.timeleap(clock, 1, ChronoUnit.MINUTES) //
						.output(HeatMyPv.ChannelId.MODE, ChannelMode.FAST_HEAT) //
						.output(HeatMyPv.ChannelId.FAST_HEAT_POWER_NOT_APPLIED, true)) //
				.next(new TestCase("active power response available: warning resets immediately") //
						.input(ElectricityMeter.ChannelId.ACTIVE_POWER, 1000) //
						.output(HeatMyPv.ChannelId.MODE, ChannelMode.FAST_HEAT) //
						.output(HeatMyPv.ChannelId.FAST_HEAT_POWER_NOT_APPLIED, false)) //
				.next(new TestCase("task ended -> OFF: warning stays reset") //
						.timeleap(clock, 6, ChronoUnit.MINUTES) //
						.output(HeatMyPv.ChannelId.MODE, ChannelMode.OFF) //
						.output(HeatMyPv.ChannelId.STATE_MACHINE, State.OFF) //
						.output(HeatMyPv.ChannelId.FAST_HEAT_POWER_NOT_APPLIED, false)) //
				.deactivate();
	}

	/**
	 * Tests the delayed stable target update in surplus mode.
	 *
	 * <p>
	 * The my-PV device applies a new power target with a delay of about 10 seconds.
	 * To avoid oscillations, the controller calculates the surplus target every
	 * cycle, but updates the stable target only after the delay has elapsed. The
	 * stable target is still written every cycle.
	 *
	 * <p>
	 * Test flow:
	 *
	 * <pre>
	 * t =  0s: 2 kW surplus is available.
	 *          FEMS sends 2 kW target. my-PV still consumes 0 kW.
	 *
	 * t =  5s: Surplus increases strongly, but the delay has not elapsed.
	 *          FEMS keeps writing the previous stable target of 2 kW.
	 *
	 * t = 10s: my-PV has applied the previous 2 kW target.
	 *          The delay has elapsed, so FEMS updates the stable target to max power.
	 *
	 * t = 15s: my-PV has not applied the new max power target yet.
	 *          FEMS keeps writing the stable target of max power.
	 *
	 * t = 20s: my-PV has applied the max power target.
	 *          FEMS keeps writing max power.
	 * </pre>
	 */
	@Test
	void testSurplusModeUpdatesStableTargetOnlyAfterDelay() throws Exception {
		var clock = createDummyClock();
		var beforeUpdateDelay = 5;
		var remainingUpdateDelay = 5;

		new ControllerTest(new HeatMyPvImpl()) //
				.addReference("configurationAdmin", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("sum", new DummySum()) //
				.activate(MyConfig.create() //
						.setId("component0") //
						.setModbusId("modbus0") //
						.setReadOnly(false) //
						.setMode(Mode.SURPLUS) //
						.setMaxHeatPower(MAX_HEAT_POWER) //
						.build()) //
				.next(new TestCase("Cycle1: set initial surplus target") //
						.input(Sum.ChannelId.GRID_ACTIVE_POWER, -2000) //
						.input(ElectricityMeter.ChannelId.ACTIVE_POWER, 0) //
						.output(ManagedHeatElement.ChannelId.TARGET_ACTIVE_POWER, 2000)) //
				.next(new TestCase("Cycle2: before delay elapsed, target remains unchanged") //
						.timeleap(clock, beforeUpdateDelay, ChronoUnit.SECONDS) //
						.input(Sum.ChannelId.GRID_ACTIVE_POWER, -10000) //
						.input(ElectricityMeter.ChannelId.ACTIVE_POWER, 0) //
						.output(ManagedHeatElement.ChannelId.TARGET_ACTIVE_POWER, 2000)) //
				.next(new TestCase("Cycle3: after delay elapsed, stable target updates") //
						.timeleap(clock, remainingUpdateDelay, ChronoUnit.SECONDS) //
						.input(Sum.ChannelId.GRID_ACTIVE_POWER, -8000) //
						.input(ElectricityMeter.ChannelId.ACTIVE_POWER, 2000) //
						.output(ManagedHeatElement.ChannelId.TARGET_ACTIVE_POWER, MAX_HEAT_POWER)) //
				.next(new TestCase("Cycle4: before my-PV applied max power, stable target remains max power") //
						.timeleap(clock, beforeUpdateDelay, ChronoUnit.SECONDS) //
						.input(Sum.ChannelId.GRID_ACTIVE_POWER, -8000) //
						.input(ElectricityMeter.ChannelId.ACTIVE_POWER, 2000) //
						.output(ManagedHeatElement.ChannelId.TARGET_ACTIVE_POWER, MAX_HEAT_POWER)) //
				.next(new TestCase("Cycle5: after my-PV applied max power, stable target remains max power") //
						.timeleap(clock, remainingUpdateDelay, ChronoUnit.SECONDS) //
						.input(Sum.ChannelId.GRID_ACTIVE_POWER, -7000) //
						.input(ElectricityMeter.ChannelId.ACTIVE_POWER, MAX_HEAT_POWER) //
						.output(ManagedHeatElement.ChannelId.TARGET_ACTIVE_POWER, MAX_HEAT_POWER)) //
				.deactivate();
	}

	private static String jsCalendarTask(String start, String duration, Mode mode) {
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
