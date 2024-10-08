package io.openems.edge.energy;

import static io.openems.common.utils.DateUtils.roundDownToQuarter;
import static io.openems.edge.energy.api.EnergyConstants.SUM_CONSUMPTION;
import static io.openems.edge.energy.api.EnergyConstants.SUM_PRODUCTION;
import static io.openems.edge.energy.optimizer.TestData.CONSUMPTION_PREDICTION_QUARTERLY;
import static io.openems.edge.energy.optimizer.TestData.HOURLY_PRICES_SUMMER;
import static io.openems.edge.energy.optimizer.TestData.PRODUCTION_PREDICTION_QUARTERLY;
import static java.time.temporal.ChronoUnit.DAYS;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;

import org.junit.Test;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.energy.api.Version;
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

		var sut = new EnergySchedulerImpl();
		new ComponentTest(sut) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("componentManager", componentManager) //
				.addReference("predictorManager", new DummyPredictorManager(predictor0, predictor1)) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.addReference("timeOfUseTariff", timeOfUseTariff) //
				.addReference("schedulables", List.of()) //
				.addReference("sum", sum) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setEnabled(false) //
						.setLogVerbosity(LogVerbosity.TRACE) //
						.setVersion(Version.V2_ENERGY_SCHEDULABLE) //
						.build()) //
				.next(new TestCase());
		return sut;
	}

}
