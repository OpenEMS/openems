package io.openems.edge.core.predictormanager;

import static io.openems.common.test.TestUtils.createDummyClock;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.time.ZonedDateTime;

import org.junit.Test;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.predictor.api.prediction.Prediction;
import io.openems.edge.predictor.api.test.DummyPredictor;

public class PredictorManagerImplTest {

	private static final String PREDICTOR_ID0 = "predictor0";
	private static final String PREDICTOR_ID1 = "predictor1";

	private static final ChannelAddress SUM_CONSUMPTION_ACTIVE_POWER = new ChannelAddress("_sum",
			"ConsumptionActivePower");

	@Test
	public void testGetPrediction_ShouldReturnCorrectPrediction() throws Exception {
		final var clock = createDummyClock();
		final var componentManager = new DummyComponentManager(clock);
		final var sum = new DummySum();

		final var now = ZonedDateTime.now(clock);

		var predictorWithId0 = new DummyPredictor(//
				PREDICTOR_ID0, //
				componentManager, //
				Prediction.from(sum, SUM_CONSUMPTION_ACTIVE_POWER, now, 0), //
				SUM_CONSUMPTION_ACTIVE_POWER //
		);

		var predictorWithId1 = new DummyPredictor(//
				PREDICTOR_ID1, //
				componentManager, //
				Prediction.from(sum, SUM_CONSUMPTION_ACTIVE_POWER, now, 1), //
				SUM_CONSUMPTION_ACTIVE_POWER //
		);

		var sut = new PredictorManagerImpl();
		new ComponentTest(sut) //
				.addReference("configurationAdmin", new DummyConfigurationAdmin()) //
				.addReference("componentManager", componentManager) //
				.activate(MyConfig.create() //
						.setPredictorIds(PREDICTOR_ID1, PREDICTOR_ID0) //
						.build() //
				);
		sut.bindPredictor(predictorWithId0);
		sut.bindPredictor(predictorWithId1);

		var prediction = sut.getPrediction(SUM_CONSUMPTION_ACTIVE_POWER);
		assertArrayEquals(new Integer[] { 1 }, prediction.asArray());
	}

	@Test
	public void testGetPrediction_ShouldFallbackToNextPredictorWhenFirstIsEmpty() throws Exception {
		final var clock = createDummyClock();
		final var componentManager = new DummyComponentManager(clock);
		final var sum = new DummySum();

		final var now = ZonedDateTime.now(clock);

		var predictorWithId0 = new DummyPredictor(//
				PREDICTOR_ID0, //
				componentManager, //
				Prediction.from(sum, SUM_CONSUMPTION_ACTIVE_POWER, now, 0), //
				SUM_CONSUMPTION_ACTIVE_POWER //
		);

		var predictorWithId1 = new DummyPredictor(//
				PREDICTOR_ID1, //
				componentManager, //
				Prediction.EMPTY_PREDICTION, //
				SUM_CONSUMPTION_ACTIVE_POWER //
		);

		var sut = new PredictorManagerImpl();
		new ComponentTest(sut) //
				.addReference("configurationAdmin", new DummyConfigurationAdmin()) //
				.addReference("componentManager", componentManager) //
				.activate(MyConfig.create() //
						.setPredictorIds(PREDICTOR_ID1, PREDICTOR_ID0) //
						.build() //
				);
		sut.bindPredictor(predictorWithId0);
		sut.bindPredictor(predictorWithId1);

		var prediction = sut.getPrediction(SUM_CONSUMPTION_ACTIVE_POWER);
		assertArrayEquals(new Integer[] { 0 }, prediction.asArray());
	}

	@Test
	public void testGetPrediction_ShouldReturnEmptyPredictionWhenNoPredictorMatchesChannel() throws Exception {
		final var clock = createDummyClock();
		final var componentManager = new DummyComponentManager(clock);
		final var sum = new DummySum();

		final var now = ZonedDateTime.now(clock);

		var predictorWithId0 = new DummyPredictor(//
				PREDICTOR_ID0, //
				componentManager, //
				Prediction.from(sum, SUM_CONSUMPTION_ACTIVE_POWER, now, 0), //
				SUM_CONSUMPTION_ACTIVE_POWER //
		);

		var predictorWithId1 = new DummyPredictor(//
				PREDICTOR_ID1, //
				componentManager, //
				Prediction.from(sum, SUM_CONSUMPTION_ACTIVE_POWER, now, 1), //
				SUM_CONSUMPTION_ACTIVE_POWER //
		);

		var sut = new PredictorManagerImpl();
		new ComponentTest(sut) //
				.addReference("configurationAdmin", new DummyConfigurationAdmin()) //
				.addReference("componentManager", componentManager) //
				.activate(MyConfig.create() //
						.setPredictorIds(PREDICTOR_ID1, PREDICTOR_ID0) //
						.build() //
				);
		sut.bindPredictor(predictorWithId0);
		sut.bindPredictor(predictorWithId1);

		var prediction = sut.getPrediction(new ChannelAddress("_sum", "foo"));
		assertEquals(Prediction.EMPTY_PREDICTION, prediction);
	}

	@Test
	public void testGetPrediction_ShouldMatchCorrectPredictorUsingWildcards() throws Exception {
		final var clock = createDummyClock();
		final var componentManager = new DummyComponentManager(clock);
		final var sum = new DummySum();

		final var now = ZonedDateTime.now(clock);

		var predictorWithId0 = new DummyPredictor(//
				PREDICTOR_ID0, //
				componentManager, //
				Prediction.from(sum, new ChannelAddress("_sum", "ProductionActivePower"), now, 0), //
				new ChannelAddress("_sum", "*ActivePower") //
		);

		var predictorWithId1 = new DummyPredictor(//
				PREDICTOR_ID1, //
				componentManager, //
				Prediction.from(sum, new ChannelAddress("_sum", "ConsumptionActivePower_L2"), now, 1), //
				new ChannelAddress("_sum", "ConsumptionActivePower*") //
		);

		var sut = new PredictorManagerImpl();
		new ComponentTest(sut) //
				.addReference("configurationAdmin", new DummyConfigurationAdmin()) //
				.addReference("componentManager", componentManager) //
				.activate(MyConfig.create() //
						.setPredictorIds(PREDICTOR_ID1, PREDICTOR_ID0) //
						.build() //
				);
		sut.bindPredictor(predictorWithId0);
		sut.bindPredictor(predictorWithId1);

		var prediction1 = sut.getPrediction(new ChannelAddress("_sum", "ProductionActivePower"));
		assertArrayEquals(new Integer[] { 0 }, prediction1.asArray());

		var prediction2 = sut.getPrediction(new ChannelAddress("_sum", "ConsumptionActivePower_L2"));
		assertArrayEquals(new Integer[] { 1 }, prediction2.asArray());
	}
}
