package io.openems.edge.timeofusetariff.swisspower;

import static io.openems.common.utils.JsonUtils.getAsDouble;
import static io.openems.common.utils.JsonUtils.getAsJsonArray;
import static io.openems.common.utils.JsonUtils.getAsString;
import static io.openems.common.utils.JsonUtils.parseToJsonObject;
import static io.openems.edge.timeofusetariff.api.utils.ExchangeRateApi.getExchangeRateOrElse;
import static io.openems.edge.timeofusetariff.api.utils.TimeOfUseTariffUtils.generateDebugLog;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.timedata.DurationUnit;
import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.edge.bridge.http.api.BridgeHttpFactory;
import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpMethod;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.bridge.http.api.UrlBuilder;
import io.openems.edge.bridge.http.time.DelayTimeProvider;
import io.openems.edge.bridge.http.time.DelayTimeProviderChain;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.currency.Currency;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "TimeOfUseTariff.Swisspower", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class TimeOfUseTariffSwisspowerImpl extends AbstractOpenemsComponent
		implements TimeOfUseTariff, OpenemsComponent, TimeOfUseTariffSwisspower {
	private static final UrlBuilder URL_BASE = UrlBuilder.parse("https://esit.code-fabrik.ch/api/v1/metering_code");
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
	private static final int INTERNAL_ERROR = -1; // parsing, handle exception...
	private static final int DEFAULT_READ_TIMEOUT = 200;
	protected static final int SERVER_ERROR_CODE = 500;
	protected static final int BAD_REQUEST_ERROR_CODE = 400;

	private final Logger log = LoggerFactory.getLogger(TimeOfUseTariffSwisspowerImpl.class);
	private final AtomicReference<TimeOfUsePrices> prices = new AtomicReference<>(TimeOfUsePrices.EMPTY_PRICES);
	private String accessToken = null;
	private String meteringCode = null;

	@Reference
	private BridgeHttpFactory httpBridgeFactory;
	private BridgeHttp httpBridge;

	@Reference
	private Meta meta;

	@Reference
	private ComponentManager componentManager;

	public TimeOfUseTariffSwisspowerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				TimeOfUseTariffSwisspower.ChannelId.values() //
		);
	}

	private final BiConsumer<Value<Integer>, Value<Integer>> onCurrencyChange = (a, b) -> {
		this.httpBridge.removeAllTimeEndpoints();
		this.httpBridge.subscribeTime(new SwisspowerProvider(this.componentManager.getClock()), //
				this::createSwisspowerEndpoint, //
				this::handleEndpointResponse, //
				this::handleEndpointError);
	};

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());

		if (!config.enabled()) {
			return;
		}

		this.accessToken = config.accessToken();
		if (this.accessToken == null) {
			this.logError(this.log, "Please configure personal Access token to access Swisspower API");
			return;
		}

		this.meteringCode = config.meteringCode();
		if (this.meteringCode == null) {
			this.logError(this.log, "Please configure meteringCode to access Swisspower API");
			return;
		}

		// React on updates to Currency.
		this.meta.getCurrencyChannel().onChange(this.onCurrencyChange);

		this.httpBridge = this.httpBridgeFactory.get();
		this.httpBridge.subscribeTime(new SwisspowerProvider(this.componentManager.getClock()), //
				this::createSwisspowerEndpoint, //
				this::handleEndpointResponse, //
				this::handleEndpointError);
	}

	private Endpoint createSwisspowerEndpoint() {
		final var now = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS);
		final var startTimestamp = now.format(DATE_FORMATTER); // eg. 2024-05-22T00:00:00+02:00
		final var endTimestamp = now.plusDays(1).format(DATE_FORMATTER);

		final var url = URL_BASE.withQueryParam("start_timestamp", startTimestamp) //
				.withQueryParam("end_timestamp", endTimestamp) //
				.withQueryParam("metering_code", this.meteringCode);

		return new Endpoint(url.toEncodedString(), //
				HttpMethod.GET, //
				BridgeHttp.DEFAULT_CONNECT_TIMEOUT, //
				DEFAULT_READ_TIMEOUT, //
				null, //
				this.buildRequestHeaders());
	}

	private Map<String, String> buildRequestHeaders() {
		return Map.of(//
				"Authorization", "Bearer " + this.accessToken //
		);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.meta.getCurrencyChannel().removeOnChangeCallback(this.onCurrencyChange);
		this.httpBridgeFactory.unget(this.httpBridge);
	}

	public static class SwisspowerProvider implements DelayTimeProvider {

		private final Clock clock;

		public SwisspowerProvider(Clock clock) {
			super();
			this.clock = clock;
		}

		@Override
		public Delay onFirstRunDelay() {
			return Delay.of(Duration.ofMinutes(1));
		}

		@Override
		public Delay onErrorRunDelay(HttpError error) {
			return DelayTimeProviderChain.fixedDelay(Duration.ofHours(1))//
					.plusRandomDelay(60, ChronoUnit.SECONDS) //
					.getDelay();
		}

		@Override
		public Delay onSuccessRunDelay(HttpResponse<String> result) {
			return DelayTimeProviderChain.fixedAtEveryFull(this.clock, DurationUnit.ofDays(1))
					.plusRandomDelay(60, ChronoUnit.SECONDS) //
					.getDelay();
		}
	}

	private void handleEndpointResponse(HttpResponse<String> response) throws OpenemsNamedException, IOException {
		this.setChannelValues(response.status().code(), false, false, false);

		final var swissPowerCurrency = Currency.CHF.name(); // Swiss Franc
		final var globalCurrency = this.meta.getCurrency();
		final double exchangeRate = getExchangeRateOrElse(swissPowerCurrency, globalCurrency, 1.);

		// Parse the response for the prices
		this.prices.set(parsePrices(response.data(), exchangeRate));
	}

	private void handleEndpointError(HttpError error) {
		var httpStatusCode = (error instanceof HttpError.ResponseError re) ? re.status.code() : INTERNAL_ERROR;
		var serverError = (httpStatusCode == SERVER_ERROR_CODE);
		var badRequest = (httpStatusCode == BAD_REQUEST_ERROR_CODE);
		var timeoutError = (error instanceof HttpError.UnknownError e
				&& e.getCause() instanceof SocketTimeoutException);

		this.setChannelValues(httpStatusCode, serverError, badRequest, timeoutError);
		this.log.error("HTTP Error [{}]: {}", httpStatusCode, error.getMessage());
	}

	@Override
	public TimeOfUsePrices getPrices() {
		return TimeOfUsePrices.from(ZonedDateTime.now(this.componentManager.getClock()), this.prices.get());
	}

	/**
	 * Parses JSON data to extract time-of-use prices and returns a
	 * {@link TimeOfUsePrices} object.
	 *
	 * @param jsonData     the JSON data as a {@code String} containing the
	 *                     electricity price information.
	 * @param exchangeRate The exchange rate of user currency to EUR.
	 * @return a {@link TimeOfUsePrices} object containing the parsed prices mapped
	 *         to their respective timestamps.
	 * @throws OpenemsNamedException if an error occurs during the parsing of the
	 *                               JSON data.
	 */
	protected static TimeOfUsePrices parsePrices(String jsonData, double exchangeRate) throws OpenemsNamedException {
		var result = new TreeMap<ZonedDateTime, Double>();
		var data = parseToJsonObject(jsonData);
		var prices = getAsJsonArray(data, "prices");

		for (var element : prices) {

			var startTimeString = getAsString(element, "start_timestamp");
			var integrated = getAsJsonArray(element, "integrated");

			// CHF/kWh -> Currency/MWh
			// Example: 0.1 CHF/kWh * 1000 = 100 CHF/MWh.
			var marketPrice = getAsDouble(integrated.get(0), "value") * 1000 * exchangeRate;

			// Convert LocalDateTime to ZonedDateTime
			var startTimeStamp = ZonedDateTime.parse(startTimeString, DateTimeFormatter.ISO_OFFSET_DATE_TIME);

			// Adding the values in the Map.
			result.put(startTimeStamp, marketPrice);
		}
		return TimeOfUsePrices.from(result);
	}

	/**
	 * This method updates the channel values in response to an HTTP request.
	 * 
	 * @param httpStatusCode the HTTP status code returned from the endpoint
	 *                       response
	 * @param serverError    a boolean indicating if a server error occurred (status
	 *                       code 500)
	 * @param badRequest     a boolean indicating if the request was invalid (status
	 *                       code 400)
	 * @param timeoutError   a boolean indicating if the request could not be read
	 *                       in time
	 */
	private void setChannelValues(int httpStatusCode, boolean serverError, boolean badRequest, boolean timeoutError) {
		this.channel(TimeOfUseTariffSwisspower.ChannelId.HTTP_STATUS_CODE).setNextValue(httpStatusCode);
		this.channel(TimeOfUseTariffSwisspower.ChannelId.STATUS_INVALID_FIELDS).setNextValue(serverError);
		this.channel(TimeOfUseTariffSwisspower.ChannelId.STATUS_BAD_REQUEST).setNextValue(badRequest);
		this.channel(TimeOfUseTariffSwisspower.ChannelId.STATUS_READ_TIMEOUT).setNextValue(timeoutError);
	}

	@Override
	public String debugLog() {
		return generateDebugLog(this, this.meta.getCurrency());
	}
}
