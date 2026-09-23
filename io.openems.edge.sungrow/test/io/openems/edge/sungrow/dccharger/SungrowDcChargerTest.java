package io.openems.edge.sungrow.dccharger;

import org.junit.jupiter.api.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.sungrow.ess.EssSungrowImpl;

public class SungrowDcChargerTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new SungrowDcChargerImpl()) //
				.addReference("ess", new EssSungrowImpl()) //
				.activate(MyConfig.create() //
						.setId("charger0") //
						.setEssId("ess0") //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}

}