package io.openems.edge.timedata.api.utils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;

class TimedataQueryRetryHandler {

	private static final Duration TIMEDATA_QUERY_RETRY_WINDOW = Duration.ofSeconds(15);

	enum Decision {
		RETRY, //
		GIVE_UP //
	}

	private final Logger log;
	private final Clock clock;
	private final String componentId;
	private final String channelId;

	private Instant firstFailedAt = null;

	TimedataQueryRetryHandler(Logger log, Clock clock, String componentId, String channelId) {
		this.log = log;
		this.clock = clock;
		this.componentId = componentId;
		this.channelId = channelId;
	}

	Decision onFailure(Throwable throwable) {
		final var now = Instant.now(this.clock);

		if (this.firstFailedAt == null) {
			this.firstFailedAt = now;
			this.log.warn("[{}/{}] Timedata query failed. Start retry window ({} ms).", this.componentId,
					this.channelId, TIMEDATA_QUERY_RETRY_WINDOW.toMillis(), throwable);
			return Decision.RETRY;
		}

		final var durationWaited = Duration.between(this.firstFailedAt, now);
		if (durationWaited.compareTo(TIMEDATA_QUERY_RETRY_WINDOW) < 0) {
			this.log.warn("[{}/{}] Timedata query failed after {} ms. Retrying.", this.componentId, this.channelId,
					durationWaited.toMillis(), throwable);
			return Decision.RETRY;
		}

		this.log.warn("[{}/{}] Timedata query failed for {} ms. Reset channel value.", this.componentId, this.channelId,
				durationWaited.toMillis(), throwable);
		this.firstFailedAt = null;
		return Decision.GIVE_UP;
	}

	void reset() {
		this.firstFailedAt = null;
	}
}
