package io.openems.edge.ess.refubeckhoff;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.ess.refubeckhoff.enums.EssState;
import io.openems.edge.ess.refubeckhoff.enums.SetOperationMode;
import io.openems.edge.ess.refubeckhoff.enums.StopStart;
import io.openems.edge.ess.refubeckhoff.enums.SystemState;
import io.openems.edge.ess.test.DummyPower;

public class MyRefuDeviceTest {

	private static final String ESS_ID = "ess0";
	private static final String MODBUS_ID = "modbus0";

	private static final ChannelAddress SYSTEMSTATE = new ChannelAddress(ESS_ID, "SystemState");
	private static final ChannelAddress SETWORKSTATE = new ChannelAddress(ESS_ID, "SetWorkState");

	@Test
	public void test() throws Exception {
		new ComponentTest(new RefuBeckhoffEssImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("power", new DummyPower()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(ESS_ID) //
						.setModbusId(MODBUS_ID) //
						.setEssState(EssState.AUTO) //
						.setSetOperationMode(SetOperationMode.PQ_SET_POINT) //
						.setAcknowledgeError(10) //
						.setEssState(EssState.AUTO).build())
				.next(new TestCase("1") //
						.input(SYSTEMSTATE, SystemState.INIT)) //
				.next(new TestCase("2") //
						.output(SETWORKSTATE, StopStart.START)) //
				.next(new TestCase("3") //
						.input(SYSTEMSTATE, SystemState.OFF)) //
				.next(new TestCase("4")) //
				.next(new TestCase("5") //
						.output(SYSTEMSTATE, SystemState.OFF)) //
				.next(new TestCase("6") //
						.input(SYSTEMSTATE, SystemState.ERROR)) //
				.next(new TestCase("1 times")) //
				.next(new TestCase("2 times")) //
				.next(new TestCase("3 times")) //
				.next(new TestCase("4 times")) //
				.next(new TestCase("5 times")) //
				.next(new TestCase("6 times")) //
				.next(new TestCase("7 times")) //
				.next(new TestCase("8 times")) //
				.next(new TestCase("9 times")) //
				.next(new TestCase("10 times")) //
				.next(new TestCase("7") //
						.output(SYSTEMSTATE, SystemState.ERROR)) //
				.next(new TestCase("8")) //
				.next(new TestCase("9") //
						.output(SYSTEMSTATE, SystemState.ERROR)) //
				.next(new TestCase("10")) //
		; //
	}

}
