package io.openems.edge.timeofusetariff.rabotcharge;

import static io.openems.common.utils.JsonUtils.getAsDouble;
import static io.openems.common.utils.JsonUtils.getAsJsonArray;
import static io.openems.common.utils.JsonUtils.getAsString;
import static io.openems.common.utils.JsonUtils.parseToJsonObject;
import static io.openems.edge.timeofusetariff.api.utils.TimeOfUseTariffUtils.generateDebugLog;

import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
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
import io.openems.common.utils.JsonUtils;
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
		name = "TimeOfUseTariff.RabotCharge", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class TimeOfUseTariffRabotChargeImpl extends AbstractOpenemsComponent
		implements TimeOfUseTariff, OpenemsComponent, TimeOfUseTariffRabotCharge {

	private static final String RABOT_CHARGE_API_URL = "https://api.rabot-charge.de/api/day-ahead-prices-limited";
	private static final int API_EXECUTE_HOUR = 14;
	private static final int INTERNAL_ERROR = -1; // parsing, handle exception...

	private final Logger log = LoggerFactory.getLogger(TimeOfUseTariffRabotChargeImpl.class);
	private final AtomicReference<TimeOfUsePrices> prices = new AtomicReference<>(TimeOfUsePrices.EMPTY_PRICES);
	private String accessToken;

	@Reference
	private Meta meta;

	@Reference
	private ComponentManager componentManager;

	@Reference
	private BridgeHttpFactory httpBridgeFactory;
	private BridgeHttp httpBridge;

	public TimeOfUseTariffRabotChargeImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				TimeOfUseTariffRabotCharge.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());

		if (!config.enabled()) {
			return;
		}

		if (config.accessToken() == null || config.accessToken().isEmpty()) {
			return;
		}

		this.accessToken = config.accessToken();
		this.httpBridge = this.httpBridgeFactory.get();
		this.httpBridge.subscribeTime(new RabotChargeDelayTimeProvider(this.componentManager.getClock()), //
				this.createRabotChargeEndpoint(), //
				this::handleEndpointResponse, //
				this::handleEndpointError);
	}

	public static class RabotChargeDelayTimeProvider implements DelayTimeProvider {

		private final Clock clock;

		public RabotChargeDelayTimeProvider(Clock clock) {
			super();
			this.clock = clock;
		}

		@Override
		public Delay onFirstRunDelay() {
			return Delay.immediate();
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

		@Override
		public Delay onErrorRunDelay(HttpError error) {
			return DelayTimeProviderChain.fixedDelay(Duration.ofHours(1))//
					.plusRandomDelay(60, ChronoUnit.SECONDS) //
					.getDelay();
		}

	}

	private Endpoint createRabotChargeEndpoint() {

		return new Endpoint(RABOT_CHARGE_API_URL, //
				HttpMethod.POST, //
				BridgeHttp.DEFAULT_CONNECT_TIMEOUT, //
				BridgeHttp.DEFAULT_READ_TIMEOUT, //
				this.buildRequestBody(), //
				this.buildRequestHeaders());
	}

	private String buildRequestBody() {
		return JsonUtils.buildJsonObject() //
				.build().toString();
	}

	private Map<String, String> buildRequestHeaders() {
		return Map.of(//
				"Authorization", "Bearer " + this.accessToken, //
				"Content-Type", "application/json" //
		);

	}

	private void handleEndpointResponse(HttpResponse<String> response) throws OpenemsNamedException {
		this.channel(TimeOfUseTariffRabotCharge.ChannelId.HTTP_STATUS_CODE).setNextValue(response.status().code());

		// Parse the response for the prices
		this.prices.set(parsePrices(response.data()));
	}

	private void handleEndpointError(HttpError error) {
		var httpStatusCode = INTERNAL_ERROR;
		if (error instanceof HttpError.ResponseError re) {
			httpStatusCode = re.status.code();
		}

		this.channel(TimeOfUseTariffRabotCharge.ChannelId.HTTP_STATUS_CODE).setNextValue(httpStatusCode);
		this.log.error(error.getMessage(), error);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.httpBridgeFactory.unget(this.httpBridge);
	}

	@Override
	public TimeOfUsePrices getPrices() {
		return TimeOfUsePrices.from(ZonedDateTime.now(this.componentManager.getClock()), this.prices.get());
	}

	/**
	 * Parse the JSON to {@link TimeOfUsePrices}.
	 *
	 * @param jsonData the JSON
	 * @return the {@link TimeOfUsePrices}
	 * @throws OpenemsNamedException on error
	 */
	public static TimeOfUsePrices parsePrices(String jsonData) throws OpenemsNamedException {
		var result = new TreeMap<ZonedDateTime, Double>();
		var data = getAsJsonArray(parseToJsonObject(jsonData), "records");
		for (var element : data) {
			// Cent/kWh -> Currency/MWh
			// Example: 12 Cent/kWh => 0.12 EUR/kWh * 1000 kWh/MWh = 120 EUR/MWh.
			var marketPrice = getAsDouble(element, "price_inCentPerKwh") * 10;

			// Converting time string to ZonedDateTime.
			var startTimeStamp = ZonedDateTime //
					.parse(getAsString(element, "moment")) //
					.truncatedTo(ChronoUnit.HOURS);

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
