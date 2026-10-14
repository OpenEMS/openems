package io.openems.edge.aircooling.blueeplus;

import org.junit.jupiter.api.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class BlueEPlusTest {

	@Test
	public void testM0Module() throws Exception {
		new ComponentTest(new BlueEPlusImpl()) //
				.addReference("cm", new DummyConfigurationAdmin())//
				.activate(MyConfig.create() //
						.setId("airCooling0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(2) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}
}
