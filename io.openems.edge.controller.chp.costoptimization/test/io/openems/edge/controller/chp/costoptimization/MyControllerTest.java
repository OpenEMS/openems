package io.openems.edge.controller.chp.costoptimization;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.controller.chp.costoptimization.ControllerChpCostOptimizationImpl;
import io.openems.edge.controller.test.ControllerTest;

public class MyControllerTest {

	@Test
	public void test() throws Exception {
		new ControllerTest(new ControllerChpCostOptimizationImpl()) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}

}
