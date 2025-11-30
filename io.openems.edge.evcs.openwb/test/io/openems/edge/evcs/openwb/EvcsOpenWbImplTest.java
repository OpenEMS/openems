package io.openems.edge.evcs.openwb;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class EvcsOpenWbImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new EvcsOpenWbImpl()) //
				.activate(MyConfig.create() //
						.setId("evcs0") //
						.setIp("127.0.0.1") //
						.setPort(443) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}
}
