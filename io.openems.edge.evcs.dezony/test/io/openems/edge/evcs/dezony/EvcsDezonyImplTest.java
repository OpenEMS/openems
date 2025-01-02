package io.openems.edge.evcs.dezony;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class EvcsDezonyImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new EvcsDezonyImpl()) //
				.activate(MyConfig.create() //
						.setId("evcs0") //
						.setIp("192.168.50.88") //
						.setPort(5000) //
						.setMaxHwCurrent(32) //
						.setMinHwCurrent(6) //
						.build())
				.next(new TestCase());
	}

}
