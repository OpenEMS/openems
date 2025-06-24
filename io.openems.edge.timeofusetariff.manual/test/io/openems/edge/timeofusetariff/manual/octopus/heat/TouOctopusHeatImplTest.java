package io.openems.edge.timeofusetariff.manual.octopus.heat;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class TouOctopusHeatImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new TouOctopusHeatImpl()) //
				.activate(MyConfig.create() //
						.setId("tou0") //
						.setHighPrice(0.40) //
						.setStandardPrice(0.30) //
						.setLowPrice(0.20) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}

}
