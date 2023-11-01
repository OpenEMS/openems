package io.openems.edge.battery.fenecon.home;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.function.ThrowingRunnable;
import io.openems.common.test.TimeLeapClock;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.fenecon.home.statemachine.StateMachine;
import io.openems.edge.battery.protection.BatteryProtection;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.io.test.DummyInputOutput;

public class BatteryFeneconHomeImplTest {

	private static final String BATTERY_ID = "battery0";
	private static final String MODBUS_ID = "modbus0";
	private static final String IO_ID = "io0";

	private static final ChannelAddress STATE_MACHINE = new ChannelAddress(BATTERY_ID,
			BatteryFeneconHome.ChannelId.STATE_MACHINE.id());
	private static final ChannelAddress MODBUS_COMMUNICATION_FAILED = new ChannelAddress(BATTERY_ID,
			ModbusComponent.ChannelId.MODBUS_COMMUNICATION_FAILED.id());
	private static final ChannelAddress BMS_CONTROL = new ChannelAddress(BATTERY_ID,
			BatteryFeneconHome.ChannelId.BMS_CONTROL.id());
	private static final ChannelAddress BP_CHARGE_BMS = new ChannelAddress(BATTERY_ID,
			BatteryProtection.ChannelId.BP_CHARGE_BMS.id());
	private static final ChannelAddress MAX_CELL_VOLTAGE = new ChannelAddress(BATTERY_ID,
			Battery.ChannelId.MAX_CELL_VOLTAGE.id());
	private static final ChannelAddress CHARGE_MAX_CURRENT = new ChannelAddress(BATTERY_ID,
			Battery.ChannelId.CHARGE_MAX_CURRENT.id());
	private static final ChannelAddress BATTERY_RELAY = new ChannelAddress(IO_ID, "InputOutput4");

	private static ThrowingRunnable<Exception> assertLog(BatteryFeneconHomeImpl sut, String message) {
		return () -> assertEquals(message, sut.stateMachine.debugLog());
	}

	/**
	 * Battery start up when the relay and battery off test.
	 *
	 * @throws Exception on error
	 */
	@Test
	public void test() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);
		var sut = new BatteryFeneconHomeImpl();
		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.addComponent(new DummyInputOutput(IO_ID))//
				.activate(MyConfig.create() //
						.setId(BATTERY_ID) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(0) //
						.setStartStop(StartStopConfig.START) //
						.setBatteryStartUpRelay("io0/InputOutput4")//
						.build())//
				.next(new TestCase() //
						.inputForce(MODBUS_COMMUNICATION_FAILED, true) //
						.input(BATTERY_RELAY, false) // Switch OFF
						.input(BMS_CONTROL, false) // Switched OFF
						.onBeforeProcessImage(assertLog(sut, "Undefined")) //
						.output(STATE_MACHINE, StateMachine.State.UNDEFINED)) //

				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-Undefined")) //
						.output(STATE_MACHINE, StateMachine.State.GO_RUNNING)) //
				.next(new TestCase()//
						.onBeforeProcessImage(assertLog(sut, "GoRunning-StartUpRelayOn"))) //
				.next(new TestCase()//
						.input(BATTERY_RELAY, true) // Switch ON
						.onBeforeProcessImage(assertLog(sut, "GoRunning-StartUpRelayOn"))) //
				.next(new TestCase()//
						.input(BATTERY_RELAY, true) // Switch ON
						.onBeforeProcessImage(assertLog(sut, "GoRunning-StartUpRelayHold"))
						.onAfterProcessImage(() -> clock.leap(11, ChronoUnit.SECONDS))) //
				.next(new TestCase() //
						.input(BATTERY_RELAY, false) // Switch OFF
						.onBeforeProcessImage(assertLog(sut, "GoRunning-StartUpRelayOff"))) //
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-RetryModbusCommunication"))) //
				.next(new TestCase() //
						.input(BMS_CONTROL, true) // Switched ON
						.onBeforeProcessImage(assertLog(sut, "GoRunning-WaitForBmsControl"))) //
				.next(new TestCase() //
						.input(MODBUS_COMMUNICATION_FAILED, false) //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-WaitForModbusCommunication"))) //

				.next(new TestCase()//
						.onBeforeProcessImage(assertLog(sut, "Running")) //
						.output(STATE_MACHINE, StateMachine.State.RUNNING)) //

				// Ramp-Up ChargeMaxCurrent (0.1 A / Second)
				.next(new TestCase() //
						.input(BP_CHARGE_BMS, 40) //
						.input(MAX_CELL_VOLTAGE, 3000)) //
				.next(new TestCase("Ramp up") //
						.timeleap(clock, 100, ChronoUnit.SECONDS) //
						.output(CHARGE_MAX_CURRENT, 10)) //
				.next(new TestCase() //
						.timeleap(clock, 300, ChronoUnit.SECONDS) //
						.output(CHARGE_MAX_CURRENT, 40))

				// Full Battery
				.next(new TestCase() //
						.input(BP_CHARGE_BMS, 15) //
						.input(MAX_CELL_VOLTAGE, 3400)) //
				.next(new TestCase() //
						.output(CHARGE_MAX_CURRENT, 15)) //
				.next(new TestCase() //
						.input(BP_CHARGE_BMS, 40)) //
				.next(new TestCase() //
						.timeleap(clock, 100, ChronoUnit.SECONDS) //
						.output(CHARGE_MAX_CURRENT, 25)) //
		;
	}

	/**
	 * Battery start up when the relay is on and battery is off test.
	 *
	 * @throws Exception on error
	 */
	@Test
	public void test2() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);
		var sut = new BatteryFeneconHomeImpl();
		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.addComponent(new DummyInputOutput(IO_ID)) //
				.activate(MyConfig.create() //
						.setId(BATTERY_ID) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(0) //
						.setStartStop(StartStopConfig.START) //
						.setBatteryStartUpRelay("io0/InputOutput4")//
						.build())//

				.next(new TestCase()//
						.input(BATTERY_RELAY, true) //
						.input(BMS_CONTROL, false) // Switched Off
						.output(STATE_MACHINE, StateMachine.State.UNDEFINED))//
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-Undefined")) //
						.output(STATE_MACHINE, StateMachine.State.GO_RUNNING)) //
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-StartUpRelayOn")) //
						.output(STATE_MACHINE, StateMachine.State.GO_RUNNING))//
				.next(new TestCase()//
						.input(BATTERY_RELAY, true) // Switch ON
						.onBeforeProcessImage(assertLog(sut, "GoRunning-StartUpRelayHold"))
						.onAfterProcessImage(() -> clock.leap(11, ChronoUnit.SECONDS))) //
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-StartUpRelayOff")) //
						.input(BMS_CONTROL, true) // Switched On
						.output(STATE_MACHINE, StateMachine.State.GO_RUNNING)) //
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-StartUpRelayOff")) //
						.input(BATTERY_RELAY, false) //
						.output(STATE_MACHINE, StateMachine.State.GO_RUNNING)) //
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-RetryModbusCommunication")) //
						.output(STATE_MACHINE, StateMachine.State.GO_RUNNING)) //
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-WaitForBmsControl")) //
						.output(STATE_MACHINE, StateMachine.State.GO_RUNNING)) //
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-WaitForModbusCommunication")) //
						.output(STATE_MACHINE, StateMachine.State.GO_RUNNING)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, StateMachine.State.RUNNING));
	}

	/**
	 * Battery start up when the relay is off and battery has already started,
	 * OpenEMS Edge restarted.
	 *
	 * @throws Exception on error
	 */
	@Test
	public void test3() throws Exception {
		var sut = new BatteryFeneconHomeImpl();
		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.addComponent(new DummyInputOutput(IO_ID))//
				.activate(MyConfig.create() //
						.setId(BATTERY_ID) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(0) //
						.setStartStop(StartStopConfig.START) //
						.setBatteryStartUpRelay("io0/InputOutput4")//
						.build()) //

				.next(new TestCase() //
						.input(BATTERY_RELAY, false) //
						.input(BMS_CONTROL, true) // Switched On
						.output(STATE_MACHINE, StateMachine.State.UNDEFINED))//
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-Undefined")) //
						.output(STATE_MACHINE, StateMachine.State.GO_RUNNING)) //
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-StartUpRelayOff")) //
						.output(STATE_MACHINE, StateMachine.State.GO_RUNNING))//
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-RetryModbusCommunication")) //
						.output(STATE_MACHINE, StateMachine.State.GO_RUNNING))//
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-WaitForBmsControl")) //
						.output(STATE_MACHINE, StateMachine.State.GO_RUNNING)) //
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-WaitForModbusCommunication")) //
						.output(STATE_MACHINE, StateMachine.State.GO_RUNNING)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, StateMachine.State.RUNNING));
	}

	/**
	 * Battery hard switch is off, should stay in GO_RUNNING.
	 *
	 * @throws Exception on error
	 */
	@Test
	public void test4() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);
		var sut = new BatteryFeneconHomeImpl();
		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.addComponent(new DummyInputOutput(IO_ID)) //
				.activate(MyConfig.create() //
						.setId(BATTERY_ID) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(0) //
						.setStartStop(StartStopConfig.START) //
						.setBatteryStartUpRelay("io0/InputOutput4") //
						.build()) //

				.next(new TestCase() //
						.input(BATTERY_RELAY, false) //
						.input(BMS_CONTROL, false) // Switched Off
						.output(STATE_MACHINE, StateMachine.State.UNDEFINED)) //
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-Undefined")) //
						.output(STATE_MACHINE, StateMachine.State.GO_RUNNING)) //
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-StartUpRelayOn")) //
						.output(STATE_MACHINE, StateMachine.State.GO_RUNNING)) //
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-StartUpRelayOn")) //
						.output(STATE_MACHINE, StateMachine.State.GO_RUNNING)) //
				// Ex; after long time if hard switch turned on....
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-StartUpRelayOn")) //
						.input(BMS_CONTROL, true) // Switched On
						.output(STATE_MACHINE, StateMachine.State.GO_RUNNING)) //
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-StartUpRelayOn")) //
						.input(BATTERY_RELAY, true) //
						.output(STATE_MACHINE, StateMachine.State.GO_RUNNING)) //
				.next(new TestCase()//
						.input(BATTERY_RELAY, true) // Switch ON
						.onBeforeProcessImage(assertLog(sut, "GoRunning-StartUpRelayHold"))
						.onAfterProcessImage(() -> clock.leap(11, ChronoUnit.SECONDS))) //
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-StartUpRelayOff")) //
						.output(STATE_MACHINE, StateMachine.State.GO_RUNNING))//
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-StartUpRelayOff")) //
						.output(STATE_MACHINE, StateMachine.State.GO_RUNNING)); //
	}

	@Test
	public void testGetHardwareTypeFromRegisterValue() {
		assertNull(BatteryFeneconHomeImpl.parseHardwareTypeFromRegisterValue(0));

		assertNull(BatteryFeneconHomeImpl.parseHardwareTypeFromRegisterValue(123));

		assertNull(BatteryFeneconHomeImpl.parseHardwareTypeFromRegisterValue(52));

		assertNull(BatteryFeneconHomeImpl.parseHardwareTypeFromRegisterValue(64));

		assertEquals(BatteryFeneconHomeHardwareType.BATTERY_52,
				BatteryFeneconHomeImpl.parseHardwareTypeFromRegisterValue(520));

		assertEquals(BatteryFeneconHomeHardwareType.BATTERY_64,
				BatteryFeneconHomeImpl.parseHardwareTypeFromRegisterValue(640));

	}
}
