package io.openems.edge.timeofusetariff.entsoe;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class TouEntsoeTest {

	private static final String COMPONENT_ID = "tou0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new TouEntsoeImpl()) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.build())
				.next(new TestCase());
	}

}
