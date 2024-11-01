package io.openems.edge.battery.fenecon.home;

import static io.openems.edge.battery.api.Battery.ChannelId.CHARGE_MAX_CURRENT;
import static io.openems.edge.battery.api.Battery.ChannelId.CURRENT;
import static io.openems.edge.battery.api.Battery.ChannelId.MAX_CELL_VOLTAGE;
import static io.openems.edge.battery.api.Battery.ChannelId.MIN_CELL_VOLTAGE;
import static io.openems.edge.battery.api.Battery.ChannelId.SOC;
import static io.openems.edge.battery.fenecon.home.BatteryFeneconHome.ChannelId.BMS_CONTROL;
import static io.openems.edge.battery.fenecon.home.BatteryFeneconHome.ChannelId.LOW_MIN_VOLTAGE_FAULT;
import static io.openems.edge.battery.fenecon.home.BatteryFeneconHome.ChannelId.LOW_MIN_VOLTAGE_FAULT_BATTERY_STOPPED;
import static io.openems.edge.battery.fenecon.home.BatteryFeneconHome.ChannelId.LOW_MIN_VOLTAGE_WARNING;
import static io.openems.edge.battery.fenecon.home.BatteryFeneconHome.ChannelId.NUMBER_OF_MODULES_PER_TOWER;
import static io.openems.edge.battery.fenecon.home.BatteryFeneconHome.ChannelId.NUMBER_OF_TOWERS;
import static io.openems.edge.battery.fenecon.home.BatteryFeneconHome.ChannelId.STATE_MACHINE;
import static io.openems.edge.battery.fenecon.home.BatteryFeneconHome.ChannelId.TOWER_0_BMS_SOFTWARE_VERSION;
import static io.openems.edge.battery.fenecon.home.BatteryFeneconHome.ChannelId.TOWER_1_BMS_SOFTWARE_VERSION;
import static io.openems.edge.battery.fenecon.home.BatteryFeneconHome.ChannelId.TOWER_2_BMS_SOFTWARE_VERSION;
import static io.openems.edge.battery.fenecon.home.BatteryFeneconHome.ChannelId.TOWER_3_BMS_SOFTWARE_VERSION;
import static io.openems.edge.battery.fenecon.home.BatteryFeneconHome.ChannelId.TOWER_4_BMS_SOFTWARE_VERSION;
import static io.openems.edge.battery.fenecon.home.BatteryFeneconHomeImpl.DEFAULT_CRITICAL_MIN_VOLTAGE;
import static io.openems.edge.battery.fenecon.home.BatteryFeneconHomeImpl.TIMEOUT;
import static io.openems.edge.battery.protection.BatteryProtection.ChannelId.BP_CHARGE_BMS;
import static io.openems.edge.battery.protection.BatteryProtection.ChannelId.BP_CHARGE_MAX_SOC;
import static io.openems.edge.bridge.modbus.api.ModbusComponent.ChannelId.MODBUS_COMMUNICATION_FAILED;
import static io.openems.edge.common.test.TestUtils.createDummyClock;
import static io.openems.edge.io.test.DummyInputOutput.ChannelId.INPUT_OUTPUT4;
import static java.lang.Math.round;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import io.openems.common.function.ThrowingRunnable;
import io.openems.edge.battery.fenecon.home.statemachine.StateMachine.State;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
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
				) //
				.next(new TestCase("MinCellVoltage below critical value - stopped") //
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
	public void testMinVoltageCharging() throws Exception {
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
						.input(NUMBER_OF_MODULES_PER_TOWER, 7) //
						.input(TOWER_0_BMS_SOFTWARE_VERSION, 262) //
						.input(TOWER_1_BMS_SOFTWARE_VERSION, 0) //
						.input(TOWER_2_BMS_SOFTWARE_VERSION, 0) //
						.input(TOWER_3_BMS_SOFTWARE_VERSION, 0) //
						.input(TOWER_4_BMS_SOFTWARE_VERSION, 256) //
						.output(NUMBER_OF_TOWERS, 1)) //
				.next(new TestCase() //
						.input(TOWER_0_BMS_SOFTWARE_VERSION, 262) //
						.input(TOWER_1_BMS_SOFTWARE_VERSION, null) //
						.input(TOWER_2_BMS_SOFTWARE_VERSION, null) //
						.input(TOWER_3_BMS_SOFTWARE_VERSION, null) //
						.input(TOWER_4_BMS_SOFTWARE_VERSION, null) //
						.output(NUMBER_OF_TOWERS, null)) //
				.next(new TestCase() //
						.input(TOWER_0_BMS_SOFTWARE_VERSION, 262) //
						.input(TOWER_1_BMS_SOFTWARE_VERSION, 0) //
						.input(TOWER_2_BMS_SOFTWARE_VERSION, 0) //
						.input(TOWER_3_BMS_SOFTWARE_VERSION, 0) //
						.input(TOWER_4_BMS_SOFTWARE_VERSION, 256) //
						.output(NUMBER_OF_TOWERS, 1)) //
				.next(new TestCase() //
						.output(NUMBER_OF_TOWERS, 1)) //
				.next(new TestCase() //
						.input(NUMBER_OF_TOWERS, null) //
						.input(NUMBER_OF_MODULES_PER_TOWER, 7) //
						.output(NUMBER_OF_TOWERS, null)) //
				.next(new TestCase() //
						.input(TOWER_0_BMS_SOFTWARE_VERSION, null) //
						.input(TOWER_1_BMS_SOFTWARE_VERSION, 0) //
						.input(TOWER_2_BMS_SOFTWARE_VERSION, 0) //
						.input(TOWER_3_BMS_SOFTWARE_VERSION, 0) //
						.input(TOWER_4_BMS_SOFTWARE_VERSION, 256) //
						.output(NUMBER_OF_TOWERS, null)) //
				.next(new TestCase() // Number of towers changes after TOWER_0_BMS_SOFTWARE_VERSION is set
						.input(TOWER_0_BMS_SOFTWARE_VERSION, 262) //
						.input(TOWER_1_BMS_SOFTWARE_VERSION, 0) //
						.input(TOWER_2_BMS_SOFTWARE_VERSION, 0) //
						.input(TOWER_3_BMS_SOFTWARE_VERSION, 0) //
						.input(TOWER_4_BMS_SOFTWARE_VERSION, 256) //
						.output(NUMBER_OF_TOWERS, 1)) //
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
				.activate(MyConfig.create() //
						.setId("battery0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(0) //
						.setStartStop(StartStopConfig.START) //
						.setBatteryStartUpRelay("io0/InputOutput4")//
						.build()) //

				.next(new TestCase() //
						.output(BatteryFeneconHome.ChannelId.TOWER_4_BMS_SOFTWARE_VERSION, 0)) //

				.deactivate();
	}
}
