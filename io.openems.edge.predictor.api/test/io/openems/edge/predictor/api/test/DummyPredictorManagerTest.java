package io.openems.edge.predictor.api.test;

import static io.openems.edge.common.test.TestUtils.createDummyClock;
import static io.openems.edge.predictor.api.prediction.Prediction.EMPTY_PREDICTION;
import static org.junit.Assert.assertEquals;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.predictor.api.prediction.Prediction;

public class DummyPredictorManagerTest {

	protected static final ChannelAddress SUM_PRODUCTION_ACTIVE_POWER = new ChannelAddress("_sum",
			"ProductionActivePower");
	protected static final ChannelAddress SUM_CONSUMPTION_ACTIVE_POWER = new ChannelAddress("_sum",
			"ConsumptionActivePower");

	@Test
	public void test() throws OpenemsNamedException {
		final var clock = createDummyClock();
		final var now = ZonedDateTime.now(clock);
		final var cm = new DummyComponentManager(clock);
		final var sum = new DummySum();
		var sut = new DummyPredictorManager(new DummyPredictor("predictor0", cm,
				Prediction.from(sum, SUM_PRODUCTION_ACTIVE_POWER, now, new Integer[] { 1, 2, 3, 4 }),
				SUM_PRODUCTION_ACTIVE_POWER));

		sut.addPredictor(new DummyPredictor("predictor1", cm,
				Prediction.from(sum, SUM_CONSUMPTION_ACTIVE_POWER, now, new Integer[] { 9, 8, 7, 6 }),
				SUM_CONSUMPTION_ACTIVE_POWER));

		assertEquals(9, (int) sut.getPrediction(SUM_CONSUMPTION_ACTIVE_POWER).asArray()[0]);
		assertEquals(EMPTY_PREDICTION, sut.getPrediction(new ChannelAddress("foo", "bar")));

		assertEquals(sut, sut.self());

		assertEquals(9, sut.getPrediction(SUM_CONSUMPTION_ACTIVE_POWER).getAt(now).intValue());
		assertEquals(9, sut.getPrediction(SUM_CONSUMPTION_ACTIVE_POWER)
				.getAt(now.withZoneSameInstant(ZoneId.of("Europe/Berlin"))).intValue());
	}

}
