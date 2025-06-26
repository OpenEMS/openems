package io.openems.edge.tesla.powerwall2.core;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class TeslaPowerwall2CoreImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new TeslaPowerwall2CoreImpl()) //
				.activate(MyConfig.create() //
						.setId("core0") //
						.setIpAddress("127.0.0.1") //
						.setPort(443) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}
}
