package io.openems.edge.predictor.profileclusteringmodel.training;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.meta.types.SubdivisionCode;
import io.openems.edge.predictor.api.mlcore.classification.Classifier;
import io.openems.edge.predictor.api.mlcore.clustering.Clusterer;
import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;
import io.openems.edge.predictor.api.mlcore.datastructures.Series;
import io.openems.edge.predictor.api.mlcore.transformer.OneHotEncoder;
import io.openems.edge.predictor.profileclusteringmodel.PredictorConfig.ClassifierFitter;
import io.openems.edge.predictor.profileclusteringmodel.PredictorConfig.ClustererFitter;
import io.openems.edge.predictor.profileclusteringmodel.TrainingCallback;
import io.openems.edge.predictor.profileclusteringmodel.TrainingCallback.ModelBundle;
import io.openems.edge.predictor.profileclusteringmodel.services.ClassifierTrainingService;
import io.openems.edge.predictor.profileclusteringmodel.services.FeatureEngineeringService;
import io.openems.edge.predictor.profileclusteringmodel.services.FeatureEngineeringService.FeatureEngineeringTrainingResult;
import io.openems.edge.predictor.profileclusteringmodel.services.ProfileClusteringTrainingService;
import io.openems.edge.predictor.profileclusteringmodel.services.ProfileClusteringTrainingService.ClusteringResult;
import io.openems.edge.predictor.profileclusteringmodel.services.TimeSeriesPreprocessingService;
import io.openems.edge.predictor.profileclusteringmodel.services.TrainingDataService;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.test.DummyTimedata;

@RunWith(MockitoJUnitRunner.class)
public class TrainingOrchestratorTest {

	private static final ChannelAddress DUMMY_CHANNEL_ADDRESS = //
			new ChannelAddress("_sum", "DummyChannel");

	@SuppressWarnings("unchecked")
	@Test
	public void testRunTraining_ShouldCallAllServicesInOrder() throws Exception {
		// Mock services
		var clock = Clock.fixed(//
				Instant.parse("2025-07-17T15:45:00Z"), //
				ZoneOffset.UTC);

		var trainingContext = new TrainingContext(//
				mock(TrainingCallback.class), //
				() -> clock, //
				mock(Timedata.class), //
				mock(ChannelAddress.class), //
				90, // trainingWindowInDays
				8, // maxGapSizeInterpolation
				30, // minTrainingSamples
				60, // maxTrainingSamples
				mock(ClustererFitter.class), //
				mock(ClassifierFitter.class), //
				() -> SubdivisionCode.DE_BY);

		var clusterer = mock(Clusterer.class);
		var classifier = mock(Classifier.class);

		var rawTimeSeriesService = mock(TrainingDataService.class);
		var timeSeriesPreprocessingService = mock(TimeSeriesPreprocessingService.class);
		var profileClusteringTrainingService = mock(ProfileClusteringTrainingService.class);
		var featureEngineeringService = mock(FeatureEngineeringService.class);
		var classifierTrainingService = mock(ClassifierTrainingService.class);
		var oneHotEncoder = mock(OneHotEncoder.class);

		// Prepare example data
		var rawTimeSeries = testSeries();
		var timeSeriesPerDay = testDataFrame();
		var clusteringResult = testDataFrame();
		var featureMatrix = testDataFrame();
		var featureEngineeringResult = new FeatureEngineeringTrainingResult(//
				featureMatrix.copy(), //
				oneHotEncoder);

		// Simulate services
		when(rawTimeSeriesService.fetchSeriesForWindow(anyInt()))//
				.thenReturn(rawTimeSeries.copy());
		when(timeSeriesPreprocessingService.preprocessTimeSeriesForTraining(any()))//
				.thenReturn(timeSeriesPerDay.copy());
		when(profileClusteringTrainingService.clusterTimeSeries(any()))//
				.thenReturn(new ClusteringResult(clusterer, clusteringResult.copy()));
		when(featureEngineeringService.transformForTraining(any()))//
				.thenReturn(featureEngineeringResult);
		when(classifierTrainingService.trainClassifier(any()))//
				.thenReturn(classifier);

		// Run training process
		var orchestrator = new TrainingOrchestrator(//
				trainingContext, //
				rawTimeSeriesService, //
				timeSeriesPreprocessingService, //
				profileClusteringTrainingService, //
				featureEngineeringService, //
				classifierTrainingService);

		final var result = orchestrator.runTraining();

		var inOrder = inOrder(//
				rawTimeSeriesService, //
				timeSeriesPreprocessingService, //
				profileClusteringTrainingService, //
				featureEngineeringService, //
				classifierTrainingService);

		// Verify that all services were called in the correct order with the expected
		// (unchanged) arguments
		inOrder.verify(rawTimeSeriesService).fetchSeriesForWindow(eq(trainingContext.trainingWindowInDays()));
		inOrder.verify(timeSeriesPreprocessingService).preprocessTimeSeriesForTraining(eq(rawTimeSeries));
		inOrder.verify(profileClusteringTrainingService).clusterTimeSeries(eq(timeSeriesPerDay));
		inOrder.verify(featureEngineeringService).transformForTraining(eq(clusteringResult));
		inOrder.verify(classifierTrainingService).trainClassifier(eq(featureMatrix));

		inOrder.verifyNoMoreInteractions();
		
		assertEquals(new ModelBundle(clusterer, classifier, oneHotEncoder, clock.instant()), result);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testRunTraining_ShouldStoreTrainedModels_WhenGivenValidTimeSeriesData() throws Exception {
		var trainingCallback = mock(TrainingCallback.class);

		var clock = Clock.fixed(//
				Instant.parse("2025-07-17T15:45:00Z"), //
				ZoneOffset.UTC);

		var timedata = new DummyTimedata("timedata0");
		for (int day = 1; day <= 10; day++) {
			var dayStart = ZonedDateTime.now(clock).truncatedTo(ChronoUnit.DAYS).minusDays(day);
			for (int min = 0; min < 24 * 60; min += 15) {
				timedata.add(dayStart.plusMinutes(min), DUMMY_CHANNEL_ADDRESS, day * 10);
			}
		}

		var clustererFitter = mock(ClustererFitter.class);
		var classifierFitter = mock(ClassifierFitter.class);

		var trainingContext = new TrainingContext(//
				trainingCallback, //
				() -> clock, //
				timedata, //
				DUMMY_CHANNEL_ADDRESS, //
				10, // trainingWindowInDays
				8, // maxGapSizeInterpolation
				4, // minTrainingSamplesRequired
				6, // maxTrainingSamplesRequired
				clustererFitter, //
				classifierFitter, //
				() -> SubdivisionCode.DE_BY);

		var clusterer = mock(Clusterer.class);
		when(clustererFitter.fit(any(DataFrame.class))).thenReturn(clusterer);
		when(clusterer.predict(any(DataFrame.class))).thenReturn(IntStream.rangeClosed(1, 10).boxed().toList());

		var classifier = mock(Classifier.class);
		when(classifierFitter.fit(any(DataFrame.class), any(Series.class))).thenReturn(classifier);

		var orchestrator = new TrainingOrchestrator(trainingContext);
		var result = orchestrator.runTraining();

		assertEquals(clusterer, result.clusterer());
		assertEquals(classifier, result.classifier());
		assertNotNull(result.oneHotEncoder());
		assertEquals(clock.instant(), result.createdAt());
	}

	private static Series<ZonedDateTime> testSeries() {
		var index = List.of(//
				ZonedDateTime.now(), //
				ZonedDateTime.now().minusMinutes(15), //
				ZonedDateTime.now().minusMinutes(30));
		var values = List.of(0., 1., 2.);
		return new Series<>(index, values);
	}

	private static DataFrame<LocalDate> testDataFrame() {
		var index = List.of(//
				LocalDate.of(2025, 7, 21), //
				LocalDate.of(2025, 7, 20), //
				LocalDate.of(2025, 7, 19));
		var columnNames = List.of("column1", "column2");
		var values = List.of(//
				List.of(0., 1.), //
				List.of(2., 3.), //
				List.of(4., 5.));
		return new DataFrame<>(index, columnNames, values);
	}
}
