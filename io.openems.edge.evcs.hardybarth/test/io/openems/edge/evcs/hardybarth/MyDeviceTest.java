package io.openems.edge.evcs.hardybarth;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.evcs.hardybarth.HardyBarthImpl;
import io.openems.edge.common.test.ComponentTest;

public class MyDeviceTest {

	private static final String COMPONENT_ID = "component0";

	@Test
	private void test() throws Exception {
		new ComponentTest(new HardyBarthImpl()) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.build())
				.next(new TestCase());
	}

}
