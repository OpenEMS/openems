package io.openems.common.bridge.http.metric;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class MetricGroupTest {

	private static final long DEFAULT_REQUEST_COUNT = 25;
	private static final long DEFAULT_REQUEST_FINISHED_COUNT = 20;
	private static final long DEFAULT_REQUEST_SUCCESS_COUNT = 10;
	private static final long DEFAULT_REQUEST_FAILED_COUNT = 10;
	private static final Duration DEFAULT_DURATION = Duration.ofSeconds(10);

	private static MetricGroup metricGroup;

	@BeforeAll
	static void setUp() throws Exception {
		metricGroup = new MetricGroup(DEFAULT_REQUEST_COUNT, DEFAULT_REQUEST_FINISHED_COUNT,
				DEFAULT_REQUEST_SUCCESS_COUNT, DEFAULT_REQUEST_FAILED_COUNT, DEFAULT_DURATION, DEFAULT_DURATION);
	}

	@Test
	void withRequestStartetCount() {
		var metricGroup = MetricGroupTest.metricGroup.withRequestStartetCount(100);

		assertEquals(100, metricGroup.requestStartetCount());
		assertEquals(DEFAULT_REQUEST_FINISHED_COUNT, metricGroup.requestFinishedCount());
		assertEquals(DEFAULT_REQUEST_SUCCESS_COUNT, metricGroup.requestSuccessCount());
		assertEquals(DEFAULT_REQUEST_FAILED_COUNT, metricGroup.requestFailedCount());
		assertEquals(DEFAULT_DURATION, metricGroup.wholeDuration());
		assertEquals(DEFAULT_DURATION, metricGroup.maxDuration());
	}

	@Test
	void withRequestFinishedCount() {
		var metricGroup = MetricGroupTest.metricGroup.withRequestFinishedCount(100);

		assertEquals(DEFAULT_REQUEST_COUNT, metricGroup.requestStartetCount());
		assertEquals(100, metricGroup.requestFinishedCount());
		assertEquals(DEFAULT_REQUEST_SUCCESS_COUNT, metricGroup.requestSuccessCount());
		assertEquals(DEFAULT_REQUEST_FAILED_COUNT, metricGroup.requestFailedCount());
		assertEquals(DEFAULT_DURATION, metricGroup.wholeDuration());
		assertEquals(DEFAULT_DURATION, metricGroup.maxDuration());
	}

	@Test
	void withRequestSuccessCount() {
		var metricGroup = MetricGroupTest.metricGroup.withRequestSuccessCount(100);

		assertEquals(DEFAULT_REQUEST_COUNT, metricGroup.requestStartetCount());
		assertEquals(DEFAULT_REQUEST_FINISHED_COUNT, metricGroup.requestFinishedCount());
		assertEquals(100, metricGroup.requestSuccessCount());
		assertEquals(DEFAULT_REQUEST_FAILED_COUNT, metricGroup.requestFailedCount());
		assertEquals(DEFAULT_DURATION, metricGroup.wholeDuration());
		assertEquals(DEFAULT_DURATION, metricGroup.maxDuration());
	}

	@Test
	void withRequestFailedCount() {
		var metricGroup = MetricGroupTest.metricGroup.withRequestFailedCount(100);

		assertEquals(DEFAULT_REQUEST_COUNT, metricGroup.requestStartetCount());
		assertEquals(DEFAULT_REQUEST_FINISHED_COUNT, metricGroup.requestFinishedCount());
		assertEquals(DEFAULT_REQUEST_SUCCESS_COUNT, metricGroup.requestSuccessCount());
		assertEquals(100, metricGroup.requestFailedCount());
		assertEquals(DEFAULT_DURATION, metricGroup.wholeDuration());
		assertEquals(DEFAULT_DURATION, metricGroup.maxDuration());
	}

	@Test
	void withWholeDuration() {
		var metricGroup = MetricGroupTest.metricGroup.withWholeDuration(Duration.ofSeconds(200));

		assertEquals(DEFAULT_REQUEST_COUNT, metricGroup.requestStartetCount());
		assertEquals(DEFAULT_REQUEST_FINISHED_COUNT, metricGroup.requestFinishedCount());
		assertEquals(DEFAULT_REQUEST_SUCCESS_COUNT, metricGroup.requestSuccessCount());
		assertEquals(DEFAULT_REQUEST_FAILED_COUNT, metricGroup.requestFailedCount());
		assertEquals(Duration.ofSeconds(200), metricGroup.wholeDuration());
		assertEquals(DEFAULT_DURATION, metricGroup.maxDuration());
	}

	@Test
	void withMaxDuration() {
		var metricGroup = MetricGroupTest.metricGroup.withMaxDuration(Duration.ofSeconds(200));

		assertEquals(DEFAULT_REQUEST_COUNT, metricGroup.requestStartetCount());
		assertEquals(DEFAULT_REQUEST_FINISHED_COUNT, metricGroup.requestFinishedCount());
		assertEquals(DEFAULT_REQUEST_SUCCESS_COUNT, metricGroup.requestSuccessCount());
		assertEquals(DEFAULT_REQUEST_FAILED_COUNT, metricGroup.requestFailedCount());
		assertEquals(DEFAULT_DURATION, metricGroup.wholeDuration());
		assertEquals(Duration.ofSeconds(200), metricGroup.maxDuration());
	}

	@Test
	void averageDuration() {
		final var averageDuration = metricGroup//
				.withRequestFinishedCount(10) //
				.withWholeDuration(Duration.ofSeconds(10)) //
				.averageDuration();

		assertEquals(Duration.ofSeconds(1), averageDuration);
	}
}