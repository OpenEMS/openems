package io.openems.edge.predictor.api.test;

import static io.openems.edge.predictor.api.prediction.Prediction.EMPTY_PREDICTION;
import static org.junit.Assert.assertEquals;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.test.TimeLeapClock;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.predictor.api.prediction.Prediction;

public class DummyPredictorManagerTest {

	private static final ChannelAddress SUM_PRODUCTION_ACTIVE_POWER = new ChannelAddress("_sum",
			"ProductionActivePower");
	private static final ChannelAddress SUM_CONSUMPTION_ACTIVE_POWER = new ChannelAddress("_sum",
			"ConsumptionActivePower");

	@Test
	public void test() throws OpenemsNamedException {
		final var clock = new TimeLeapClock(Instant.parse("2020-01-01T20:00:00.00Z"), ZoneOffset.UTC);
		final var now = ZonedDateTime.now(clock);
		final var cm = new DummyComponentManager(clock);
		var sut = new DummyPredictorManager(new DummyPredictor("predictor0", cm,
				Prediction.from(SUM_PRODUCTION_ACTIVE_POWER, now, new Integer[] { 1, 2, 3, 4 }),
				SUM_PRODUCTION_ACTIVE_POWER));

		sut.addPredictor(new DummyPredictor("predictor1", cm,
				Prediction.from(SUM_CONSUMPTION_ACTIVE_POWER, now, new Integer[] { 9, 8, 7, 6 }),
				SUM_CONSUMPTION_ACTIVE_POWER));

		assertEquals(9, (int) sut.getPrediction(SUM_CONSUMPTION_ACTIVE_POWER).asArray()[0]);
		assertEquals(EMPTY_PREDICTION, sut.getPrediction(new ChannelAddress("foo", "bar")));

		assertEquals(sut, sut.self());
	}

}
