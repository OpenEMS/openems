package io.openems.edge.predictor.production.linearmodel;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.predictor.api.common.PredictionException;
import io.openems.edge.predictor.api.common.PredictionState;
import io.openems.edge.predictor.api.mlcore.datastructures.Series;
import io.openems.edge.predictor.api.mlcore.regression.Regressor;
import io.openems.edge.predictor.api.prediction.Prediction;
import io.openems.edge.predictor.persistencemodel.PredictorPersistenceModel;
import io.openems.edge.predictor.production.linearmodel.PredictorProductionLinearModelImpl.DefaultPredictorConfig;
import io.openems.edge.predictor.production.linearmodel.TrainingCallback.ModelBundle;
import io.openems.edge.predictor.production.linearmodel.prediction.PredictionError;
import io.openems.edge.predictor.production.linearmodel.prediction.PredictionOrchestrator;
import io.openems.edge.predictor.production.linearmodel.prediction.SnowStateMachine;
import io.openems.edge.predictor.production.linearmodel.prediction.SnowStateMachine.State;
import io.openems.edge.timedata.test.DummyTimedata;
import io.openems.edge.weather.api.Weather;

@RunWith(MockitoJUnitRunner.class)
public class PredictorProductionLinearModelImplTest {

	private static final ChannelAddress PRODUCTION_CHANNEL_ADDRESS = new ChannelAddress("_sum", "ProductionChannel");

	private final ZonedDateTime now = ZonedDateTime.of(2025, 9, 10, 15, 30, 0, 0, ZoneId.of("Europe/Berlin"));

	@Spy
	private PredictorProductionLinearModelImpl sut;

	@Mock
	private PredictionOrchestrator predictionOrchestrator;

	@Mock
	private SnowStateMachine snowStateMachine;

	@Mock
	private PredictorPersistenceModel predictorPersistenceModel;

	@Mock
	private PredictionPersistenceService predictionPersistenceService;

	private DummyTimedata timedata;

	@Before
	public void setUp() throws Exception {
		this.sut.setProductionChannelAddress(PRODUCTION_CHANNEL_ADDRESS);
		this.sut.setSnowStateMachine(this.snowStateMachine);
		this.sut.setPredictionPersistenceService(this.predictionPersistenceService);

		this.timedata = new DummyTimedata("timedata0");
		new ComponentTest(this.sut)//
				.addReference("componentManager",
						new DummyComponentManager(Clock.fixed(this.now.toInstant(), this.now.getZone())))//
				.addReference("timedata", this.timedata)//
				.addReference("weather", mock(Weather.class))//
				.addReference("sum", new DummySum())//
				.addReference("predictorConfig", new DummyPredictorConfig(//
						Duration.ofDays(1), //
						this.predictionOrchestrator))//
				.addReference("predictorPersistenceModel", this.predictorPersistenceModel);
	}

	@Test
	public void testCreateNewPrediction_ShouldReturnEmptyPrediction_WhenSnowStateMachineThrowsException()
			throws Exception {
		doThrow(new PredictionException(PredictionError.NO_WEATHER_DATA, "No weather data"))//
				.when(this.snowStateMachine)//
				.run();

		var result = this.sut.createNewPrediction(PRODUCTION_CHANNEL_ADDRESS);
		assertEquals(Prediction.EMPTY_PREDICTION, result);
	}

	@Test
	public void testCreateNewPrediction_ShouldReturnLongtermPrediction_WhenNoSnow() {
		var longtermPrediction = Prediction.from(this.now, 1, 2, 3);
		when(this.sut.createLongTermPrediction(any())).thenReturn(longtermPrediction);
		when(this.snowStateMachine.getCurrentState()).thenReturn(State.NORMAL);

		var result = this.sut.createNewPrediction(PRODUCTION_CHANNEL_ADDRESS);

		verify(this.sut)._setSnowState(eq(State.NORMAL));
		verify(this.predictionPersistenceService).updatePredictionAheadChannels(eq(result));
		assertEquals(longtermPrediction, result);
	}

	@Test
	public void testCreateNewPrediction_ShouldReturnPersistencePrediction_WhenSnow() {
		var persistencePrediction = Prediction.from(this.now, 4, 5, 6);
		when(this.predictorPersistenceModel.getPrediction(any())).thenReturn(persistencePrediction);
		when(this.snowStateMachine.getCurrentState()).thenReturn(State.SNOW);
		when(this.snowStateMachine.getSnowStart()).thenReturn(this.now.minusHours(24));

		var result = this.sut.createNewPrediction(PRODUCTION_CHANNEL_ADDRESS);

		verify(this.sut)._setSnowState(eq(State.SNOW));
		verify(this.predictionPersistenceService).updatePredictionAheadChannels(eq(result));
		assertEquals(persistencePrediction, result);
	}

	@Test
	public void testCreateNewPrediction_ShouldReturnLongtermPrediction_WhenSnowStartWithinLast24h() {
		var longtermPrediction = Prediction.from(this.now, 1, 2, 3);
		when(this.sut.createLongTermPrediction(any())).thenReturn(longtermPrediction);
		when(this.snowStateMachine.getCurrentState()).thenReturn(State.SNOW);
		when(this.snowStateMachine.getSnowStart()).thenReturn(this.now.minusHours(23));

		var result = this.sut.createNewPrediction(PRODUCTION_CHANNEL_ADDRESS);

		verify(this.sut)._setSnowState(eq(State.SNOW));
		verify(this.predictionPersistenceService).updatePredictionAheadChannels(eq(result));
		assertEquals(result, longtermPrediction);
	}

	@Test
	public void testCreateNewPrediction_ShouldReturnLongtermPrediction_WhenSnowButPersistenceModelNotAvailable()
			throws Exception {
		var longtermPrediction = Prediction.from(this.now, 1, 2, 3);
		when(this.sut.createLongTermPrediction(any())).thenReturn(longtermPrediction);
		when(this.snowStateMachine.getCurrentState()).thenReturn(State.SNOW);
		when(this.snowStateMachine.getSnowStart()).thenReturn(this.now.minusHours(Integer.MAX_VALUE));

		this.predictorPersistenceModel = null;
		this.setUp();

		var result = this.sut.createNewPrediction(PRODUCTION_CHANNEL_ADDRESS);

		verify(this.sut)._setSnowState(eq(State.SNOW));
		verify(this.predictionPersistenceService).updatePredictionAheadChannels(eq(result));
		assertEquals(result, longtermPrediction);
	}

	@Test
	public void testCreateLongtermPrediction_ShouldReturnExpectedValues() throws Exception {
		when(this.predictionOrchestrator.runPrediction())//
				.thenReturn(new Series<>(//
						List.of(this.now, this.now.plusMinutes(15)), //
						List.of(-100.0, 200.0)));

		this.sut.onTrainingSuccess(new ModelBundle(mock(Regressor.class), this.now.toInstant()));
		this.sut.setPredictionPersistenceService(mock(PredictionPersistenceService.class));

		var result = this.sut.createLongTermPrediction(PRODUCTION_CHANNEL_ADDRESS);

		assertArrayEquals(new Integer[] { 0, 200 }, result.asArray());
		assertEquals(this.now, result.getFirstTime());
	}

	@Test
	public void testCreateLongtermPrediction_ShouldReturnEmptyPrediction_WhenNoModel() throws Exception {
		var result = this.sut.createLongTermPrediction(PRODUCTION_CHANNEL_ADDRESS);

		verify(this.sut)._setPredictionState(eq(PredictionState.FAILED_NO_MODEL));
		assertEquals(Prediction.EMPTY_PREDICTION, result);
	}

	@Test
	public void testCreateLongtermPrediction_ShouldReturnEmptyPrediction_WhenModelOutdated() throws Exception {
		this.sut.onTrainingSuccess(new ModelBundle(//
				mock(Regressor.class), //
				this.now.minusDays(1).minusMinutes(1).toInstant()));

		var result = this.sut.createLongTermPrediction(PRODUCTION_CHANNEL_ADDRESS);

		verify(this.sut)._setPredictionState(eq(PredictionState.FAILED_MODEL_OUTDATED));
		assertEquals(Prediction.EMPTY_PREDICTION, result);
	}

	@Test
	public void testComputeAndSetMaxProduction_ShouldSetCorrectPercentile() {
		for (int i = 1; i <= 100; i++) {
			this.timedata.add(this.now.minusMinutes(i * 15), PRODUCTION_CHANNEL_ADDRESS, i);
		}

		this.sut.computeAndSetMaxProduction();

		assertEquals(96, this.sut.getMaxProduction());
	}

	private static class DummyPredictorConfig extends DefaultPredictorConfig {

		private final Duration maxModelAge;
		private final PredictionOrchestrator predictionOrchestrator;

		public DummyPredictorConfig(//
				Duration maxModelAge, //
				PredictionOrchestrator predictionOrchestrator) {
			this.maxModelAge = maxModelAge;
			this.predictionOrchestrator = predictionOrchestrator;
		}

		@Override
		public Duration maxModelAge() {
			return this.maxModelAge;
		}

		@Override
		public PredictionOrchestratorFactory predictionOrchestratorFactory() {
			return (context) -> this.predictionOrchestrator;
		}
	}
}
