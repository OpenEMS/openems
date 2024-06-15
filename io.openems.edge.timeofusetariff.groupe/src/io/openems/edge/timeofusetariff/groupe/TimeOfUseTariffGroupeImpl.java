package io.openems.edge.timeofusetariff.groupe;

import static io.openems.common.utils.JsonUtils.getAsDouble;
import static io.openems.common.utils.JsonUtils.getAsString;
import static io.openems.common.utils.JsonUtils.parseToJsonArray;
import static io.openems.common.utils.StringUtils.definedOrElse;
import static io.openems.edge.timeofusetariff.api.utils.TimeOfUseTariffUtils.generateDebugLog;
import static java.util.Collections.emptyMap;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.timedata.DurationUnit;
import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.edge.bridge.http.api.BridgeHttpFactory;
import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpMethod;
import io.openems.edge.bridge.http.api.HttpResponse;
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
import io.openems.edge.timeofusetariff.api.utils.ExchangeRateApi;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "TimeOfUseTariff.GroupeE", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class TimeOfUseTariffGroupeImpl extends AbstractOpenemsComponent
		implements TimeOfUseTariff, OpenemsComponent, TimeOfUseTariffGroupe {
	private static final String URL = "https://api.tariffs.groupe-e.ch/v1/tariffs?start_timestamp=%s&end_timestamp=%s";
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
	private static final int INTERNAL_ERROR = -1; // parsing, handle exception...

	private final Logger log = LoggerFactory.getLogger(TimeOfUseTariffGroupeImpl.class);
	private final AtomicReference<TimeOfUsePrices> prices = new AtomicReference<>(TimeOfUsePrices.EMPTY_PRICES);
	private String exchangerateAccesskey = null;

	@Reference
	private BridgeHttpFactory httpBridgeFactory;
	private BridgeHttp httpBridge;

	@Reference
	private OpenemsEdgeOem oem;

	@Reference
	private Meta meta;

	@Reference
	private ComponentManager componentManager;

	public TimeOfUseTariffGroupeImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				TimeOfUseTariffGroupe.ChannelId.values() //
		);
	}

	private final BiConsumer<Value<Integer>, Value<Integer>> onCurrencyChange = (a, b) -> {
		this.httpBridge = this.httpBridgeFactory.get();
		this.httpBridge.subscribeTime(new GroupeDelayTimeProvider(this.componentManager.getClock()), //
				this::createGroupeEndpoint, //
				this::handleEndpointResponse, //
				this::handleEndpointError);
	};

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());

		if (!config.enabled()) {
			return;
		}

		this.exchangerateAccesskey = definedOrElse(config.exchangerateAccesskey(), this.oem.getExchangeRateAccesskey());
		if (this.exchangerateAccesskey == null) {
			this.logError(this.log, "Please configure personal Access key to access Exchange rate host API");
			return;
		}

		// React on updates to Currency.
		this.meta.getCurrencyChannel().onChange(this.onCurrencyChange);

		this.httpBridge = this.httpBridgeFactory.get();
		this.httpBridge.subscribeTime(new GroupeDelayTimeProvider(this.componentManager.getClock()), //
				this::createGroupeEndpoint, //
				this::handleEndpointResponse, //
				this::handleEndpointError);
	}

	private Endpoint createGroupeEndpoint() {
		final var now = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS);
		final var startTimestamp = now.format(DATE_FORMATTER); // eg. 2024-05-22T00:00:00+02:00
		final var endTimestamp = now.plusDays(1).format(DATE_FORMATTER);

		return new Endpoint(String.format(URL, startTimestamp, endTimestamp), //
				HttpMethod.GET, //
				BridgeHttp.DEFAULT_CONNECT_TIMEOUT, //
				BridgeHttp.DEFAULT_READ_TIMEOUT, //
				null, //
				emptyMap());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.meta.getCurrencyChannel().removeOnChangeCallback(this.onCurrencyChange);
		this.httpBridgeFactory.unget(this.httpBridge);
	}

	public static class GroupeDelayTimeProvider implements DelayTimeProvider {

		private final Clock clock;

		public GroupeDelayTimeProvider(Clock clock) {
			super();
			this.clock = clock;
		}

		@Override
		public Delay onFirstRunDelay() {
			return Delay.immediate();
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
		this.channel(TimeOfUseTariffGroupe.ChannelId.HTTP_STATUS_CODE).setNextValue(response.status().code());

		final var groupeCurrency = Currency.CHF.name(); // Swiss Franc
		final var globalCurrency = this.meta.getCurrency();
		final var exchangerateAccesskey = this.exchangerateAccesskey;
		if (globalCurrency == Currency.UNDEFINED) {
			throw new OpenemsException("Global Currency is UNDEFINED. Please configure it in Core.Meta component");
		}

		final var exchangeRate = globalCurrency.name().equals(groupeCurrency) //
				? 1. // No need to fetch exchange rate from API.
				: ExchangeRateApi.getExchangeRate(exchangerateAccesskey, groupeCurrency, globalCurrency);

		// Parse the response for the prices
		this.prices.set(parsePrices(response.data(), exchangeRate));
	}

	private void handleEndpointError(HttpError error) {
		var httpStatusCode = INTERNAL_ERROR;
		if (error instanceof HttpError.ResponseError re) {
			httpStatusCode = re.status.code();
		}

		this.channel(TimeOfUseTariffGroupe.ChannelId.HTTP_STATUS_CODE).setNextValue(httpStatusCode);
		this.log.error(error.getMessage(), error);
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
	public static TimeOfUsePrices parsePrices(String jsonData, double exchangeRate) throws OpenemsNamedException {
		var result = new TreeMap<ZonedDateTime, Double>();
		var data = parseToJsonArray(jsonData);
		for (var element : data) {
			var priceString = "vario_plus";

			// Cent/kWh -> Currency/MWh
			// Example: 12 Cent/kWh => 0.12 EUR/kWh * 1000 kWh/MWh = 120 EUR/MWh.
			var marketPrice = getAsDouble(element, priceString) * 10 * exchangeRate;
			var startTimeString = getAsString(element, "start_timestamp");

			// Convert LocalDateTime to ZonedDateTime
			var startTimeStamp = ZonedDateTime.parse(startTimeString, DateTimeFormatter.ISO_OFFSET_DATE_TIME);

			// Adding the values in the Map.
			result.put(startTimeStamp, marketPrice);
		}
		return TimeOfUsePrices.from(result);
	}

	@Override
	public String debugLog() {
		return generateDebugLog(this, this.meta.getCurrency());
	}
}
