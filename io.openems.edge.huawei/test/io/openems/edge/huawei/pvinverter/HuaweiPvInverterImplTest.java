package io.openems.edge.huawei.pvinverter;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.ComponentTest;

public class HuaweiPvInverterImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new HuaweiPvInverterImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("pvInverter0") //
						.setReadOnly(true) //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setMaxApparentPower(10000) //
						.setScaleFactor(1) //
						.setType(MeterType.PRODUCTION) //
						.build()) //
				.deactivate() //
		;

	}
}