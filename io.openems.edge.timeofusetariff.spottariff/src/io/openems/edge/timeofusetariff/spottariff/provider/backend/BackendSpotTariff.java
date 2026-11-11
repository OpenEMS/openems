package io.openems.edge.timeofusetariff.spottariff.provider.backend;

import java.time.Clock;
import java.util.function.Consumer;

import io.openems.common.jsonrpc.syncable.SyncableEntry;
import io.openems.common.jsonrpc.syncable.SyncableKey;
import io.openems.common.tariff.api.TariffProvider;
import io.openems.common.tariff.api.TariffUpdateStatus;
import io.openems.common.tariff.api.spottariff.SpotTariff;
import io.openems.common.types.BiddingZone;
import io.openems.common.types.TariffData;
import io.openems.common.utils.BehaviorSubject;
import io.openems.common.utils.SingletonPool;
import io.openems.edge.common.services.syncable.SyncableRepository;
import io.openems.edge.common.services.syncable.SyncableSubscription;

public class BackendSpotTariff implements SpotTariff, SingletonPool.SingletonPoolImplementation {
	private final BiddingZone biddingZone;
	private final SyncableRepository syncableRepository;
	private final Clock clock;
	private final String identifier;

	private final BehaviorSubject<TariffData> tariffData = new BehaviorSubject<>(null);
	private final BehaviorSubject<TariffUpdateStatus> lastUpdateStatus = new BehaviorSubject<>(null);

	private final Consumer<SyncableEntry<TariffData>> onNewTariffDataFunc = this::onNewTariffData;

	private SyncableSubscription<TariffData> tariffSubscription;

	BackendSpotTariff(BiddingZone biddingZone, SyncableRepository syncableRepository, Clock clock, String componentId) {
		this.biddingZone = biddingZone;
		this.syncableRepository = syncableRepository;
		this.clock = clock;
		this.identifier = String.format("%s[%s]", componentId, biddingZone.name());

		this.lastUpdateStatus.setValue(new TariffUpdateStatus.NotFetchedYet(this.clock.instant()));
	}

	@Override
	public void activate() {
		this.tariffSubscription = this.syncableRepository.subscribe(new SyncableKey.SpotTariff(this.biddingZone));
		this.tariffSubscription.getEntry().subscribe(this.onNewTariffDataFunc);
	}

	@Override
	public void deactivate() {
		if (this.tariffSubscription != null) {
			this.tariffSubscription.getEntry().unsubscribe(this.onNewTariffDataFunc);
			this.tariffSubscription.dispose();
			this.tariffSubscription = null;
		}
	}

	@Override
	public String getIdentifier() {
		return this.identifier;
	}

	@Override
	public BehaviorSubject<TariffData> getTariffData() {
		return this.tariffData;
	}

	@Override
	public BehaviorSubject<TariffUpdateStatus> getLastUpdateStatus() {
		return this.lastUpdateStatus;
	}

	@Override
	public BiddingZone getBiddingZone() {
		return this.biddingZone;
	}

	public TariffProvider.LoadedTariffInfo getInfo() {
		return new TariffProvider.LoadedTariffInfo(this, this.lastUpdateStatus.getValue());
	}

	protected void onNewTariffData(SyncableEntry<TariffData> tariffData) {
		if (tariffData != null) {
			this.tariffData.setValue(tariffData.value());
			this.lastUpdateStatus.setValue(new TariffUpdateStatus.Successful(this.clock.instant()));
		}
	}
}
