package io.openems.edge.sungrow.meter;

import org.junit.jupiter.api.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.sungrow.ess.EssSungrowImpl;

public class SungrowGridMeterTest {

	private static final String METER_ID = "meter0";
	private static final String ESS_ID = "ess0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new SungrowGridMeterImpl()) //
				.addReference("ess", new EssSungrowImpl()) //
				.activate(MyConfig.create() //
						.setId(METER_ID) //
						.setEssId(ESS_ID) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}

}