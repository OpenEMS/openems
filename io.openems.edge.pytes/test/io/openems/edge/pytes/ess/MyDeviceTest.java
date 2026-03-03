package io.openems.edge.pytes.ess;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.pytes.ess.PytesJs3Impl;
import io.openems.edge.common.test.ComponentTest;

public class MyDeviceTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new PytesJs3Impl()) //
				.activate(MyConfig.create() //
						.setId("component0") //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}

}
