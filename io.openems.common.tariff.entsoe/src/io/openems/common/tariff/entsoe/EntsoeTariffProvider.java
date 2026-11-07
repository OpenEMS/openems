package io.openems.common.tariff.entsoe;

import java.time.Clock;
import java.util.List;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.bridge.http.time.HttpBridgeTimeService;
import io.openems.common.bridge.http.time.HttpBridgeTimeServiceDefinition;
import io.openems.common.tariff.api.TariffUpdateStatus;
import io.openems.common.tariff.api.spottariff.SpotTariff;
import io.openems.common.tariff.api.spottariff.SpotTariffProvider;
import io.openems.common.types.BiddingZone;
import io.openems.common.utils.BehaviorSubject;
import io.openems.common.utils.SingletonPool;

public class EntsoeTariffProvider implements SpotTariffProvider {

	private final EntsoeSpotPool spotPool = new EntsoeSpotPool();

	private final EntsoeTariffConfig config;
	private final BridgeHttpFactory bridgeHttpFactory;
	private final Clock clock;
	private final String componentId;

	private final BehaviorSubject<List<LoadedTariffInfo>> infosAboutLoadedTariffs = new BehaviorSubject<>(List.of());

	private BridgeHttp httpBridge;
	private HttpBridgeTimeService timeService;

	public EntsoeTariffProvider(EntsoeTariffConfig config, BridgeHttpFactory bridgeHttpFactory, Clock clock,
			String componentId) {
		this.config = config;
		this.bridgeHttpFactory = bridgeHttpFactory;
		this.clock = clock;
		this.componentId = componentId;
	}

	public void activate() {
		this.httpBridge = this.bridgeHttpFactory.get();
		this.timeService = this.httpBridge.createService(HttpBridgeTimeServiceDefinition.INSTANCE);
	}

	public void deactivate() {
		this.spotPool.dispose();

		this.timeService.removeAllTimeEndpoints();
		this.bridgeHttpFactory.unget(this.httpBridge);
		this.httpBridge = null;
		this.timeService = null;
	}

	@Override
	public BehaviorSubject<List<LoadedTariffInfo>> getInfosAboutLoadedTariffs() {
		return this.infosAboutLoadedTariffs;
	}

	@Override
	public boolean isSpotTariffSupported(BiddingZone biddingZone) {
		return biddingZone.getEicCode() != null && !biddingZone.getEicCode().isEmpty();
	}

	@Override
	public SpotTariff getSpotTariff(BiddingZone biddingZone) {
		return this.spotPool.get(biddingZone);
	}

	@Override
	public void ungetSpotTariff(SpotTariff tariffObj) {
		this.spotPool.unget((EntsoeSpotTariff) tariffObj);
	}

	@Override
	public int getRanking() {
		return this.config.ranking();
	}

	private void onTariffUpdateStatusChange(EntsoeSpotTariff tariff, TariffUpdateStatus updateStatus) {
		this.updateLoadedTariffInfos();
	}

	private void onTariffUnload(EntsoeSpotTariff tariff) {
		this.updateLoadedTariffInfos();
	}

	protected synchronized void updateLoadedTariffInfos() {
		var infos = this.spotPool.getAllInstances().stream() //
				.map(EntsoeSpotTariff::getInfo) //
				.toList();
		this.infosAboutLoadedTariffs.setValue(infos);
	}

	private class EntsoeSpotPool extends SingletonPool<BiddingZone, EntsoeSpotTariff> {

		@Override
		protected EntsoeSpotTariff createNewInstance(BiddingZone biddingZone) {
			return new EntsoeSpotTariff(biddingZone, EntsoeTariffProvider.this.config,
					EntsoeTariffProvider.this.timeService, EntsoeTariffProvider.this.clock,
					EntsoeTariffProvider.this.componentId);
		}

		@Override
		protected void onPostLoadInstance(EntsoeSpotTariff tariff) {
			tariff.getLastUpdateStatus()
					.subscribe(s -> EntsoeTariffProvider.this.onTariffUpdateStatusChange(tariff, s));
		}

		@Override
		protected void onPostUnloadInstance(EntsoeSpotTariff instance) {
			EntsoeTariffProvider.this.onTariffUnload(instance);
		}
	}
}
