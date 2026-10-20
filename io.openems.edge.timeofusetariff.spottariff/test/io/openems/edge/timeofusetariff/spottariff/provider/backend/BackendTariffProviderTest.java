package io.openems.edge.timeofusetariff.spottariff.provider.backend;

import static io.openems.common.test.TestUtils.createDummyClock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Currency;

import org.junit.jupiter.api.Test;

import io.openems.common.jsonrpc.syncable.SyncableEntry;
import io.openems.common.jsonrpc.syncable.SyncableKey;
import io.openems.common.timedata.DurationUnit;
import io.openems.common.types.BiddingZone;
import io.openems.common.types.TariffData;
import io.openems.common.types.TariffType;
import io.openems.common.utils.TimeRangeValues;
import io.openems.common.utils.TimeSpan;
import io.openems.edge.common.services.syncable.DummySyncableRepository;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.controller.api.backend.services.BackendConnectionState;
import io.openems.edge.controller.api.backend.services.DummyBackendConnectionStateListener;

class BackendTariffProviderTest {

	private static final SyncableKey<TariffData> SYNCABLE_KEY = new SyncableKey.SpotTariff(BiddingZone.GERMANY);

	@Test
	void testBackendProvider() throws Exception {
		final var clock = createDummyClock();

		final var syncableRepository = new DummySyncableRepository();
		syncableRepository.setEntry(SYNCABLE_KEY, new SyncableEntry<>(SYNCABLE_KEY, null, clock.instant()));

		final var backendConnectionStateListener = new DummyBackendConnectionStateListener();
		backendConnectionStateListener.setConnectionState(new BackendConnectionState.Connected());

		final var tariffProvider = new BackendTariffProviderImpl();
		final var componentTest = new ComponentTest(tariffProvider) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("syncableRepository", syncableRepository) //
				.addReference("backendConnectionStateListener", backendConnectionStateListener) //
				.activate(MyConfig.create() //
						.setId("tariffSpot0") //
						.build());

		final var spotTariff = tariffProvider.getSpotTariff(BiddingZone.GERMANY);

		componentTest //
				.next(new TestCase("Prices not available yet") //
						.onAfterProcessImage(() -> {
							assertNull(spotTariff.getTariffData().getValue());
						})) //
				.next(new TestCase("Prices available") //
						.onBeforeProcessImage(() -> {
							final var testTariffData = buildStaticTariff(25.0D, clock);
							syncableRepository.setEntry(SYNCABLE_KEY,
									new SyncableEntry<>(SYNCABLE_KEY, testTariffData, clock.instant()));
						}) //
						.onAfterProcessImage(() -> {
							final var tariffData = spotTariff.getTariffData().getValue();
							assertNotNull(tariffData);
							assertEquals(25.0D, tariffData.prices().getAt(clock.instant()), 0.0001D);
						})) //

				.next(new TestCase("Price change") //
						.onBeforeProcessImage(() -> {
							final var testTariffData = buildStaticTariff(30.0D, clock);
							syncableRepository.setEntry(SYNCABLE_KEY,
									new SyncableEntry<>(SYNCABLE_KEY, testTariffData, clock.instant()));
						}) //
						.onAfterProcessImage(() -> {
							final var tariffData = spotTariff.getTariffData().getValue();
							assertNotNull(tariffData);
							assertEquals(30.0D, tariffData.prices().getAt(clock.instant()), 0.0001D);
						})) //

				.deactivate();
	}

	private static TariffData buildStaticTariff(double price, Clock clock) {
		final var testPrices = TimeRangeValues
				.builder(TimeSpan.between(clock.instant(), clock.instant().plus(2, ChronoUnit.HOURS)),
						DurationUnit.ofMinutes(15), Double.class)
				.setByPosition(0, price) //
				.fillMissingDataWithPreviousData() //
				.build();

		return new TariffData(new TariffType.DayAheadEpexSpot(BiddingZone.GERMANY), testPrices,
				Currency.getInstance("EUR"), clock.instant(), clock.instant(), "");
	}
}
