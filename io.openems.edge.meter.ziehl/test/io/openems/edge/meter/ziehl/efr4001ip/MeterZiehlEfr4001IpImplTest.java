package io.openems.edge.meter.ziehl.efr4001ip;

import static io.openems.common.types.MeterType.GRID;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class MeterZiehlEfr4001IpImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new MeterZiehlEfr4001IpImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("component0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setMeterType(GRID) //
						.setInvert(false) //
						.build())
				.next(new TestCase());
	}

}
