package io.openems.edge.bridge.modbus.tester;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.common.test.DummyConfigurationAdmin;

public class BridgeModbusTesterImplTest {

	@Test
	public void testActivateDeactivate() throws Exception {
		var bridge = new DummyModbusBridge("modbus0") //
				.withRegisters(0, 0x003F, 0x00A1);

		new ComponentTest(new BridgeModbusTesterImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", bridge) //
				.activate(MyConfig.create() //
						.setId("modbusTester0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setRegisterAddress(0) //
						.setRegisterCount(2) //
						.setModbusProtocolType(ModbusProtocolType.TCP) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}

	@Test
	public void testSingleRegister() throws Exception {
		var bridge = new DummyModbusBridge("modbus0") //
				.withRegister(5, 0x1234);

		new ComponentTest(new BridgeModbusTesterImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", bridge) //
				.activate(MyConfig.create() //
						.setId("modbusTester0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setRegisterAddress(5) //
						.setRegisterCount(1) //
						.setModbusProtocolType(ModbusProtocolType.ASCII) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}

}
