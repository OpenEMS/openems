package io.openems.edge.timedata.api.utils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;

@ExtendWith(MockitoExtension.class)
class CalculateActiveTimeTest {

	private static final RuntimeException QUERY_ERROR = new RuntimeException("db unavailable");

	private enum TestChannelId implements ChannelId {
		ACTIVE_TIME_CHANNEL;

		private final io.openems.edge.common.channel.Doc doc = io.openems.edge.common.channel.Doc.of(OpenemsType.LONG);

		@Override
		public io.openems.edge.common.channel.Doc doc() {
			return this.doc;
		}
	}

	@Mock
	private TimedataProvider timedataProvider;

	@Mock
	private Timedata timedata;

	@Mock
	private LongReadChannel channel;

	private Instant now = Instant.EPOCH;
	private Clock mutableClock;

	@BeforeEach
	void setUp() {
		this.mutableClock = new Clock() {
			@Override
			public ZoneId getZone() {
				return ZoneId.of("UTC");
			}

			@Override
			public Clock withZone(ZoneId zone) {
				return Clock.fixed(CalculateActiveTimeTest.this.now, zone);
			}

			@Override
			public Instant instant() {
				return CalculateActiveTimeTest.this.now;
			}
		};

		when(this.timedataProvider.id()).thenReturn("component0");
	}

	@Test
	void shouldKeepQueryRunning_whenTimedataQueryFailsWithinRetryWindow() {
		when(this.timedataProvider.getTimedata()).thenReturn(this.timedata);
		when(this.timedata.getLatestValue(any(ChannelAddress.class)))
				.thenReturn(CompletableFuture.failedFuture(QUERY_ERROR));

		final var sut = new CalculateActiveTime(this.timedataProvider, TestChannelId.ACTIVE_TIME_CHANNEL,
				this.mutableClock);

		sut.update(true);
		this.now = Instant.EPOCH.plusSeconds(5);
		sut.update(true);

		verify(this.channel, never()).setNextValue(any(Long.class));
	}

	@Test
	void shouldStartWithZeroBaseActiveTime_whenTimedataQueryFailsAfterRetryWindow() {
		when(this.timedataProvider.getTimedata()).thenReturn(this.timedata);
		when(this.timedataProvider.channel(TestChannelId.ACTIVE_TIME_CHANNEL)).thenReturn(this.channel);
		when(this.timedata.getLatestValue(any(ChannelAddress.class)))
				.thenReturn(CompletableFuture.failedFuture(QUERY_ERROR));

		final var sut = new CalculateActiveTime(this.timedataProvider, TestChannelId.ACTIVE_TIME_CHANNEL,
				this.mutableClock);

		sut.update(true);
		this.now = Instant.EPOCH.plusSeconds(15);
		sut.update(true);

		this.now = Instant.EPOCH.plusSeconds(16);
		sut.update(true);

		verify(this.channel).setNextValue(0L);
	}

	@Test
	void shouldUseValueFromTimedata_whenQuerySucceeds() {
		when(this.timedataProvider.getTimedata()).thenReturn(this.timedata);
		when(this.timedataProvider.channel(TestChannelId.ACTIVE_TIME_CHANNEL)).thenReturn(this.channel);
		when(this.timedata.getLatestValue(any(ChannelAddress.class)))
				.thenReturn(CompletableFuture.completedFuture(Optional.of(42L)));

		final var sut = new CalculateActiveTime(this.timedataProvider, TestChannelId.ACTIVE_TIME_CHANNEL,
				this.mutableClock);

		sut.update(false);
		this.now = Instant.EPOCH.plusSeconds(1);
		sut.update(false);

		verify(this.channel).setNextValue(42L);
	}

	@Test
	void shouldStartWithZeroBaseActiveTime_whenTimedataQueryReturnsEmpty() {
		when(this.timedataProvider.getTimedata()).thenReturn(this.timedata);
		when(this.timedataProvider.channel(TestChannelId.ACTIVE_TIME_CHANNEL)).thenReturn(this.channel);
		when(this.timedata.getLatestValue(any(ChannelAddress.class)))
				.thenReturn(CompletableFuture.completedFuture(Optional.empty()));

		final var sut = new CalculateActiveTime(this.timedataProvider, TestChannelId.ACTIVE_TIME_CHANNEL,
				this.mutableClock);

		sut.update(false);
		this.now = Instant.EPOCH.plusSeconds(1);
		sut.update(false);

		verify(this.channel).setNextValue(0L);
	}

	@Test
	void shouldIncreaseActiveTime_whenConsecutiveUpdatesAreActive() {
		when(this.timedataProvider.getTimedata()).thenReturn(this.timedata);
		when(this.timedataProvider.channel(TestChannelId.ACTIVE_TIME_CHANNEL)).thenReturn(this.channel);
		when(this.timedata.getLatestValue(any(ChannelAddress.class)))
				.thenReturn(CompletableFuture.completedFuture(Optional.of(10L)));

		final var sut = new CalculateActiveTime(this.timedataProvider, TestChannelId.ACTIVE_TIME_CHANNEL,
				this.mutableClock);

		sut.update(true);
		this.now = Instant.EPOCH.plusMillis(1_500);
		sut.update(true);

		verify(this.channel).setNextValue(11L);
	}
}
