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
import io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffControllerImpl.EshContext;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.energy.api.EnergyScheduleHandler;
import io.openems.edge.energy.api.Version;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.test.DummyTimedata;
import io.openems.edge.timeofusetariff.test.DummyTimeOfUseTariffProvider;

public class TimeOfUseTariffControllerImplTest {

	public static final Clock CLOCK = new TimeLeapClock(Instant.parse("2020-03-04T14:19:00.00Z"), ZoneOffset.UTC);

	private static final String CTRL_ID = "ctrl0";

	@Test
	public void test() throws Exception {
		create(CLOCK, //
				new DummyManagedSymmetricEss("ess0") //
						.withSoc(60) //
						.withCapacity(10000), //
				new DummyTimedata("timedata0"));
	}

	/**
	 * Creates a {@link TimeOfUseTariffControllerImpl} instance.
	 * 
	 * @param clock    a {@link Clock}
	 * @param ess      the {@link SymmetricEss}
	 * @param timedata the {@link Timedata}
	 * @return the object
	 * @throws Exception on error
	 */
	public static TimeOfUseTariffControllerImpl create(Clock clock, SymmetricEss ess, Timedata timedata)
			throws Exception {
		var componentManager = new DummyComponentManager(clock);
		var sum = new DummySum();
		var timeOfUseTariff = DummyTimeOfUseTariffProvider.empty(clock);

		var sut = new TimeOfUseTariffControllerImpl();
		new ControllerTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", componentManager) //
				.addReference("timedata", timedata) //
				.addReference("timeOfUseTariff", timeOfUseTariff) //
				.addReference("sum", sum) //
				.addReference("ess", ess) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEnabled(false) //
						.setEssId("ess0") //
						.setMode(AUTOMATIC) //
						.setControlMode(CHARGE_CONSUMPTION) //
						.setEssMaxChargePower(5000) //
						.setMaxChargePowerFromGrid(10000) //
						.setLimitChargePowerFor14aEnWG(false) //
						.setVersion(Version.V2_ENERGY_SCHEDULABLE) //
						.setRiskLevel(MEDIUM) //
						.build()) //
				.next(new TestCase());
		return sut;
	}

	/**
	 * Gets the {@link EnergyScheduleHandler}.
	 * 
	 * @param ctrl the {@link TimeOfUseTariffControllerImpl}
	 * @return the object
	 * @throws Exception on error
	 */
	public static EnergyScheduleHandler.WithDifferentStates<StateMachine, EshContext> getEnergyScheduleHandler(
			TimeOfUseTariffControllerImpl ctrl) throws Exception {
		return ctrl.getEnergyScheduleHandler();
	}
}
