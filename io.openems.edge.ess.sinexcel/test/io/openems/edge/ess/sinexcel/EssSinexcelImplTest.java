package io.openems.edge.ess.sinexcel;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.battery.test.DummyBattery;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.ess.sinexcel.statemachine.StateMachine;
import io.openems.edge.ess.test.DummyPower;
import io.openems.edge.ess.test.ManagedSymmetricEssTest;
import io.openems.edge.io.test.DummyInputOutput;

public class EssSinexcelImplTest {

	private static final String ESS_ID = "ess0";
	private static final String MODBUS_ID = "modbus0";
	private static final String BATTERY_ID = "battery0";
	
	private static final ChannelAddress STATE_MACHINE = new ChannelAddress(ESS_ID, "StateMachine");

	private final static String IO_ID = "io0";
	private static final ChannelAddress DIGITAL_INPUT_1 = new ChannelAddress(IO_ID, "InputOutput0");
	private static final ChannelAddress DIGITAL_INPUT_2 = new ChannelAddress(IO_ID, "InputOutput1");
	private static final ChannelAddress DIGITAL_INPUT_3 = new ChannelAddress(IO_ID, "InputOutput2");
	private static final ChannelAddress DIGITAL_OUTPUT_1 = new ChannelAddress(IO_ID, "InputOutput3");
	private static final ChannelAddress DIGITAL_OUTPUT_2 = new ChannelAddress(IO_ID, "InputOutput4");
	private static final ChannelAddress DIGITAL_OUTPUT_3 = new ChannelAddress(IO_ID, "InputOutput5");

	@Test
	public void test() throws Exception {
		new ManagedSymmetricEssTest(new EssSinexcelImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("power", new DummyPower()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.addReference("setBattery", new DummyBattery(BATTERY_ID)) //
				.addComponent(new DummyInputOutput(IO_ID)) //
				.activate(MyConfig.create() //
						.setId(ESS_ID) //
						.setModbusId(MODBUS_ID) //
						.setBatteryId(BATTERY_ID) //
						.setInverterState(InverterState.ON) //
						.setToppingCharge(3900) //
						.setDigitalInput1(DIGITAL_INPUT_1.toString()) //
						.setDigitalInput2(DIGITAL_INPUT_2.toString()) //
						.setDigitalInput3(DIGITAL_INPUT_3.toString()) //
						.setDigitalOutput1(DIGITAL_OUTPUT_1.toString()) //
						.setDigitalOutput2(DIGITAL_OUTPUT_2.toString()) //
						.setDigitalOutput3(DIGITAL_OUTPUT_3.toString()) //						
						.build()) //
				.next(new TestCase() //
						.output(STATE_MACHINE, StateMachine.State.UNDEFINED)) //
				.next(new TestCase() //
						.input(DIGITAL_INPUT_1, false) //
						.input(DIGITAL_INPUT_2, false) //
						.input(DIGITAL_INPUT_3, true)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, StateMachine.State.TOTAL_ONGRID))//						
				.next(new TestCase() //
						.output(STATE_MACHINE, StateMachine.State.START)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, StateMachine.State.TOTAL_ONGRID)) //				
				.next(new TestCase() //
						.input(DIGITAL_INPUT_1, false) //
						.input(DIGITAL_INPUT_2, true) //
						.input(DIGITAL_INPUT_3, false)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, StateMachine.State.STOP)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, StateMachine.State.GROUNDSET)
						.output(DIGITAL_OUTPUT_2, false)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, StateMachine.State.TOTAL_OFFGRID)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, StateMachine.State.START)) //
				.next(new TestCase())
				.next(new TestCase() //
						.output(STATE_MACHINE, StateMachine.State.TOTAL_OFFGRID)) //
				.next(new TestCase())
				.next(new TestCase() //
						.output(STATE_MACHINE, StateMachine.State.TOTAL_OFFGRID)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, StateMachine.State.TOTAL_OFFGRID)) //
				.next(new TestCase() //
						.input(DIGITAL_INPUT_1, false) //
						.input(DIGITAL_INPUT_2, false) //
						.input(DIGITAL_INPUT_3, true)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, StateMachine.State.STOP)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, StateMachine.State.GROUNDSET)
						.output(DIGITAL_OUTPUT_2, true)
						.output(DIGITAL_OUTPUT_1, false)) //
				.next(new TestCase() //
						.output(STATE_MACHINE, StateMachine.State.TOTAL_ONGRID)) //

				.next(new TestCase() //
						.output(STATE_MACHINE, StateMachine.State.TOTAL_ONGRID)) //
				
				
		;
	}

}
