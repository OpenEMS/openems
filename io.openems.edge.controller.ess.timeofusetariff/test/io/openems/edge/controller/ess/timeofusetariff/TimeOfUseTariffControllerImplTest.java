package io.openems.edge.controller.ess.timeofusetariff;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.controller.ess.timeofusetariff.ControlMode.CHARGE_CONSUMPTION;
import static io.openems.edge.controller.ess.timeofusetariff.Mode.AUTOMATIC;
import static io.openems.edge.controller.ess.timeofusetariff.StateMachine.BALANCING;

import java.time.Clock;

import org.junit.jupiter.api.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyMeta;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.test.DummyTimedata;
import io.openems.edge.timeofusetariff.test.DummyTariffGridSellProvider;
import io.openems.edge.timeofusetariff.test.DummyTariffManager;
import io.openems.edge.timeofusetariff.test.DummyTimeOfUseTariffProvider;

public class TimeOfUseTariffControllerImplTest {

	@Test
	public void test() throws Exception {
		final var clock = createDummyClock();
		create(//
				clock, //
				new DummyManagedSymmetricEss("ess0") //
						.withSoc(60) //
						.withCapacity(10000), //
				new DummyTimedata("timedata0")) //
				.deactivate();
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
		var tariffManager = new DummyTariffManager()//
				.withTariffGridBuyProvider(DummyTimeOfUseTariffProvider.empty(clock))//
				.withTariffGridSellProvider(DummyTariffGridSellProvider.empty(clock));

		var sut = new TimeOfUseTariffControllerImpl();
		new ControllerTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", componentManager) //
				.addReference("timedata", timedata) //
				.addReference("tariffManager", tariffManager) //
				.addReference("sum", sum) //
				.addReference("ess", ess) //
				.addReference("meta", new DummyMeta())//
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setEnabled(true) //
						.setEssId("ess0") //
						.setMode(AUTOMATIC) //
						.setManualMode(BALANCING) //
						.setControlMode(CHARGE_CONSUMPTION) //
						.setEssMaxChargePower(5000) //
						.setMaxChargePowerFromGrid(10000) //
						.build()) //
				.next(new TestCase());
		return sut;
	}
}
