package io.openems.edge.battery.fenecon.home;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.battery.api.Battery.ChannelId.CHARGE_MAX_CURRENT;
import static io.openems.edge.battery.api.Battery.ChannelId.CURRENT;
import static io.openems.edge.battery.api.Battery.ChannelId.MAX_CELL_VOLTAGE;
import static io.openems.edge.battery.api.Battery.ChannelId.MIN_CELL_VOLTAGE;
import static io.openems.edge.battery.api.Battery.ChannelId.SOC;
import static io.openems.edge.battery.fenecon.home.BatteryFeneconHome.ChannelId.BMS_CONTROL;
import static io.openems.edge.battery.fenecon.home.BatteryFeneconHome.ChannelId.LOW_MIN_VOLTAGE_FAULT;
import static io.openems.edge.battery.fenecon.home.BatteryFeneconHome.ChannelId.LOW_MIN_VOLTAGE_FAULT_BATTERY_STOPPED;
import static io.openems.edge.battery.fenecon.home.BatteryFeneconHome.ChannelId.LOW_MIN_VOLTAGE_WARNING;
import static io.openems.edge.battery.fenecon.home.BatteryFeneconHome.ChannelId.NUMBER_OF_TOWERS;
import static io.openems.edge.battery.fenecon.home.BatteryFeneconHome.ChannelId.STATE_MACHINE;
import static io.openems.edge.battery.fenecon.home.BatteryFeneconHomeImpl.DEFAULT_CRITICAL_MIN_VOLTAGE;
import static io.openems.edge.battery.fenecon.home.BatteryFeneconHomeImpl.TIMEOUT;
import static io.openems.edge.battery.protection.BatteryProtection.ChannelId.BP_CHARGE_BMS;
import static io.openems.edge.battery.protection.BatteryProtection.ChannelId.BP_CHARGE_MAX_SOC;
import static io.openems.edge.bridge.modbus.api.ModbusComponent.ChannelId.MODBUS_COMMUNICATION_FAILED;
import static io.openems.edge.io.test.DummyInputOutput.ChannelId.INPUT_OUTPUT4;
import static java.lang.Math.round;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import io.openems.common.channel.Unit;
import io.openems.common.function.ThrowingRunnable;
import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.battery.fenecon.home.statemachine.StateMachine.State;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummySerialNumberStorage;
import io.openems.edge.io.test.DummyInputOutput;

public class BatteryFeneconHomeImplTest {

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
		final var clock = createDummyClock();
		var sut = new BatteryFeneconHomeImpl();
		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("serialNumberStorage", new DummySerialNumberStorage()) //
				.addComponent(new DummyInputOutput("io0"))//
				.activate(MyConfig.create() //
						.setId("battery0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(0) //
						.setStartStop(StartStopConfig.START) //
						.setBatteryStartUpRelay("io0/InputOutput4")//
						.build())//
				.next(new TestCase() //
						.inputForce(MODBUS_COMMUNICATION_FAILED, true) //
						.input("io0", INPUT_OUTPUT4, false) // Switch OFF
						.input(BMS_CONTROL, false) // Switched OFF
						.onBeforeProcessImage(assertLog(sut, "Undefined")) //
						.output(STATE_MACHINE, State.UNDEFINED)) //

				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-Undefined")) //
						.output(STATE_MACHINE, State.GO_RUNNING)) //
				.next(new TestCase()//
						.onBeforeProcessImage(assertLog(sut, "GoRunning-StartUpRelayOn"))) //
				.next(new TestCase()//
						.input("io0", INPUT_OUTPUT4, true) // Switch ON
						.onBeforeProcessImage(assertLog(sut, "GoRunning-StartUpRelayOn"))) //
				.next(new TestCase()//
						.input("io0", INPUT_OUTPUT4, true) // Switch ON
						.onBeforeProcessImage(assertLog(sut, "GoRunning-StartUpRelayHold"))
						.onAfterProcessImage(() -> clock.leap(11, SECONDS))) //
				.next(new TestCase() //
						.input("io0", INPUT_OUTPUT4, false) // Switch OFF
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
						.output(STATE_MACHINE, State.RUNNING)) //

				// Ramp-Up ChargeMaxCurrent (0.1 A / Second)
				.next(new TestCase() //
						.input(BP_CHARGE_BMS, 40) //
						.input(MAX_CELL_VOLTAGE, 3000)) //
				.next(new TestCase("Ramp up") //
						.timeleap(clock, 100, SECONDS) //
						.output(CHARGE_MAX_CURRENT, 10)) //
				.next(new TestCase() //
						.timeleap(clock, 300, SECONDS) //
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
						.timeleap(clock, 100, SECONDS) //
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
		final var clock = createDummyClock();
		var sut = new BatteryFeneconHomeImpl();
		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("serialNumberStorage", new DummySerialNumberStorage()) //
				.addComponent(new DummyInputOutput("io0")) //
				.activate(MyConfig.create() //
						.setId("battery0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(0) //
						.setStartStop(StartStopConfig.START) //
						.setBatteryStartUpRelay("io0/InputOutput4")//
						.build())//

				.next(new TestCase()//
						.input("io0", INPUT_OUTPUT4, true) //
						.input(BMS_CONTROL, false) // Switched Off
						.output(STATE_MACHINE, State.UNDEFINED))//
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-Undefined")) //
						.output(STATE_MACHINE, State.GO_RUNNING)) //
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-StartUpRelayOn")) //
						.output(STATE_MACHINE, State.GO_RUNNING))//
				.next(new TestCase()//
						.input("io0", INPUT_OUTPUT4, true) // Switch ON
						.onBeforeProcessImage(assertLog(sut, "GoRunning-StartUpRelayHold"))
						.onAfterProcessImage(() -> clock.leap(11, SECONDS))) //
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-StartUpRelayOff")) //
						.input(BMS_CONTROL, true) // Switched On
						.output(STATE_MACHINE, State.GO_RUNNING)) //
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-StartUpRelayOff")) //
						.input("io0", INPUT_OUTPUT4, false) //
						.output(STATE_MACHINE, State.GO_RUNNING)) //
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-RetryModbusCommunication")) //
						.output(STATE_MACHINE, State.GO_RUNNING)) //
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-WaitForBmsControl")) //
						.output(STATE_MACHINE, State.GO_RUNNING)) //
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-WaitForModbusCommunication")) //
						.output(STATE_MACHINE, State.GO_RUNNING)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.RUNNING));
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
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("serialNumberStorage", new DummySerialNumberStorage()) //
				.addComponent(new DummyInputOutput("io0"))//
				.activate(MyConfig.create() //
						.setId("battery0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(0) //
						.setStartStop(StartStopConfig.START) //
						.setBatteryStartUpRelay("io0/InputOutput4")//
						.build()) //

				.next(new TestCase() //
						.input("io0", INPUT_OUTPUT4, false) //
						.input(BMS_CONTROL, true) // Switched On
						.output(STATE_MACHINE, State.UNDEFINED))//
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-Undefined")) //
						.output(STATE_MACHINE, State.GO_RUNNING)) //
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-StartUpRelayOff")) //
						.output(STATE_MACHINE, State.GO_RUNNING))//
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-RetryModbusCommunication")) //
						.output(STATE_MACHINE, State.GO_RUNNING))//
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-WaitForBmsControl")) //
						.output(STATE_MACHINE, State.GO_RUNNING)) //
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-WaitForModbusCommunication")) //
						.output(STATE_MACHINE, State.GO_RUNNING)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.RUNNING));
	}

	/**
	 * Battery hard switch is off, should stay in GO_RUNNING.
	 *
	 * @throws Exception on error
	 */
	@Test
	public void test4() throws Exception {
		final var clock = createDummyClock();
		var sut = new BatteryFeneconHomeImpl();
		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("serialNumberStorage", new DummySerialNumberStorage()) //
				.addComponent(new DummyInputOutput("io0")) //
				.activate(MyConfig.create() //
						.setId("battery0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(0) //
						.setStartStop(StartStopConfig.START) //
						.setBatteryStartUpRelay("io0/InputOutput4") //
						.build()) //

				.next(new TestCase() //
						.input("io0", INPUT_OUTPUT4, false) //
						.input(BMS_CONTROL, false) // Switched Off
						.output(STATE_MACHINE, State.UNDEFINED)) //
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-Undefined")) //
						.output(STATE_MACHINE, State.GO_RUNNING)) //
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-StartUpRelayOn")) //
						.output(STATE_MACHINE, State.GO_RUNNING)) //
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-StartUpRelayOn")) //
						.output(STATE_MACHINE, State.GO_RUNNING)) //
				// Ex; after long time if hard switch turned on....
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-StartUpRelayOn")) //
						.input(BMS_CONTROL, true) // Switched On
						.output(STATE_MACHINE, State.GO_RUNNING)) //
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-StartUpRelayOn")) //
						.input("io0", INPUT_OUTPUT4, true) //
						.output(STATE_MACHINE, State.GO_RUNNING)) //
				.next(new TestCase()//
						.input("io0", INPUT_OUTPUT4, true) // Switch ON
						.onBeforeProcessImage(assertLog(sut, "GoRunning-StartUpRelayHold"))
						.onAfterProcessImage(() -> clock.leap(11, SECONDS))) //
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-StartUpRelayOff")) //
						.output(STATE_MACHINE, State.GO_RUNNING))//
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-StartUpRelayOff")) //
						.output(STATE_MACHINE, State.GO_RUNNING)); //
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

	@Test
	public void testMinVoltageGoStopped() throws Exception {
		final var clock = createDummyClock();
		var sut = new BatteryFeneconHomeImpl();
		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("serialNumberStorage", new DummySerialNumberStorage()) //
				.addComponent(new DummyInputOutput("io0")) //
				.activate(MyConfig.create() //
						.setId("battery0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(0) //
						.setStartStop(StartStopConfig.START) //
						.setBatteryStartUpRelay("io0/InputOutput4")//
						.build())//

				.next(new TestCase() //
						.input("io0", INPUT_OUTPUT4, false) //
						.input(BMS_CONTROL, true) // Switched On
						.output(STATE_MACHINE, State.UNDEFINED))//
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-Undefined")) //
						.output(STATE_MACHINE, State.GO_RUNNING)) //
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-StartUpRelayOff")) //
						.output(STATE_MACHINE, State.GO_RUNNING))//
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-RetryModbusCommunication")) //
						.output(STATE_MACHINE, State.GO_RUNNING))//
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-WaitForBmsControl")) //
						.output(STATE_MACHINE, State.GO_RUNNING)) //
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-WaitForModbusCommunication")) //
						.output(STATE_MACHINE, State.GO_RUNNING)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.RUNNING))

				/*
				 * Critical min voltage
				 */
				.next(new TestCase("MinCellVoltage below critical value") //
						.input(MIN_CELL_VOLTAGE, (DEFAULT_CRITICAL_MIN_VOLTAGE - 100)) //
						.input(CURRENT, 0) //
						.output(LOW_MIN_VOLTAGE_WARNING, true) //
						.output(LOW_MIN_VOLTAGE_FAULT, false) //
						.output(STATE_MACHINE, State.RUNNING) //
						.output(LOW_MIN_VOLTAGE_FAULT_BATTERY_STOPPED, false) //
						.onAfterControllersCallbacks(() -> clock.leap(TIMEOUT - 10, SECONDS))) //
				.next(new TestCase("MinCellVoltage below critical value - charging resets time") //
						.input(MIN_CELL_VOLTAGE, (DEFAULT_CRITICAL_MIN_VOLTAGE - 100)) //
						.input(CURRENT, -300) //
						.output(LOW_MIN_VOLTAGE_WARNING, true) //
						.output(LOW_MIN_VOLTAGE_FAULT, false) //
						.output(LOW_MIN_VOLTAGE_FAULT_BATTERY_STOPPED, false)) //
				.next(new TestCase("MinCellVoltage below critical value - timer starts again") //
						.input(CURRENT, 0) //
						.input(MIN_CELL_VOLTAGE, (DEFAULT_CRITICAL_MIN_VOLTAGE - 100)) //
						.output(LOW_MIN_VOLTAGE_WARNING, true) //
						.output(LOW_MIN_VOLTAGE_FAULT, false) //
						.output(LOW_MIN_VOLTAGE_FAULT_BATTERY_STOPPED, false) //
						.onAfterControllersCallbacks(() -> clock.leap(TIMEOUT - 10, SECONDS))) //
				.next(new TestCase("MinCellVoltage below critical value - time not passed") //
						.input(CURRENT, 0) //
						.input(MIN_CELL_VOLTAGE, (DEFAULT_CRITICAL_MIN_VOLTAGE - 100)) //
						.output(LOW_MIN_VOLTAGE_WARNING, true) //
						.output(LOW_MIN_VOLTAGE_FAULT, false) //
						.output(LOW_MIN_VOLTAGE_FAULT_BATTERY_STOPPED, false) //
						.onAfterControllersCallbacks(() -> clock.leap(15, SECONDS))) //
				.next(new TestCase("MinCellVoltage below critical value - time passed") //
						.input(CURRENT, 0) //
						.input(MIN_CELL_VOLTAGE, (DEFAULT_CRITICAL_MIN_VOLTAGE - 100)) //
						.output(LOW_MIN_VOLTAGE_FAULT, true) //
						.output(LOW_MIN_VOLTAGE_WARNING, false) //
						.output(LOW_MIN_VOLTAGE_FAULT_BATTERY_STOPPED, false) //
						.output(STATE_MACHINE, State.RUNNING)) //
				.next(new TestCase("MinCellVoltage below critical value - error") //
						.input(LOW_MIN_VOLTAGE_FAULT, true) //
						.input(CURRENT, 0) //
						.input(MIN_CELL_VOLTAGE, (DEFAULT_CRITICAL_MIN_VOLTAGE - 100)) //
						.output(LOW_MIN_VOLTAGE_FAULT, true) //
						.output(LOW_MIN_VOLTAGE_WARNING, false) //
						.output(LOW_MIN_VOLTAGE_FAULT_BATTERY_STOPPED, false)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.ERROR)) //
				.next(new TestCase("MinCellVoltage below critical value - go stopped") //
						.input(LOW_MIN_VOLTAGE_FAULT, true) //
						.input(CURRENT, 0) //
						.input(MIN_CELL_VOLTAGE, (DEFAULT_CRITICAL_MIN_VOLTAGE - 100)) //
						.output(LOW_MIN_VOLTAGE_FAULT, true) //
						.output(LOW_MIN_VOLTAGE_WARNING, false) //
						.output(LOW_MIN_VOLTAGE_FAULT_BATTERY_STOPPED, false) //
						.output(STATE_MACHINE, State.GO_STOPPED) //
						.onAfterControllersCallbacks(() -> clock.leap(2_100, SECONDS))) // 35 minutes
				.next(new TestCase("MinCellVoltage below critical value - test modbus") //
						.input(CURRENT, 0) //
						.input(MODBUS_COMMUNICATION_FAILED, true) //
						.input(MIN_CELL_VOLTAGE, (DEFAULT_CRITICAL_MIN_VOLTAGE - 100)) //
						.output(LOW_MIN_VOLTAGE_WARNING, false) //
						.output(LOW_MIN_VOLTAGE_FAULT, false) //
						.output(LOW_MIN_VOLTAGE_FAULT_BATTERY_STOPPED, true) //
						.output(STATE_MACHINE, State.GO_STOPPED)) // stateMachine changed in next cycle

				.next(new TestCase("MinCellVoltage below critical value - stopped fault") //
						.input(CURRENT, 0) //
						.input(MODBUS_COMMUNICATION_FAILED, true) //
						.input(MIN_CELL_VOLTAGE, (DEFAULT_CRITICAL_MIN_VOLTAGE - 100)) //
						.output(LOW_MIN_VOLTAGE_WARNING, false) //
						.output(LOW_MIN_VOLTAGE_FAULT, false) //
						.output(LOW_MIN_VOLTAGE_FAULT_BATTERY_STOPPED, true) //
						.output(STATE_MACHINE, State.STOPPED) //
				);
	}

	@Test
	public void testMinVoltageGoStoppedPassedValues() throws Exception {
		final var clock = createDummyClock();

		var sut = new BatteryFeneconHomeImpl();
		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addComponent(new DummyInputOutput("io0")) //
				.activate(MyConfig.create() //
						.setId("battery0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(0) //
						.setStartStop(StartStopConfig.START) //
						.setBatteryStartUpRelay("io0/InputOutput4")//
						.build())//

				.next(new TestCase() //
						.input("io0", INPUT_OUTPUT4, false) //
						.input(BMS_CONTROL, true) // Switched On
						.output(STATE_MACHINE, State.UNDEFINED))//
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-Undefined")) //
						.output(STATE_MACHINE, State.GO_RUNNING)) //
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-StartUpRelayOff")) //
						.output(STATE_MACHINE, State.GO_RUNNING))//
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-RetryModbusCommunication")) //
						.output(STATE_MACHINE, State.GO_RUNNING))//
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-WaitForBmsControl")) //
						.output(STATE_MACHINE, State.GO_RUNNING)) //
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-WaitForModbusCommunication")) //
						.output(STATE_MACHINE, State.GO_RUNNING)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.RUNNING))

				/*
				 * Critical min voltage
				 */
				.next(new TestCase("MinCellVoltage below critical value") //
						.input(MIN_CELL_VOLTAGE, (DEFAULT_CRITICAL_MIN_VOLTAGE - 100)) //
						.input(CURRENT, 0) //
						.output(LOW_MIN_VOLTAGE_WARNING, true) //
						.output(LOW_MIN_VOLTAGE_FAULT, false) //
						.output(STATE_MACHINE, State.RUNNING) //
						.output(LOW_MIN_VOLTAGE_FAULT_BATTERY_STOPPED, false) //
						.onAfterControllersCallbacks(() -> clock.leap(TIMEOUT + 10, SECONDS))) //

				.next(new TestCase("MinCellVoltage below critical value - time passed") //
						.input(CURRENT, 0) //
						.input(MIN_CELL_VOLTAGE, (DEFAULT_CRITICAL_MIN_VOLTAGE - 100)) //
						.output(LOW_MIN_VOLTAGE_FAULT, true) //
						.output(LOW_MIN_VOLTAGE_WARNING, false) //
						.output(LOW_MIN_VOLTAGE_FAULT_BATTERY_STOPPED, false) //
						.output(STATE_MACHINE, State.RUNNING)) //
				.next(new TestCase("MinCellVoltage below critical value - error") //
						.input(LOW_MIN_VOLTAGE_FAULT, true) //
						.input(CURRENT, 0) //
						.input(MIN_CELL_VOLTAGE, (DEFAULT_CRITICAL_MIN_VOLTAGE - 100)) //
						.output(LOW_MIN_VOLTAGE_FAULT, true) //
						.output(LOW_MIN_VOLTAGE_WARNING, false) //
						.output(LOW_MIN_VOLTAGE_FAULT_BATTERY_STOPPED, false)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.ERROR)) //
				.next(new TestCase("MinCellVoltage below critical value - go stopped") //
						.input(LOW_MIN_VOLTAGE_FAULT, true) //
						.input(CURRENT, 0) //

						// MinCellVoltage would be null, but there is not DummyTimedata for not to test
						// "getPastValues"
						.input(MIN_CELL_VOLTAGE, (DEFAULT_CRITICAL_MIN_VOLTAGE - 100)) //
						.output(LOW_MIN_VOLTAGE_FAULT, true) //
						.output(LOW_MIN_VOLTAGE_WARNING, false) //
						.output(LOW_MIN_VOLTAGE_FAULT_BATTERY_STOPPED, false) //
						.output(STATE_MACHINE, State.GO_STOPPED) //
						.onAfterControllersCallbacks(() -> clock.leap(2_100, SECONDS))) // 35 minutes
				.next(new TestCase() //
						.input(MODBUS_COMMUNICATION_FAILED, true) //
						.input(MIN_CELL_VOLTAGE, null) //
				) //
				.next(new TestCase() //
						.input(MODBUS_COMMUNICATION_FAILED, true) //
						.input(MIN_CELL_VOLTAGE, null) //
				) //
				.next(new TestCase("MinCellVoltage below critical value - stopped") //
						.input(CURRENT, 0) //
						.input(MODBUS_COMMUNICATION_FAILED, true) //
						.input(MIN_CELL_VOLTAGE, null) //
						.output(LOW_MIN_VOLTAGE_WARNING, false) //
						.output(LOW_MIN_VOLTAGE_FAULT, false) //
						.output(LOW_MIN_VOLTAGE_FAULT_BATTERY_STOPPED, true) //
						.output(STATE_MACHINE, State.STOPPED) //
				);
	}

	@Test
	public void testMinVoltageCharging() throws Exception {
		final var clock = createDummyClock();
		var sut = new BatteryFeneconHomeImpl();
		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("serialNumberStorage", new DummySerialNumberStorage()) //
				.addComponent(new DummyInputOutput("io0")) //
				.activate(MyConfig.create() //
						.setId("battery0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(0) //
						.setStartStop(StartStopConfig.START) //
						.setBatteryStartUpRelay("io0/InputOutput4")//
						.build())//

				.next(new TestCase() //
						.input("io0", INPUT_OUTPUT4, false) //
						.input(BMS_CONTROL, true) // Switched On
						.output(STATE_MACHINE, State.UNDEFINED))//
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-Undefined")) //
						.output(STATE_MACHINE, State.GO_RUNNING)) //
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-StartUpRelayOff")) //
						.output(STATE_MACHINE, State.GO_RUNNING))//
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-RetryModbusCommunication")) //
						.output(STATE_MACHINE, State.GO_RUNNING))//
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-WaitForBmsControl")) //
						.output(STATE_MACHINE, State.GO_RUNNING)) //
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-WaitForModbusCommunication")) //
						.output(STATE_MACHINE, State.GO_RUNNING)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.RUNNING))

				/*
				 * Critical min voltage
				 */
				.next(new TestCase("MinCellVoltage below critical value") //
						.input(MIN_CELL_VOLTAGE, (DEFAULT_CRITICAL_MIN_VOLTAGE - 100)) //
						.input(CURRENT, 0) //
						.output(LOW_MIN_VOLTAGE_WARNING, true) //
						.output(LOW_MIN_VOLTAGE_FAULT, false) //
						.output(STATE_MACHINE, State.RUNNING) //
						.output(LOW_MIN_VOLTAGE_FAULT_BATTERY_STOPPED, false) //
						.onAfterControllersCallbacks(() -> clock.leap(TIMEOUT - 10, SECONDS))) //
				.next(new TestCase("MinCellVoltage below critical value - charging resets time") //
						.input(MIN_CELL_VOLTAGE, (DEFAULT_CRITICAL_MIN_VOLTAGE - 100)) //
						.input(CURRENT, -300) //
						.output(LOW_MIN_VOLTAGE_WARNING, true) //
						.output(LOW_MIN_VOLTAGE_FAULT, false) //
						.output(STATE_MACHINE, State.RUNNING) //
						.output(LOW_MIN_VOLTAGE_FAULT_BATTERY_STOPPED, false)) //
				.next(new TestCase("MinCellVoltage below critical value - charging") //
						.input(CURRENT, -2000) //
						.input(MIN_CELL_VOLTAGE, (DEFAULT_CRITICAL_MIN_VOLTAGE + 50)) //
						.output(LOW_MIN_VOLTAGE_WARNING, false) //
						.output(LOW_MIN_VOLTAGE_FAULT, false) //
						.output(STATE_MACHINE, State.RUNNING) //
						.output(LOW_MIN_VOLTAGE_FAULT_BATTERY_STOPPED, false) //
				);
	}

	@Test
	public void testNumberOfTowers() throws Exception {
		final var clock = createDummyClock();
		var sut = new BatteryFeneconHomeImpl();
		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("serialNumberStorage", new DummySerialNumberStorage()) //
				.addComponent(new DummyInputOutput("io0")) //
				.activate(MyConfig.create() //
						.setId("battery0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(0) //
						.setStartStop(StartStopConfig.START) //
						.setBatteryStartUpRelay("io0/InputOutput4") //
						.build()) //

				.next(new TestCase() //
						.input("io0", INPUT_OUTPUT4, false) //
						.input(BMS_CONTROL, true) // Switched On
						.output(STATE_MACHINE, State.UNDEFINED))//
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-Undefined")) //
						.output(STATE_MACHINE, State.GO_RUNNING)) //
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-StartUpRelayOff")) //
						.output(STATE_MACHINE, State.GO_RUNNING))//
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-RetryModbusCommunication")) //
						.output(STATE_MACHINE, State.GO_RUNNING))//
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-WaitForBmsControl")) //
						.output(STATE_MACHINE, State.GO_RUNNING)) //
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-WaitForModbusCommunication")) //
						.output(STATE_MACHINE, State.GO_RUNNING)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.RUNNING))
				.next(new TestCase() //
						.output(NUMBER_OF_TOWERS, null))
				.next(new TestCase() //
						.input(BatteryFeneconHome.ChannelId.RACK_NUMBER_OF_BATTERY_BCU, 1) //
						.input(BatteryFeneconHome.ChannelId.NUMBER_OF_MODULES_PER_TOWER, 7) //
						.output(NUMBER_OF_TOWERS, 1)) //
				.next(new TestCase() //
						.input(BatteryFeneconHome.ChannelId.NUMBER_OF_MODULES_PER_TOWER, 7) //
						.input(BatteryFeneconHome.ChannelId.RACK_NUMBER_OF_BATTERY_BCU, 2) //
						.output(NUMBER_OF_TOWERS, 2) //
				) //
		;
	}

	/**
	 * Battery charge power limited by the {@link FeneconHomeBatteryProtection52}.
	 *
	 * @throws Exception on error
	 */
	@Test
	public void testBatteryProtectionSocLimitations() throws Exception {
		var sut = new BatteryFeneconHomeImpl();
		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.addReference("serialNumberStorage", new DummySerialNumberStorage()) //
				.addComponent(new DummyInputOutput("io0"))//
				.activate(MyConfig.create() //
						.setId("battery0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(0) //
						.setStartStop(StartStopConfig.START) //
						.setBatteryStartUpRelay("io0/InputOutput4")//
						.build()) //

				.next(new TestCase() //
						.input("io0", INPUT_OUTPUT4, false) //
						.input(BMS_CONTROL, true) // Switched On
						.output(STATE_MACHINE, State.UNDEFINED))//
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-Undefined")) //
						.output(STATE_MACHINE, State.GO_RUNNING)) //
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-StartUpRelayOff")) //
						.output(STATE_MACHINE, State.GO_RUNNING))//
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-RetryModbusCommunication")) //
						.output(STATE_MACHINE, State.GO_RUNNING))//
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-WaitForBmsControl")) //
						.output(STATE_MACHINE, State.GO_RUNNING)) //
				.next(new TestCase() //
						.onBeforeProcessImage(assertLog(sut, "GoRunning-WaitForModbusCommunication")) //
						.output(STATE_MACHINE, State.GO_RUNNING)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.RUNNING)) //
				.next(new TestCase() //
						.output(BP_CHARGE_MAX_SOC, 40)) //

				.next(new TestCase() //
						.input(SOC, 97) //
						.output(SOC, 97)) //
				.next(new TestCase() //
						.output(BP_CHARGE_MAX_SOC, round(40 * 0.625F))) //
				.next(new TestCase() //
						.input(SOC, 98)) //
				.next(new TestCase() //
						.output(BP_CHARGE_MAX_SOC, round(40 * 0.4F))) //
				.next(new TestCase() //
						.input(SOC, 99)) //
				.next(new TestCase() //
						.output(BP_CHARGE_MAX_SOC, round(40 * 0.2F))) //
				.next(new TestCase() //
						.input(SOC, 100)) //
				.next(new TestCase() //
						.output(BP_CHARGE_MAX_SOC, round(40 * 0.05F))) //
				.next(new TestCase() //
						.input(SOC, 99)) //
				.next(new TestCase() //
						.output(BP_CHARGE_MAX_SOC, round(40 * 0.2F)) //
				);
	}

	@Test
	public void testReadModbus() throws Exception {
		var sut = new BatteryFeneconHomeImpl();
		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0") //
						.withRegister(18000, (byte) 0x00, (byte) 0x00)) // TOWER_4_BMS_SOFTWARE_VERSION
				.addReference("serialNumberStorage", new DummySerialNumberStorage()) //
				.activate(MyConfig.create() //
						.setId("battery0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(0) //
						.setStartStop(StartStopConfig.START) //
						.setBatteryStartUpRelay("io0/InputOutput4")//
						.build()) //

				.next(new TestCase() //
						.output(BatteryFeneconHome.ChannelId.TOWER_4_BMS_SOFTWARE_VERSION, "0.0")) //

				.deactivate();
	}

	@Test
	public void testReadSoftwareVersionModbus() throws Exception {
		var sut = new BatteryFeneconHomeImpl();
		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0") //
						.withRegister(18000, (byte) 0x01, (byte) 0x11)) // TOWER_4_BMS_SOFTWARE_VERSION
				.addReference("serialNumberStorage", new DummySerialNumberStorage()) //
				.activate(MyConfig.create() //
						.setId("battery0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(0) //
						.setStartStop(StartStopConfig.START) //
						.setBatteryStartUpRelay("io0/InputOutput4")//
						.build()) //

				.next(new TestCase() //
						.output(BatteryFeneconHome.ChannelId.TOWER_4_BMS_SOFTWARE_VERSION, "1.17"))
				.next(new TestCase() //
						.output(BatteryFeneconHome.ChannelId.TOWER_4_BMS_SOFTWARE_VERSION_MAJ, 1))
				.next(new TestCase() //
						.output(BatteryFeneconHome.ChannelId.TOWER_4_BMS_SOFTWARE_VERSION_MIN, 17))
				.deactivate();

		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0") //
						.withRegister(18000, (byte) 0x01, (byte) 0x02)) // TOWER_4_BMS_SOFTWARE_VERSION
				.addReference("serialNumberStorage", new DummySerialNumberStorage()) //
				.activate(MyConfig.create() //
						.setId("battery0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(0) //
						.setStartStop(StartStopConfig.START) //
						.setBatteryStartUpRelay("io0/InputOutput4")//
						.build()) //

				.next(new TestCase() //
						.output(BatteryFeneconHome.ChannelId.TOWER_4_BMS_SOFTWARE_VERSION, "1.2"))
				.next(new TestCase() //
						.output(BatteryFeneconHome.ChannelId.TOWER_4_BMS_SOFTWARE_VERSION_MAJ, 1))
				.next(new TestCase() //
						.output(BatteryFeneconHome.ChannelId.TOWER_4_BMS_SOFTWARE_VERSION_MIN, 2))
				.deactivate();

		var sut2 = new BatteryFeneconHomeImpl();
		new ComponentTest(sut2) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) // with no registers
				.addReference("serialNumberStorage", new DummySerialNumberStorage()) //
				.activate(MyConfig.create() //
						.setId("battery0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(0) //
						.setStartStop(StartStopConfig.START) //
						.setBatteryStartUpRelay("io0/InputOutput4")//
						.build()) //

				.next(new TestCase() //
						.output(BatteryFeneconHome.ChannelId.TOWER_4_BMS_SOFTWARE_VERSION, null))
				.next(new TestCase() //
						.output(BatteryFeneconHome.ChannelId.TOWER_4_BMS_SOFTWARE_VERSION_MAJ, null))
				.next(new TestCase() //
						.output(BatteryFeneconHome.ChannelId.TOWER_4_BMS_SOFTWARE_VERSION_MIN, null))
				.deactivate();
	}

	@Test
	public void testReadRegister44002_onGrid() throws Exception {
		// 0x01F4 = on-grid (bit15=0), 500 mA (bits14-0=0x1F4)
		testReadRegister44002(0x01F4, 500, false);
	}

	@Test
	public void testReadRegister44002_offGrid() throws Exception {
		// 0x81F4 = off-grid (bit15=1), 500 mA (bits14-0=0x1F4)
		testReadRegister44002(0x81F4, 500, true);
	}

	private static void testReadRegister44002(int registerValue, int expectedMilliAmpere, boolean expectedOffGrid)
			throws Exception {
		var sut = new BatteryFeneconHomeImpl();

		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0") //
						.withRegister(44000, (byte) 0x00, (byte) 0x00) //
						.withRegister(44001, (byte) 0x00, (byte) 0x00) //
						.withRegister(44002, registerValue)) //
				.activate(MyConfig.create() //
						.setId("battery0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(0) //
						.setStartStop(StartStopConfig.START) //
						.setBatteryStartUpRelay("io0/InputOutput4") //
						.build())
				.next(new TestCase() //
						.output(BatteryFeneconHome.ChannelId.EMS_POWER_CONSUMPTION, expectedMilliAmpere) //
						.output(BatteryFeneconHome.ChannelId.EMS_OFF_GRID, expectedOffGrid))
				.deactivate();
	}

	/**
	 * Tests the new tower registers (10052-10079) that were added.
	 *
	 * <p>
	 * This test verifies that the new BMS health and energy registers can be read
	 * without causing Integer overflow errors.
	 *
	 * <p>
	 * Based on real data from server log:
	 * 
	 * <pre>
	 * Execute FC3ReadHoldingRegisters [battery0;unitid=1;priority=HIGH;ref=10001/0x2711;length=79;
	 * response=0202 00b0 0000 0000 0000 0000 0000 03d4 03e8 0b04 0046 0d1b 0d22 0d1e 01f4 01f4
	 *          0000 0002 0280 0273 0266 0e74 0b54 0006 0000 0000 0000 0000 0000 0000 0000 0000
	 *          0002 0b07 00e9 00d3 0000 0000 0000 0000 0000 0000 0105 0105 0000 0000 0000 016b
	 *          0000 012e 330e 00b4 0032 03ac 0064 0000 0000 0000 0000 0000 0000 0000 0000 0000
	 *          0000 0000 3714 0000 0000 0000 0000 0000 0000 0000 0000 0000 0513 0000 0460]
	 * </pre>
	 *
	 * <p>
	 * New registers include:
	 * <ul>
	 * <li>10052: BMS_SERIAL_NUMBER (Low word)</li>
	 * <li>10053: BATTERY_SELF_DISCHARGING_RATE</li>
	 * <li>10054: BATTERY_CHARGE_AND_DISCHARGE_ROUND_TRIP_EFFICIENCY</li>
	 * <li>10055: OHMIC_RESISTANCE_OF_BATTERY</li>
	 * <li>10056: DEEP_DISCHARGE_EVENT_COUNTER</li>
	 * <li>10057: OVER_DISCHARGE_EVENT_COUNTER</li>
	 * <li>10058-10059: ACC_DEEP_DISCHARGE_TIME</li>
	 * <li>10060-10061: ACC_OVER_DISCHARGE_TIME</li>
	 * <li>10062-10063: EXTREME_HIGH_TEMPERATURE_TIME</li>
	 * <li>10064-10065: EXTREME_LOW_TEMPERATURE_TIME</li>
	 * <li>10066-10067: REMAINING_ENERGY</li>
	 * <li>10068-10069: HIGH_TEMPERATURE (time)</li>
	 * <li>10070-10071: LOW_TEMPERATURE (time)</li>
	 * <li>10072-10073: OVER_CURRENT_DISCHARGE_TIME</li>
	 * <li>10074-10075: OVER_CURRENT_CHARGE_TIME</li>
	 * <li>10076-10077: ACC_CHARGE_CAPACITY</li>
	 * <li>10078-10079: ACC_DISCHARGE_CAPACITY</li>
	 * </ul>
	 *
	 * @throws Exception on error
	 */
	@Test
	public void testReadTowerRegisters() throws Exception {
		var sut = new BatteryFeneconHomeImpl();
		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0") //
						// Static registers at 10000 (length=25)
						// 10000: BMS_SOFTWARE_VERSION
						// 10001-10018: DUMMY (handled by tower task)
						// 10019: BATTERY_HARDWARE_TYPE (520 = BATTERY_52)
						// 10020-10023: DUMMY
						// 10024: NUMBER_OF_MODULES_PER_TOWER
						.withRegisters(10000, //
								0x0100, // 10000: BMS_SOFTWARE_VERSION
								// 10001-10018: filled by tower registers below
								0x0202, 0x00b0, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, // 10001-10007
								0x03d4, 0x03e8, 0x0b04, 0x0046, 0x0d1b, 0x0d22, 0x0d1e, // 10008-10014
								0x01f4, 0x01f4, 0x0000, 0x0002, // 10015-10018
								0x0208, // 10019: BATTERY_HARDWARE_TYPE = 520 (0x208)
								0x0273, 0x0266, 0x0e74, 0x0b54, // 10020-10023
								0x0005) // 10024: NUMBER_OF_MODULES_PER_TOWER = 5
						// Continue with tower registers 10025-10079
						.withRegisters(10025, //
								// 10025-10033: DUMMY/BCU_FAULT_DETAIL
								0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, //
								// 10034: PACK_VOLTAGE
								0x0002, //
								// 10035: MAX_TEMPERATURE
								0x0b07, //
								// 10036: MIN_TEMPERATURE
								0x00e9, //
								// 10037-10042: DUMMY
								0x00d3, 0x0000, 0x0000, 0x0000, 0x0000, 0x0000, //
								// 10043: TEMPERATURE_PRE_MOS
								0x0105, //
								// 10044-10046: DUMMY
								0x0105, 0x0000, 0x0000, //
								// 10047-10048: ACC_CHARGE_ENERGY (Doubleword) = 0x0000016b = 363 -> *100 =
								// 36300 Wh
								0x0000, 0x016b, //
								// 10049-10050: ACC_DISCHARGE_ENERGY (Doubleword) = 0x0000012e = 302 -> *100 =
								// 30200 Wh
								0x0000, 0x012e, //
								// 10051-10052: BMS_SERIAL_NUMBER (Doubleword)
								0x330e, 0x00b4, //
								// 10053: BATTERY_SELF_DISCHARGING_RATE = 0x0032 = 50
								0x0032, //
								// 10054: EFFICIENCY = 0x03ac = 940 -> /10 = 94 (SCALE_FACTOR_MINUS_1)
								0x03ac, //
								// 10055: OHMIC_RESISTANCE = 0x0064 = 100 -> *10 = 1000 µΩ (SCALE_FACTOR_1)
								0x0064, //
								// 10056: DEEP_DISCHARGE_EVENT_COUNTER = 5
								0x0005, //
								// 10057: OVER_DISCHARGE_EVENT_COUNTER = 3
								0x0003, //
								// 10058-10059: ACC_DEEP_DISCHARGE_TIME = 1000 seconds
								0x0000, 0x03e8, //
								// 10060-10061: ACC_OVER_DISCHARGE_TIME = 500 seconds
								0x0000, 0x01f4, //
								// 10062-10063: EXTREME_HIGH_TEMPERATURE_TIME = 100 seconds
								0x0000, 0x0064, //
								// 10064-10065: EXTREME_LOW_TEMPERATURE_TIME = 200 seconds
								0x0000, 0x00c8, //
								// 10066-10067: REMAINING_ENERGY = 0x00003714 = 14100 Wh (DIRECT_1_TO_1)
								0x0000, 0x3714, //
								// 10068-10069: HIGH_TEMPERATURE_TIME = 300 seconds
								0x0000, 0x012c, //
								// 10070-10071: LOW_TEMPERATURE_TIME = 400 seconds
								0x0000, 0x0190, //
								// 10072-10073: OVER_CURRENT_DISCHARGE_TIME = 50 seconds
								0x0000, 0x0032, //
								// 10074-10075: OVER_CURRENT_CHARGE_TIME = 60 seconds
								0x0000, 0x003c, //
								// 10076-10077: ACC_CHARGE_CAPACITY = 100 -> *100 = 10000 mAh
								0x0000, 0x0064, //
								// 10078-10079: ACC_DISCHARGE_CAPACITY = 90 -> *100 = 9000 mAh
								0x0000, 0x005a) //
						// Other tower software versions
						.withRegister(12000, 0x0100) // TOWER_1_BMS_SOFTWARE_VERSION
						.withRegister(14000, 0x0100) // TOWER_2_BMS_SOFTWARE_VERSION
						.withRegister(16000, 0x0100) // TOWER_3_BMS_SOFTWARE_VERSION
						.withRegister(18000, 0x0100) // TOWER_4_BMS_SOFTWARE_VERSION
						// BMS Control registers (bit 0 is inverted: 0=ON, 1=OFF)
						// BMS_CONTROL=ON (inverted), DUMMY, EMS_POWER_CONSUMPTION
						.withRegisters(44000, 0x0000, 0x0000, 0x01F4)
						// Alarm registers at 500 (length=29)
						.withRegisters(500, //
								0x0000, 0x0000, 0x0000, 0x0000, 0x0000, // 500-504
								0x0000, 0x0000, 0x0000, 0x0000, 0x0000, // 505-509
								0x0000, 0x0000, 0x0000, 0x0000, 0x0000, // 510-514
								0x0000, 0x0000, 0x0000, 0x0000, 0x0000, // 515-519
								0x0000, 0x0000, 0x0000, 0x0000, 0x0000, // 520-524
								0x0000, 0x0000, 0x0000, 0x0000)) // 525-528 //
				.addReference("serialNumberStorage", new DummySerialNumberStorage()) //
				.addComponent(new DummyInputOutput("io0")) //
				.activate(MyConfig.create() //
						.setId("battery0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(0) //
						.setStartStop(StartStopConfig.START) //
						.setBatteryStartUpRelay("io0/InputOutput4") //
						.build()) //

				.next(new TestCase() //
						.input("io0", INPUT_OUTPUT4, false) //
						.input(BMS_CONTROL, true) // Switched On - battery is already running
						.output(STATE_MACHINE, State.UNDEFINED)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.GO_RUNNING)) //
				// Empty TestCases simulate cycle time for state machine transitions
				.next(new TestCase(), 5) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.RUNNING)) //
				// Trigger tower channel initialization
				.next(new TestCase("Initialize Tower 0") //
						.input(BatteryFeneconHome.ChannelId.RACK_NUMBER_OF_BATTERY_BCU, 1) //
						.input(BatteryFeneconHome.ChannelId.NUMBER_OF_MODULES_PER_TOWER, 5)) //
				// Verify the new register values
				.next(new TestCase("Verify new Tower 0 registers")
						// ACC_CHARGE_ENERGY: 363 * 100 = 36300 Wh
						.output("battery0", "Tower0AccChargeEnergy", 36300L) //
						// ACC_DISCHARGE_ENERGY: 302 * 100 = 30200 Wh
						.output("battery0", "Tower0AccDischargeEnergy", 30200L) //
						// BATTERY_SELF_DISCHARGING_RATE: 50
						.output("battery0", "Tower0BatterySelfDischargingRate", 50) //
						// EFFICIENCY: 940 / 10 = 94
						.output("battery0", "Tower0BatteryChargeAndDischargeRoundTripEfficiency", 94) //
						// OHMIC_RESISTANCE: 100 * 10 = 1000 µΩ
						.output("battery0", "Tower0OhmicResistanceOfBattery", 1000) //
						// DEEP_DISCHARGE_EVENT_COUNTER: 5
						.output("battery0", "Tower0DeepDischargeEventCounter", 5) //
						// OVER_DISCHARGE_EVENT_COUNTER: 3
						.output("battery0", "Tower0OverChargeEventCounter", 3) //
						// ACC_DEEP_DISCHARGE_TIME: 1000 seconds
						.output("battery0", "Tower0AccDeepDischargeTime", 1000) //
						// ACC_OVER_DISCHARGE_TIME: 500 seconds
						.output("battery0", "Tower0AccOverChargeTime", 500) //
						// EXTREME_HIGH_TEMPERATURE_TIME: 100 seconds
						.output("battery0", "Tower0ExtremeHighTemperatureTime", 100) //
						// EXTREME_LOW_TEMPERATURE_TIME: 200 seconds
						.output("battery0", "Tower0ExtremeLowTemperatureTime", 200) //
						// REMAINING_ENERGY: 14100 Wh
						.output("battery0", "Tower0RemainingEnergy", 14100) //
						// HIGH_TEMPERATURE_TIME: 300 seconds
						.output("battery0", "Tower0HighTemperatureTime", 300) //
						// LOW_TEMPERATURE_TIME: 400 seconds
						.output("battery0", "Tower0LowTemperatureTime", 400) //
						// OVER_CURRENT_DISCHARGE_TIME: 50 seconds
						.output("battery0", "Tower0OverCurrentDischargeTime", 50) //
						// OVER_CURRENT_CHARGE_TIME: 60 seconds
						.output("battery0", "Tower0OverCurrentChargeTime", 60) //
						// ACC_CHARGE_CAPACITY: 100 * 100 = 10000 mAh
						.output("battery0", "Tower0AccChargeCapacity", 10000) //
						// ACC_DISCHARGE_CAPACITY: 90 * 100 = 9000 mAh
						.output("battery0", "Tower0AccDischargeCapacity", 9000) //
						// Verify units
						.onAfterProcessImage(() -> {
							assertUnit(Unit.WATT_HOURS, sut, "Tower0AccChargeEnergy");
							assertUnit(Unit.WATT_HOURS, sut, "Tower0AccDischargeEnergy");
							assertUnit(Unit.TENTHOUSANDTH, sut, "Tower0BatterySelfDischargingRate");
							assertUnit(Unit.PERCENT, sut, "Tower0BatteryChargeAndDischargeRoundTripEfficiency");
							assertUnit(Unit.MICROOHM, sut, "Tower0OhmicResistanceOfBattery");
							assertUnit(Unit.NONE, sut, "Tower0DeepDischargeEventCounter");
							assertUnit(Unit.NONE, sut, "Tower0OverChargeEventCounter");
							assertUnit(Unit.SECONDS, sut, "Tower0AccDeepDischargeTime");
							assertUnit(Unit.SECONDS, sut, "Tower0AccOverChargeTime");
							assertUnit(Unit.SECONDS, sut, "Tower0ExtremeHighTemperatureTime");
							assertUnit(Unit.SECONDS, sut, "Tower0ExtremeLowTemperatureTime");
							assertUnit(Unit.WATT_HOURS, sut, "Tower0RemainingEnergy");
							assertUnit(Unit.SECONDS, sut, "Tower0HighTemperatureTime");
							assertUnit(Unit.SECONDS, sut, "Tower0LowTemperatureTime");
							assertUnit(Unit.SECONDS, sut, "Tower0OverCurrentDischargeTime");
							assertUnit(Unit.SECONDS, sut, "Tower0OverCurrentChargeTime");
							assertUnit(Unit.MILLIAMPERE_HOURS, sut, "Tower0AccChargeCapacity");
							assertUnit(Unit.MILLIAMPERE_HOURS, sut, "Tower0AccDischargeCapacity");
						})) //
				.deactivate();
	}

	private static void assertUnit(Unit expected, BatteryFeneconHomeImpl component, String channelId) {
		assertEquals(expected, component.channel(channelId).channelDoc().getUnit());
	}
}
