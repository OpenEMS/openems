package io.openems.edge.energy;

import static io.openems.common.utils.DateUtils.roundDownToQuarter;
import static io.openems.common.utils.ReflectionUtils.getValueViaReflection;
import static io.openems.edge.energy.LogVerbosity.TRACE;
import static io.openems.edge.energy.api.EnergyConstants.SUM_PRODUCTION;
import static io.openems.edge.energy.api.EnergyConstants.SUM_UNMANAGED_CONSUMPTION;
import static io.openems.edge.energy.api.EnergyUtils.toEnergy;
import static io.openems.edge.energy.api.Version.V2_ENERGY_SCHEDULABLE;
import static io.openems.edge.energy.optimizer.TestData.CONSUMPTION_PREDICTION_QUARTERLY;
import static io.openems.edge.energy.optimizer.TestData.HOURLY_PRICES_SUMMER;
import static io.openems.edge.energy.optimizer.TestData.PRODUCTION_PREDICTION_QUARTERLY;
import static io.openems.edge.ess.power.api.Relationship.GREATER_OR_EQUALS;
import static java.time.temporal.ChronoUnit.DAYS;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.Test;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.ess.emergencycapacityreserve.ControllerEssEmergencyCapacityReserveImpl;
import io.openems.edge.controller.ess.fixactivepower.ControllerEssFixActivePowerImpl;
import io.openems.edge.controller.ess.gridoptimizedcharge.ControllerEssGridOptimizedChargeImpl;
import io.openems.edge.controller.ess.limittotaldischarge.ControllerEssLimitTotalDischargeImpl;
import io.openems.edge.controller.ess.timeofusetariff.ControlMode;
import io.openems.edge.controller.ess.timeofusetariff.TimeOfUseTariffControllerImpl;
import io.openems.edge.energy.api.test.DummyEnergySchedulable;
import io.openems.edge.energy.optimizer.Optimizer;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.predictor.api.prediction.Prediction;
import io.openems.edge.predictor.api.test.DummyPredictor;
import io.openems.edge.predictor.api.test.DummyPredictorManager;
import io.openems.edge.scheduler.api.test.DummyScheduler;
import io.openems.edge.timedata.test.DummyTimedata;
import io.openems.edge.timeofusetariff.test.DummyTimeOfUseTariffProvider;

public class EnergySchedulerImplTest {

	public static final Clock CLOCK = new TimeLeapClock(Instant.parse("2020-03-04T14:19:00.00Z"), ZoneOffset.UTC);

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
		final var now = roundDownToQuarter(ZonedDateTime.now(clock));
		final var midnight = now.truncatedTo(DAYS);
		final var componentManager = new DummyComponentManager(clock);
		final var sum = new DummySum() //
				.withEssCapacity(10000) //
				.withEssSoc(50);
		final var ess = new DummyManagedSymmetricEss("ess0");
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
				.addReference("addSchedulable",
						new DummyEnergySchedulable("ctrlEmergencyCapacityReserve0",
								ControllerEssEmergencyCapacityReserveImpl.buildEnergyScheduleHandler(//
										() -> /* reserveSoc */ 10))) //
				.addReference("addSchedulable",
						new DummyEnergySchedulable("ctrlLimitTotalDischarge0",
								ControllerEssLimitTotalDischargeImpl.buildEnergyScheduleHandler(//
										() -> /* minSoc */ 12))) //
				.addReference("addSchedulable",
						new DummyEnergySchedulable("ctrlFixActivePower0",
								ControllerEssFixActivePowerImpl.buildEnergyScheduleHandler(//
										() -> new ControllerEssFixActivePowerImpl.EshContext(
												io.openems.edge.controller.ess.fixactivepower.Mode.MANUAL_ON, //
												toEnergy(-1000), GREATER_OR_EQUALS)))) //
				.addReference("addSchedulable",
						new DummyEnergySchedulable("ctrlGridOptimizedCharge0",
								ControllerEssGridOptimizedChargeImpl.buildEnergyScheduleHandler(//
										() -> io.openems.edge.controller.ess.gridoptimizedcharge.Mode.MANUAL, //
										() -> LocalTime.of(10, 00)))) //
				.addReference("addSchedulable",
						new DummyEnergySchedulable("ctrlEssTimeOfUseTariff0",
								TimeOfUseTariffControllerImpl.buildEnergyScheduleHandler(//
										() -> ess, //
										() -> ControlMode.CHARGE_CONSUMPTION, //
										() -> /* maxChargePowerFromGrid */ 20_000)))
				.addReference("sum", sum) //
				.activate(MyConfig.create() //
						.setId("_energy") //
						.setEnabled(false) //
						.setLogVerbosity(TRACE) //
						.setVersion(V2_ENERGY_SCHEDULABLE) //
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
