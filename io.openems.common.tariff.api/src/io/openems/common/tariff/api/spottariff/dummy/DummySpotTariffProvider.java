package io.openems.common.tariff.api.spottariff.dummy;

import java.time.Clock;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.openems.common.tariff.api.TariffUpdateStatus;
import io.openems.common.tariff.api.spottariff.SpotTariff;
import io.openems.common.tariff.api.spottariff.SpotTariffProvider;
import io.openems.common.timedata.DurationUnit;
import io.openems.common.types.BiddingZone;
import io.openems.common.types.TariffData;
import io.openems.common.types.TariffType;
import io.openems.common.utils.BehaviorSubject;
import io.openems.common.utils.SingletonPool;
import io.openems.common.utils.TimeRangeValues;
import io.openems.common.utils.TimeSpan;

public class DummySpotTariffProvider implements SpotTariffProvider {
	private final Clock clock;
	private final ConcurrentMap<BiddingZone, StoredSpotTariff> store = new ConcurrentHashMap<>();
	private final DummySpotTariffPool pool = new DummySpotTariffPool();

	private final BehaviorSubject<List<LoadedTariffInfo>> infosAboutLoadedTariffs = new BehaviorSubject<>(List.of());

	public DummySpotTariffProvider() {
		this(Clock.systemDefaultZone());
	}

	public DummySpotTariffProvider(Clock clock) {
		this.clock = clock;
	}

	protected void update() {
		this.infosAboutLoadedTariffs.setValue(this.pool.getAllInstances().stream() //
				.map(DummySpotTariff::getInfo) //
				.toList());
	}

	private StoredSpotTariff getStoredTariff(BiddingZone biddingZone) {
		return this.store.computeIfAbsent(biddingZone, b -> new StoredSpotTariff(new BehaviorSubject<>(null),
				new BehaviorSubject<>(new TariffUpdateStatus.NotFetchedYet(Instant.now(this.clock)))));
	}

	public TariffData setRandomTariffData(BiddingZone biddingZone, TimeSpan timeSpan, Currency currency) {
		var builder = TimeRangeValues.builder(timeSpan, DurationUnit.ofMinutes(15), Double.class);
		for (int pos = 0; pos < builder.getPositionsAmount(); pos++) {
			var price = Math.floor(Math.random() * 10000) / 100.0D;
			builder.setByPosition(pos, price);
		}
		var prices = builder.build();

		var data = new TariffData(new TariffType.DayAheadEpexSpot(biddingZone), prices, currency,
				Instant.now(this.clock), Instant.now(this.clock), "DUMMY");
		this.setTariffData(biddingZone, data);
		return data;
	}

	public void setStaticTariffData(BiddingZone biddingZone, TimeSpan timeSpan, double price, Currency currency) {
		var prices = TimeRangeValues.builder(timeSpan, DurationUnit.ofMinutes(15), Double.class) //
				.setByPosition(0, price) //
				.fillMissingDataWithPreviousData() //
				.build();

		var data = new TariffData(new TariffType.DayAheadEpexSpot(biddingZone), prices, currency,
				Instant.now(this.clock), Instant.now(this.clock), "DUMMY");
		this.setTariffData(biddingZone, data);
	}

	public void setEmptyTariffData(BiddingZone biddingZone) {
		var stored = this.getStoredTariff(biddingZone);
		if (stored.tariffData().getValue() != null) {
			stored.tariffData().setValue(null);
			stored.lastUpdateStatus().setValue(new TariffUpdateStatus.NotFetchedYet(Instant.now(this.clock)));
		}

		this.update();
	}

	public void setTariffData(BiddingZone biddingZone, TariffData tariffData) {
		var stored = this.getStoredTariff(biddingZone);
		stored.tariffData().setValue(tariffData);
		stored.lastUpdateStatus().setValue(new TariffUpdateStatus.Successful(Instant.now(this.clock)));

		this.update();
	}

	public void setUpdateStatus(BiddingZone biddingZone, TariffUpdateStatus status) {
		var stored = this.getStoredTariff(biddingZone);
		stored.lastUpdateStatus().setValue(status);

		this.update();
	}

	@Override
	public boolean isSpotTariffSupported(BiddingZone biddingZone) {
		return this.store.containsKey(biddingZone);
	}

	@Override
	public SpotTariff getSpotTariff(BiddingZone biddingZone) {
		return this.pool.get(biddingZone);
	}

	@Override
	public void ungetSpotTariff(SpotTariff tariffObj) {
		this.pool.unget((DummySpotTariff) tariffObj);
	}

	@Override
	public BehaviorSubject<List<LoadedTariffInfo>> getInfosAboutLoadedTariffs() {
		return this.infosAboutLoadedTariffs;
	}

	@Override
	public int getRanking() {
		return 0;
	}

	private record StoredSpotTariff(BehaviorSubject<TariffData> tariffData,
			BehaviorSubject<TariffUpdateStatus> lastUpdateStatus) {
	}

	private class DummySpotTariffPool extends SingletonPool<BiddingZone, DummySpotTariff> {
		@Override
		protected DummySpotTariff createNewInstance(BiddingZone biddingZone) {
			var stored = DummySpotTariffProvider.this.getStoredTariff(biddingZone);
			return new DummySpotTariff(biddingZone, stored.tariffData(), stored.lastUpdateStatus(),
					DummySpotTariffProvider.this::update);
		}

		@Override
		protected void onPostUnloadInstance(DummySpotTariff instance) {
			// Not required
		}
	}
}
