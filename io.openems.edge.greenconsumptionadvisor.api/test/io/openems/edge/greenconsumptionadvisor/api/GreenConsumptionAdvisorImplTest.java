package io.openems.edge.greenconsumptionadvisor.api;

import org.junit.Test;

import io.openems.edge.common.test.ComponentTest;

public class GreenConsumptionAdvisorImplTest {

	private static final String COMPONENT_ID = "greenConsumptionAdvisor0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new GreenConsumptionAdvisorImpl()) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setZipCode("52070") //
						.build()); //
	}

}
