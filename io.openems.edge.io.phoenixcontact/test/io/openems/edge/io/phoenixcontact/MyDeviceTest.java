package io.openems.edge.io.phoenixcontact;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class MyDeviceTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new PlcNextHttpDeviceImpl()) //
				.activate(MyConfig.create() //
						.setId("component0") //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}

}
