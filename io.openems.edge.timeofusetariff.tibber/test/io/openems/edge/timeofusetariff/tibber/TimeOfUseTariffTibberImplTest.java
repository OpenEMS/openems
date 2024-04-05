package io.openems.edge.timeofusetariff.tibber;

import org.junit.Test;

import io.openems.edge.common.test.ComponentTest;

public class TimeOfUseTariffTibberImplTest {

	private static final String CTRL_ID = "ctrl0";

	@Test
	public void test() throws Exception {
		var tibber = new TimeOfUseTariffTibberImpl();
		new ComponentTest(tibber) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setAccessToken("foo-bar") //
						.setFilter("") //
						.build()) //
		;
		// tibber.task.run();
	}

}
