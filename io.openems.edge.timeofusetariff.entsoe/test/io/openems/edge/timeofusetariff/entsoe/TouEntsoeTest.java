package io.openems.edge.timeofusetariff.entsoe;

import static io.openems.edge.common.currency.Currency.EUR;

import org.junit.Test;

import io.openems.common.oem.DummyOpenemsEdgeOem;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyMeta;

public class TouEntsoeTest {

	@Test
	public void test() throws Exception {
		var entsoe = new TouEntsoeImpl();
		var dummyMeta = new DummyMeta("foo0") //
				.withCurrency(EUR);
		new ComponentTest(entsoe) //
				.addReference("meta", dummyMeta) //
				.addReference("oem", new DummyOpenemsEdgeOem()) //
				.activate(MyConfig.create() //
						.setId("tou0") //
						.setSecurityToken("") //
						.setBiddingZone(BiddingZone.GERMANY) //
						.setResolution(Resolution.HOURLY) //
						.build());
	}
}
