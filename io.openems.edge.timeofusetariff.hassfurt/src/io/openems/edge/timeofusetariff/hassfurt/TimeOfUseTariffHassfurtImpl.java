package io.openems.edge.timeofusetariff.hassfurt;

import static io.openems.common.utils.JsonUtils.getAsDouble;
import static io.openems.common.utils.JsonUtils.getAsJsonArray;
import static io.openems.common.utils.JsonUtils.getAsString;
import static io.openems.common.utils.JsonUtils.parseToJsonObject;
import static io.openems.edge.timeofusetariff.api.utils.TimeOfUseTariffUtils.generateDebugLog;
import static java.util.Collections.emptyMap;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

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
import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.edge.bridge.http.api.BridgeHttpFactory;
import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpMethod;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.bridge.http.time.DelayTimeProvider;
import io.openems.edge.bridge.http.time.DelayTimeProviderChain;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "TimeOfUseTariff.Hassfurt", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class TimeOfUseTariffHassfurtImpl extends AbstractOpenemsComponent
		implements TimeOfUseTariff, OpenemsComponent, TimeOfUseTariffHassfurt {
	private static final String FLEX_PRO_URL = "http://eex.stwhas.de/api/spotprices/flexpro";
	private static final String FLEX_PRO_START_END_URL = FLEX_PRO_URL + "?start_date=%s&end_date=%s";
	private static final String FLEX_URL = "http://eex.stwhas.de/api/spotprices";
	private static final String FLEX_START_END_URL = FLEX_URL + "?start_date=%s&end_date=%s";
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	private static final int API_EXECUTE_HOUR = 14;
	private static final int INTERNAL_ERROR = -1; // parsing, handle exception...

	private final Logger log = LoggerFactory.getLogger(TimeOfUseTariffHassfurtImpl.class);
	private final AtomicReference<TimeOfUsePrices> prices = new AtomicReference<>(TimeOfUsePrices.EMPTY_PRICES);

	@Reference
	private BridgeHttpFactory httpBridgeFactory;
	private BridgeHttp httpBridge;

	private Config config = null;

	@Reference
	private Meta meta;

	@Reference
	private ComponentManager componentManager;

	public TimeOfUseTariffHassfurtImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				TimeOfUseTariffHassfurt.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());

		if (!config.enabled()) {
			return;
		}

		this.config = config;
		this.httpBridge = this.httpBridgeFactory.get();
		this.httpBridge.subscribeTime(new HassfurtDelayTimeProvider(this.componentManager.getClock()), //
				this::createHassfurtEndpoint, //
				this::handleEndpointResponse, //
				this::handleEndpointError);
	}

	private Endpoint createHassfurtEndpoint() {

		var now = ZonedDateTime.now().truncatedTo(ChronoUnit.HOURS);
		var dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
		var startDate = now.format(dateFormatter);
		var endDate = now.plusDays(1).format(dateFormatter);

		var url = switch (this.config.tariffType()) {

		case STROM_FLEX -> {
			if (now.getHour() < 14) {
				yield FLEX_URL;
			} else {
				yield String.format(FLEX_START_END_URL, startDate, endDate);
			}
		}
		case STROM_FLEX_PRO -> {
			if (now.getHour() < 14) {
				yield FLEX_PRO_URL;
			} else {
				yield String.format(FLEX_PRO_START_END_URL, startDate, endDate);
			}
		}
		};

		return new Endpoint(url, //
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
		this.httpBridgeFactory.unget(this.httpBridge);
	}

	public static class HassfurtDelayTimeProvider implements DelayTimeProvider {

		private final Clock clock;

		public HassfurtDelayTimeProvider(Clock clock) {
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
			var now = ZonedDateTime.now(this.clock).truncatedTo(ChronoUnit.HOURS);
			ZonedDateTime nextRun;

			if (now.getHour() < API_EXECUTE_HOUR) {
				nextRun = now.withHour(API_EXECUTE_HOUR);
			} else {
				nextRun = now.plusDays(1).withHour(API_EXECUTE_HOUR);
			}

			return DelayTimeProviderChain.fixedDelay(Duration.between(now, nextRun))
					.plusRandomDelay(60, ChronoUnit.SECONDS) //
					.getDelay();
		}
	}

	private void handleEndpointResponse(HttpResponse<String> response) throws OpenemsNamedException {
		this.channel(TimeOfUseTariffHassfurt.ChannelId.HTTP_STATUS_CODE).setNextValue(response.status().code());

		// Parse the response for the prices
		this.prices.set(parsePrices(response.data(), this.config.tariffType()));
	}

	private void handleEndpointError(HttpError error) {
		var httpStatusCode = INTERNAL_ERROR;
		if (error instanceof HttpError.ResponseError re) {
			httpStatusCode = re.status.code();
		}

		this.channel(TimeOfUseTariffHassfurt.ChannelId.HTTP_STATUS_CODE).setNextValue(httpStatusCode);
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
	 * @param jsonData   the JSON data as a {@code String} containing the
	 *                   electricity price information.
	 * @param tariffType the tariff type which determines the specific price field
	 *                   to be extracted from the JSON data.
	 * @return a {@link TimeOfUsePrices} object containing the parsed prices mapped
	 *         to their respective timestamps.
	 * @throws OpenemsNamedException if an error occurs during the parsing of the
	 *                               JSON data.
	 */
	public static TimeOfUsePrices parsePrices(String jsonData, TariffType tariffType) throws OpenemsNamedException {
		var result = new TreeMap<ZonedDateTime, Double>();
		final var data = getAsJsonArray(parseToJsonObject(jsonData), "data");

		final var priceString = switch (tariffType) {
		case STROM_FLEX -> "t_price_has_incl_vat";
		case STROM_FLEX_PRO -> "t_price_has_pro_incl_vat";
		};
		for (var element : data) {

			// Cent/kWh -> Currency/MWh
			// Example: 12 Cent/kWh => 0.12 EUR/kWh * 1000 kWh/MWh = 120 EUR/MWh.
			final var marketPrice = getAsDouble(element, priceString) * 10;
			final var startTimeString = getAsString(element, "start_timestamp");

			// Parse the string to LocalDateTime
			final var localDateTime = LocalDateTime.parse(startTimeString, FORMATTER);

			// Convert LocalDateTime to ZonedDateTime
			final var startTimeStamp = localDateTime.atZone(ZoneId.systemDefault()).truncatedTo(ChronoUnit.HOURS);

			// Adding the values in the Map.
			result.put(startTimeStamp, marketPrice);
			result.put(startTimeStamp.plusMinutes(15), marketPrice);
			result.put(startTimeStamp.plusMinutes(30), marketPrice);
			result.put(startTimeStamp.plusMinutes(45), marketPrice);
		}
		return TimeOfUsePrices.from(result);
	}

	@Override
	public String debugLog() {
		return generateDebugLog(this, this.meta.getCurrency());
	}
}
