package io.openems.edge.kaco.blueplanet.hybrid10.core;

import org.junit.Test;

import io.openems.edge.common.test.ComponentTest;

public class KacoBlueplanetHybrid10CoreImplTest {

	private static final String CORE_ID = "kacoCore0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new KacoBlueplanetHybrid10CoreImpl()) //
				.activate(MyConfig.create() //
						.setId(CORE_ID) //
						.setIdentkey("") //
						.setIp("192.168.0.1") //
						.setSerialnumber("123456") //
						.setUserkey("user") //
						.build()) //
		;
	}
}
