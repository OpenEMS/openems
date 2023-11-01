package io.openems.edge.controller.io.analog;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.test.TimeLeapClock;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.io.api.AnalogOutput.Range;
import io.openems.edge.io.test.DummyAnalogVoltageOutput;
import io.openems.edge.timedata.test.DummyTimedata;

public class MyControllerTest {

	private static final String CTRL_ID = "ctrl0";
	private static final String IO_ID = "analogIo0";

	// Sum channels
	private static final ChannelAddress SUM_ESS_DISCHARGE_POWER = new ChannelAddress("_sum", "EssDischargePower");
	private static final ChannelAddress SUM_GRID_ACTIVE_POWER = new ChannelAddress("_sum", "GridActivePower");

	// AnalogIO channels
	private static final ChannelAddress DEBUG_SET_OUTPUT_VOLTAGE = new ChannelAddress(IO_ID, "DebugSetOutputVoltage");
	private static final ChannelAddress DEBUG_SET_OUTPUT_PERCENT = new ChannelAddress(IO_ID, "DebugSetOutputPercent");

	@Test
	public void testOff() throws Exception {

		final var analogOutput = new DummyAnalogVoltageOutput(IO_ID);
		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);
		new ControllerTest(new ControllerIoAnalogImpl(clock)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.addReference("analogOutput", analogOutput) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setAnalogOutputId(IO_ID) //
						.setManualTarget(6_000) //
						.setMaximumPower(10_000) //
						.setMode(Mode.OFF) //
						.setPowerBehaviour(PowerBehavior.LINEAR) //
						.build())
				.next(new TestCase() //
						.input(SUM_ESS_DISCHARGE_POWER, 2000) //
						.input(SUM_GRID_ACTIVE_POWER, -5000)//
						.output(DEBUG_SET_OUTPUT_PERCENT, 0f) //
						.output(DEBUG_SET_OUTPUT_VOLTAGE, 0)) //
		;
	}

	@Test
	public void testOn() throws Exception {

		final var analogOutput = new DummyAnalogVoltageOutput(IO_ID, new Range(0, 100, 10000));
		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);
		new ControllerTest(new ControllerIoAnalogImpl(clock)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.addReference("analogOutput", analogOutput) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setAnalogOutputId(IO_ID) //
						.setManualTarget(6_000) //
						.setMaximumPower(10_000) //
						.setMode(Mode.ON) //
						.setPowerBehaviour(PowerBehavior.LINEAR) //
						.build())
				.next(new TestCase() //
						.input(SUM_ESS_DISCHARGE_POWER, 0) //
						.input(SUM_GRID_ACTIVE_POWER, -5000)//
						.output(DEBUG_SET_OUTPUT_PERCENT, 60.000004f) //
						.output(DEBUG_SET_OUTPUT_VOLTAGE, 6000)) //
		;

		new ControllerTest(new ControllerIoAnalogImpl(clock)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.addReference("analogOutput", analogOutput) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setAnalogOutputId(IO_ID) //
						.setManualTarget(0) //
						.setMaximumPower(10_000) //
						.setMode(Mode.ON) //
						.setPowerBehaviour(PowerBehavior.LINEAR) //
						.build())
				.next(new TestCase() //
						.input(SUM_ESS_DISCHARGE_POWER, 0) //
						.input(SUM_GRID_ACTIVE_POWER, -5000)//
						.output(DEBUG_SET_OUTPUT_PERCENT, 0f) //
						.output(DEBUG_SET_OUTPUT_VOLTAGE, 0)) //
		;
	}

	@Test
	public void testAutomatic() throws Exception {

		final var analogOutput = new DummyAnalogVoltageOutput(IO_ID);
		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);
		new ControllerTest(new ControllerIoAnalogImpl(clock)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("sum", new DummySum()) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.addReference("analogOutput", analogOutput) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setAnalogOutputId(IO_ID) //
						.setManualTarget(6_000) //
						.setMaximumPower(10_000) //
						.setMode(Mode.AUTOMATIC) //
						.setPowerBehaviour(PowerBehavior.LINEAR) //
						.build())
				.next(new TestCase() //
						.input(SUM_ESS_DISCHARGE_POWER, 0) //
						.input(SUM_GRID_ACTIVE_POWER, -5000)//
						.output(DEBUG_SET_OUTPUT_PERCENT, 50f) //
						.output(DEBUG_SET_OUTPUT_VOLTAGE, 5000)) //
				.next(new TestCase() //
						.input(SUM_ESS_DISCHARGE_POWER, 0) //
						.input(SUM_GRID_ACTIVE_POWER, -2444)//
						.output(DEBUG_SET_OUTPUT_PERCENT, 50f) //
						.output(DEBUG_SET_OUTPUT_VOLTAGE, 5000)) //

				.next(new TestCase() //
						.timeleap(clock, 4, ChronoUnit.SECONDS) //
						.input(SUM_ESS_DISCHARGE_POWER, 0) //
						.input(SUM_GRID_ACTIVE_POWER, -2444)//
						.output(DEBUG_SET_OUTPUT_PERCENT, 24.439999f) //
						.output(DEBUG_SET_OUTPUT_VOLTAGE, 2400)) // 100mV Steps

				.next(new TestCase() //
						.timeleap(clock, 4, ChronoUnit.SECONDS) //
						.input(SUM_ESS_DISCHARGE_POWER, 0) //
						.input(SUM_GRID_ACTIVE_POWER, -12444)//
						.output(DEBUG_SET_OUTPUT_PERCENT, 100f) //
						.output(DEBUG_SET_OUTPUT_VOLTAGE, 10000)) //

				.next(new TestCase() //
						.timeleap(clock, 4, ChronoUnit.SECONDS) //
						.input(SUM_ESS_DISCHARGE_POWER, 0) //
						.input(SUM_GRID_ACTIVE_POWER, 1000)//
						.output(DEBUG_SET_OUTPUT_PERCENT, 0f) //
						.output(DEBUG_SET_OUTPUT_VOLTAGE, 0)) //
		;
	}
}
