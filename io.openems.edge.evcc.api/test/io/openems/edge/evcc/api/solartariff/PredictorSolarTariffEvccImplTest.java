package io.openems.edge.evcc.api.solartariff;

import org.junit.Test;

import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.predictor.api.prediction.LogVerbosity;

public class PredictorSolarTariffEvccImplTest {

	@Test
	public void test() throws Exception {
		String[] channels = { "_sum/ProductionActivePower" };
		new ComponentTest(new PredictorSolarTariffEvccImpl()) //
				.activate(MyConfig.create() //
						.setId("predictor0") //
						.setUrl("http://evcc:7070/api/tariff/solar") //
						.setLogVerbosity(LogVerbosity.REQUESTED_PREDICTIONS) //
						.setChannelAddresses(channels) //
						.build()) //
		;
	}

}
