package io.openems.edge.evcc.solartariff;

import static io.openems.common.test.TestUtils.createDummyClock;

import org.junit.Test;

import io.openems.edge.bridge.http.dummy.DummyBridgeHttpBundle;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.evcc.solartariff.PredictorSolarTariffEvccImpl;
import io.openems.edge.predictor.api.prediction.LogVerbosity;

public class PredictorSolarTariffEvccImplTest {

	@Test
	public void test() throws Exception {
		final var httpTestBundle = new DummyBridgeHttpBundle();
		final var clock = createDummyClock();
		new ComponentTest(new PredictorSolarTariffEvccImpl()) //
				.addReference("httpBridgeFactory", httpTestBundle.factory()) //
				.addReference("componentManager",
						new DummyComponentManager(clock)) //				
				.activate(MyConfig.create() //
						.setId("predictor0") //
						.setUrl("http://evcc:7070/api/tariff/solar") //
						.setLogVerbosity(LogVerbosity.REQUESTED_PREDICTIONS) //
						.build()) //
				.deactivate();
	}

}
