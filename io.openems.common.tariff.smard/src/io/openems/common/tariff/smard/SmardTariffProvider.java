package io.openems.common.tariff.smard;

import java.time.Clock;
import java.util.List;

import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.tariff.api.TariffUpdateStatus;
import io.openems.common.tariff.api.spottariff.SpotTariff;
import io.openems.common.tariff.api.spottariff.SpotTariffProvider;
import io.openems.common.types.BiddingZone;
import io.openems.common.utils.BehaviorSubject;
import io.openems.common.utils.SingletonPool;

public class SmardTariffProvider implements SpotTariffProvider {

	private final SmardSpotPool spotPool = new SmardSpotPool();

	private final SmardTariffConfig config;
	private final BridgeHttpFactory bridgeHttpFactory;
	private final Clock clock;
	private final String componentId;

	private final BehaviorSubject<List<LoadedTariffInfo>> infosAboutLoadedTariffs = new BehaviorSubject<>(List.of());

	public SmardTariffProvider(SmardTariffConfig config, BridgeHttpFactory bridgeHttpFactory, Clock clock,
			String componentId) {
		this.config = config;
		this.bridgeHttpFactory = bridgeHttpFactory;
		this.clock = clock;
		this.componentId = componentId;
	}

	public void activate() {
		// Currently nothing needs to be initialized
	}

	public void deactivate() {
		this.spotPool.dispose();
	}

	@Override
	public BehaviorSubject<List<LoadedTariffInfo>> getInfosAboutLoadedTariffs() {
		return this.infosAboutLoadedTariffs;
	}

	@Override
	public boolean isSpotTariffSupported(BiddingZone biddingZone) {
		return SmardBiddingZone.getByBiddingZone(biddingZone) != null;
	}

	@Override
	public SpotTariff getSpotTariff(BiddingZone biddingZone) {
		var smardBiddingZone = SmardBiddingZone.getByBiddingZone(biddingZone);
		if (smardBiddingZone == null) {
			throw new IllegalArgumentException("SMARD does not support bidding zone " + biddingZone.name());
		}

		return this.spotPool.get(smardBiddingZone);
	}

	@Override
	public void ungetSpotTariff(SpotTariff tariffObj) {
		this.spotPool.unget((SmardSpotTariff) tariffObj);
	}

	@Override
	public int getRanking() {
		return this.config.ranking();
	}

	private void onTariffUpdateStatusChange(SmardSpotTariff tariff, TariffUpdateStatus updateStatus) {
		this.updateLoadedTariffInfos();
	}

	private void onTariffUnload(SmardSpotTariff tariff) {
		this.updateLoadedTariffInfos();
	}

	protected synchronized void updateLoadedTariffInfos() {
		var infos = this.spotPool.getAllInstances().stream() //
				.map(SmardSpotTariff::getInfo) //
				.toList();
		this.infosAboutLoadedTariffs.setValue(infos);
	}

	private class SmardSpotPool extends SingletonPool<SmardBiddingZone, SmardSpotTariff> {

		@Override
		protected SmardSpotTariff createNewInstance(SmardBiddingZone biddingZone) {
			return new SmardSpotTariff(biddingZone, SmardTariffProvider.this.config,
					SmardTariffProvider.this.bridgeHttpFactory, SmardTariffProvider.this.clock,
					SmardTariffProvider.this.componentId);
		}

		@Override
		protected void onPostLoadInstance(SmardSpotTariff tariff) {
			tariff.getLastUpdateStatus().subscribe(s -> SmardTariffProvider.this.onTariffUpdateStatusChange(tariff, s));
		}

		@Override
		protected void onPostUnloadInstance(SmardSpotTariff instance) {
			SmardTariffProvider.this.onTariffUnload(instance);
		}
	}
}
