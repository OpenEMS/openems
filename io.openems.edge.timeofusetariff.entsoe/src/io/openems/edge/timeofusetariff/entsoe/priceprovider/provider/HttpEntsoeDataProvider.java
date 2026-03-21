package io.openems.edge.timeofusetariff.entsoe.priceprovider.provider;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.SECONDS;

import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.function.Consumer;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.thirdparty.entsoe.EntsoeApi;
import io.openems.common.bridge.http.time.DelayTimeProvider;
import io.openems.common.bridge.http.time.DelayTimeProviderChain;
import io.openems.common.bridge.http.time.HttpBridgeTimeService;
import io.openems.common.bridge.http.time.HttpBridgeTimeServiceDefinition;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.EntsoeBiddingZone;
import io.openems.common.utils.StringUtils;
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
		this.timeService.subscribeTime(new EntsoeDelayTimeProvider(), //
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
	 */
	private void handleEndpointResponse(HttpResponse<String> response) throws OpenemsError.OpenemsNamedException {
		var marketPriceData = EntsoeApi.INSTANCE.handleResponse(response, this.biddingZone, this.clock);

		this.triggerUpdate(new MarketPriceUpdateEvent.Successful(marketPriceData));
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

	/**
	 * Delay time provider for ENTSO-E API requests.
	 */
	public class EntsoeDelayTimeProvider implements DelayTimeProvider {

		@Override
		public Delay onFirstRunDelay() {
			return Delay.immediate();
		}

		@Override
		public Delay onErrorRunDelay(HttpError error) {
			// On error, retry after 1 minute with some randomness
			return DelayTimeProviderChain.fixedDelay(Duration.ofMinutes(1)) //
					.plusRandomDelay(30, SECONDS) //
					.getDelay();
		}

		@Override
		public Delay onSuccessRunDelay(HttpResponse<String> response) {
			try {
				return EntsoeApi.INSTANCE.calculateNextFetchDelay(response, EntsoeApi.ENTSOE_UPDATE_HOUR + 1,
						HttpEntsoeDataProvider.this.clock);
			} catch (Exception e) {
				// Wait 30 minutes before retry
				return Delay.of(Duration.ofMinutes(30));
			}
		}
	}

}
