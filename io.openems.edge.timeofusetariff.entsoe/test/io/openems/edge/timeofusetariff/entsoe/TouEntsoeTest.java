package io.openems.edge.timeofusetariff.entsoe;

import org.junit.Test;

import io.openems.edge.common.test.ComponentTest;

public class TouEntsoeTest {

	private static final String COMPONENT_ID = "tou0";
	private static final String CURRENCY_PROVIDER_NAME = "currencyProvider";

	@Test
	public void test() throws Exception {
		var entsoe = new TouEntsoeImpl();
		new ComponentTest(entsoe) //
				.addReference(CURRENCY_PROVIDER_NAME, new CurrencyProviderTest())//
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setSecurityToken("foo-bar") //
						.setBiddingZone(BiddingZone.GERMANY) //
						.build());

		// Thread.sleep(5000);
	}
}
