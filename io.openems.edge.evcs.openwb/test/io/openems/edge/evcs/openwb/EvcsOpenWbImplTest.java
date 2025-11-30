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
						.setMqttUri("tcp://127.0.0.1:1883") //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}
}
