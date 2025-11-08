package io.openems.edge.predictor.profileclusteringmodel;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.osgi.service.component.annotations.Component;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.test.TimeLeapClock;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.DateUtils;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.predictor.api.common.PredictionException;
import io.openems.edge.predictor.api.mlcore.classification.Classifier;
import io.openems.edge.predictor.api.mlcore.clustering.Clusterer;
import io.openems.edge.predictor.api.mlcore.transformer.OneHotEncoder;
import io.openems.edge.predictor.api.prediction.Prediction;
import io.openems.edge.predictor.profileclusteringmodel.PredictorProfileClusteringModelImpl.DefaultPredictorConfig;
import io.openems.edge.predictor.profileclusteringmodel.TrainingCallback.ModelBundle;
import io.openems.edge.predictor.profileclusteringmodel.prediction.PredictionError;
import io.openems.edge.predictor.profileclusteringmodel.prediction.PredictionOrchestrator;
import io.openems.edge.timedata.test.DummyTimedata;

@Component
@RunWith(MockitoJUnitRunner.class)
public class PredictorProfileClusteringModelImplTest {

	private static final ChannelAddress DUMMY_CHANNEL_ADDRESS = new ChannelAddress(//
			"_sum", "DummyChannel");

	@SuppressWarnings("unchecked")
	@Test
	public void testCreateNewPrediction_ShouldReturnValidPrediction() throws OpenemsException, Exception {
		var clock = Clock.fixed(//
				Instant.parse("2025-07-17T15:45:00Z"), //
				ZoneOffset.UTC);

		var sut = new PredictorProfileClusteringModelImpl();
		sut.setPredictionPersistenceService(mock(PredictionPersistenceService.class));

		var orchestrator = mock(PredictionOrchestrator.class);
		var predictorConfig = new DummyPredictorConfig((ctx) -> orchestrator);

		new ComponentTest(sut) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.addReference("predictorConfig", predictorConfig);

		var clusterer = mock(Clusterer.class);
		var classifier = mock(Classifier.class);
		var oneHotEncoder = mock(OneHotEncoder.class);
		sut.onTrainingSuccess(new ModelBundle(clusterer, classifier, oneHotEncoder, clock.instant()));

		var todaysProfile = createConstantProfile(0, 10.0);
		var tomorrowsProfile = createConstantProfile(3, 30.0);

		when(orchestrator.predictProfiles(anyInt()))//
				.thenReturn(List.of(//
						todaysProfile, //
						tomorrowsProfile));

		var now = DateUtils.roundDownToQuarter(ZonedDateTime.now(clock));
		var baseTime = now.truncatedTo(ChronoUnit.DAYS);
		int quarterIndex = (int) ChronoUnit.MINUTES.between(baseTime, now) / 15;

		var expectedPredictedValues = Stream.concat(//
				todaysProfile.values().getValues().stream().skip(quarterIndex), //
				tomorrowsProfile.values().getValues().stream())//
				.map(d -> (int) Math.round(d))//
				.toArray(Integer[]::new);

		var prediction = sut.getPrediction(DUMMY_CHANNEL_ADDRESS);
		assertEquals(now, prediction.getFirstTime());
		assertArrayEquals(expectedPredictedValues, prediction.asArray());
	}

	@Test
	public void testCreateNewPrediction_ShouldReturnEmptyPrediction_WhenNoModelAvailable() throws Exception {
		var clock = Clock.fixed(//
				Instant.parse("2025-07-17T15:45:00Z"), //
				ZoneOffset.UTC);

		var sut = new PredictorProfileClusteringModelImpl();

		new ComponentTest(sut) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.addReference("predictorConfig", new DefaultPredictorConfig());

		var prediction = sut.getPrediction(DUMMY_CHANNEL_ADDRESS);
		assertEquals(Prediction.EMPTY_PREDICTION, prediction);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testCreateNewPrediction_ShouldReturnEmptyPrediction_WhenModelTooOld() throws Exception {
		var clock = new TimeLeapClock(//
				Instant.parse("2025-07-17T15:45:00Z"), //
				ZoneOffset.UTC);

		var predictorConfig = new DefaultPredictorConfig();

		var sut = new PredictorProfileClusteringModelImpl();
		new ComponentTest(sut) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.addReference("predictorConfig", predictorConfig);

		var clusterer = mock(Clusterer.class);
		var classifier = mock(Classifier.class);
		var oneHotEncoder = mock(OneHotEncoder.class);
		sut.onTrainingSuccess(new ModelBundle(clusterer, classifier, oneHotEncoder, clock.instant()));

		clock.leap(predictorConfig.maxModelAge().plusDays(1).toDays(), ChronoUnit.DAYS);

		var prediction = sut.getPrediction(DUMMY_CHANNEL_ADDRESS);
		assertEquals(Prediction.EMPTY_PREDICTION, prediction);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testCreateNewPrediction_ShouldReturnEmptyPrediction_WhenPredictionFails() throws Exception {
		var clock = Clock.fixed(//
				Instant.parse("2025-07-17T15:45:00Z"), //
				ZoneOffset.UTC);

		var sut = new PredictorProfileClusteringModelImpl();
		sut.setPredictionPersistenceService(mock(PredictionPersistenceService.class));

		var orchestrator = mock(PredictionOrchestrator.class);
		var predictorConfig = new DummyPredictorConfig((ctx) -> orchestrator);

		new ComponentTest(sut) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("timedata", new DummyTimedata("timedata0")) //
				.addReference("predictorConfig", predictorConfig);

		var clusterer = mock(Clusterer.class);
		var classifier = mock(Classifier.class);
		var oneHotEncoder = mock(OneHotEncoder.class);
		sut.onTrainingSuccess(new ModelBundle(clusterer, classifier, oneHotEncoder, clock.instant()));

		when(orchestrator.predictProfiles(anyInt()))//
				.thenThrow(new PredictionException(PredictionError.INSUFFICIENT_PREDICTION_DATA, ""));

		var prediction = sut.getPrediction(DUMMY_CHANNEL_ADDRESS);
		assertEquals(Prediction.EMPTY_PREDICTION, prediction);
	}

	private static Profile createConstantProfile(int clusterIndex, double value) {
		double[] values = new double[Profile.LENGTH];
		for (int i = 0; i < Profile.LENGTH; i++) {
			values[i] = value;
		}
		return Profile.fromArray(clusterIndex, values);
	}

	private static class DummyPredictorConfig extends DefaultPredictorConfig {

		private final PredictionOrchestratorFactory predictionOrchestratorFactory;

		public DummyPredictorConfig(//
				PredictionOrchestratorFactory predictionOrchestratorFactory) {
			this.predictionOrchestratorFactory = predictionOrchestratorFactory;
		}

		@Override
		public PredictionOrchestratorFactory predictionOrchestratorFactory() {
			return this.predictionOrchestratorFactory;
		}
	}
}
