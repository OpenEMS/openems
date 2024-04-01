package io.openems.edge.ess.sungrow.dccharger;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class MyModbusDeviceTest {

	private static final String COMPONENT_ID = "charger0";
	private static final String CORE_ID = "ess0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new SungrowVirtualDcCharger()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setCoreId(CORE_ID) //
						.build())
				.next(new TestCase());
	}

}
