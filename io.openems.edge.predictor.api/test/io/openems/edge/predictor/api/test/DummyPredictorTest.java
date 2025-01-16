package io.openems.edge.predictor.api.test;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.predictor.api.test.DummyPredictorManagerTest.SUM_PRODUCTION_ACTIVE_POWER;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.junit.Assert.assertEquals;

import java.time.ZonedDateTime;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.predictor.api.prediction.Prediction;

public class DummyPredictorTest {

	@Test
	public void test() throws OpenemsNamedException {
		final var clock = createDummyClock();
		final var now = ZonedDateTime.now(clock);
		final var cm = new DummyComponentManager(clock);
		final var sum = new DummySum();
		var sut = new DummyPredictor("predictor0", cm,
				Prediction.from(sum, SUM_PRODUCTION_ACTIVE_POWER, now, new Integer[] { 1, 2, 3, 4 }),
				SUM_PRODUCTION_ACTIVE_POWER);

		assertEquals(4, sut.getPrediction(SUM_PRODUCTION_ACTIVE_POWER).asArray().length);
		clock.leap(15, MINUTES);
		assertEquals(3, sut.getPrediction(SUM_PRODUCTION_ACTIVE_POWER).asArray().length);
		clock.leap(1, MINUTES);
		assertEquals(3, sut.getPrediction(SUM_PRODUCTION_ACTIVE_POWER).asArray().length);
	}

	@Test
	public void testEmpty() throws OpenemsNamedException {
		final var clock = createDummyClock();
		final var now = ZonedDateTime.now(clock);
		final var cm = new DummyComponentManager(clock);
		final var sum = new DummySum();
		var sut = new DummyPredictor("predictor0", cm,
				Prediction.from(sum, SUM_PRODUCTION_ACTIVE_POWER, now, new Integer[0]), SUM_PRODUCTION_ACTIVE_POWER);

		assertEquals(0, sut.getPrediction(SUM_PRODUCTION_ACTIVE_POWER).asArray().length);
		clock.leap(15, MINUTES);
		assertEquals(0, sut.getPrediction(SUM_PRODUCTION_ACTIVE_POWER).asArray().length);
	}

}
