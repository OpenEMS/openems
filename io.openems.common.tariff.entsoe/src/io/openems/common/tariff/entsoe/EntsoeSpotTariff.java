package io.openems.common.tariff.entsoe;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;

import java.time.Clock;
import java.time.LocalTime;
import java.time.ZonedDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.time.HttpBridgeTimeService;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.tariff.api.TariffProvider;
import io.openems.common.tariff.api.TariffUpdateStatus;
import io.openems.common.tariff.api.http.TariffDelayTimeProvider;
import io.openems.common.tariff.api.spottariff.SpotTariff;
import io.openems.common.types.BiddingZone;
import io.openems.common.types.TariffData;
import io.openems.common.utils.BehaviorSubject;
import io.openems.common.utils.SingletonPool;

public class EntsoeSpotTariff implements SpotTariff, SingletonPool.SingletonPoolImplementation {

	private final Logger log = LoggerFactory.getLogger(EntsoeSpotTariff.class);

	private final BiddingZone biddingZone;
	private final EntsoeTariffConfig config;
	private final HttpBridgeTimeService timeService;
	private final Clock clock;
	private final String identifier;

	private final BehaviorSubject<TariffData> data = new BehaviorSubject<>(null);
	private final BehaviorSubject<TariffUpdateStatus> lastUpdateStatus = new BehaviorSubject<>(null);

	private HttpBridgeTimeService.TimeEndpoint<TariffData> timeEndpoint;

	EntsoeSpotTariff(BiddingZone biddingZone, EntsoeTariffConfig config, HttpBridgeTimeService timeService, Clock clock,
			String componentId) {
		this.biddingZone = biddingZone;
		this.config = config;
		this.timeService = timeService;
		this.clock = clock;
		this.identifier = String.format("%s[%s]", componentId, this.biddingZone.name());

		this.lastUpdateStatus.setValue(new TariffUpdateStatus.NotFetchedYet(this.clock.instant()));
	}

	@Override
	public void activate() {
		final var updateTime = LocalTime.of(EntsoeApi.ENTSOE_UPDATE_HOUR, 0);
		this.timeEndpoint = this.timeService.subscribeTime(new TariffDelayTimeProvider(this.clock, updateTime), //
				this::createEntsoeEndpoint, //
				this::handleEndpointResponse, //
				this::handleEndpointError);
	}

	@Override
	public void deactivate() {
		if (this.timeEndpoint != null) {
			this.timeService.removeTimeEndpoint(this.timeEndpoint);
			this.timeEndpoint = null;
		}
	}

	public TariffProvider.LoadedTariffInfo getInfo() {
		return new TariffProvider.LoadedTariffInfo(this, this.lastUpdateStatus.getValue());
	}

	protected void triggerUpdate(TariffUpdateStatus status) {
		this.lastUpdateStatus.setValue(status);
	}

	/**
	 * Creates the ENTSO-E API endpoint for querying day-ahead prices.
	 *
	 * @return the configured {@link BridgeHttp.Endpoint}
	 */
	private BridgeHttp.Endpoint createEntsoeEndpoint() {
		final var fromDate = ZonedDateTime.now(this.clock).truncatedTo(HOURS);
		final var toDate = fromDate.plusDays(2).truncatedTo(DAYS).minusMinutes(1);
		return EntsoeApi.INSTANCE.createEndPoint(this.biddingZone, this.config.securityToken(), fromDate, toDate);
	}

	/**
	 * Handles successful response from ENTSO-E API.
	 *
	 * @param response the HTTP response
	 * @return Returns parsed data
	 */
	private TariffData handleEndpointResponse(HttpResponse<String> response) throws OpenemsError.OpenemsNamedException {
		var tariffData = EntsoeApi.INSTANCE.handleResponse(response, this.biddingZone, this.clock);

		this.data.setValue(tariffData);
		this.triggerUpdate(new TariffUpdateStatus.Successful(this.clock.instant()));

		this.log.info("Received new ENTSO-E spot prices for bidding zone {} with price range {}",
				this.biddingZone.name(), tariffData.prices().getTimeSpan());
		return tariffData;
	}

	/**
	 * Handles errors from ENTSO-E API.
	 *
	 * @param error the HTTP error
	 */
	private void handleEndpointError(HttpError error) {
		if (error instanceof HttpError.ResponseError responseError) {
			this.log.info("Failed to fetch ENTSO-E spot prices for bidding zone {}: HTTP {} - {}",
					this.biddingZone.name(), responseError.status.code(), responseError.body);
			this.triggerUpdate(new TariffUpdateStatus.FailedWithHttpError(this.clock.instant(), responseError.status,
					responseError.body));
		} else {
			this.log.info("Failed to fetch ENTSO-E spot prices for bidding zone {}", this.biddingZone.name(),
					error.getCause());
			this.triggerUpdate(new TariffUpdateStatus.FailedWithException(this.clock.instant(), error));
		}
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
		return this.biddingZone;
	}
}
