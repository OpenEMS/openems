package io.openems.edge.controller.ess.timeofusetariff;

import static io.openems.edge.controller.ess.timeofusetariff.ControlMode.CHARGE_CONSUMPTION;
import static io.openems.edge.controller.ess.timeofusetariff.Mode.AUTOMATIC;
import static io.openems.edge.controller.ess.timeofusetariff.RiskLevel.MEDIUM;

import java.time.ZonedDateTime;

import org.junit.Test;

import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.predictor.api.test.DummyPredictorManager;
import io.openems.edge.timeofusetariff.test.DummyTimeOfUseTariffProvider;

public class TimeOfUseTariffControllerImplTest {

	private static final String CTRL_ID = "ctrl0";

	@Test
	public void test() throws Exception {
		new ControllerTest(new TimeOfUseTariffControllerImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("predictorManager", new DummyPredictorManager()) //
				.addReference("timeOfUseTariff", new DummyTimeOfUseTariffProvider(ZonedDateTime.now())) //
				.addReference("sum", new DummySum()) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId("ess0") //
						.setMode(AUTOMATIC) //
						.setControlMode(CHARGE_CONSUMPTION) //
						.setRiskLevel(
								MEDIUM) //
						.build()); //
	}
}
