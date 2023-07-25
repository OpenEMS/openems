package io.openems.edge.timeofusetariff.entsoe;

import org.junit.Test;

import io.openems.edge.common.currency.Currency;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class TouEntsoeTest {

	private static final String COMPONENT_ID = "tou0";

	@Test
	public void test() throws Exception {
		var entsoe = new TouEntsoeImpl();
		new ComponentTest(entsoe) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setSecurityToken("29ea7484-f60c-421a-b312-9db19dfd930a") //
						.setBididngZone(BiddingZone.GERMANY) //
						.setCurrency(Currency.EUR) //
						.build())
				.next(new TestCase());

		// Thread.sleep(5000);
		// System.out.println("prices" + entsoe.getPrices().getValues());
	}

}
