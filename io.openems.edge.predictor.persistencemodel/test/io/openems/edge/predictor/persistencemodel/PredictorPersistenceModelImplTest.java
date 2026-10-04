package io.openems.edge.predictor.persistencemodel;

import static io.openems.common.utils.DateUtils.QUARTERS_PER_DAY;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.SortedMap;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.timedata.Resolution;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.sum.DummySum;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.predictor.api.mlcore.datastructures.Series;
import io.openems.edge.predictor.api.mlcore.interpolation.LinearInterpolator;
import io.openems.edge.predictor.api.prediction.Prediction;
import io.openems.edge.timedata.test.DummyTimedata;

class PredictorPersistenceModelImplTest {

	private static final ChannelAddress CHANNEL = new ChannelAddress("_sum", "UnmanagedProductionActivePower");

	@Test
	void buildForecastRepeatsValuesForTwoDays() {
		final var sut = new PredictorPersistenceModelImpl();
		final var series = series(Instant.parse("2026-01-10T00:00:00Z"), 1., 2., 3.);

		final var result = sut.buildForecast(series);

		assertEquals(QUARTERS_PER_DAY * 2, result.length);
		assertEquals(1., result[0], 0.000001);
		assertEquals(2., result[1], 0.000001);
		assertEquals(3., result[2], 0.000001);
		assertEquals(1., result[3], 0.000001);
	}

	@Test
	void fillRemainingGapsWithPeriodAverageUsesMatchingQuarterAndSkipsMissingDays() {
		final var sut = new PredictorPersistenceModelImpl();
		final var lastDayStart = ZonedDateTime
				.of(2026, Month.JANUARY.getValue(), 10, 23, 30, 0, 0, ZoneId.of("Europe/Berlin")).toInstant();
		final var values = series(lastDayStart, null, 0., null, null);
		final var historicValues = seriesWithIndices(//
				new Instant[] { //
						lastDayStart.minus(Duration.ofDays(1)), //
						lastDayStart.minus(Duration.ofDays(3)), //
						lastDayStart.plus(Duration.ofMinutes(30)).minus(Duration.ofDays(1)), //
						lastDayStart.plus(Duration.ofMinutes(30)).minus(Duration.ofDays(4)), //
						lastDayStart.plus(Duration.ofMinutes(45)).minus(Duration.ofDays(2)), //
						lastDayStart.plus(Duration.ofMinutes(45)).minus(Duration.ofDays(3)) //
				}, //
				100., 300., 280., 120., 330., 110.);

		sut.fillRemainingGapsWithPeriodAverage(values, historicValues);

		assertEquals(200., values.getAt(0), 0.000001);
		assertEquals(0., values.getAt(1), 0.000001);
		assertEquals(200., values.getAt(2), 0.000001);
		assertEquals(220., values.getAt(3), 0.000001);
	}

	@Test
	void getValuesOfLastDayKeepsQuarterAlignmentOnDstSpringForward() {
		final var start = ZonedDateTime.of(2026, 3, 29, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")).toInstant();
		final var historicValues = series(start, sequentialDayValues());

		final var result = PredictorPersistenceModelImpl.getValuesOfLastDay(historicValues);

		assertArrayEquals(sequentialDayValues(), result.getValues().toArray(Double[]::new));
	}

	@Test
	void getValuesOfLastDayKeepsQuarterAlignmentOnDstFallBack() {
		final var start = ZonedDateTime.of(2026, 10, 25, 0, 0, 0, 0, ZoneId.of("Europe/Berlin")).toInstant();
		final var historicValues = series(start, sequentialDayValues());

		final var result = PredictorPersistenceModelImpl.getValuesOfLastDay(historicValues);

		assertArrayEquals(sequentialDayValues(), result.getValues().toArray(Double[]::new));
	}

	@Test
	void applyTransitionSmoothingBlendsFirstQuarters() {
		final var sut = new PredictorPersistenceModelImpl();
		final var forecast = new double[] { 30., 40., 50. };

		sut.applyTransitionSmoothing(forecast, 90.);

		assertEquals(70., forecast[0], 0.000001);
		assertEquals(56.666666666666664, forecast[1], 0.000001);
		assertEquals(50., forecast[2], 0.000001);
	}

	@Test
	void createNewPredictionReturnsEmptyPredictionWhenHistoricQueryReturnsNull() {
		final var now = ZonedDateTime.of(2026, 1, 11, 0, 0, 0, 0, ZoneId.of("Europe/Berlin"));
		final var sut = createSut(new NullResultTimedata(), now);

		final var result = sut.createNewPrediction(CHANNEL);

		assertEquals(Prediction.EMPTY_PREDICTION, result);
	}

	@Test
	void createNewPredictionReturnsEmptyPredictionWhenHistoryHasTooFewQuarters() {
		final var now = ZonedDateTime.of(2026, 1, 11, 0, 0, 0, 0, ZoneId.of("Europe/Berlin"));
		final var timedata = new DummyTimedata("timedata0");
		final var start = now.minusMinutes(95L * 15);

		for (int i = 0; i < 95; i++) {
			timedata.add(start.plusMinutes(15L * i), CHANNEL, new JsonPrimitive(i));
		}

		final var sut = createSut(timedata, now);

		final var result = sut.createNewPrediction(CHANNEL);

		assertEquals(Prediction.EMPTY_PREDICTION, result);
	}

	@Test
	void createNewPredictionReturnsEmptyPredictionWhenGapsRemainAfterFill() {
		final var now = ZonedDateTime.of(2026, 1, 11, 0, 0, 0, 0, ZoneId.of("Europe/Berlin"));
		final var timedata = new DummyTimedata("timedata0");
		final var start = now.minusDays(1);

		for (int i = 0; i < QUARTERS_PER_DAY; i++) {
			final var value = i < 5 ? JsonNull.INSTANCE : new JsonPrimitive(i);
			timedata.add(start.plusMinutes(15L * i), CHANNEL, value);
		}

		final var sut = createSut(timedata, now);

		final var result = sut.createNewPrediction(CHANNEL);

		assertEquals(Prediction.EMPTY_PREDICTION, result);
	}

	@Test
	void createNewPredictionReturnsSmoothedAndRoundedForecast() {
		final var now = ZonedDateTime.of(2026, 1, 11, 0, 0, 0, 0, ZoneId.of("Europe/Berlin"));
		final var timedata = new DummyTimedata("timedata0");
		final var start = now.minusDays(1);

		for (int i = 0; i < QUARTERS_PER_DAY; i++) {
			timedata.add(start.plusMinutes(15L * i), CHANNEL, new JsonPrimitive(i));
		}

		final var sut = createSut(timedata, now);

		final var result = sut.createNewPrediction(CHANNEL);
		final var values = result.asArray();

		assertEquals(now.toInstant(), result.getFirstTime());
		assertEquals(QUARTERS_PER_DAY * 2, values.length);
		assertEquals(63, values[0]);
		assertEquals(32, values[1]);
		assertEquals(2, values[2]);
	}

	private static Series<Instant> series(Instant start, Double... values) {
		final var index = new ArrayList<Instant>(values.length);
		for (int i = 0; i < values.length; i++) {
			index.add(start.plus(Duration.ofMinutes(15L * i)));
		}
		return new Series<>(index, Arrays.asList(values));
	}

	private static Series<Instant> seriesWithIndices(Instant[] index, Double... values) {
		return new Series<>(Arrays.asList(index), Arrays.asList(values));
	}

	private static PredictorPersistenceModelImpl createSut(DummyTimedata timedata, ZonedDateTime now) {
		return new PredictorPersistenceModelImpl(//
				new LinearInterpolator(4), //
				values -> values, //
				new DummySum(), //
				timedata, //
				new DummyComponentManager(Clock.fixed(now.toInstant(), now.getZone())));
	}

	private static Double[] sequentialDayValues() {
		return IntStream.range(0, QUARTERS_PER_DAY) //
				.mapToObj(i -> (double) i) //
				.toArray(Double[]::new);
	}

	private static class NullResultTimedata extends DummyTimedata {

		private NullResultTimedata() {
			super("timedata0");
		}

		@Override
		public SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> queryHistoricData(String edgeId,
				ZonedDateTime fromDate, ZonedDateTime toDate, Set<ChannelAddress> channels, Resolution resolution)
				throws OpenemsNamedException {
			return null;
		}
	}
}
