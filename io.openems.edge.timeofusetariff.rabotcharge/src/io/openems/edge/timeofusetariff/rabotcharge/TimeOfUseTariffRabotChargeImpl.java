package io.openems.edge.timeofusetariff.rabotcharge;

import static io.openems.common.types.HttpStatus.UNAUTHORIZED;
import static io.openems.edge.timeofusetariff.api.utils.TimeOfUseTariffUtils.generateDebugLog;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.SECONDS;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.service.cm.ConfigurationAdmin;
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

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.bridge.http.api.HttpAuthorization;
import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpHeader;
import io.openems.common.bridge.http.api.HttpMediaType;
import io.openems.common.bridge.http.api.HttpMethod;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.authentication.HttpBridgeAuthenticationService;
import io.openems.common.bridge.http.authentication.HttpBridgeAuthenticationServiceDefinition;
import io.openems.common.bridge.http.logging.HttpBridgeLoggingServiceConfiguration;
import io.openems.common.bridge.http.logging.HttpBridgeLoggingServiceDefinition;
import io.openems.common.bridge.http.time.DelayTimeProvider;
import io.openems.common.bridge.http.time.DelayTimeProviderChain;
import io.openems.common.bridge.http.time.HttpBridgeTimeService.TimeEndpoint;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.types.DebugMode;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.common.oauth.ConnectionState;
import io.openems.edge.common.oauth.OAuthBackend;
import io.openems.edge.common.oauth.OAuthCore;
import io.openems.edge.common.oauth.OAuthProvider;
import io.openems.edge.common.oauth.jsonrpc.InitiateOAuthConnect;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;
import io.openems.edge.timeofusetariff.rabotcharge.RabotChargeApi.PriceComponents;
import io.openems.edge.timeofusetariff.rabotcharge.RabotChargeApiService.RabotApiException;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "TimeOfUseTariff.RabotCharge", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class TimeOfUseTariffRabotChargeImpl extends AbstractOpenemsComponent
		implements TimeOfUseTariff, OpenemsComponent, TimeOfUseTariffRabotCharge, OAuthProvider {

	protected static final String RABOT_CHARGE_API_URL = "https://api.rabot-charge.de/partner/v1/day-ahead-prices/limited";

	private static final DateTimeFormatter URL_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
	private static final ZoneId UTC = ZoneId.of("UTC");
	private static final int API_EXECUTE_HOUR = 16;
	private static final int INTERNAL_ERROR = -1; // parsing, handle exception...
	private static final double VAT_GERMANY = 1.19;

	private final Logger log = LoggerFactory.getLogger(TimeOfUseTariffRabotChargeImpl.class);
	private final AtomicReference<TimeOfUsePrices> prices = new AtomicReference<>(TimeOfUsePrices.EMPTY_PRICES);

	@Reference
	private Meta meta;

	@Reference
	private OpenemsEdgeOem oem;

	@Reference
	private ComponentManager componentManager;

	@Reference
	private ConfigurationAdmin configurationAdmin;

	@Reference
	private OAuthBackend authBackend;

	@Reference
	private BridgeHttpFactory httpBridgeFactory;
	private BridgeHttp httpBridge;

	private RabotChargeApiService apiService;
	private RabotChargeApiService authenticatedApiService;
	private HttpBridgeAuthenticationService<HttpHeader> authenticationService;
	private String customerNumber;
	private String contractId;
	private TimeEndpoint pricePollingEndpoint;

	private record InitiatedConnectState(String state) {
	}

	private OAuthBackend.OAuthClientBackendRegistration oAuthClientBackendRegistration;
	private InitiatedConnectState lastConnectionStateInitiator;

	public TimeOfUseTariffRabotChargeImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				OAuthProvider.ChannelId.values(), //
				TimeOfUseTariffRabotCharge.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());

		if (!config.enabled()) {
			return;
		}

		this.httpBridge = this.httpBridgeFactory.get();
		this.httpBridge.setDebugMode(DebugMode.DETAILED);
		this.httpBridge.setMaximumPoolSize(10);

		this.httpBridge.createService(
				new HttpBridgeLoggingServiceDefinition(HttpBridgeLoggingServiceConfiguration.contextId(config.id())));

		this.oAuthClientBackendRegistration = new OAuthBackend.OAuthClientBackendRegistration(
				config.backendOAuthClientIdentifier(), List.of("openid", "profile"));

		// Determine Partner Credentials
		var partnerReg = this.oem.getRabotChargeCredentials();

		if (partnerReg == null) {
			this.log.error("Missing Rabot Charge Partner Credentials");
			this._setOAuthConnectionState(ConnectionState.UNDEFINED);
			return;
		}

		// Initialize Service
		this.apiService = new RabotChargeApiService(this.httpBridge, partnerReg);
		this.authenticationService = this.httpBridge
				.createService(HttpBridgeAuthenticationServiceDefinition.of(() -> this.apiService.getPartnerToken()
						.thenApply(token -> HttpHeader.authorization(HttpAuthorization.bearer(token)))));
		this.authenticatedApiService = new RabotChargeApiService(this.authenticationService, partnerReg);

		this.scheduleDataRefresh();
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.httpBridgeFactory.unget(this.httpBridge);
	}

	private void scheduleDataRefresh() {

		if (this.customerNumber == null || this.customerNumber.isBlank()) {
			this.log.info("No customer number configured. Falling back to base net market prices.");
			this._setOAuthConnectionState(ConnectionState.NOT_CONNECTED);
			this.startFallbackDayAheadPriceLoop();
		} else {
			this._setOAuthConnectionState(ConnectionState.CONNECTED);
			this.fetchContracts();
		}
	}

	private void fetchContracts() {
		// Fetch the contract ID for the customer
		if (this.contractId == null || this.contractId.isBlank()) {
			this.authenticatedApiService.getContracts(this.customerNumber).thenAccept(response -> {
				final var contracts = response.contracts();
				if (contracts != null && !contracts.isEmpty()) {
					var contract = contracts.stream().filter(c -> "Delivery".equalsIgnoreCase(c.contractState()))
							.findFirst().orElse(contracts.getFirst());

					this.contractId = contract.contractNumber();
					this.log.info("Found Contract ID: {}", this.contractId);
					this.fetchCostsAndStartLoop();
				} else {
					this.log.warn("No contracts found for customer {}", this.customerNumber);
					this.startFallbackDayAheadPriceLoop();
				}
			}).exceptionally(this::handleDataFetchErrorAndFallback);
		} else {
			this.fetchCostsAndStartLoop();
		}
	}

	private void fetchCostsAndStartLoop() {
		// Use the authenticated service to fetch contract costs automatically
		this.authenticatedApiService.getCosts(this.customerNumber, this.contractId) //
				.thenAccept(costs -> {
					this.startDayAheadPriceLoop(costs);
				}).exceptionally(this::handleApiError);
	}

	private void startDayAheadPriceLoop(PriceComponents costs) {
		var timeService = this.authenticatedApiService.getTimeService();

		if (this.pricePollingEndpoint != null) {
			timeService.removeTimeEndpoint(this.pricePollingEndpoint);
		}

		this.pricePollingEndpoint = timeService.subscribeTime(
				new RabotChargeDelayTimeProvider(this.componentManager.getClock()), //
				this.createRabotChargeEndpoint(), //
				response -> this.handleEndpointResponse(response, costs), //
				this::handleEndpointError);
	}

	private Endpoint createRabotChargeEndpoint() {
		return BridgeHttp.create(RABOT_CHARGE_API_URL) //
				.setMethod(HttpMethod.GET) //
				.setHeader(HttpHeader.accept(HttpMediaType.Application.JSON)) //
				.build();
	}

	private Void handleApiError(Throwable e) {
		var cause = e instanceof CompletionException ce ? ce.getCause() : e;

		if (cause != null && cause.getMessage() != null && cause.getMessage().contains("Closed by interrupt")) {
			this.log.info("HTTP request interrupted (usually due to component deactivation).");
			return null;
		}

		if (cause instanceof RabotApiException apiException) {
			this.handleEndpointError(apiException.getHttpError());
		} else {
			this.log.error("Rabot API Error", cause);
			this._setHttpStatusCode(INTERNAL_ERROR);
			this._setStatusAuthenticationFailed(true);
		}
		return null;
	}

	/**
	 * Handles errors during the initial data fetch (Customer, Contract, Costs). If
	 * the error is an authentication error, it triggers the appropriate handling.
	 * For other errors, it logs a warning and starts the fallback price loop with
	 * base net market prices.
	 * 
	 * @param e the exception thrown during data fetch
	 * @return null (required for exceptionally callback)
	 */
	private Void handleDataFetchErrorAndFallback(Throwable e) {
		var cause = e instanceof CompletionException ce ? ce.getCause() : e;

		// Immediately exit if component is being deactivated
		if (cause != null && cause.getMessage() != null && cause.getMessage().contains("Closed by interrupt")) {
			return this.handleApiError(e);
		}

		if (cause instanceof HttpError.ResponseError re && re.status.code() == UNAUTHORIZED.code()) {
			return this.handleApiError(e);
		}

		if (cause instanceof RabotApiException apiException
				&& apiException.getHttpError() instanceof HttpError.ResponseError re
				&& re.status.code() == UNAUTHORIZED.code()) {
			return this.handleApiError(e);
		}

		this.log.warn("Customer/Contract/Cost data unavailable. Falling back to base net market prices. Reason: {}",
				cause.getMessage());
		this.startFallbackDayAheadPriceLoop();

		return null;
	}

	private void startFallbackDayAheadPriceLoop() {
		// empty / Default PriceComponents object (all zero costs) to calculate the
		// universal base net price
		this.startDayAheadPriceLoop(PriceComponents.DEFAULT);
	}

	@Override
	public OAuthCore.OAuthMetaInfo getMetaInfo() {
		return new OAuthCore.OAuthMetaInfo(this.id(), this.alias(), "Connect to Rabot Charge");
	}

	@Override
	public CompletableFuture<InitiateOAuthConnect.Response> initiateConnect() {
		final var state = UUID.randomUUID().toString();
		this.lastConnectionStateInitiator = new InitiatedConnectState(state);

		// Get Metadata (Redirect URL)
		return this.authBackend.getInitMetadata(this.oAuthClientBackendRegistration.identifier()) //
				.thenCompose(metadata -> {
					final var redirectUrl = metadata.redirectUrl();
					return this.authenticatedApiService.createCustomerLink(redirectUrl).thenApply(authUrl -> {
						return new InitiateOAuthConnect.Response(//
								authUrl.authorizationUrl(), // The Rabot link URL
								"", // ClientID not used here
								metadata.redirectUrl(), //
								this.oAuthClientBackendRegistration.scopes(), //
								state, // State
								null, // Code Challenge
								null // Method
						);
					});
				});
	}

	@Override
	public CompletableFuture<Void> connectCode(String state, String code) {
		final var lastConnectionStateInitiator = this.lastConnectionStateInitiator;
		if (lastConnectionStateInitiator == null) {
			throw new RuntimeException("No ongoing connection");
		}
		if (!lastConnectionStateInitiator.state.equals(state)) {
			throw new RuntimeException("States do not match");
		}

		// The 'code' here IS the customerNumber because we set
		// 'customerNumberQueryParameterName': 'code'
		var receivedCustomerNumber = code;
		this.log.info("Received Customer Number via OAuth flow: " + receivedCustomerNumber);

		// Store it
		this.customerNumber = receivedCustomerNumber;
		this.contractId = null; // Clear contract to force refetch
		this._setOAuthConnectionState(ConnectionState.CONNECTED);

		// Refresh logic (fetch contract, etc)
		this.scheduleDataRefresh();

		return CompletableFuture.completedFuture(null);
	}

	@Override
	public void disconnect() {
		this.customerNumber = null;
		this.contractId = null;
		this._setOAuthConnectionState(ConnectionState.NOT_CONNECTED);
		this.prices.set(TimeOfUsePrices.EMPTY_PRICES);
	}

	private void handleEndpointResponse(HttpResponse<String> response, PriceComponents costs)
			throws OpenemsNamedException {
		this._setHttpStatusCode(response.status().code());
		this._setStatusBadRequest(false);
		this._setStatusAuthenticationFailed(false);

		// Parse prices directly without components
		this.prices.set(parsePrices(response.data(), costs));
	}

	private void handleEndpointError(HttpError error) {
		var code = error instanceof HttpError.ResponseError re //
				? re.status.code() //
				: INTERNAL_ERROR;

		this._setHttpStatusCode(code);
		this._setStatusAuthenticationFailed(code == UNAUTHORIZED.code());

		// If unauthorized (token expired), restart the flow to get a new token
		if (code == UNAUTHORIZED.code()) {
			this.scheduleDataRefresh();
		}
	}

	protected static TimeOfUsePrices parsePrices(String jsonData, PriceComponents costs) throws OpenemsNamedException {
		var result = ImmutableSortedMap.<Instant, Double>naturalOrder();

		var jsonObject = JsonUtils.parseToJsonObject(jsonData);
		var data = JsonUtils.getAsJsonObject(jsonObject, "data");
		var prices = JsonUtils.getAsJsonArray(data, "prices");

		for (var element : prices) {
			var elementObj = JsonUtils.getAsJsonObject(element);

			// Raw Price in Cent/kWh -> Convert to EUR/MWh
			// 1 Cent/kWh = 10 EUR/MWh
			final var rawPriceCentPerKwh = JsonUtils.getAsDouble(elementObj, "price");
			final var grossDayAhead = rawPriceCentPerKwh * VAT_GERMANY;
			final var totalGrossCentPerKwh = grossDayAhead + costs.getVariableFeesGross();
			final var totalEurPerMwh = totalGrossCentPerKwh * 10;

			var timeString = JsonUtils.getAsString(elementObj, "at");
			var localDateTime = LocalDateTime.parse(timeString, URL_DATE_FORMATTER);
			var timestamp = localDateTime.atZone(UTC).toInstant();
			result.put(timestamp, totalEurPerMwh);
		}
		return TimeOfUsePrices.from(result.build());
	}

	@Override
	public TimeOfUsePrices getPrices() {
		return TimeOfUsePrices.from(Instant.now(this.componentManager.getClock()), this.prices.get());
	}

	@Override
	public String debugLog() {
		return generateDebugLog(this, this.meta.getCurrency());
	}

	public class RabotChargeDelayTimeProvider implements DelayTimeProvider {

		private final Clock clock;

		public RabotChargeDelayTimeProvider(Clock clock) {
			this.clock = clock;
		}

		@Override
		public Delay onFirstRunDelay() {
			return Delay.immediate();
		}

		@Override
		public Delay onSuccessRunDelay(HttpResponse<String> result) {
			var now = ZonedDateTime.now(this.clock).truncatedTo(ChronoUnit.HOURS);
			final ZonedDateTime nextRun;
			// EPEX Spot prices usually available around 14:00 for the next day
			if (now.getHour() < API_EXECUTE_HOUR) {
				nextRun = now.withHour(API_EXECUTE_HOUR);
			} else {
				nextRun = now.plusDays(1).withHour(API_EXECUTE_HOUR);
			}
			return DelayTimeProviderChain.fixedDelay(Duration.between(now, nextRun)) //
					.plusRandomDelay(10, MINUTES) //
					.plusRandomDelay(60, SECONDS) //
					.getDelay();
		}

		@Override
		public Delay onErrorRunDelay(HttpError error) {
			return switch (error) {
			case HttpError.ResponseError r when r.status.equals(UNAUTHORIZED) -> {
				// Reschedule triggers a token refresh immediately via scheduleDataRefresh
				// but here we wait to avoid spin loops if refresh fails
				yield Delay.of(Duration.ofMinutes(1));
			}
			default -> DelayTimeProviderChain.fixedDelay(Duration.ofHours(1))//
					.plusRandomDelay(60, SECONDS) //
					.getDelay();
			};
		}
	}

}
