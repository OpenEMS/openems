package io.openems.edge.shelly;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.shelly.core.ShellyCoreImpl;
import io.openems.edge.common.test.ComponentTest;

public class MyDeviceTest {

	private static final String COMPONENT_ID = "component0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new ShellyCoreImpl()) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.build())
				.next(new TestCase());
	}

}
