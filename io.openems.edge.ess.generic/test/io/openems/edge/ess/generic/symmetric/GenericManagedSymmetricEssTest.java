package io.openems.edge.ess.generic.symmetric;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.battery.test.DummyBattery;
import io.openems.edge.batteryinverter.test.DummyManagedSymmetricBatteryInverter;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStopConfig;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.ess.generic.symmetric.statemachine.State;

public class GenericManagedSymmetricEssTest {

	private final static String ESS_ID = "ess0";
	private final static String BATTERY_ID = "battery0";
	private final static String BATTERY_INVERTER_ID = "batteryInverter0";

	private final static ChannelAddress ESS_STATE_MACHINE = new ChannelAddress(ESS_ID, "StateMachine");
	private final static ChannelAddress BATTERY_START_STOP = new ChannelAddress(BATTERY_ID, "StartStop");
	private final static ChannelAddress BATTERY_INVERTER_START_STOP = new ChannelAddress(BATTERY_INVERTER_ID,
			"StartStop");

	@Test
	public void testStart() throws Exception {
		new ComponentTest(new GenericManagedSymmetricEssImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("batteryInverter", new DummyManagedSymmetricBatteryInverter(BATTERY_INVERTER_ID)) //
				.addReference("battery", new DummyBattery(BATTERY_ID)) //
				.activate(MyConfig.create() //
						.setId(ESS_ID) //
						.setStartStopConfig(StartStopConfig.START) //
						.setBatteryInverterId(BATTERY_INVERTER_ID) //
						.setBatteryId(BATTERY_ID) //
						.build()) //
				.next(new TestCase() //
						.output(ESS_STATE_MACHINE, State.UNDEFINED)) //
				.next(new TestCase() //
						.output(ESS_STATE_MACHINE, State.START_BATTERY)) //
				.next(new TestCase("Start the Battery") //
						.input(BATTERY_START_STOP, StartStop.START)) //
				.next(new TestCase() //
						.output(ESS_STATE_MACHINE, State.START_BATTERY_INVERTER)) //
				.next(new TestCase("Start the Battery-Inverter") //
						.input(BATTERY_INVERTER_START_STOP, StartStop.START)) //
				.next(new TestCase() //
						.output(ESS_STATE_MACHINE, State.STARTED)) //
		;
	}

}
