package io.openems.edge.firealarmsystem.hekatron;

import org.junit.jupiter.api.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class HekatronB9X2Test {

	@Test
	public void testM0Module() throws Exception {
		final var blue0 = new HekatronB9X2Impl();
		new ComponentTest(blue0) //
				.addReference("cm", new DummyConfigurationAdmin())//
				.activate(MyConfig.create() //
						.setId("bma0") //
						.setConfigVersion(ConfigVersion.INDUSTRIAL_XL_V1)//
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}
}
