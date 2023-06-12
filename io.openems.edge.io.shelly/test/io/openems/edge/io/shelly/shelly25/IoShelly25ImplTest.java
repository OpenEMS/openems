package io.openems.edge.io.shelly.shelly25;

import org.junit.Test;

import io.openems.edge.common.test.ComponentTest;

public class IoShelly25ImplTest {

	private static final String COMPONENT_ID = "io0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new IoShelly25Impl()) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setIp("127.0.0.1") //
						.build()) //
		;
	}

}
