package io.openems.edge.evcc.gridtariff;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.common.currency.Currency.EUR;

import org.junit.Test;

import io.openems.edge.bridge.http.dummy.DummyBridgeHttpBundle;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyMeta;
import io.openems.edge.evcc.gridtariff.TimeOfUseGridTariffEvccImpl;
import io.openems.edge.predictor.api.prediction.LogVerbosity;

public class TimeOfUseGridTariffEvccImplTest {

	@Test
	public void test() throws Exception {
		final var httpTestBundle = new DummyBridgeHttpBundle();
		final var clock = createDummyClock();
		var dummyMeta = new DummyMeta("foo0") //
				.withCurrency(EUR);
		new ComponentTest(new TimeOfUseGridTariffEvccImpl()) //
				.addReference("httpBridgeFactory", httpTestBundle.factory()) //
				.addReference("meta", dummyMeta) //
				.addReference("componentManager",
						new DummyComponentManager(clock)) //
				.activate(MyConfig.create() //
						.setId("timeofusetariff0") //
						.setApiUrl("http://evcc:7070/api/tariff/grid")
						.setLogVerbosity(LogVerbosity.REQUESTED_PREDICTIONS)
						.build()) //
				.deactivate();
	}

}
