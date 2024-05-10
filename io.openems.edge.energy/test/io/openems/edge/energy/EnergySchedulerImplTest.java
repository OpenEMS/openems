package io.openems.edge.energy;

import static io.openems.common.utils.DateUtils.roundDownToQuarter;
import static io.openems.edge.energy.TestData.CONSUMPTION_PREDICTION_QUARTERLY;
import static io.openems.edge.energy.TestData.HOURLY_PRICES_SUMMER;
import static io.openems.edge.energy.TestData.PRODUCTION_PREDICTION_QUARTERLY;
import static io.openems.edge.energy.optimizer.Utils.SUM_CONSUMPTION;
import static io.openems.edge.energy.optimizer.Utils.SUM_PRODUCTION;
import static java.time.temporal.ChronoUnit.DAYS;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.function.Supplier;

import org.junit.Test;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffControllerImpl;
import io.openems.edge.energy.optimizer.GlobalContext;
import io.openems.edge.energy.optimizer.Optimizer;
import io.openems.edge.predictor.api.prediction.Prediction;
import io.openems.edge.predictor.api.test.DummyPredictor;
import io.openems.edge.predictor.api.test.DummyPredictorManager;
import io.openems.edge.timedata.test.DummyTimedata;
import io.openems.edge.timeofusetariff.test.DummyTimeOfUseTariffProvider;

public class EnergySchedulerImplTest {

	public static final Clock CLOCK = new TimeLeapClock(Instant.parse("2020-03-04T14:19:00.00Z"), ZoneOffset.UTC);

	private static final String CTRL_ID = "ctrl0";

	@Test
	public void test() throws Exception {
		create(CLOCK);
	}

	/**
	 * Creates a {@link EnergySchedulerImplTest} instance.
	 * 
	 * @param clock a {@link Clock}
	 * @return the object
	 * @throws Exception on error
	 */
	public static EnergySchedulerImpl create(Clock clock) throws Exception {
		var now = roundDownToQuarter(ZonedDateTime.now(clock));
		final var midnight = now.truncatedTo(DAYS);
		var componentManager = new DummyComponentManager(clock);
		var sum = new DummySum();
		var predictor0 = new DummyPredictor("predictor0", componentManager,
				Prediction.from(sum, SUM_PRODUCTION, midnight, PRODUCTION_PREDICTION_QUARTERLY), SUM_PRODUCTION);
		var predictor1 = new DummyPredictor("predictor0", componentManager,
				Prediction.from(sum, SUM_CONSUMPTION, midnight, CONSUMPTION_PREDICTION_QUARTERLY), SUM_CONSUMPTION);
		var timeOfUseTariff = DummyTimeOfUseTariffProvider.fromHourlyPrices(clock, HOURLY_PRICES_SUMMER);
		var ctrl = new TimeOfUseTariffControllerImpl(); // this is not fully activated; config is null

		var sut = new EnergySchedulerImpl();
		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", componentManager) //
				.addReference("predictorManager", new DummyPredictorManager(predictor0, predictor1)) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.addReference("timeOfUseTariff", timeOfUseTariff) //
				.addReference("schedulables", List.of(ctrl)) //
				.addReference("sum", sum) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEnabled(false) //
						.setEssId("ess0") //
						.setEssMaxChargePower(5000) //
						.setMaxChargePowerFromGrid(10000) //
						.setLimitChargePowerFor14aEnWG(false) //
						.build()) //
				.next(new TestCase());
		return sut;
	}

	/**
	 * Gets the {@link Optimizer} via Java Reflection.
	 * 
	 * @param energyScheduler the {@link EnergySchedulerImpl}
	 * @return the object
	 * @throws Exception on error
	 */
	public static Optimizer getOptimizer(EnergySchedulerImpl energyScheduler) throws Exception {
		var field = EnergySchedulerImpl.class.getDeclaredField("optimizer");
		field.setAccessible(true);
		return (Optimizer) field.get(energyScheduler);
	}

	/**
	 * Calls the 'createParams()' method in the {@link Optimizer} via Java
	 * Reflection.
	 * 
	 * @param optimizer the {@link Optimizer}
	 * @throws Exception on error
	 */
	public static void callCreateParams(Optimizer optimizer) throws Exception {
		var method = Optimizer.class.getDeclaredMethod("createParams");
		method.setAccessible(true);
		method.invoke(optimizer);
	}

	/**
	 * Gets the {@link GlobalContext} via Java Reflection.
	 * 
	 * @param energyScheduler the {@link EnergySchedulerImpl}
	 * @return the object
	 * @throws Exception on error
	 */
	@SuppressWarnings("unchecked")
	public static GlobalContext getGlobalContext(EnergySchedulerImpl energyScheduler) throws Exception {
		var optimizer = getOptimizer(energyScheduler);
		var field = Optimizer.class.getDeclaredField("globalContext");
		field.setAccessible(true);
		return ((Supplier<GlobalContext>) field.get(optimizer)).get();
	}
}
