package io.openems.edge.timeofusetariff.manual.octopus.go;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class TouOctopusGoImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new TouOctopusGoImpl()) //
				.activate(MyConfig.create() //
						.setId("tou0") //
						.setStandardPrice(0.30) //
						.setLowPrice(0.20) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}

}
