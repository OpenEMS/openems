package io.openems.edge.predictor.profileclusteringmodel.prediction;

import static io.openems.edge.predictor.profileclusteringmodel.ColumnNames.DAY_OF_WEEK;
import static io.openems.edge.predictor.profileclusteringmodel.ColumnNames.LABEL_LAG_1_DAY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.meta.types.SubdivisionCode;
import io.openems.edge.predictor.api.mlcore.classification.Classifier;
import io.openems.edge.predictor.api.mlcore.clustering.Clusterer;
import io.openems.edge.predictor.api.mlcore.datastructures.DataFrame;
import io.openems.edge.predictor.api.mlcore.transformer.OneHotEncoder;
import io.openems.edge.predictor.profileclusteringmodel.CurrentProfile;
import io.openems.edge.predictor.profileclusteringmodel.PredictorProfileClusteringModelImpl;
import io.openems.edge.predictor.profileclusteringmodel.Profile;
import io.openems.edge.predictor.profileclusteringmodel.services.FeatureEngineeringService;
import io.openems.edge.predictor.profileclusteringmodel.services.ProfileClusteringPredictionService;
import io.openems.edge.predictor.profileclusteringmodel.services.RawTimeSeriesService;
import io.openems.edge.predictor.profileclusteringmodel.services.TimeSeriesPreprocessingService;
import io.openems.edge.timedata.test.DummyTimedata;

@RunWith(MockitoJUnitRunner.class)
public class PredictionOrchestratorTest {

	private static final ChannelAddress DUMMY_CHANNEL_ADDRESS = new ChannelAddress(//
			"_sum", "DummyChannel");

	@Mock
	private PredictorProfileClusteringModelImpl parent;

	@Mock
	private Clusterer clusterer;

	@Mock
	private Classifier classifier;

	@Mock
	private RawTimeSeriesService rawTimeSeriesService;

	@Mock
	private TimeSeriesPreprocessingService timeSeriesPreprocessingService;

	@Mock
	private ProfileClusteringPredictionService profileClusteringPredictionService;

	@Mock
	private FeatureEngineeringService featureEngineeringService;

	@Mock
	private ProfileSwitcher profileSwitcher;

	private Clock clock;
	private DummyTimedata timedata;
	private OneHotEncoder<LocalDate> oneHotEncoder;

	@Before
	public void setUp() {
		this.clock = Clock.fixed(//
				Instant.parse("2025-07-17T15:45:00Z"), //
				ZoneOffset.UTC);

		this.timedata = new DummyTimedata("timedata0");
		for (int day = 1; day <= 2; day++) {
			var dayStart = ZonedDateTime.now(this.clock).truncatedTo(ChronoUnit.DAYS).minusDays(day);
			for (int min = 0; min < 24 * 60; min += 15) {
				this.timedata.add(dayStart.plusMinutes(min), DUMMY_CHANNEL_ADDRESS, day * 10);
			}
		}

		this.oneHotEncoder = new OneHotEncoder<LocalDate>(//
				List.of(DAY_OF_WEEK, LABEL_LAG_1_DAY), //
				true);
	}

	@Test
	public void testPredictProfiles_ShouldThrowException_WhenForecastDaysIsLessThanOne() {
		var orchestrator = new PredictionOrchestrator(//
				null, //
				this.rawTimeSeriesService, //
				this.timeSeriesPreprocessingService, //
				this.profileClusteringPredictionService, //
				this.featureEngineeringService);

		assertThrows(IllegalArgumentException.class, () -> {
			orchestrator.predictProfiles(0);
		});
	}

	@Test
	public void testPredictProfiles_ShouldPredictNewProfile_WhenCurrentProfileIsNull() throws Exception {
		when(this.clusterer.predict(any())).thenAnswer(invocation -> {
			DataFrame<LocalDate> dataframe = invocation.getArgument(0);

			return IntStream.range(1, dataframe.rowCount() + 1)//
					.boxed()//
					.toList();
		});

		var centroids = IntStream.rangeClosed(1, 3).mapToObj(i -> {
			double[] arr = new double[96];
			Arrays.fill(arr, i);
			return arr;
		}).toList();
		when(this.clusterer.getCentroids()).thenReturn(centroids);

		when(this.classifier.predict(anyList())).thenReturn(1);

		CurrentProfile currentProfile = null;

		var predictionContext = new PredictionContext(//
				() -> this.clock, //
				this.timedata, //
				DUMMY_CHANNEL_ADDRESS, //
				8, // maxGapSizeInterpolation
				this.clusterer, //
				this.classifier, //
				this.oneHotEncoder, //
				() -> SubdivisionCode.DE_BY, //
				(a, b, c) -> this.profileSwitcher, //
				currentProfile);

		var orchestrator = new PredictionOrchestrator(predictionContext);

		int forecastDays = 1;
		final var predictedProfiles = orchestrator.predictProfiles(forecastDays);

		var predictedCurrentProfile = predictedProfiles.getFirst();
		assertEquals(1, predictedCurrentProfile.clusterIndex());
		assertEquals(Arrays.stream(centroids.get(1)).boxed().toList(), predictedCurrentProfile.values().getValues());
		assertEquals(predictedCurrentProfile, predictedProfiles.getFirst());
	}

	@Test
	public void testPredictProfiles_ShouldPredictNewProfile_WhenCurrentProfileIsOutdated() throws Exception {
		when(this.clusterer.predict(any())).thenAnswer(invocation -> {
			DataFrame<LocalDate> dataframe = invocation.getArgument(0);

			return IntStream.range(1, dataframe.rowCount() + 1)//
					.boxed()//
					.toList();
		});

		var centroids = IntStream.rangeClosed(1, 3).mapToObj(i -> {
			double[] arr = new double[96];
			Arrays.fill(arr, i);
			return arr;
		}).toList();
		when(this.clusterer.getCentroids()).thenReturn(centroids);

		when(this.classifier.predict(anyList())).thenReturn(1);

		var currentProfile = new CurrentProfile(//
				LocalDate.now(this.clock).minusDays(1), //
				Profile.fromArray(2, centroids.get(2)));

		var predictionContext = new PredictionContext(//
				() -> this.clock, //
				this.timedata, //
				DUMMY_CHANNEL_ADDRESS, //
				8, // maxGapSizeInterpolation
				this.clusterer, //
				this.classifier, //
				this.oneHotEncoder, //
				() -> SubdivisionCode.DE_BY, //
				(a, b, c) -> this.profileSwitcher, //
				currentProfile);

		var orchestrator = new PredictionOrchestrator(predictionContext);

		int forecastDays = 1;
		final var predictedProfiles = orchestrator.predictProfiles(forecastDays);

		var predictedCurrentProfile = predictedProfiles.getFirst();
		assertEquals(1, predictedCurrentProfile.clusterIndex());
		assertEquals(Arrays.stream(centroids.get(1)).boxed().toList(), predictedCurrentProfile.values().getValues());
		assertEquals(predictedCurrentProfile, predictedProfiles.getFirst());
	}

	@Test
	public void testPredictProfiles_ShouldUseBetterProfile_WhenBetterProfileIsAvailable() throws Exception {
		when(this.clusterer.predict(any())).thenAnswer(invocation -> {
			DataFrame<LocalDate> dataframe = invocation.getArgument(0);

			return IntStream.range(1, dataframe.rowCount() + 1)//
					.boxed()//
					.toList();
		});

		var centroids = IntStream.rangeClosed(1, 3).mapToObj(i -> {
			double[] arr = new double[96];
			Arrays.fill(arr, i);
			return arr;
		}).toList();
		when(this.clusterer.getCentroids()).thenReturn(centroids);

		when(this.profileSwitcher.findBetterProfile()).thenReturn(//
				Optional.of(Profile.fromArray(1, centroids.get(1))));

		var currentProfile = new CurrentProfile(//
				LocalDate.now(this.clock), //
				Profile.fromArray(2, centroids.get(2)));

		var predictionContext = new PredictionContext(//
				() -> this.clock, //
				this.timedata, //
				DUMMY_CHANNEL_ADDRESS, //
				8, // maxGapSizeInterpolation
				this.clusterer, //
				this.classifier, //
				this.oneHotEncoder, //
				() -> SubdivisionCode.DE_BY, //
				(a, b, c) -> this.profileSwitcher, //
				currentProfile);

		var orchestrator = new PredictionOrchestrator(predictionContext);

		int forecastDays = 1;
		final var predictedProfiles = orchestrator.predictProfiles(forecastDays);

		var predictedCurrentProfile = predictedProfiles.getFirst();
		assertEquals(1, predictedCurrentProfile.clusterIndex());
		assertEquals(Arrays.stream(centroids.get(1)).boxed().toList(), predictedCurrentProfile.values().getValues());
		assertEquals(predictedCurrentProfile, predictedProfiles.getFirst());
	}

	@Test
	public void testPredictProfiles_ShouldKeepCurrentProfile_WhenNoBetterProfileIsAvailable() throws Exception {
		when(this.clusterer.predict(any())).thenAnswer(invocation -> {
			DataFrame<LocalDate> dataframe = invocation.getArgument(0);

			return IntStream.range(1, dataframe.rowCount() + 1)//
					.boxed()//
					.toList();
		});

		var centroids = IntStream.rangeClosed(1, 3).mapToObj(i -> {
			double[] arr = new double[96];
			Arrays.fill(arr, i);
			return arr;
		}).toList();

		when(this.clusterer.getCentroids()).thenReturn(centroids);

		when(this.profileSwitcher.findBetterProfile()).thenReturn(Optional.empty());

		var currentProfile = new CurrentProfile(//
				LocalDate.now(this.clock), //
				Profile.fromArray(2, centroids.get(2)));

		var predictionContext = new PredictionContext(//
				() -> this.clock, //
				this.timedata, //
				DUMMY_CHANNEL_ADDRESS, //
				8, // maxGapSizeInterpolation
				this.clusterer, //
				this.classifier, //
				this.oneHotEncoder, //
				() -> SubdivisionCode.DE_BY, //
				(a, b, c) -> this.profileSwitcher, //
				currentProfile);

		var orchestrator = new PredictionOrchestrator(predictionContext);

		int forecastDays = 1;
		var predictedProfiles = orchestrator.predictProfiles(forecastDays);

		var predictedCurrentProfile = predictedProfiles.getFirst();
		assertEquals(currentProfile.profile(), predictedCurrentProfile);
		assertEquals(currentProfile.profile(), predictedProfiles.getFirst());
	}

	@Test
	public void testPredictProfiles_ShouldPredictFutureProfiles_WhenForecastDaysAreMoreThanOne() throws Exception {
		when(this.clusterer.predict(any())).thenAnswer(invocation -> {
			DataFrame<LocalDate> dataframe = invocation.getArgument(0);

			return IntStream.range(1, dataframe.rowCount() + 1)//
					.boxed()//
					.toList();
		});

		var centroids = IntStream.rangeClosed(1, 3).mapToObj(i -> {
			double[] arr = new double[96];
			Arrays.fill(arr, i);
			return arr;
		}).toList();

		when(this.clusterer.getCentroids()).thenReturn(centroids);

		when(this.classifier.predict(anyList()))//
				.thenReturn(1)//
				.thenReturn(2);

		when(this.profileSwitcher.findBetterProfile()).thenReturn(Optional.empty());

		var currentProfile = new CurrentProfile(//
				LocalDate.now(this.clock), //
				Profile.fromArray(2, centroids.get(2)));

		var predictionContext = new PredictionContext(//
				() -> this.clock, //
				this.timedata, //
				DUMMY_CHANNEL_ADDRESS, //
				8, // maxGapSizeInterpolation
				this.clusterer, //
				this.classifier, //
				this.oneHotEncoder, //
				() -> SubdivisionCode.DE_BY, //
				(a, b, c) -> this.profileSwitcher, //
				currentProfile);

		var orchestrator = new PredictionOrchestrator(predictionContext);

		int forecastDays = 3;
		var predictedProfiles = orchestrator.predictProfiles(forecastDays);

		assertEquals(3, predictedProfiles.size());
		assertEquals(1, predictedProfiles.get(1).clusterIndex());
		assertEquals(Arrays.stream(centroids.get(1)).boxed().toList(), predictedProfiles.get(1).values().getValues());
		assertEquals(2, predictedProfiles.get(2).clusterIndex());
		assertEquals(Arrays.stream(centroids.get(2)).boxed().toList(), predictedProfiles.get(2).values().getValues());
	}
}
