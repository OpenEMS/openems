package io.openems.common.tariff.smard;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.dummy.DummyBridgeHttpBundle;
import io.openems.common.bridge.http.time.DelayTimeProvider;
import io.openems.common.tariff.api.TariffUpdateStatus;
import io.openems.common.test.TimeLeapClock;
import io.openems.common.types.BiddingZone;

class SmardApiTest {

	@Test
	void testFetch() {
		final var httpTestBundle = DummyBridgeHttpBundle.of();
		final var clock = new TimeLeapClock(Instant.ofEpochMilli(1785744000000L));
		final var config = new SmardTariffConfig(80);
		final var tariffProvider = new SmardTariffProvider(config, httpTestBundle.factory(), clock, "tariffSpot0");
		tariffProvider.activate();

		httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok(TestData.TIMESTAMP_JSON));
		httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok(TestData.TIMESERIES_JSON));

		final var smardTariff = tariffProvider.getSpotTariff(BiddingZone.GERMANY);
		httpTestBundle.runTasksImmediately();
		httpTestBundle.runTasksImmediately();

		var updateStatus = smardTariff.getLastUpdateStatus().getValue();
		assertInstanceOf(TariffUpdateStatus.Successful.class, updateStatus);

		var data = smardTariff.getTariffData().getValue();
		assertEquals(Instant.ofEpochMilli(1785929551499L), data.lastUpdate());
		assertEquals(150.99, data.prices().getAt(Instant.ofEpochMilli(1785708000000L)), 0.0001);
		assertNull(data.prices().getAt(Instant.ofEpochMilli(1786053600000L)));

		tariffProvider.deactivate();
	}

	@Test
	void testDelay() {
		final var httpTestBundle = DummyBridgeHttpBundle.of();
		final var clock = new TimeLeapClock(Instant.parse("2026-01-01T13:00:00+01:00"));
		final var config = new SmardTariffConfig(80);
		final var smardTariff = new SmardSpotTariff(SmardBiddingZone.GERMANY, config, httpTestBundle.factory(), clock,
				"tariffSpot0");

		smardTariff.activate();

		assertThat("nextFetchDelay",
				getSeconds(smardTariff.smardApi.calculateNextFetchDelay(Instant.parse("2026-01-01T23:45:00+01:00"))),
				greaterThanOrEqualTo(Duration.ofHours(1).getSeconds()));
	}

	private static long getSeconds(DelayTimeProvider.Delay delay) {
		return ((DelayTimeProvider.Delay.DurationDelay) delay).getDuration().getSeconds();
	}
}
