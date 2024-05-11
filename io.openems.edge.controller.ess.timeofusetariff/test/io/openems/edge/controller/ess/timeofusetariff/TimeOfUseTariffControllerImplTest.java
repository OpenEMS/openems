package io.openems.edge.controller.ess.timeofusetariff;

import static io.openems.edge.controller.ess.timeofusetariff.ControlMode.CHARGE_CONSUMPTION;
import static io.openems.edge.controller.ess.timeofusetariff.Mode.AUTOMATIC;
import static io.openems.edge.controller.ess.timeofusetariff.RiskLevel.MEDIUM;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.Test;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.timedata.test.DummyTimedata;
import io.openems.edge.timeofusetariff.test.DummyTimeOfUseTariffProvider;

public class TimeOfUseTariffControllerImplTest {

	public static final Clock CLOCK = new TimeLeapClock(Instant.parse("2020-03-04T14:19:00.00Z"), ZoneOffset.UTC);

	private static final String CTRL_ID = "ctrl0";

	@Test
	public void test() throws Exception {
		create(CLOCK);
	}

	/**
	 * Creates a {@link TimeOfUseTariffControllerImpl} instance.
	 * 
	 * @param clock a {@link Clock}
	 * @return the object
	 * @throws Exception on error
	 */
	public static TimeOfUseTariffControllerImpl create(Clock clock) throws Exception {
		var componentManager = new DummyComponentManager(clock);
		var sum = new DummySum();
		var timeOfUseTariff = DummyTimeOfUseTariffProvider.empty(clock);

		var sut = new TimeOfUseTariffControllerImpl();
		new ControllerTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", componentManager) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.addReference("timeOfUseTariff", timeOfUseTariff) //
				.addReference("sum", sum) //
				.addReference("ess", new DummyManagedSymmetricEss("ess0") //
						.withSoc(60) //
						.withCapacity(10000)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEnabled(false) //
						.setEssId("ess0") //
						.setMode(AUTOMATIC) //
						.setControlMode(CHARGE_CONSUMPTION) //
						.setEssMaxChargePower(5000) //
						.setMaxChargePowerFromGrid(10000) //
						.setLimitChargePowerFor14aEnWG(false) //
						.setRiskLevel(MEDIUM) //
						.build()) //
				.next(new TestCase());
		return sut;
	}
}
