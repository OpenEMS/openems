package io.openems.edge.evcc.api.gridtariff;

import org.junit.Test;

import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.predictor.api.prediction.LogVerbosity;

public class TimeOfUseGridTariffEvccImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new TimeOfUseGridTariffEvccImpl()) //
				.activate(MyConfig.create() //
						.setId("timeofusetariff0") //
						.setApiUrl("http://evcc:7070/api/tariff/grid")
						.setLogVerbosity(LogVerbosity.REQUESTED_PREDICTIONS)
						.build()) //
		;
	}

}
