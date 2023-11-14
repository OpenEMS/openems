package io.openems.edge.controller.ess.timeofusetariff;

import static io.openems.edge.controller.ess.timeofusetariff.TestData.CONSUMPTION_PREDICTION_HOURLY;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.CONSUMPTION_PREDICTION_QUARTERLY;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.HOURLY_PRICES;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.HOURLY_PRICES_SUMMER;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.PRODUCTION_PREDICTION_HOURLY;
import static io.openems.edge.controller.ess.timeofusetariff.TestData.PRODUCTION_PREDICTION_QUARTERLY;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.Test;

import io.openems.common.test.TimeLeapClock;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.predictor.api.test.DummyPrediction24Hours;
import io.openems.edge.predictor.api.test.DummyPredictor24Hours;
import io.openems.edge.predictor.api.test.DummyPredictorManager;
import io.openems.edge.timeofusetariff.test.DummyTimeOfUseTariffProvider;

public class TimeOfUseTariffControllerTest {

	// Ids
	private static final String CTRL_ID = "ctrlEssTimeOfUseTariff0";
	private static final String PREDICTOR_ID = "predictor0";
	private static final String ESS_ID = "ess0";

	// Ess channels
	private static final ChannelAddress ESS_CAPACITY = new ChannelAddress(ESS_ID, "Capacity");
	private static final ChannelAddress ESS_SOC = new ChannelAddress(ESS_ID, "Soc");

	// Controller channels
	private static final ChannelAddress STATE_MACHINE = new ChannelAddress(CTRL_ID, "StateMachine");
	private static final ChannelAddress MIN_SOC = new ChannelAddress(CTRL_ID, "MinSoc");
	private static final ChannelAddress USABLE_CAPACITY = new ChannelAddress(CTRL_ID, "UsableCapacity");
	private static final ChannelAddress AVAILABLE_CAPACITY = new ChannelAddress(CTRL_ID, "AvailableCapacity");

	// Predictions
	final DummyPrediction24Hours productionPredictionQuarterly = new DummyPrediction24Hours(
			PRODUCTION_PREDICTION_QUARTERLY);
	final DummyPrediction24Hours consumptionPredictionQuarterly = new DummyPrediction24Hours(
			CONSUMPTION_PREDICTION_QUARTERLY);
	final DummyPrediction24Hours productionPredictionHourly = new DummyPrediction24Hours(
			PRODUCTION_PREDICTION_HOURLY);
	final DummyPrediction24Hours consumptionPredictionHourly = new DummyPrediction24Hours(
			CONSUMPTION_PREDICTION_HOURLY);

	@Test
	public void scheduleChargeForEveryQuarter() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2022-01-01T00:00:00.00Z"), ZoneOffset.UTC);
		final var cm = new DummyComponentManager(clock);

		// Predictors
		final var productionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, this.productionPredictionQuarterly,
				"_sum/ProductionActivePower");
		final var consumptionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm,
				this.consumptionPredictionQuarterly, "_sum/UnmanagedConsumptionActivePower");

		// Predictor Manager
		final var predictorManager = new DummyPredictorManager(productionPredictor, consumptionPredictor);

		// Price provider
		final var timeOfUseTariffProvider = DummyTimeOfUseTariffProvider.quarterlyPrices(ZonedDateTime.now(clock),
				HOURLY_PRICES);

		new ControllerTest(new TimeOfUseTariffControllerImpl()) //
				.addReference("predictorManager", predictorManager) //
				.addReference("componentManager", cm) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID)) //
				.addReference("timeOfUseTariff", timeOfUseTariffProvider) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setMode(Mode.AUTOMATIC) //
						.setControlMode(ControlMode.CHARGE_CONSUMPTION) //
						.setRiskLevel(RiskLevel.HIGH) //
						.setMaxPower(4000) //
						.build())
				.next(new TestCase("Cycle - 1") //
						.input(ESS_CAPACITY, 12000) //
						.input(ESS_SOC, 100)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setMode(Mode.OFF) //
						.setControlMode(ControlMode.CHARGE_CONSUMPTION) //
						.setRiskLevel(RiskLevel.HIGH) //
						.setMaxPower(4000) //
						.build())
				.next(new TestCase("Cycle - 2") //
						.output(STATE_MACHINE, StateMachine.ALLOWS_DISCHARGE)) //
		;
	}

	@Test
	public void scheduleChargeForEveryHour() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2022-01-01T00:00:00.00Z"), ZoneOffset.UTC);
		final var cm = new DummyComponentManager(clock);

		// Predictors
		final var productionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, this.productionPredictionHourly,
				"_sum/ProductionActivePower");
		final var consumptionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, this.consumptionPredictionHourly,
				"_sum/UnmanagedConsumptionActivePower");

		// PredictorManager
		final var predictorManager = new DummyPredictorManager(productionPredictor, consumptionPredictor);

		// Price provider
		final var timeOfUseTariffProvider = DummyTimeOfUseTariffProvider.hourlyPrices(ZonedDateTime.now(clock),
				HOURLY_PRICES_SUMMER);

		new ControllerTest(new TimeOfUseTariffControllerImpl()) //
				.addReference("predictorManager", predictorManager) //
				.addReference("componentManager", cm) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID)) //
				.addReference("timeOfUseTariff", timeOfUseTariffProvider) //
				.addReference("sum", new DummySum()) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setMode(Mode.AUTOMATIC) //
						.setControlMode(ControlMode.CHARGE_CONSUMPTION) //
						.setRiskLevel(RiskLevel.HIGH) //
						.setMaxPower(6000) //
						.build())
				.next(new TestCase("Cycle - 1") //
						.input(ESS_CAPACITY, 12000) //
						.input(ESS_SOC, 50) //
				);
	}

	@Test
	public void scheduleDelayDischargeForEveryHour() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2022-01-01T00:00:00.00Z"), ZoneOffset.UTC);
		final var cm = new DummyComponentManager(clock);

		// Predictors
		final var productionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, this.productionPredictionHourly,
				"_sum/ProductionActivePower");
		final var consumptionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, this.consumptionPredictionHourly,
				"_sum/UnmanagedConsumptionActivePower");

		// PredictorManager
		final var predictorManager = new DummyPredictorManager(productionPredictor, consumptionPredictor);

		// Price provider
		final var timeOfUseTariffProvider = DummyTimeOfUseTariffProvider.hourlyPrices(ZonedDateTime.now(clock),
				HOURLY_PRICES_SUMMER);

		new ControllerTest(new TimeOfUseTariffControllerImpl()) //
				.addReference("predictorManager", predictorManager) //
				.addReference("componentManager", cm) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID)) //
				.addReference("timeOfUseTariff", timeOfUseTariffProvider) //
				.addReference("sum", new DummySum()) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setMode(Mode.AUTOMATIC) //
						.setControlMode(ControlMode.DELAY_DISCHARGE) //
						.setRiskLevel(RiskLevel.HIGH) //
						.build())
				.next(new TestCase("Cycle - 1") //
						.input(ESS_CAPACITY, 12000) //
						.input(ESS_SOC, 50) //
						.output(MIN_SOC, 10) //
						.output(AVAILABLE_CAPACITY, 6000) //
						.output(USABLE_CAPACITY, 3600) //
				);
	}

	@Test
	public void scheduleDelayDischargeForEveryQuarter() throws Exception {
		final var clock = new TimeLeapClock(Instant.parse("2022-01-01T00:00:00.00Z"), ZoneOffset.UTC);
		final var cm = new DummyComponentManager(clock);

		// Predictors
		final var productionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm, this.productionPredictionQuarterly,
				"_sum/ProductionActivePower");
		final var consumptionPredictor = new DummyPredictor24Hours(PREDICTOR_ID, cm,
				this.consumptionPredictionQuarterly, "_sum/UnmanagedConsumptionActivePower");

		// PredictorManager
		final var predictorManager = new DummyPredictorManager(productionPredictor, consumptionPredictor);

		// Price provider
		final var timeOfUseTariffProvider = DummyTimeOfUseTariffProvider.quarterlyPrices(ZonedDateTime.now(clock),
				HOURLY_PRICES);

		new ControllerTest(new TimeOfUseTariffControllerImpl()) //
				.addReference("predictorManager", predictorManager) //
				.addReference("componentManager", cm) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("ess", new DummyManagedSymmetricEss(ESS_ID)) //
				.addReference("timeOfUseTariff", timeOfUseTariffProvider) //
				.addReference("sum", new DummySum()) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEssId(ESS_ID) //
						.setMode(Mode.AUTOMATIC) //
						.setRiskLevel(RiskLevel.HIGH) //
						.setControlMode(ControlMode.DELAY_DISCHARGE).build())
				.next(new TestCase("Cycle - 1") //
						.input(ESS_CAPACITY, 12000) //
						.input(ESS_SOC, 50) //
				);
	}
}
