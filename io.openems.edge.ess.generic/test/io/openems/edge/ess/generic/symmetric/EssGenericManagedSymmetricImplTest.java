package io.openems.edge.ess.generic.symmetric;

import static io.openems.edge.battery.api.Battery.ChannelId.CHARGE_MAX_CURRENT;
import static io.openems.edge.battery.api.Battery.ChannelId.DISCHARGE_MAX_CURRENT;
import static io.openems.edge.batteryinverter.api.SymmetricBatteryInverter.ChannelId.ACTIVE_POWER;
import static io.openems.edge.common.startstop.StartStoppable.ChannelId.START_STOP;
import static io.openems.edge.ess.api.ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER;
import static io.openems.edge.ess.generic.common.GenericManagedEss.EFFICIENCY_FACTOR;
import static io.openems.edge.ess.generic.symmetric.EssGenericManagedSymmetric.ChannelId.STATE_MACHINE;
import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.battery.test.DummyBattery;
import io.openems.edge.batteryinverter.test.DummyManagedSymmetricBatteryInverter;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.ess.generic.symmetric.statemachine.StateMachine.State;
import io.openems.edge.ess.test.DummyPower;
import io.openems.edge.ess.test.ManagedSymmetricEssTest;

public class EssGenericManagedSymmetricImplTest {

	@Test
	public void testStart() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);
		new ComponentTest(new EssGenericManagedSymmetricImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("batteryInverter", new DummyManagedSymmetricBatteryInverter("batteryInverter0")) //
				.addReference("battery", new DummyBattery("battery0")) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setStartStopConfig(StartStopConfig.START) //
						.setBatteryInverterId("batteryInverter0") //
						.setBatteryId("battery0") //
						.build()) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.UNDEFINED)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.START_BATTERY)) //
				.next(new TestCase("Start the Battery") //
						.input("battery0", START_STOP, StartStop.START)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.START_BATTERY_INVERTER)) //
				.next(new TestCase("Start the Battery-Inverter") //
						.input("batteryInverter0", START_STOP, StartStop.START)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.STARTED)) //
		;
	}

	@Test
	public void testForceCharge() throws Exception {
		new ManagedSymmetricEssTest(new EssGenericManagedSymmetricImpl()) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("batteryInverter", new DummyManagedSymmetricBatteryInverter("batteryInverter0")) //
				.addReference("battery", new DummyBattery("battery0") //
						.withVoltage(500) //
						.withChargeMaxCurrent(50) //
						.withDischargeMaxCurrent(-5) //
				) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setStartStopConfig(StartStopConfig.START) //
						.setBatteryInverterId("batteryInverter0") //
						.setBatteryId("battery0") //
						.build()) //
				.next(new TestCase("Start the Battery") //
						.input("battery0", START_STOP, StartStop.START) //
						.output(ALLOWED_DISCHARGE_POWER, 0) //
						.output(ACTIVE_POWER, 0)) //
				.next(new TestCase()) //
				.next(new TestCase("Start the Battery-Inverter") //
						.input("batteryInverter0", START_STOP, StartStop.START)) //
				.next(new TestCase()) //
				.next(new TestCase() //
						.input("battery0", CHARGE_MAX_CURRENT, 50) //
						.input("battery0", DISCHARGE_MAX_CURRENT, -5) //
						.output(ALLOWED_DISCHARGE_POWER, (int) (-2500 * EFFICIENCY_FACTOR))) //
		;
	}

	@Test
	public void testDebugLog() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);
		var sut = new EssGenericManagedSymmetricImpl();
		new ManagedSymmetricEssTest(sut) //
				.addReference("power", new DummyPower()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("batteryInverter", new DummyManagedSymmetricBatteryInverter("batteryInverter0") //
						.withStartStop(StartStop.START) //
						.withMaxApparentPower(92_000)) //
				.addReference("battery", new DummyBattery("battery0") //
						.withStartStop(StartStop.START) //
						.withSoc(60) //
						.withVoltage(700) //
						.withChargeMaxCurrent(80) //
						.withDischargeMaxCurrent(70)) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setStartStopConfig(StartStopConfig.START) //
						.setBatteryInverterId("batteryInverter0") //
						.setBatteryId("battery0") //
						.build()) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> clock.leap(10, ChronoUnit.SECONDS)), 10);
		assertEquals("Started|SoC:60 %|L:0 W|Allowed:-56000;46550", sut.debugLog());
	}

	@Test
	public void testTimeout() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);
		new ComponentTest(new EssGenericManagedSymmetricImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("batteryInverter", new DummyManagedSymmetricBatteryInverter("batteryInverter0")) //
				.addReference("battery", new DummyBattery("battery0")) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setStartStopConfig(StartStopConfig.START) //
						.setBatteryInverterId("batteryInverter0") //
						.setBatteryId("battery0") //
						.build()) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.UNDEFINED)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.START_BATTERY)) //
				.next(new TestCase("Start the Battery") //
						.input("battery0", START_STOP, StartStop.START)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.START_BATTERY_INVERTER)) //
				.next(new TestCase()//
						.input("batteryInverter0", START_STOP, StartStop.STOP)//
						.timeleap(clock, 350, ChronoUnit.SECONDS)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.ERROR)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, State.ERROR)) //
		;
	}
}
