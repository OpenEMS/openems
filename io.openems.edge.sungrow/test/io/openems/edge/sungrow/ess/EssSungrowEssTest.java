package io.openems.edge.sungrow.ess;

import org.junit.jupiter.api.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.sungrow.ess.enums.ControlMode;

public class EssSungrowEssTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new EssSungrowImpl()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setModbusId("modbus0") //
						.setControlMode(ControlMode.SMART) //
						.build())
				.next(new TestCase()) //
				.deactivate();
	}

}
