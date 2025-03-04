package io.openems.edge.energy;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.common.utils.DateUtils.roundDownToQuarter;
import static io.openems.common.utils.ReflectionUtils.getValueViaReflection;
import static io.openems.edge.energy.EnergySchedulerTestUtils.dummyEmergencyCapacityReserve;
import static io.openems.edge.energy.EnergySchedulerTestUtils.dummyFixActivePower;
import static io.openems.edge.energy.EnergySchedulerTestUtils.dummyGridOptimizedChargeManual;
import static io.openems.edge.energy.EnergySchedulerTestUtils.dummyLimitTotalDischarge;
import static io.openems.edge.energy.EnergySchedulerTestUtils.dummyTimeOfUseTariff;
import static io.openems.edge.energy.LogVerbosity.TRACE;
import static io.openems.edge.energy.api.EnergyConstants.SUM_PRODUCTION;
import static io.openems.edge.energy.api.EnergyConstants.SUM_UNMANAGED_CONSUMPTION;
import static io.openems.edge.energy.api.RiskLevel.MEDIUM;
import static io.openems.edge.energy.api.Version.V2_ENERGY_SCHEDULABLE;
import static io.openems.edge.energy.optimizer.TestData.CONSUMPTION_PREDICTION_QUARTERLY;
import static io.openems.edge.energy.optimizer.TestData.HOURLY_PRICES_SUMMER;
import static io.openems.edge.energy.optimizer.TestData.PRODUCTION_PREDICTION_QUARTERLY;
import static java.time.temporal.ChronoUnit.DAYS;

import java.time.Clock;
import java.time.ZonedDateTime;

import org.junit.Test;

import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.energy.optimizer.Optimizer;
import io.openems.edge.predictor.api.prediction.Prediction;
import io.openems.edge.predictor.api.test.DummyPredictor;
import io.openems.edge.predictor.api.test.DummyPredictorManager;
import io.openems.edge.scheduler.api.test.DummyScheduler;
import io.openems.edge.timedata.test.DummyTimedata;
import io.openems.edge.timeofusetariff.test.DummyTimeOfUseTariffProvider;

public class EnergySchedulerImplTest {

	@Test
	public void test() throws Exception {
		create(createDummyClock());
	}

	/**
	 * Creates a {@link EnergySchedulerImplTest} instance.
	 * 
	 * @param clock a {@link Clock}
	 * @return the object
	 * @throws Exception on error
	 */
	public static EnergySchedulerImpl create(Clock clock) throws Exception {
		final var now = roundDownToQuarter(ZonedDateTime.now(clock));
		final var midnight = now.truncatedTo(DAYS);
		final var componentManager = new DummyComponentManager(clock);
		final var sum = new DummySum() //
				.withEssCapacity(10000) //
				.withEssSoc(50);
		final var predictor0 = new DummyPredictor("predictor0", componentManager,
				Prediction.from(sum, SUM_PRODUCTION, midnight, PRODUCTION_PREDICTION_QUARTERLY), SUM_PRODUCTION);
		final var predictor1 = new DummyPredictor("predictor1", componentManager,
				Prediction.from(sum, SUM_UNMANAGED_CONSUMPTION, midnight, CONSUMPTION_PREDICTION_QUARTERLY),
				SUM_UNMANAGED_CONSUMPTION);
		final var timeOfUseTariff = DummyTimeOfUseTariffProvider.fromHourlyPrices(clock, HOURLY_PRICES_SUMMER);

		final var sut = new EnergySchedulerImpl();
		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", componentManager) //
				.addReference("predictorManager", new DummyPredictorManager(predictor0, predictor1)) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.addReference("timeOfUseTariff", timeOfUseTariff) //
				.addReference("scheduler", new DummyScheduler("scheduler0")) //
				.addReference("addSchedulable", dummyEmergencyCapacityReserve()) //
				.addReference("addSchedulable", dummyLimitTotalDischarge()) //
				.addReference("addSchedulable", dummyFixActivePower()) //
				.addReference("addSchedulable", dummyGridOptimizedChargeManual()) //
				.addReference("addSchedulable", dummyTimeOfUseTariff()) //
				.addReference("sum", sum) //
				.activate(MyConfig.create() //
						.setId("_energy") //
						.setEnabled(false) //
						.setLogVerbosity(TRACE) //
						.setVersion(V2_ENERGY_SCHEDULABLE) //
						.setRiskLevel(MEDIUM) //
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
		return getValueViaReflection(energyScheduler, "optimizer");
	}

}
