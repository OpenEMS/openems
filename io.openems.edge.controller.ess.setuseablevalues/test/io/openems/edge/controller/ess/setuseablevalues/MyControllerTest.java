package io.openems.edge.controller.ess.setuseablevalues;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.controller.test.ControllerTest;

public class MyControllerTest {

	@Test
	public void test() throws Exception {
		new ControllerTest(new ControllerEssSetUseableValuesImpl()) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}

}
