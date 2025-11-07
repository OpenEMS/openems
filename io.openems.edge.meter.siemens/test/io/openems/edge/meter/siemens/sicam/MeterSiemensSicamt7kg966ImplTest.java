package io.openems.edge.meter.siemens.sicam;

import static io.openems.common.types.MeterType.GRID;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;

public class MeterSiemensSicamt7kg966ImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new MeterSiemensSicamt7kg966Impl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("meter0") //
						.setModbusId("modbus0") //
						.setType(GRID) //
						.build()) //
		;
	}

}