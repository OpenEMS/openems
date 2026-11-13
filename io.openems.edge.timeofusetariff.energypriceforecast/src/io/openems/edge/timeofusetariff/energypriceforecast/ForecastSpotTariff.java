package io.openems.edge.timeofusetariff.energypriceforecast;

import java.time.Clock;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.time.DefaultDelayTimeProvider;
import io.openems.common.bridge.http.time.DelayTimeProvider.Delay;
import io.openems.common.bridge.http.time.HttpBridgeTimeService;
import io.openems.common.bridge.http.time.HttpBridgeTimeServiceDefinition;
import io.openems.common.tariff.api.TariffUpdateStatus;
import io.openems.common.tariff.api.spottariff.SpotTariff;
import io.openems.common.types.BiddingZone;
import io.openems.common.types.TariffData;
import io.openems.common.utils.BehaviorSubject;
import io.openems.common.utils.SingletonPool.SingletonPoolImplementation;

final class ForecastSpotTariff implements SpotTariff, SingletonPoolImplementation {
	private static final String ENDPOINT = "https://api.energypriceforecast.eu/api/v1/evcc/tariff-quarterhour";
	private final Logger log = LoggerFactory.getLogger(ForecastSpotTariff.class);
	private final BiddingZone biddingZone;
	private final String identifier;
	private final BridgeHttpFactory bridgeHttpFactory;
	private final Clock clock;
	private final Duration refreshInterval;
	private final Duration maxAge;
	private final BehaviorSubject<TariffData> data = new BehaviorSubject<>(null);
	private final BehaviorSubject<TariffUpdateStatus> status;
	private BridgeHttp bridge;
	private HttpBridgeTimeService timeService;
	private HttpBridgeTimeService.TimeEndpoint<HttpResponse<String>> subscription;
	private volatile boolean active;

	ForecastSpotTariff(BiddingZone biddingZone, String componentId, BridgeHttpFactory bridgeHttpFactory, Clock clock,
			Duration refreshInterval, Duration maxAge) {
		this.biddingZone = biddingZone;
		this.identifier = componentId + "[" + biddingZone.name() + "]";
		this.bridgeHttpFactory = bridgeHttpFactory;
		this.clock = clock;
		this.refreshInterval = refreshInterval;
		this.maxAge = maxAge;
		this.status = new BehaviorSubject<>(new TariffUpdateStatus.NotFetchedYet(clock.instant()));
	}

	@Override
	public void activate() {
		this.active = true;
		this.bridge = this.bridgeHttpFactory.get();
		this.timeService = this.bridge.createService(HttpBridgeTimeServiceDefinition.INSTANCE);
		var endpoint = BridgeHttp
				.create(ENDPOINT + "?country=" + ForecastMarket.forBiddingZone(this.biddingZone)
						+ "&type=grid&hours=48&currency=EUR&mode=forecast_only&debug=1")
				.setHeader("User-Agent", "OpenEMS/EnergyPriceForecast").build();
		this.subscription = this.timeService
				.subscribeTime(
						new DefaultDelayTimeProvider<>(Delay::immediate, error -> Delay.of(Duration.ofMinutes(5)),
								response -> Delay.of(this.refreshInterval)),
						endpoint, this::handleResponse, this::handleError);
	}

	@Override
	public void deactivate() {
		// Pool release must not wait for an HTTP callback that may re-enter the pool.
		this.active = false;
		if (this.timeService != null && this.subscription != null) {
			this.timeService.removeTimeEndpoint(this.subscription);
		}
		this.bridgeHttpFactory.unget(this.bridge);
		this.bridge = null;
	}

	private synchronized void handleResponse(HttpResponse<String> response) throws Exception {
		if (!this.active) {
			return;
		}
		var forecast = Utils.parseForecast(response.data(), this.biddingZone, this.clock, this.maxAge);
		this.data.setValue(forecast);
		this.status.setValue(new TariffUpdateStatus.Successful(this.clock.instant()));
	}

	private synchronized void handleError(HttpError error) {
		if (!this.active) {
			return;
		}
		this.log.warn("Unable to refresh forecast for {}: {}", this.biddingZone, error.getMessage());
		this.status.setValue(switch (error) {
		case HttpError.ResponseError e ->
			new TariffUpdateStatus.FailedWithHttpError(this.clock.instant(), e.status, e.body);
		default -> new TariffUpdateStatus.FailedWithException(this.clock.instant(), error);
		});
		this.expireOutdatedPrices();
	}

	synchronized void expireOutdatedPrices() {
		var forecast = this.data.getValue();
		if (forecast != null && (forecast.lastUpdate().plus(this.maxAge).isBefore(this.clock.instant())
				|| !forecast.prices().hasDataInFuture(this.clock))) {
			this.data.setValue(null);
			this.status.setValue(new TariffUpdateStatus.FailedWithException(this.clock.instant(),
					new IllegalStateException("Forecast has expired")));
		}
	}

	@Override
	public String getIdentifier() {
		return this.identifier;
	}

	@Override
	public BiddingZone getBiddingZone() {
		return this.biddingZone;
	}

	@Override
	public BehaviorSubject<TariffData> getTariffData() {
		return this.data;
	}

	@Override
	public BehaviorSubject<TariffUpdateStatus> getLastUpdateStatus() {
		return this.status;
	}
}
