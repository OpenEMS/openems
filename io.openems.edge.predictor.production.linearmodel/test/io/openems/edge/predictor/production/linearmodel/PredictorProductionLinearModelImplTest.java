package io.openems.edge.predictor.production.linearmodel;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.gson.JsonNull;

import io.openems.common.exceptions.OpenemsException;
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

@ExtendWith(MockitoExtension.class)
class PredictorProductionLinearModelImplTest {

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

	@BeforeEach
	void setUp() throws Exception {
		this.setupComponent();
	}

	void setupComponent() throws Exception {
		this.sut.setProductionChannelAddress(PRODUCTION_CHANNEL_ADDRESS);
		this.sut.setSnowStateMachine(this.snowStateMachine);
		this.sut.setPredictionPersistenceService(this.predictionPersistenceService);

		this.timedata = spy(new DummyTimedata("timedata0"));
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
	void testCreateNewPrediction_ShouldReturnEmptyPrediction_WhenSnowStateMachineThrowsException() throws Exception {
		doThrow(new PredictionException(PredictionError.NO_WEATHER_DATA, "No weather data"))//
				.when(this.snowStateMachine)//
				.run();

		var result = this.sut.createNewPrediction(PRODUCTION_CHANNEL_ADDRESS);
		assertEquals(Prediction.EMPTY_PREDICTION, result);
	}

	@Test
	void testCreateNewPrediction_ShouldReturnLongtermPrediction_WhenNoSnow() {
		var longtermPrediction = Prediction.from(this.now.toInstant(), 1, 2, 3);
		when(this.sut.createLongTermPrediction(any())).thenReturn(longtermPrediction);
		when(this.snowStateMachine.getCurrentState()).thenReturn(State.NORMAL);

		var result = this.sut.createNewPrediction(PRODUCTION_CHANNEL_ADDRESS);

		verify(this.sut)._setSnowState(eq(State.NORMAL));
		verify(this.predictionPersistenceService).updatePredictionAheadChannels(eq(result));
		assertEquals(longtermPrediction, result);
	}

	@Test
	void testCreateNewPrediction_ShouldReturnPersistencePrediction_WhenSnow() throws Exception {
		this.timedata.add(this.now.minusMinutes(15), PRODUCTION_CHANNEL_ADDRESS, 1000);
		var persistencePrediction = Prediction.from(this.now.toInstant(), 4, 5, 6);
		when(this.predictorPersistenceModel.getPrediction(any())).thenReturn(persistencePrediction);
		when(this.snowStateMachine.getCurrentState()).thenReturn(State.SNOW);
		when(this.snowStateMachine.getSnowStart()).thenReturn(this.now.minusHours(24));

		var result = this.sut.createNewPrediction(PRODUCTION_CHANNEL_ADDRESS);

		verify(this.sut)._setSnowState(eq(State.SNOW));
		verify(this.predictionPersistenceService).updatePredictionAheadChannels(eq(result));
		assertSame(persistencePrediction, result);
		verify(this.timedata, never()).queryHistoricData(any(), any(), any(), any(), any());
	}

	@Test
	void testCreateNewPrediction_ShouldReturnLongtermPrediction_WhenSnowStartWithinLast24h() {
		var longtermPrediction = Prediction.from(this.now.toInstant(), 1, 2, 3);
		when(this.sut.createLongTermPrediction(any())).thenReturn(longtermPrediction);
		when(this.snowStateMachine.getCurrentState()).thenReturn(State.SNOW);
		when(this.snowStateMachine.getSnowStart()).thenReturn(this.now.minusHours(23));

		var result = this.sut.createNewPrediction(PRODUCTION_CHANNEL_ADDRESS);

		verify(this.sut)._setSnowState(eq(State.SNOW));
		verify(this.predictionPersistenceService).updatePredictionAheadChannels(eq(result));
		assertEquals(result, longtermPrediction);
	}

	@Test
	void testCreateNewPrediction_ShouldReturnLongtermPrediction_WhenSnowButPersistenceModelNotAvailable()
			throws Exception {
		this.predictorPersistenceModel = null;
		this.setupComponent();

		var longtermPrediction = Prediction.from(this.now.toInstant(), 1, 2, 3);
		when(this.sut.createLongTermPrediction(any())).thenReturn(longtermPrediction);
		when(this.snowStateMachine.getCurrentState()).thenReturn(State.SNOW);
		when(this.snowStateMachine.getSnowStart()).thenReturn(this.now.minusHours(Integer.MAX_VALUE));

		var result = this.sut.createNewPrediction(PRODUCTION_CHANNEL_ADDRESS);

		verify(this.sut)._setSnowState(eq(State.SNOW));
		verify(this.predictionPersistenceService).updatePredictionAheadChannels(eq(result));
		assertEquals(result, longtermPrediction);
	}

	@ParameterizedTest
	@CsvSource({ //
			"1000, 2000, 1684, 1853, 1979, 2063, 2105", //
			"3000, 1000, 1500, 1233, 1033, 900, 833", //
			"0, 2000, 1333, 1689, 1956, 2133, 2222", //
			"1000, 0, 0, 0, 0, 0, 0" //
	})
	void testCreateNewPrediction_ShouldSmoothHistoryAndPreserveForecastSum(int historicValue, int forecastValue,
			int firstValue, int secondValue, int thirdValue, int fourthValue, int remainingValue) throws Exception {
		for (int i = 1; i <= 4; i++) {
			this.timedata.add(this.now.minusMinutes(i * 15), PRODUCTION_CHANNEL_ADDRESS, historicValue);
		}
		var values = new Integer[8];
		Arrays.fill(values, forecastValue);
		when(this.snowStateMachine.getCurrentState()).thenReturn(State.NORMAL);
		when(this.predictionOrchestrator.runPrediction())
				.thenReturn(new Series<>(IntStream.range(0, 8).mapToObj(i -> this.now.plusMinutes(i * 15)).toList(),
						Arrays.stream(values).map(Integer::doubleValue).toList()));
		this.sut.onTrainingSuccess(new ModelBundle(mock(Regressor.class), this.now.toInstant()));

		var result = this.sut.createNewPrediction(PRODUCTION_CHANNEL_ADDRESS);

		assertArrayEquals(new Integer[] { firstValue, secondValue, thirdValue, fourthValue, remainingValue,
				remainingValue, remainingValue, remainingValue }, result.asArray());
		assertEquals(forecastValue * 8, Arrays.stream(result.asArray()).mapToInt(Integer::intValue).sum(), 4);
		assertEquals(this.now.toInstant(), result.getFirstTime());
		verify(this.predictionPersistenceService).updatePredictionAheadChannels(eq(result));
	}

	@Test
	void testCreateNewPrediction_ShouldNotUseStaleOrFutureHistory() throws Exception {
		this.timedata.add(this.now.minusMinutes(30), PRODUCTION_CHANNEL_ADDRESS, 500);
		this.timedata.add(this.now.minusMinutes(15), PRODUCTION_CHANNEL_ADDRESS, JsonNull.INSTANCE);
		this.timedata.add(this.now, PRODUCTION_CHANNEL_ADDRESS, 1000);
		when(this.snowStateMachine.getCurrentState()).thenReturn(State.NORMAL);
		when(this.predictionOrchestrator.runPrediction())//
				.thenReturn(new Series<>(List.of(this.now, this.now.plusMinutes(15)), List.of(-100.0, 200.0)));
		this.sut.onTrainingSuccess(new ModelBundle(mock(Regressor.class), this.now.toInstant()));

		var result = this.sut.createNewPrediction(PRODUCTION_CHANNEL_ADDRESS);

		assertArrayEquals(new Integer[] { 42, 53, 42, 32, 21, 11, 0, 0 }, result.asArray());
	}

	@Test
	void testCreateNewPrediction_ShouldKeepForecast_WhenHistoryQueryFails() throws Exception {
		doThrow(new OpenemsException("Historic data unavailable"))//
				.when(this.timedata).queryHistoricData(any(), any(), any(), any(), any());
		when(this.snowStateMachine.getCurrentState()).thenReturn(State.NORMAL);
		when(this.predictionOrchestrator.runPrediction())//
				.thenReturn(new Series<>(List.of(this.now, this.now.plusMinutes(15)), List.of(-100.0, 200.0)));
		this.sut.onTrainingSuccess(new ModelBundle(mock(Regressor.class), this.now.toInstant()));

		var result = this.sut.createNewPrediction(PRODUCTION_CHANNEL_ADDRESS);

		assertArrayEquals(new Integer[] { 42, 53, 42, 32, 21, 11, 0, 0 }, result.asArray());
		verify(this.predictionPersistenceService).updatePredictionAheadChannels(eq(result));
		verify(this.sut)._setPredictionState(eq(PredictionState.SUCCESSFUL));
	}

	@ParameterizedTest
	@CsvSource({ //
			", 42, 53, 42, 32, 21, 11", //
			"100, 62, 55, 38, 24, 14, 7" //
	})
	void testCreateNewPrediction_ShouldCombineGaussianSmoothingAndHistory(Integer historicValue, int firstValue,
			int secondValue, int thirdValue, int fourthValue, int fifthValue, int sixthValue) throws Exception {
		if (historicValue != null) {
			this.timedata.add(this.now.minusMinutes(15), PRODUCTION_CHANNEL_ADDRESS, historicValue);
		}
		when(this.predictionOrchestrator.runPrediction())//
				.thenReturn(new Series<>(//
						List.of(this.now, this.now.plusMinutes(15)), //
						List.of(-100.0, 200.0)));
		when(this.snowStateMachine.getCurrentState()).thenReturn(State.NORMAL);

		this.sut.onTrainingSuccess(new ModelBundle(mock(Regressor.class), this.now.toInstant()));

		var result = this.sut.createNewPrediction(PRODUCTION_CHANNEL_ADDRESS);
		var resultArray = result.asArray();

		assertEquals(firstValue, resultArray[0]);
		assertEquals(secondValue, resultArray[1]);
		assertEquals(thirdValue, resultArray[2]);
		assertEquals(fourthValue, resultArray[3]);
		assertEquals(fifthValue, resultArray[4]);
		assertEquals(sixthValue, resultArray[5]);
		assertEquals(0, resultArray[6]);
		assertEquals(this.now.toInstant(), result.getFirstTime());
	}

	@Test
	void testCreateNewPrediction_ShouldUseHistoryInChronologicalOrder() throws Exception {
		this.timedata.add(this.now.minusMinutes(60), PRODUCTION_CHANNEL_ADDRESS, 10);
		this.timedata.add(this.now.minusMinutes(45), PRODUCTION_CHANNEL_ADDRESS, 20);
		this.timedata.add(this.now.minusMinutes(30), PRODUCTION_CHANNEL_ADDRESS, 30);
		this.timedata.add(this.now.minusMinutes(15), PRODUCTION_CHANNEL_ADDRESS, 100);
		this.timedata.add(this.now, PRODUCTION_CHANNEL_ADDRESS, 10000);
		when(this.predictionOrchestrator.runPrediction())//
				.thenReturn(new Series<>(List.of(this.now, this.now.plusMinutes(15)), List.of(0.0, 200.0)));
		when(this.snowStateMachine.getCurrentState()).thenReturn(State.NORMAL);
		this.sut.onTrainingSuccess(new ModelBundle(mock(Regressor.class), this.now.toInstant()));

		var result = this.sut.createNewPrediction(PRODUCTION_CHANNEL_ADDRESS);

		assertArrayEquals(new Integer[] { 53, 55, 41, 28, 16, 8, 0, 0 }, result.asArray());
	}

	@Test
	void testCreateLongtermPrediction_ShouldReturnEmptyPrediction_WhenNoModel() {
		var result = this.sut.createLongTermPrediction(PRODUCTION_CHANNEL_ADDRESS);

		verify(this.sut)._setPredictionState(eq(PredictionState.FAILED_NO_MODEL));
		assertEquals(Prediction.EMPTY_PREDICTION, result);
	}

	@Test
	void testCreateLongtermPrediction_ShouldReturnEmptyPrediction_WhenModelOutdated() {
		this.sut.onTrainingSuccess(new ModelBundle(//
				mock(Regressor.class), //
				this.now.minusDays(1).minusMinutes(1).toInstant()));

		var result = this.sut.createLongTermPrediction(PRODUCTION_CHANNEL_ADDRESS);

		verify(this.sut)._setPredictionState(eq(PredictionState.FAILED_MODEL_OUTDATED));
		assertEquals(Prediction.EMPTY_PREDICTION, result);
	}

	@Test
	void testComputeAndSetMaxProduction_ShouldSetCorrectPercentile() {
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
		public int forecastQuarters() {
			return 8;
		}

		@Override
		public PredictionOrchestratorFactory predictionOrchestratorFactory() {
			return context -> this.predictionOrchestrator;
		}
	}
}
