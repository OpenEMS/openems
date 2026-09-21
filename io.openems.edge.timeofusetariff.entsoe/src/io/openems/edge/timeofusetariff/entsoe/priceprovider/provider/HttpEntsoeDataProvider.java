package io.openems.edge.timeofusetariff.entsoe.priceprovider.provider;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;

import java.time.Clock;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.function.Consumer;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.thirdparty.entsoe.EntsoeApi;
import io.openems.common.bridge.http.time.HttpBridgeTimeService;
import io.openems.common.bridge.http.time.HttpBridgeTimeServiceDefinition;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.EntsoeBiddingZone;
import io.openems.common.utils.StringUtils;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;
import io.openems.edge.timeofusetariff.api.utils.TimeOfUseDelayTimeProvider;
import io.openems.edge.timeofusetariff.entsoe.priceprovider.MarketPriceUpdateEvent;

public class HttpEntsoeDataProvider extends EntsoeDataProvider {
	private final BridgeHttpFactory httpBridgeFactory;
	private final String securityToken;

	private BridgeHttp httpBridge;
	private HttpBridgeTimeService timeService;

	public HttpEntsoeDataProvider(BridgeHttpFactory httpBridgeFactory, String securityToken, Clock clock,
			EntsoeBiddingZone biddingZone, Consumer<MarketPriceUpdateEvent> onUpdate) {
		super(clock, biddingZone, onUpdate);
		if (StringUtils.isNullOrEmpty(securityToken)) {
			throw new RuntimeException("Please configure Security Token to access ENTSO-E");
		}

		this.httpBridgeFactory = httpBridgeFactory;
		this.securityToken = securityToken;

		this.httpBridge = this.httpBridgeFactory.get();
		this.timeService = this.httpBridge.createService(HttpBridgeTimeServiceDefinition.INSTANCE);
	}

	@Override
	public void activate() {
		this.timeService.subscribeTime(
				new TimeOfUseDelayTimeProvider(this.clock, LocalTime.of(EntsoeApi.ENTSOE_UPDATE_HOUR, 0)), //
				this::createEntsoeEndpoint, //
				this::handleEndpointResponse, //
				this::handleEndpointError);
	}

	@Override
	public void deactivate() {
		this.timeService.removeAllTimeEndpoints();
	}

	@Override
	public void dispose() {
		this.httpBridgeFactory.unget(this.httpBridge);
		this.httpBridge = null;
		this.timeService = null;
	}

	/**
	 * Creates the ENTSO-E API endpoint for querying day-ahead prices.
	 *
	 * @return the configured {@link BridgeHttp.Endpoint}
	 */
	private BridgeHttp.Endpoint createEntsoeEndpoint() {
		final var fromDate = ZonedDateTime.now().truncatedTo(HOURS);
		final var toDate = fromDate.plusDays(2).truncatedTo(DAYS).minusMinutes(1);
		return EntsoeApi.INSTANCE.createEndPoint(this.biddingZone, this.securityToken, fromDate, toDate);
	}

	/**
	 * Handles successful response from ENTSO-E API.
	 *
	 * @param response the HTTP response
	 * @return the prices as {@link TimeOfUsePrices}
	 */
	private TimeOfUsePrices handleEndpointResponse(HttpResponse<String> response)
			throws OpenemsError.OpenemsNamedException {
		var marketPriceData = EntsoeApi.INSTANCE.handleResponse(response, this.biddingZone, this.clock);

		this.triggerUpdate(new MarketPriceUpdateEvent.Successful(marketPriceData));

		return TimeOfUsePrices.from(marketPriceData.getValues().toMap());
	}

	/**
	 * Handles errors from ENTSO-E API.
	 *
	 * @param error the HTTP error
	 */
	private void handleEndpointError(HttpError error) {
		if (error instanceof HttpError.ResponseError responseError) {
			this.triggerUpdate(
					new MarketPriceUpdateEvent.FailedWithHttpError(responseError.status, responseError.body));
		} else {
			this.triggerUpdate(new MarketPriceUpdateEvent.FailedWithException(error));
		}
	}

}
