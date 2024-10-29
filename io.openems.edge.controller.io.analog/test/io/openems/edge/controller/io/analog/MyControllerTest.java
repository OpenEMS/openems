package io.openems.edge.controller.io.analog;

import static io.openems.edge.common.test.TestUtils.createDummyClock;

import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.io.api.AnalogOutput.Range;
import io.openems.edge.io.test.DummyAnalogVoltageOutput;
import io.openems.edge.timedata.test.DummyTimedata;

public class MyControllerTest {

	private static final ChannelAddress DEBUG_SET_OUTPUT_VOLTAGE = new ChannelAddress("analogIo0",
			"DebugSetOutputVoltage");
	private static final ChannelAddress DEBUG_SET_OUTPUT_PERCENT = new ChannelAddress("analogIo0",
			"DebugSetOutputPercent");

	@Test
	public void testOff() throws Exception {
		final var clock = createDummyClock();
		new ControllerTest(new ControllerIoAnalogImpl(clock)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.addReference("analogOutput", new DummyAnalogVoltageOutput("analogIo0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setAnalogOutputId("analogIo0") //
						.setManualTarget(6_000) //
						.setMaximumPower(10_000) //
						.setMode(Mode.OFF) //
						.setPowerBehaviour(PowerBehavior.LINEAR) //
						.build())
				.next(new TestCase() //
						.input(Sum.ChannelId.ESS_DISCHARGE_POWER, 2000) //
						.input(Sum.ChannelId.GRID_ACTIVE_POWER, -5000)//
						.output(DEBUG_SET_OUTPUT_PERCENT, 0f) //
						.output(DEBUG_SET_OUTPUT_VOLTAGE, 0)) //
				.deactivate();
	}

	@Test
	public void testOn() throws Exception {
		final var clock = createDummyClock();
		new ControllerTest(new ControllerIoAnalogImpl(clock)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.addReference("analogOutput", new DummyAnalogVoltageOutput("analogIo0") //
						.setRange(new Range(0, 100, 10000))) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setAnalogOutputId("analogIo0") //
						.setManualTarget(6_000) //
						.setMaximumPower(10_000) //
						.setMode(Mode.ON) //
						.setPowerBehaviour(PowerBehavior.LINEAR) //
						.build())
				.next(new TestCase() //
						.input(Sum.ChannelId.ESS_DISCHARGE_POWER, 0) //
						.input(Sum.ChannelId.GRID_ACTIVE_POWER, -5000)//
						.output(DEBUG_SET_OUTPUT_PERCENT, 60.000004f) //
						.output(DEBUG_SET_OUTPUT_VOLTAGE, 6000)) //
				.deactivate();

		new ControllerTest(new ControllerIoAnalogImpl(clock)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.addReference("analogOutput", new DummyAnalogVoltageOutput("analogIo0") //
						.setRange(new Range(0, 100, 10000))) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setAnalogOutputId("analogIo0") //
						.setManualTarget(0) //
						.setMaximumPower(10_000) //
						.setMode(Mode.ON) //
						.setPowerBehaviour(PowerBehavior.LINEAR) //
						.build())
				.next(new TestCase() //
						.input(Sum.ChannelId.ESS_DISCHARGE_POWER, 0) //
						.input(Sum.ChannelId.GRID_ACTIVE_POWER, -5000)//
						.output(DEBUG_SET_OUTPUT_PERCENT, 0f) //
						.output(DEBUG_SET_OUTPUT_VOLTAGE, 0)) //
				.deactivate();
	}

	@Test
	public void testAutomatic() throws Exception {
		final var clock = createDummyClock();
		new ControllerTest(new ControllerIoAnalogImpl(clock)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.addReference("analogOutput", new DummyAnalogVoltageOutput("analogIo0")) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setAnalogOutputId("analogIo0") //
						.setManualTarget(6_000) //
						.setMaximumPower(10_000) //
						.setMode(Mode.AUTOMATIC) //
						.setPowerBehaviour(PowerBehavior.LINEAR) //
						.build())
				.next(new TestCase() //
						.input(Sum.ChannelId.ESS_DISCHARGE_POWER, 0) //
						.input(Sum.ChannelId.GRID_ACTIVE_POWER, -5000)//
						.output(DEBUG_SET_OUTPUT_PERCENT, 50f) //
						.output(DEBUG_SET_OUTPUT_VOLTAGE, 5000)) //
				.next(new TestCase() //
						.input(Sum.ChannelId.ESS_DISCHARGE_POWER, 0) //
						.input(Sum.ChannelId.GRID_ACTIVE_POWER, -2444)//
						.output(DEBUG_SET_OUTPUT_PERCENT, 50f) //
						.output(DEBUG_SET_OUTPUT_VOLTAGE, 5000)) //

				.next(new TestCase() //
						.timeleap(clock, 4, ChronoUnit.SECONDS) //
						.input(Sum.ChannelId.ESS_DISCHARGE_POWER, 0) //
						.input(Sum.ChannelId.GRID_ACTIVE_POWER, -2444)//
						.output(DEBUG_SET_OUTPUT_PERCENT, 24.439999f) //
						.output(DEBUG_SET_OUTPUT_VOLTAGE, 2400)) // 100mV Steps

				.next(new TestCase() //
						.timeleap(clock, 4, ChronoUnit.SECONDS) //
						.input(Sum.ChannelId.ESS_DISCHARGE_POWER, 0) //
						.input(Sum.ChannelId.GRID_ACTIVE_POWER, -12444)//
						.output(DEBUG_SET_OUTPUT_PERCENT, 100f) //
						.output(DEBUG_SET_OUTPUT_VOLTAGE, 10000)) //

				.next(new TestCase() //
						.timeleap(clock, 4, ChronoUnit.SECONDS) //
						.input(Sum.ChannelId.ESS_DISCHARGE_POWER, 0) //
						.input(Sum.ChannelId.GRID_ACTIVE_POWER, 1000)//
						.output(DEBUG_SET_OUTPUT_PERCENT, 0f) //
						.output(DEBUG_SET_OUTPUT_VOLTAGE, 0)) //

				.deactivate();
	}
}
