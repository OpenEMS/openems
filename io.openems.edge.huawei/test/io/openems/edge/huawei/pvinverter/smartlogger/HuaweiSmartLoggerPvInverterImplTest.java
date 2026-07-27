package io.openems.edge.huawei.pvinverter.smartlogger;

import org.junit.jupiter.api.Test;

import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class HuaweiSmartLoggerPvInverterImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new HuaweiSmartLoggerPvInverterImpl()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("pvInverter0") //
						.setReadOnly(true) //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setType(MeterType.PRODUCTION) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}
}