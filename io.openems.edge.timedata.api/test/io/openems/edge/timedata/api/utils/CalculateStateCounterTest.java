package io.openems.edge.timedata.api.utils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
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
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;

@ExtendWith(MockitoExtension.class)
class CalculateStateCounterTest {

	private static final RuntimeException QUERY_ERROR = new RuntimeException("db unavailable");

	private enum TestChannelId implements ChannelId {
		STATE_COUNTER_CHANNEL;

		private final io.openems.edge.common.channel.Doc doc = io.openems.edge.common.channel.Doc
				.of(OpenemsType.INTEGER);

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
	private IntegerReadChannel channel;

	private Instant now = Instant.EPOCH;
	private java.time.Clock mutableClock;

	@BeforeEach
	void setUp() {
		this.mutableClock = new java.time.Clock() {
			@Override
			public java.time.ZoneId getZone() {
				return java.time.ZoneId.of("UTC");
			}

			@Override
			public java.time.Clock withZone(java.time.ZoneId zone) {
				return java.time.Clock.fixed(CalculateStateCounterTest.this.now, zone);
			}

			@Override
			public Instant instant() {
				return CalculateStateCounterTest.this.now;
			}
		};
		when(this.timedataProvider.id()).thenReturn("component0");
	}

	@Test
	void shouldKeepQueryRunning_whenTimedataQueryFailsWithinRetryWindow() {
		when(this.timedataProvider.getTimedata()).thenReturn(this.timedata);
		when(this.timedata.getLatestValue(any(ChannelAddress.class)))
				.thenReturn(CompletableFuture.failedFuture(QUERY_ERROR));

		final var sut = new CalculateStateCounter(this.timedataProvider, TestChannelId.STATE_COUNTER_CHANNEL,
				this.mutableClock);

		sut.update(true);
		this.now = Instant.EPOCH.plusSeconds(5);
		sut.update(true);

		verify(this.channel, never()).setNextValue(any(Integer.class));
	}

	@Test
	void shouldStartWithZeroCounter_whenTimedataQueryFailsAfterRetryWindow() {
		when(this.timedataProvider.getTimedata()).thenReturn(this.timedata);
		when(this.timedataProvider.channel(TestChannelId.STATE_COUNTER_CHANNEL)).thenReturn(this.channel);
		when(this.timedata.getLatestValue(any(ChannelAddress.class)))
				.thenReturn(CompletableFuture.failedFuture(QUERY_ERROR));

		final var sut = new CalculateStateCounter(this.timedataProvider, TestChannelId.STATE_COUNTER_CHANNEL,
				this.mutableClock);

		sut.update(true);
		this.now = Instant.EPOCH.plusSeconds(15);
		sut.update(true);

		verify(this.channel).setNextValue(0);
	}

	@Test
	void shouldUseValueFromTimedataAndIncrement_whenCounterIsActive() {
		when(this.timedataProvider.getTimedata()).thenReturn(this.timedata);
		when(this.timedataProvider.channel(TestChannelId.STATE_COUNTER_CHANNEL)).thenReturn(this.channel);
		when(this.timedata.getLatestValue(any(ChannelAddress.class)))
				.thenReturn(CompletableFuture.completedFuture(Optional.of(5)));

		final var sut = new CalculateStateCounter(this.timedataProvider, TestChannelId.STATE_COUNTER_CHANNEL,
				this.mutableClock);

		sut.update(false);
		sut.update(true);

		verify(this.channel).setNextValue(5);
		verify(this.channel).setNextValue(6);
	}

	@Test
	void shouldStartWithZeroCounter_whenTimedataQueryReturnsEmpty() {
		when(this.timedataProvider.getTimedata()).thenReturn(this.timedata);
		when(this.timedataProvider.channel(TestChannelId.STATE_COUNTER_CHANNEL)).thenReturn(this.channel);
		when(this.timedata.getLatestValue(any(ChannelAddress.class)))
				.thenReturn(CompletableFuture.completedFuture(Optional.empty()));

		final var sut = new CalculateStateCounter(this.timedataProvider, TestChannelId.STATE_COUNTER_CHANNEL,
				this.mutableClock);

		sut.update(true);

		verify(this.channel).setNextValue(0);
	}

	@Test
	void shouldResetCounterToZero_whenResetCounterIsCalled() {
		when(this.timedataProvider.channel(TestChannelId.STATE_COUNTER_CHANNEL)).thenReturn(this.channel);

		final var sut = new CalculateStateCounter(this.timedataProvider, TestChannelId.STATE_COUNTER_CHANNEL,
				this.mutableClock);

		sut.resetCounter();

		verify(this.channel).setNextValue(0);
		verify(this.timedata, never()).getLatestValue(any());
	}
}
