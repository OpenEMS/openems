package io.openems.common.bridge.http.metric;

import static org.junit.Assert.assertEquals;

import java.time.Duration;

import org.junit.Before;
import org.junit.Test;

public class MetricGroupTest {

	private static final long DEFAULT_REQUEST_COUNT = 25;
	private static final long DEFAULT_REQUEST_FINISHED_COUNT = 20;
	private static final long DEFAULT_REQUEST_SUCCESS_COUNT = 10;
	private static final long DEFAULT_REQUEST_FAILED_COUNT = 10;
	private static final Duration DEFAULT_DURATION = Duration.ofSeconds(10);

	private MetricGroup metricGroup;

	@Before
	public void setUp() throws Exception {
		this.metricGroup = new MetricGroup(DEFAULT_REQUEST_COUNT, DEFAULT_REQUEST_FINISHED_COUNT,
				DEFAULT_REQUEST_SUCCESS_COUNT, DEFAULT_REQUEST_FAILED_COUNT, DEFAULT_DURATION, DEFAULT_DURATION);
	}

	@Test
	public void withRequestStartetCount() {
		this.metricGroup = this.metricGroup.withRequestStartetCount(100);

		assertEquals(100, this.metricGroup.requestStartetCount());
		assertEquals(DEFAULT_REQUEST_FINISHED_COUNT, this.metricGroup.requestFinishedCount());
		assertEquals(DEFAULT_REQUEST_SUCCESS_COUNT, this.metricGroup.requestSuccessCount());
		assertEquals(DEFAULT_REQUEST_FAILED_COUNT, this.metricGroup.requestFailedCount());
		assertEquals(DEFAULT_DURATION, this.metricGroup.wholeDuration());
		assertEquals(DEFAULT_DURATION, this.metricGroup.maxDuration());
	}

	@Test
	public void withRequestFinishedCount() {
		this.metricGroup = this.metricGroup.withRequestFinishedCount(100);

		assertEquals(DEFAULT_REQUEST_COUNT, this.metricGroup.requestStartetCount());
		assertEquals(100, this.metricGroup.requestFinishedCount());
		assertEquals(DEFAULT_REQUEST_SUCCESS_COUNT, this.metricGroup.requestSuccessCount());
		assertEquals(DEFAULT_REQUEST_FAILED_COUNT, this.metricGroup.requestFailedCount());
		assertEquals(DEFAULT_DURATION, this.metricGroup.wholeDuration());
		assertEquals(DEFAULT_DURATION, this.metricGroup.maxDuration());
	}

	@Test
	public void withRequestSuccessCount() {
		this.metricGroup = this.metricGroup.withRequestSuccessCount(100);

		assertEquals(DEFAULT_REQUEST_COUNT, this.metricGroup.requestStartetCount());
		assertEquals(DEFAULT_REQUEST_FINISHED_COUNT, this.metricGroup.requestFinishedCount());
		assertEquals(100, this.metricGroup.requestSuccessCount());
		assertEquals(DEFAULT_REQUEST_FAILED_COUNT, this.metricGroup.requestFailedCount());
		assertEquals(DEFAULT_DURATION, this.metricGroup.wholeDuration());
		assertEquals(DEFAULT_DURATION, this.metricGroup.maxDuration());
	}

	@Test
	public void withRequestFailedCount() {
		this.metricGroup = this.metricGroup.withRequestFailedCount(100);

		assertEquals(DEFAULT_REQUEST_COUNT, this.metricGroup.requestStartetCount());
		assertEquals(DEFAULT_REQUEST_FINISHED_COUNT, this.metricGroup.requestFinishedCount());
		assertEquals(DEFAULT_REQUEST_SUCCESS_COUNT, this.metricGroup.requestSuccessCount());
		assertEquals(100, this.metricGroup.requestFailedCount());
		assertEquals(DEFAULT_DURATION, this.metricGroup.wholeDuration());
		assertEquals(DEFAULT_DURATION, this.metricGroup.maxDuration());
	}

	@Test
	public void withWholeDuration() {
		this.metricGroup = this.metricGroup.withWholeDuration(Duration.ofSeconds(200));

		assertEquals(DEFAULT_REQUEST_COUNT, this.metricGroup.requestStartetCount());
		assertEquals(DEFAULT_REQUEST_FINISHED_COUNT, this.metricGroup.requestFinishedCount());
		assertEquals(DEFAULT_REQUEST_SUCCESS_COUNT, this.metricGroup.requestSuccessCount());
		assertEquals(DEFAULT_REQUEST_FAILED_COUNT, this.metricGroup.requestFailedCount());
		assertEquals(Duration.ofSeconds(200), this.metricGroup.wholeDuration());
		assertEquals(DEFAULT_DURATION, this.metricGroup.maxDuration());
	}

	@Test
	public void withMaxDuration() {
		this.metricGroup = this.metricGroup.withMaxDuration(Duration.ofSeconds(200));

		assertEquals(DEFAULT_REQUEST_COUNT, this.metricGroup.requestStartetCount());
		assertEquals(DEFAULT_REQUEST_FINISHED_COUNT, this.metricGroup.requestFinishedCount());
		assertEquals(DEFAULT_REQUEST_SUCCESS_COUNT, this.metricGroup.requestSuccessCount());
		assertEquals(DEFAULT_REQUEST_FAILED_COUNT, this.metricGroup.requestFailedCount());
		assertEquals(DEFAULT_DURATION, this.metricGroup.wholeDuration());
		assertEquals(Duration.ofSeconds(200), this.metricGroup.maxDuration());
	}

	@Test
	public void averageDuration() {
		final var averageDuration = this.metricGroup//
				.withRequestFinishedCount(10) //
				.withWholeDuration(Duration.ofSeconds(10)) //
				.averageDuration();

		assertEquals(Duration.ofSeconds(1), averageDuration);
	}
}