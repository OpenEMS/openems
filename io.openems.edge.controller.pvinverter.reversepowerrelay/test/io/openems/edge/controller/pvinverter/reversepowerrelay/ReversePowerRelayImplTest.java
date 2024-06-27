package io.openems.edge.controller.pvinverter.reversepowerrelay;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;

import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.meter.test.DummyElectricityMeter;
import io.openems.edge.pvinverter.test.DummyManagedSymmetricPvInverter;

import io.openems.edge.io.test.DummyInputOutput;

public class ReversePowerRelayImplTest {

	private static final String CTRL_ID = "ctrl0";

	private static final String METER_ID = "meter0";
	private static final ChannelAddress GRID_ACTIVE_POWER = new ChannelAddress(METER_ID, "ActivePower");

	private static final String PV_INVERTER = "pvInverter0";
	private static final ChannelAddress PV_INVERTER_ACTIVE_POWER = new ChannelAddress(PV_INVERTER, "ActivePower");
	private static final ChannelAddress PV_INVERTER_SET_ACTIVE_POWER_EQUALS = new ChannelAddress(PV_INVERTER,
			"ActivePowerLimit");

	private static final String IO_ID = "io0";
	private static final ChannelAddress IO_INPUT_OUTPUT0 = new ChannelAddress(IO_ID, "InputOutput0");
	private static final ChannelAddress IO_INPUT_OUTPUT1 = new ChannelAddress(IO_ID, "InputOutput1");
	private static final ChannelAddress IO_INPUT_OUTPUT2 = new ChannelAddress(IO_ID, "InputOutput2");
	private static final ChannelAddress IO_INPUT_OUTPUT3 = new ChannelAddress(IO_ID, "InputOutput3");

	@Test
	public void testReversePowerRelay() throws Exception {
		new ControllerTest(new ReversePowerRelayImpl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addComponent(new DummyElectricityMeter(METER_ID)) //
				.addComponent(new DummyManagedSymmetricPvInverter(PV_INVERTER)) //

				.addComponent(new DummyInputOutput(IO_ID)) //

				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setPvInverterId(PV_INVERTER) //
						.setId(CTRL_ID) //
						.setInputChannelAddress0Percent(IO_INPUT_OUTPUT0.toString()) //
						.setInputChannelAddress30Percent(IO_INPUT_OUTPUT1.toString()) //
						.setInputChannelAddress60Percent(IO_INPUT_OUTPUT2.toString()) //
						.setInputChannelAddress100Percent(IO_INPUT_OUTPUT3.toString()) //
						.setPowerLimit30(30) //
						.setPowerLimit60(60) //
						.build())
				.next(new TestCase() // undefined
						.input(GRID_ACTIVE_POWER, -15000) //
						.input(PV_INVERTER_ACTIVE_POWER, 15000) //
						.input(IO_INPUT_OUTPUT0, true) //
						.input(IO_INPUT_OUTPUT1, true) //
						.input(IO_INPUT_OUTPUT2, true) //
						.input(IO_INPUT_OUTPUT3, true) //
						.output(PV_INVERTER_SET_ACTIVE_POWER_EQUALS, 0))

				.next(new TestCase() // 100% -> limit null
						.input(GRID_ACTIVE_POWER, -15000) //
						.input(PV_INVERTER_ACTIVE_POWER, 1000) //
						.input(IO_INPUT_OUTPUT0, false) //
						.input(IO_INPUT_OUTPUT1, false) //
						.input(IO_INPUT_OUTPUT2, false) //
						.input(IO_INPUT_OUTPUT3, true) //
						.output(PV_INVERTER_SET_ACTIVE_POWER_EQUALS, null))

				.next(new TestCase() // 100% -> limit null
						.input(GRID_ACTIVE_POWER, -15000) //
						.input(PV_INVERTER_ACTIVE_POWER, 1000) //
						.input(IO_INPUT_OUTPUT0, false) //
						.input(IO_INPUT_OUTPUT1, true) //
						.input(IO_INPUT_OUTPUT2, false) //
						.input(IO_INPUT_OUTPUT3, false) //
						.output(PV_INVERTER_SET_ACTIVE_POWER_EQUALS, 30))

		; //

	}
}
