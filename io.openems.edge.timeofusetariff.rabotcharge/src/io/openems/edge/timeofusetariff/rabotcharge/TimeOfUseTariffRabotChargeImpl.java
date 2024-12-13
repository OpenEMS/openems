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
import java.util.concurrent.CompletableFuture;
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

import com.google.common.collect.ImmutableSortedMap;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.oem.OpenemsEdgeOem.OAuthClientRegistration;
import io.openems.common.timedata.DurationUnit;
import io.openems.common.types.HttpStatus;
import io.openems.edge.bridge.http.api.BridgeHttp;
import io.openems.edge.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.edge.bridge.http.api.BridgeHttpFactory;
import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpMethod;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.bridge.http.api.UrlBuilder;
import io.openems.edge.bridge.http.time.DefaultDelayTimeProvider;
import io.openems.edge.bridge.http.time.DelayTimeProvider;
import io.openems.edge.bridge.http.time.DelayTimeProvider.Delay;
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

	protected static final UrlBuilder RABOT_CHARGE_TOKEN_URL = UrlBuilder
			.parse("https://auth.rabot-charge.de/connect/token");

	private static final UrlBuilder RABOT_CHARGE_BASE_URL = UrlBuilder.parse("https://api.rabot-charge.de");
	protected static final UrlBuilder RABOT_CHARGE_PRIZE_COMPONENT_URL = RABOT_CHARGE_BASE_URL
			.withPath("/hems/v1/price-components");
	protected static final UrlBuilder RABOT_CHARGE_API_URL = RABOT_CHARGE_BASE_URL
			.withPath("/hems/v1/day-ahead-prices/limited");

	private static final int INTERNAL_ERROR = -1; // parsing, handle exception...

	private final Logger log = LoggerFactory.getLogger(TimeOfUseTariffRabotChargeImpl.class);
	private final AtomicReference<TimeOfUsePrices> prices = new AtomicReference<>(TimeOfUsePrices.EMPTY_PRICES);

	@Reference
	private Meta meta;

	@Reference
	private OpenemsEdgeOem oem;

	@Reference
	private ComponentManager componentManager;

	@Reference
	private BridgeHttpFactory httpBridgeFactory;
	private BridgeHttp httpBridge;

	private OAuthClientRegistration clientRegistration;
	private String zipcode;

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

		if (config.zipcode() == null || config.zipcode().isEmpty()) {
			return;
		}

		OAuthClientRegistration clientRegistration;
		if (config.clientId() != null && !config.clientId().isBlank() //
				&& config.clientSecret() != null && !config.clientSecret().isBlank()) {
			clientRegistration = new OAuthClientRegistration(config.clientId(), config.clientSecret());
		} else {
			clientRegistration = this.oem.getRabotChargeCredentials();
		}

		if (clientRegistration == null) {
			return;
		}
		this.clientRegistration = clientRegistration;

		this.zipcode = config.zipcode();

		this.httpBridge = this.httpBridgeFactory.get();

		this.scheduleRequest();
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.httpBridgeFactory.unget(this.httpBridge);
	}

	private CompletableFuture<String> refreshToken() {
		final var endpoint = new Endpoint(RABOT_CHARGE_TOKEN_URL.toEncodedString(), HttpMethod.POST,
				BridgeHttp.DEFAULT_CONNECT_TIMEOUT, BridgeHttp.DEFAULT_READ_TIMEOUT, "grant_type=client_credentials" //
						+ "&scope=api:hems" //
						+ "&client_id=" + this.clientRegistration.clientId() //
						+ "&client_secret=" + this.clientRegistration.clientSecret(),
				Map.of("Content-Type", "application/x-www-form-urlencoded"));

		final var tokenFuture = new CompletableFuture<String>();
		this.httpBridge.subscribeJsonTime(new DefaultDelayTimeProvider(() -> Delay.immediate(), t -> Delay.infinite(),
				error -> Delay.of(Duration.ofMinutes(30))), endpoint, response -> {
					final var token = response.data().getAsJsonObject().get("access_token").getAsString();
					this._setStatusAuthenticationFailed(false);
					tokenFuture.complete(token);
				}, error -> {
					this.log.error("Unable to get token", error);
					this._setHttpStatusCode(
							error instanceof HttpError.ResponseError r ? r.status.code() : INTERNAL_ERROR);
					this._setStatusAuthenticationFailed(true);
				});

		return tokenFuture;
	}

	private void scheduleRequest() {
		this.refreshToken().thenAccept(token -> {
			this.httpBridge
					.requestJson(new Endpoint(
							RABOT_CHARGE_PRIZE_COMPONENT_URL.withQueryParam("location.postCode", this.zipcode)
									.toEncodedString(),
							HttpMethod.GET, BridgeHttp.DEFAULT_CONNECT_TIMEOUT, BridgeHttp.DEFAULT_READ_TIMEOUT, null,
							this.buildRequestHeaders(token)))
					.thenApply(response -> {
						final var object = response.data().getAsJsonObject();
						return new PriceComponents(//
								object.get("taxAndFeeKwHPrice").getAsDouble(), //
								object.get("gridFeeKwHPrice").getAsDouble(), //
								object.get("gridFeeFixed").getAsDouble() //
						);
					}).whenComplete((priceComponent, error) -> {
						if (priceComponent == null) {
							this.log.error("Unable to get price components", error);
							this._setHttpStatusCode(
									error instanceof HttpError.ResponseError r ? r.status.code() : INTERNAL_ERROR);
							return;
						}
						this._setHttpStatusCode(HttpStatus.OK.code());

						this.httpBridge.subscribeTime(
								new RabotChargeDelayTimeProvider(this.componentManager.getClock()), //
								this.createRabotChargeEndpoint(token), //
								// pass priceComponent
								response -> this.handleEndpointResponse(response, priceComponent),
								this::handleEndpointError);
					});

		});
	}

	public class RabotChargeDelayTimeProvider implements DelayTimeProvider {

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
			return DelayTimeProviderChain.fixedAtEveryFull(this.clock, DurationUnit.ofDays(1)) //
					.plusRandomDelay(60, ChronoUnit.SECONDS) //
					.getDelay();
		}

		@Override
		public Delay onErrorRunDelay(HttpError error) {
			if (error instanceof HttpError.ResponseError r && r.status.equals(HttpStatus.UNAUTHORIZED)) {
				// reschedule after authenticated
				TimeOfUseTariffRabotChargeImpl.this.scheduleRequest();
				return Delay.infinite();
			}

			return DelayTimeProviderChain.fixedDelay(Duration.ofHours(1))//
					.plusRandomDelay(60, ChronoUnit.SECONDS) //
					.getDelay();
		}

	}

	private Endpoint createRabotChargeEndpoint(String accessToken) {
		return new Endpoint(RABOT_CHARGE_API_URL.toEncodedString(), //
				HttpMethod.GET, //
				BridgeHttp.DEFAULT_CONNECT_TIMEOUT, //
				BridgeHttp.DEFAULT_READ_TIMEOUT, //
				null, //
				this.buildRequestHeaders(accessToken));
	}

	private Map<String, String> buildRequestHeaders(String accessToken) {
		return Map.of(//
				"Authorization", "Bearer " + accessToken, //
				"Accept", "application/json" //
		);

	}

	private void handleEndpointResponse(HttpResponse<String> response, PriceComponents priceComponent)
			throws OpenemsNamedException {
		this.channel(TimeOfUseTariffRabotCharge.ChannelId.HTTP_STATUS_CODE).setNextValue(response.status().code());
		this._setStatusBadRequest(false);
		this._setStatusAuthenticationFailed(false);

		// Parse the response for the prices, passing the priceComponent.
		this.prices.set(parsePrices(response.data(), priceComponent));
	}

	private void handleEndpointError(HttpError error) {
		var httpStatusCode = INTERNAL_ERROR;
		if (error instanceof HttpError.ResponseError re) {
			httpStatusCode = re.status.code();

			this._setStatusAuthenticationFailed(httpStatusCode == HttpStatus.UNAUTHORIZED.code());
			this._setStatusBadRequest(httpStatusCode == HttpStatus.BAD_REQUEST.code());
		}

		this.channel(TimeOfUseTariffRabotCharge.ChannelId.HTTP_STATUS_CODE).setNextValue(httpStatusCode);
		this.log.error(error.getMessage(), error);
	}

	@Override
	public TimeOfUsePrices getPrices() {
		return TimeOfUsePrices.from(ZonedDateTime.now(this.componentManager.getClock()), this.prices.get());
	}

	/**
	 * Parse the JSON to {@link TimeOfUsePrices}.
	 *
	 * @param jsonData       the JSON
	 * @param priceComponent the {@link PriceComponents}
	 * @return the {@link TimeOfUsePrices}
	 * @throws OpenemsNamedException on error
	 */
	public static TimeOfUsePrices parsePrices(String jsonData, PriceComponents priceComponent)
			throws OpenemsNamedException {
		var result = ImmutableSortedMap.<ZonedDateTime, Double>naturalOrder();
		var data = getAsJsonArray(parseToJsonObject(jsonData), "records");
		for (var element : data) {
			// Cent/kWh -> Currency/MWh
			// Example: 12 Cent/kWh => 0.12 EUR/kWh * 1000 kWh/MWh = 120 EUR/MWh.
			final var basePrice = getAsDouble(element, "priceInCentPerKwh") * 10;
			final var additionalCosts = (priceComponent.taxAndFeeKwHPrice + priceComponent.gridFeeKwHPrice
					+ priceComponent.gridFeeFixed) * 10;

			final var marketPrice = basePrice + additionalCosts;

			// Converting time string to ZonedDateTime.
			final var startTimeStamp = ZonedDateTime //
					.parse(getAsString(element, "timestamp")) //
					.truncatedTo(ChronoUnit.HOURS);

			// Adding the values in the Map for each 15-minute interval.
			for (var minutes = 0; minutes <= 45; minutes += 15) {
				result.put(startTimeStamp.plusMinutes(minutes), marketPrice);
			}
		}
		return TimeOfUsePrices.from(result.build());
	}

	@Override
	public String debugLog() {
		return generateDebugLog(this, this.meta.getCurrency());
	}

	protected record PriceComponents(//
			double taxAndFeeKwHPrice, //
			double gridFeeKwHPrice, //
			double gridFeeFixed //
	) {

	}

}
