package io.openems.edge.energy.api.simulation;

import static io.openems.edge.energy.api.simulation.GlobalSimulationsContext.calculatePeriodDurationHourFromIndex;
import static io.openems.edge.energy.api.simulation.GlobalSimulationsContext.generateProductionPrediction;
import static io.openems.edge.energy.api.simulation.GlobalSimulationsContext.joinConsumptionPredictions;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.test.TimeLeapClock;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.energy.api.EnergyConstants;
import io.openems.edge.predictor.api.prediction.Prediction;
import io.openems.edge.predictor.api.test.DummyPredictor;
import io.openems.edge.predictor.api.test.DummyPredictorManager;
import io.openems.edge.timeofusetariff.test.DummyTimeOfUseTariffProvider;

public class GlobalSimulationsContextTest {

	private static final TimeLeapClock CLOCK = new TimeLeapClock(Instant.ofEpochSecond(946684800), ZoneId.of("UTC"));

	@Test
	public void testBuild() throws OpenemsNamedException {
		final var cm = new DummyComponentManager(CLOCK);
		final var now = ZonedDateTime.now(CLOCK);
		final var sum = new DummySum() //
				.withEssCapacity(10000) //
				.withEssSoc(50) //
				.withEssMinDischargePower(-4000) //
				.withEssMaxDischargePower(5000);
		final var predictorManager = new DummyPredictorManager(//
				new DummyPredictor("predictor0", cm, Prediction.from(sum, //
						EnergyConstants.SUM_UNMANAGED_CONSUMPTION, now, new Integer[] { //
								4000, 8000, 6000, 2000, 3000, 5000, 7000, 9000, //
								4001, 8001, 6001, 2001, 3001, 5001, 7001, 9001, //
								4002, 8002, 6002, 2002, 3002, 5002, 7002, 9002, //
								4003, 8003, 6003, 2003, 3003, 5003, 7003, 9003, //
								4004, 8004, 6004, 2004, 3004, 5004, 7004, 9004, //
						}), EnergyConstants.SUM_UNMANAGED_CONSUMPTION),
				new DummyPredictor("predictor1", cm, Prediction.from(sum, //
						EnergyConstants.SUM_CONSUMPTION, now, new Integer[] { //
								5000, 9000, 7000, 3000, 4000, 6000, 8000, 2000, //
								5001, 9001, 7001, 3001, 4001, 6001, 8001, 2001, //
								5002, 9002, 7002, 3002, 4002, 6002, 8002, 2002, //
								5003, 9003, 7003, 3003, 4003, 6003, 8003, 2003, //
								5004, 9004, 7004, 3004, 4004, 6004, 8004, 2004, //
						}), EnergyConstants.SUM_CONSUMPTION), //
				new DummyPredictor("predictor2", cm, Prediction.from(sum, //
						EnergyConstants.SUM_PRODUCTION, now,
						new Integer[] { 8000, 9000, 10000, 11000, 7000, 4000, 3000, 5000, //
								8001, 9001, 10001, 11001, 7001, 4001, 3001, 5001, //
								8002, 9002, 10002, 11002, 7002, 4002, 3002, 5002, //
								8003, 9003, 10003, 11003, 7003, 4003, 3003, 5003, //
								8004, 9004, 10004, 11004, 7004, 4004, 3004, 5004, //
						}), EnergyConstants.SUM_PRODUCTION));
		final var prices = DummyTimeOfUseTariffProvider.fromQuarterlyPrices(CLOCK, //
				11.0, 12.0, 13.0, 14.0, 15.0, 16.0, 17.0, 18.0, //
				11.1, 12.1, 13.1, 14.1, 15.1, 16.1, 17.1, 18.1, //
				11.2, 12.2, 13.2, 14.2, 15.2, 16.2, 17.2, 18.2, //
				11.3, 12.3, 13.3, 14.3, 15.3, 16.3, 17.3, 18.3, //
				11.4, 12.4, 13.4, 14.4, 15.4, 16.4, 17.4, 18.4 //
		);

		var gsc = GlobalSimulationsContext.create() //
				.setClock(CLOCK) //
				.setEnergyScheduleHandlers(ImmutableList.of()) //
				.setSum(sum) //
				.setPredictorManager(predictorManager) //
				.setTimeOfUseTariff(prices) //
				.build();

		assertEquals(1000 /* -4000 W */, gsc.ess().maxChargeEnergy());
		assertEquals(1250 /* 5000 W */, gsc.ess().maxDischargeEnergy());
		assertEquals(28, gsc.periods().size());
		var p0 = gsc.periods().get(0);
		assertEquals(2000 /* Wh */, p0.production());
		assertEquals(1250 /* Wh */, p0.consumption());
	}

	@Test
	public void testGenerateProductionPrediction() {
		final var arr = new Integer[] { 1, 2, 3 };
		assertArrayEquals(arr, generateProductionPrediction(arr, 2));
		assertArrayEquals(new Integer[] { 1, 2, 3, 0 }, generateProductionPrediction(arr, 4));
	}

	@Test
	public void testJoinConsumptionPredictions() {
		assertArrayEquals(//
				new Integer[] { 1, 2, 3, 4, 55, 66, 77, 88, 99 }, //
				joinConsumptionPredictions(4, //
						new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, //
						new Integer[] { 11, 22, 33, 44, 55, 66, 77, 88, 99 }));
	}

	@Test
	public void testCalculatePeriodDurationHourFromIndex() {
		assertEquals(24, calculatePeriodDurationHourFromIndex(ZonedDateTime.parse("2020-03-04T14:00:00.00Z")));
		assertEquals(24 + 3, calculatePeriodDurationHourFromIndex(ZonedDateTime.parse("2020-03-04T14:15:00.00Z")));
		assertEquals(24 + 2, calculatePeriodDurationHourFromIndex(ZonedDateTime.parse("2020-03-04T14:30:00.00Z")));
		assertEquals(24 + 1, calculatePeriodDurationHourFromIndex(ZonedDateTime.parse("2020-03-04T14:45:00.00Z")));
		assertEquals(24, calculatePeriodDurationHourFromIndex(ZonedDateTime.parse("2020-03-04T15:00:00.00Z")));
	}
}
