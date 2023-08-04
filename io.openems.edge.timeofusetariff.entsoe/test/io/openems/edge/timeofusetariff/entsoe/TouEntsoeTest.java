package io.openems.edge.timeofusetariff.entsoe;

import org.junit.Test;

import io.openems.edge.common.currency.Currency;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyMeta;

public class TouEntsoeTest {

	private static final String COMPONENT_ID = "tou0";

	@Test
	public void test() throws Exception {
		var entsoe = new TouEntsoeImpl();
		var dummyMeta = new DummyMeta("foo0", Currency.EUR);
		new ComponentTest(entsoe) //
				.addReference("meta", dummyMeta)//
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setSecurityToken("foo-bar") //
						.setBiddingZone(BiddingZone.GERMANY) //
						.build());

		// Thread.sleep(5000);
	}
}
