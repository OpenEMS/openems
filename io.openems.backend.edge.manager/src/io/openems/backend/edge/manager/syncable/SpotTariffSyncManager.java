package io.openems.backend.edge.manager.syncable;

import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

import io.openems.backend.common.jsonrpc.syncable.SyncableRepository;
import io.openems.backend.tariff.manager.BackendTariffManager;
import io.openems.backend.tariff.manager.SpotTariffTracker;
import io.openems.common.jsonrpc.syncable.SyncableEntry;
import io.openems.common.jsonrpc.syncable.SyncableKey;
import io.openems.common.types.BiddingZone;
import io.openems.common.types.TariffData;

@Component(immediate = true, service = SpotTariffSyncManager.class)
public class SpotTariffSyncManager {

	@Reference
	private BackendTariffManager tariffManager;

	@Reference
	private SyncableRepository syncableRepository;

	private final Consumer<Void> onSpotTariffProvidersChangeFunc = v -> this.onSpotTariffProvidersChange();

	private final Map<BiddingZone, SpotTariffSyncInstance> currentSyncedTariffs = new EnumMap<>(BiddingZone.class);

	@Activate
	protected void activate() {
		this.tariffManager.getOnSpotTariffProvidersChange().subscribe(this.onSpotTariffProvidersChangeFunc);
		this.updateProviders();
	}

	@Deactivate
	protected void deactivate() {
		this.tariffManager.getOnSpotTariffProvidersChange().unsubscribe(this.onSpotTariffProvidersChangeFunc);

		this.currentSyncedTariffs.values().forEach(SpotTariffSyncInstance::deactivate);
		this.currentSyncedTariffs.clear();
	}

	private void onSpotTariffProvidersChange() {
		this.updateProviders();
	}

	private void updateProviders() {
		for (var biddingZone : this.tariffManager.getAllSupportedBiddingZones()) {
			var tariffTracker = this.tariffManager.getSpotTariff(biddingZone);
			var syncInstance = this.currentSyncedTariffs.get(biddingZone);

			if (syncInstance == null) {
				syncInstance = new SpotTariffSyncInstance(biddingZone, tariffTracker);
				syncInstance.activate();
				this.currentSyncedTariffs.put(biddingZone, syncInstance);
			} else if (syncInstance.tariffTracker != tariffTracker) {
				syncInstance.deactivate();
				syncInstance = new SpotTariffSyncInstance(biddingZone, tariffTracker);
				syncInstance.activate();
				this.currentSyncedTariffs.put(biddingZone, syncInstance);
			}
		}
	}

	private class SpotTariffSyncInstance {
		private final BiddingZone biddingZone;
		private final SpotTariffTracker tariffTracker;
		private final Consumer<TariffData> onTariffDataChangeFunc = this::onTariffDataChange;

		public SpotTariffSyncInstance(BiddingZone biddingZone, SpotTariffTracker tariffTracker) {
			this.biddingZone = biddingZone;
			this.tariffTracker = tariffTracker;
		}

		public void activate() {
			this.tariffTracker.getTariffData().subscribe(this.onTariffDataChangeFunc);
		}

		public void deactivate() {
			this.tariffTracker.getTariffData().unsubscribe(this.onTariffDataChangeFunc);
		}

		private void onTariffDataChange(TariffData tariffData) {
			if (tariffData == null) {
				return;
			}

			SpotTariffSyncManager.this.syncableRepository.setStaticEntry(
					new SyncableEntry<>(new SyncableKey.SpotTariff(this.biddingZone), tariffData, Instant.now()));
		}
	}
}
