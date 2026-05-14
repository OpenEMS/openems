package io.openems.edge.battery.fenecon.home;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.battery.fenecon.home.BatteryFeneconHome.ChannelId.BMS_CONTROL;
import static io.openems.edge.bridge.modbus.api.ModbusComponent.ChannelId.MODBUS_COMMUNICATION_FAILED;
import static java.time.temporal.ChronoUnit.SECONDS;

import org.junit.jupiter.api.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.fenecon.home.statemachine.StateMachine;
import io.openems.edge.battery.protection.BatteryProtection;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.test.AbstractComponentTest;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummySerialNumberStorage;
import io.openems.edge.io.test.DummyInputOutput;

public class FeneconHomeBatteryProtectionTests {
	@Test
	public void testRunningAndFirmwareUpdate() throws Exception {
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

				// Set battery to RUNNING, the batteryProtection is still reporting zero because
				// of maxIncreaseAmpereLimit
				.next(new AbstractComponentTest.TestCase("Start battery") //
						.inputForce(MODBUS_COMMUNICATION_FAILED, false) //
						.input(StartStoppable.ChannelId.START_STOP, StartStop.START) //
						.input(BMS_CONTROL, true) //
						.input(BatteryProtection.ChannelId.BP_CHARGE_BMS, 10) //
						.input(BatteryProtection.ChannelId.BP_DISCHARGE_BMS, 10) //
						.onBeforeProcessImage(() -> {
							sut.stateMachine.forceNextState(StateMachine.State.RUNNING);
						}) //
						.output(Battery.ChannelId.CHARGE_MAX_CURRENT, 0) //
						.output(Battery.ChannelId.DISCHARGE_MAX_CURRENT, 0)) //

				.next(new AbstractComponentTest.TestCase("Test maxIncreaseAmpereLimit after 10 seconds") //
						.input(StartStoppable.ChannelId.START_STOP, StartStop.START) //
						.onBeforeProcessImage(() -> {
							clock.leap(10, SECONDS);
						}) //
						.output(Battery.ChannelId.CHARGE_MAX_CURRENT, 1) //
						.output(Battery.ChannelId.DISCHARGE_MAX_CURRENT, 1)) //

				.next(new AbstractComponentTest.TestCase("Test maxIncreaseAmpereLimit after 20 seconds") //
						.input(StartStoppable.ChannelId.START_STOP, StartStop.START) //
						.onBeforeProcessImage(() -> {
							clock.leap(10, SECONDS);
						}) //
						.output(Battery.ChannelId.CHARGE_MAX_CURRENT, 2) //
						.output(Battery.ChannelId.DISCHARGE_MAX_CURRENT, 2)) //

				.next(new AbstractComponentTest.TestCase("Set state to firmware update") //
						.onBeforeProcessImage(() -> {
							sut.stateMachine.forceNextState(StateMachine.State.FIRMWARE_UPDATE);
						})) //

				.next(new AbstractComponentTest.TestCase("Test blocked charge/discharge while firmware update") //
						.input(StartStoppable.ChannelId.START_STOP, StartStop.START) //
						.output(Battery.ChannelId.CHARGE_MAX_CURRENT, 0) //
						.output(Battery.ChannelId.DISCHARGE_MAX_CURRENT, 0)) //

				.next(new AbstractComponentTest.TestCase("Check charge current while running") //
						.input(StartStoppable.ChannelId.START_STOP, StartStop.START) //
						.onBeforeProcessImage(() -> { //
							sut.stateMachine.forceNextState(StateMachine.State.RUNNING);
							sut.handleStateMachine();
							clock.leap(10, SECONDS);
						}) //
						.output(Battery.ChannelId.CHARGE_MAX_CURRENT, 1) //
						.output(Battery.ChannelId.DISCHARGE_MAX_CURRENT, 1));

		;

	}
}
