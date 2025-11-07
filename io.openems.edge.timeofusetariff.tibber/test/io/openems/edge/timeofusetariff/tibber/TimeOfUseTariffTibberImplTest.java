package io.openems.edge.timeofusetariff.tibber;

import org.junit.Test;

import io.openems.edge.common.test.ComponentTest;

public class TimeOfUseTariffTibberImplTest {

	@Test
	public void test() throws Exception {
		final var sut = new TimeOfUseTariffTibberImpl();
		new ComponentTest(sut) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setAccessToken("foo-bar") //
						.setFilter("") //
						.build()) //
		;
		// sut.task.run();
	}

}
