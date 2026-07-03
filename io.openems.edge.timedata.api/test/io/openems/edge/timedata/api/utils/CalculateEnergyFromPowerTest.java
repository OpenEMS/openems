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
class CalculateEnergyFromPowerTest {

	private static final RuntimeException QUERY_ERROR = new RuntimeException("db unavailable");

	private enum TestChannelId implements ChannelId {
		ENERGY_CHANNEL;

		private final io.openems.edge.common.channel.Doc doc = io.openems.edge.common.channel.Doc.of(OpenemsType.LONG);

		@Override
		public io.openems.edge.common.channel.Doc doc() {
			return this.doc;
		}
	}

	@Mock
	private TimedataProvider component;

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
				return Clock.fixed(CalculateEnergyFromPowerTest.this.now, zone);
			}

			@Override
			public Instant instant() {
				return CalculateEnergyFromPowerTest.this.now;
			}
		};

		when(this.component.id()).thenReturn("component0");
	}

	@Test
	void shouldKeepQueryRunning_whenTimedataQueryFailsWithinRetryWindow() {
		when(this.component.getTimedata()).thenReturn(this.timedata);
		when(this.timedata.getLatestValue(any(ChannelAddress.class)))
				.thenReturn(CompletableFuture.failedFuture(QUERY_ERROR));

		final var sut = new CalculateEnergyFromPower(this.component, TestChannelId.ENERGY_CHANNEL, this.mutableClock);

		sut.update(100);

		this.now = Instant.EPOCH.plusSeconds(5);
		sut.update(100);

		verify(this.channel, never()).setNextValue(any(Long.class));
	}

	@Test
	void shouldStartWithZeroBaseEnergy_whenTimedataQueryFailsAfterRetryWindow() {
		when(this.component.getTimedata()).thenReturn(this.timedata);
		when(this.component.channel(TestChannelId.ENERGY_CHANNEL)).thenReturn(this.channel);
		when(this.timedata.getLatestValue(any(ChannelAddress.class)))
				.thenReturn(CompletableFuture.failedFuture(QUERY_ERROR));

		final var sut = new CalculateEnergyFromPower(this.component, TestChannelId.ENERGY_CHANNEL, this.mutableClock);

		sut.update(100);

		this.now = Instant.EPOCH.plusSeconds(15);
		sut.update(100);

		this.now = Instant.EPOCH.plusSeconds(16);
		sut.update(100);

		verify(this.channel).setNextValue(0L);
	}

	@Test
	void shouldUseValueFromTimedata_whenQuerySucceeds() {
		when(this.component.getTimedata()).thenReturn(this.timedata);
		when(this.component.channel(TestChannelId.ENERGY_CHANNEL)).thenReturn(this.channel);
		when(this.timedata.getLatestValue(any(ChannelAddress.class)))
				.thenReturn(CompletableFuture.completedFuture(Optional.of(42L)));

		final var sut = new CalculateEnergyFromPower(this.component, TestChannelId.ENERGY_CHANNEL, this.mutableClock);

		sut.update(0);

		this.now = Instant.EPOCH.plusSeconds(1);
		sut.update(0);

		verify(this.channel).setNextValue(42L);
	}

	@Test
	void shouldStartWithZeroBaseEnergy_whenTimedataQueryReturnsEmpty() {
		when(this.component.getTimedata()).thenReturn(this.timedata);
		when(this.component.channel(TestChannelId.ENERGY_CHANNEL)).thenReturn(this.channel);
		when(this.timedata.getLatestValue(any(ChannelAddress.class)))
				.thenReturn(CompletableFuture.completedFuture(Optional.empty()));

		final var sut = new CalculateEnergyFromPower(this.component, TestChannelId.ENERGY_CHANNEL, this.mutableClock);

		sut.update(0);

		this.now = Instant.EPOCH.plusSeconds(1);
		sut.update(0);

		verify(this.channel).setNextValue(0L);
	}

	@Test
	void shouldSkipTimedataQuery_whenBaseEnergySetManually() {
		when(this.component.channel(TestChannelId.ENERGY_CHANNEL)).thenReturn(this.channel);

		final var sut = new CalculateEnergyFromPower(this.component, TestChannelId.ENERGY_CHANNEL, this.mutableClock);

		sut.setBaseEnergyManually(100L);

		this.now = Instant.EPOCH.plusSeconds(1);
		sut.update(0);

		verify(this.channel).setNextValue(100L);
		verify(this.timedata, never()).getLatestValue(any());
	}
}