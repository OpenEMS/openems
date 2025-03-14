package io.openems.edge.meter.carlo.gavazzi.em100;

import static io.openems.common.types.MeterType.GRID;
import static io.openems.edge.meter.api.SinglePhase.L1;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class MeterCarloGavazziEm100ImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new MeterCarloGavazziEm100Impl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("meter0") //
						.setModbusId("modbus0") //
						.setType(GRID) //
						.setPhase(L1) //
						.setInvert(false) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}
}