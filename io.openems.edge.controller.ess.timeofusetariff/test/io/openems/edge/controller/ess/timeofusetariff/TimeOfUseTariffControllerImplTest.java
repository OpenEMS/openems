package io.openems.edge.controller.ess.timeofusetariff;

import static io.openems.common.utils.DateUtils.roundDownToQuarter;
import static io.openems.edge.controller.ess.timeofusetariff.ControlMode.CHARGE_CONSUMPTION;
import static io.openems.edge.controller.ess.timeofusetariff.Mode.AUTOMATIC;
import static io.openems.edge.controller.ess.timeofusetariff.RiskLevel.MEDIUM;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.CONSUMPTION_PREDICTION_QUARTERLY;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.PRODUCTION_PREDICTION_QUARTERLY;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.SUM_CONSUMPTION;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.SUM_PRODUCTION;
import static java.time.temporal.ChronoUnit.DAYS;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.function.Supplier;

import org.junit.Test;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.ess.timeofusetariff.optimizer.Context;
import io.openems.edge.controller.ess.timeofusetariff.optimizer.Optimizer;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.predictor.api.prediction.Prediction;
import io.openems.edge.predictor.api.test.DummyPredictor;
import io.openems.edge.predictor.api.test.DummyPredictorManager;
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
	 * Creates a {@link TimeOfUseTariffControllerImplTest} instance.
	 * 
	 * @param clock a {@link Clock}
	 * @return the object
	 * @throws Exception on error
	 */
	public static TimeOfUseTariffControllerImpl create(Clock clock) throws Exception {
		var now = roundDownToQuarter(ZonedDateTime.now(clock));
		final var midnight = now.truncatedTo(DAYS);
		var componentManager = new DummyComponentManager(clock);
		var sum = new DummySum();
		var predictor0 = new DummyPredictor("predictor0", componentManager,
				Prediction.from(sum, SUM_PRODUCTION, midnight, PRODUCTION_PREDICTION_QUARTERLY), SUM_PRODUCTION);
		var predictor1 = new DummyPredictor("predictor0", componentManager,
				Prediction.from(sum, SUM_CONSUMPTION, midnight, CONSUMPTION_PREDICTION_QUARTERLY), SUM_CONSUMPTION);
		var timeOfUseTariff = DummyTimeOfUseTariffProvider.fromHourlyPrices(clock, TestData.HOURLY_PRICES_SUMMER);

		var sut = new TimeOfUseTariffControllerImpl();
		new ControllerTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", componentManager) //
				.addReference("predictorManager", new DummyPredictorManager(predictor0, predictor1)) //
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
						.setMaxChargePowerFromGrid14aEnWG(false) //
						.setRiskLevel(MEDIUM) //
						.build()) //
				.next(new TestCase());
		return sut;
	}

	/**
	 * Gets the {@link Optimizer} via Java Reflection.
	 * 
	 * @param ctrl the {@link TimeOfUseTariffControllerImplTest}
	 * @return the object
	 * @throws Exception on error
	 */
	public static Optimizer getOptimizer(TimeOfUseTariffControllerImpl ctrl) throws Exception {
		var field = TimeOfUseTariffControllerImpl.class.getDeclaredField("optimizer");
		field.setAccessible(true);
		return (Optimizer) field.get(ctrl);
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
	 * Gets the {@link Context} via Java Reflection.
	 * 
	 * @param ctrl the {@link TimeOfUseTariffControllerImplTest}
	 * @return the object
	 * @throws Exception on error
	 */
	@SuppressWarnings("unchecked")
	public static Context getContext(TimeOfUseTariffControllerImpl ctrl) throws Exception {
		var optimizer = getOptimizer(ctrl);
		var field = Optimizer.class.getDeclaredField("context");
		field.setAccessible(true);
		return ((Supplier<Context>) field.get(optimizer)).get();
	}
}
