package io.openems.edge.timeofusetariff.ews;

import org.junit.Test;

import io.openems.edge.common.test.ComponentTest;

public class TimeOfUseTariffEwsImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new TimeOfUseTariffEwsImpl()) //
		.activate(MyConfig.create() //
				.setId("ctrl0") //
				.setAccessToken("foo-bar") //
				.build()) //
		;
	}

}
