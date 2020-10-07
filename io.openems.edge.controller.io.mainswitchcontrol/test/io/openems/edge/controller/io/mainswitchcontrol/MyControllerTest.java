package io.openems.edge.controller.io.mainswitchcontrol;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.controller.test.ControllerTest;

public class MyControllerTest {

	private static final String CTRL_ID = "ctrl0";

	@Test
	private void test() throws Exception {
		new ControllerTest(new ControllerMainSwitch()) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.build())
				.next(new TestCase());
	}

}
