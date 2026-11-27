package io.openems.common.tariff.smard;

import java.time.Clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.tariff.api.TariffProvider;
import io.openems.common.tariff.api.TariffUpdateStatus;
import io.openems.common.tariff.api.spottariff.SpotTariff;
import io.openems.common.types.BiddingZone;
import io.openems.common.types.TariffData;
import io.openems.common.utils.BehaviorSubject;
import io.openems.common.utils.SingletonPool;

public class SmardSpotTariff implements SpotTariff, SingletonPool.SingletonPoolImplementation {

	private final Logger log = LoggerFactory.getLogger(SmardSpotTariff.class);
	private final SmardBiddingZone biddingZone;
	private final BridgeHttpFactory httpBridgeFactory;
	private final Clock clock;
	private final String identifier;

	private final BehaviorSubject<TariffData> data = new BehaviorSubject<>(null);
	private final BehaviorSubject<TariffUpdateStatus> lastUpdateStatus = new BehaviorSubject<>(null);

	protected BridgeHttp httpBridge;
	protected SmardApi smardApi;

	public SmardSpotTariff(SmardBiddingZone biddingZone, SmardTariffConfig config, BridgeHttpFactory httpBridgeFactory,
			Clock clock, String componentId) {
		this.biddingZone = biddingZone;
		this.httpBridgeFactory = httpBridgeFactory;
		this.clock = clock;
		this.identifier = String.format("%s[%s]", componentId, biddingZone.getBiddingZone().name());

		this.lastUpdateStatus.setValue(new TariffUpdateStatus.NotFetchedYet(this.clock.instant()));
	}

	@Override
	public void activate() {
		this.httpBridge = this.httpBridgeFactory.get();
		this.smardApi = this.httpBridge.createService(new SmardApiDefinition(this.biddingZone, this.clock));
		this.smardApi.subscribePrices(this::handleNewPrices, this::handleError);
	}

	@Override
	public void deactivate() {
		this.httpBridgeFactory.unget(this.httpBridge);
		this.httpBridge = null;
		this.smardApi = null;
	}

	public TariffProvider.LoadedTariffInfo getInfo() {
		return new TariffProvider.LoadedTariffInfo(this, this.lastUpdateStatus.getValue());
	}

	@Override
	public String getIdentifier() {
		return this.identifier;
	}

	@Override
	public BehaviorSubject<TariffData> getTariffData() {
		return this.data;
	}

	@Override
	public BehaviorSubject<TariffUpdateStatus> getLastUpdateStatus() {
		return this.lastUpdateStatus;
	}

	@Override
	public BiddingZone getBiddingZone() {
		return this.biddingZone.getBiddingZone();
	}

	private void handleNewPrices(TariffData tariffData) {
		this.log.info("Received new SMARD spot prices for bidding zone {} with price range {}", this.biddingZone.name(),
				tariffData.prices().getTimeSpan());
		this.data.setValue(tariffData);
		this.triggerUpdate(new TariffUpdateStatus.Successful(this.clock.instant()));
	}

	private void handleError(Throwable ex) {
		if (ex instanceof HttpError.ResponseError httpError) {
			this.log.warn("Failed to fetch SMARD prices for {}: HTTP Status {} - {}", this.biddingZone.name(),
					httpError.status, httpError.body);
			this.triggerUpdate(
					new TariffUpdateStatus.FailedWithHttpError(this.clock.instant(), httpError.status, httpError.body));
		} else {
			this.log.warn("Failed to fetch SMARD prices for {}", this.biddingZone.name(), ex);
			this.triggerUpdate(new TariffUpdateStatus.FailedWithException(this.clock.instant(), ex));
		}
	}

	protected void triggerUpdate(TariffUpdateStatus status) {
		this.lastUpdateStatus.setValue(status);
	}
}
