package io.openems.edge.timeofusetariff.octopus.dynamic.gridbuy;

import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.timeofusetariff.api.utils.TimeOfUseTariffUtils.generateDebugLog;
import static io.openems.edge.timeofusetariff.octopus.dynamic.gridbuy.Utils.classify;
import static io.openems.edge.timeofusetariff.octopus.dynamic.gridbuy.Utils.createPriceEndpoint;
import static io.openems.edge.timeofusetariff.octopus.dynamic.gridbuy.Utils.createTokenEndpoint;
import static io.openems.edge.timeofusetariff.octopus.dynamic.gridbuy.Utils.getHttpStatusCode;
import static io.openems.edge.timeofusetariff.octopus.dynamic.gridbuy.Utils.hasGraphQlAuthorizationError;
import static io.openems.edge.timeofusetariff.octopus.dynamic.gridbuy.Utils.parsePrices;
import static io.openems.edge.timeofusetariff.octopus.dynamic.gridbuy.Utils.parseToken;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
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

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpHeader;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.authentication.HttpBridgeAuthenticationServiceDefinition;
import io.openems.common.bridge.http.authentication.HttpBridgeAuthenticationServiceDefinition.HttpBridgeAuthenticationServiceConfigHttpHeader;
import io.openems.common.bridge.http.time.HttpBridgeTimeServiceDefinition;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.HttpStatus;
import io.openems.common.utils.StringUtils;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;
import io.openems.edge.timeofusetariff.octopus.dynamic.gridbuy.Utils.KrakenApiException;
import io.openems.edge.timeofusetariff.octopus.dynamic.gridbuy.Utils.OctopusDelayTimeProvider;
import io.openems.edge.timeofusetariff.octopus.dynamic.gridbuy.Utils.UpdateResult;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Tariff.Octopus.Dynamic.GridBuy", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class TariffOctopusDynamicGridBuyImpl extends AbstractOpenemsComponent
		implements TimeOfUseTariff, OpenemsComponent, TariffOctopusDynamicGridBuy {

	private static final int INTERNAL_ERROR = -1;

	private final Logger log = LoggerFactory.getLogger(TariffOctopusDynamicGridBuyImpl.class);
	private final AtomicReference<TimeOfUsePrices> prices = new AtomicReference<>(TimeOfUsePrices.EMPTY_PRICES);

	@Reference
	private Meta meta;

	@Reference
	private ComponentManager componentManager;

	@Reference
	private BridgeHttpFactory httpBridgeFactory;
	private BridgeHttp httpBridge;

	private Config config;

	public TariffOctopusDynamicGridBuyImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				TariffOctopusDynamicGridBuy.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;

		if (!config.enabled()) {
			return;
		}

		final var countrySupported = config.country().isSupported();
		setValue(this, TariffOctopusDynamicGridBuy.ChannelId.STATUS_COUNTRY_NOT_SUPPORTED, !countrySupported);
		if (!countrySupported) {
			this.logWarn(this.log, "Country [" + config.country() + "] is not yet supported; no requests are sent");
			return;
		}

		final var configurationComplete = !StringUtils.isNullOrBlank(config.email()) //
				&& !StringUtils.isNullOrBlank(config.password()) //
				&& !StringUtils.isNullOrBlank(config.accountNumber());
		setValue(this, TariffOctopusDynamicGridBuy.ChannelId.STATUS_CONFIGURATION_INCOMPLETE, !configurationComplete);
		if (!configurationComplete) {
			this.logWarn(this.log, "Email, password or account number is not configured");
			return;
		}

		this.httpBridge = this.httpBridgeFactory.get();

		// Obtains the Kraken token on the first request and again (once per request)
		// if the session expired. Kraken may report an expired token as HTTP 401 or as
		// GraphQL authorization error in an HTTP 200 response.
		final var authenticationService = this.httpBridge.createService(new HttpBridgeAuthenticationServiceDefinition<>(
				new HttpBridgeAuthenticationServiceConfigHttpHeader(this::fetchToken, //
						(response, error) -> getHttpStatusCode(error) == HttpStatus.UNAUTHORIZED.code() //
								|| response != null && hasGraphQlAuthorizationError(response.data()))));
		final var timeService = authenticationService.createService(HttpBridgeTimeServiceDefinition.INSTANCE);

		final var priceEndpoint = createPriceEndpoint(config.country(), config.accountNumber());
		timeService.subscribeTime(//
				new OctopusDelayTimeProvider(config.refreshMinutes()), //
				() -> priceEndpoint, //
				this::handleResponse, //
				this::handleError);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		if (this.httpBridge != null) {
			this.httpBridgeFactory.unget(this.httpBridge);
			this.httpBridge = null;
		}
	}

	private CompletableFuture<HttpHeader> fetchToken() {
		final var bridge = this.httpBridge;
		if (bridge == null) {
			return CompletableFuture.failedFuture(new IllegalStateException("Component is deactivated"));
		}
		final var endpoint = createTokenEndpoint(this.config.country(), this.config.email(), this.config.password());
		// HTTP errors (e.g. 401/403) are passed through and classified by handleError
		return bridge.request(endpoint).thenApply(response -> {
			try {
				return HttpHeader.authorization(parseToken(response.data()));
			} catch (KrakenApiException | OpenemsNamedException e) {
				throw new CompletionException(e);
			}
		});
	}

	/**
	 * Handles a price response. Never throws, so that the returned
	 * {@link UpdateResult} always decides on the next request.
	 *
	 * @param response the {@link HttpResponse}
	 * @return the {@link UpdateResult}
	 */
	private UpdateResult handleResponse(HttpResponse<String> response) {
		setValue(this, TariffOctopusDynamicGridBuy.ChannelId.HTTP_STATUS_CODE, response.status().code());
		final var now = Instant.now(this.componentManager.getClock());
		final TimeOfUsePrices newPrices;
		try {
			newPrices = parsePrices(response.data(), now);
		} catch (KrakenApiException | OpenemsNamedException e) {
			return this.handleFailure(classify(e), response.status().code(), e);
		}
		this.prices.set(newPrices);

		setValue(this, TariffOctopusDynamicGridBuy.ChannelId.LAST_SUCCESSFUL_UPDATE, now.getEpochSecond());
		setValue(this, TariffOctopusDynamicGridBuy.ChannelId.FORECAST_VALID_UNTIL,
				newPrices.getEndTime().getEpochSecond());
		this.setStatus(UpdateResult.SUCCESS);
		return UpdateResult.SUCCESS;
	}

	private void handleError(HttpError error) {
		final var httpStatusCode = getHttpStatusCode(error);
		setValue(this, TariffOctopusDynamicGridBuy.ChannelId.HTTP_STATUS_CODE,
				httpStatusCode > 0 ? httpStatusCode : INTERNAL_ERROR);
		this.handleFailure(classify(error), httpStatusCode, error);
	}

	private UpdateResult handleFailure(UpdateResult result, int httpStatusCode, Throwable error) {
		this.setStatus(result);

		// Never log the error itself: messages and bodies may contain account data
		this.logWarn(this.log, switch (result) {
		case AUTHENTICATION_FAILED -> "Authentication failed; check email, password and account number. "
				+ "No further requests until the configuration is changed";
		case RATE_LIMITED -> "Rate limit reached; retrying later. Previous prices are kept";
		case FAILED, SUCCESS -> "Unable to update prices (HTTP " + httpStatusCode + ", "
				+ getRootCauseName(error) + "); retrying shortly. Previous prices are kept";
		});
		return result;
	}

	private void setStatus(UpdateResult result) {
		setValue(this, TariffOctopusDynamicGridBuy.ChannelId.STATUS_AUTHENTICATION_FAILED,
				result == UpdateResult.AUTHENTICATION_FAILED);
		setValue(this, TariffOctopusDynamicGridBuy.ChannelId.STATUS_RATE_LIMITED, result == UpdateResult.RATE_LIMITED);
		setValue(this, TariffOctopusDynamicGridBuy.ChannelId.STATUS_UPDATE_FAILED, result == UpdateResult.FAILED);
	}

	private static String getRootCauseName(Throwable error) {
		var t = error;
		while (t.getCause() != null && t.getCause() != t) {
			t = t.getCause();
		}
		return t.getClass().getSimpleName();
	}

	@Override
	public TimeOfUsePrices getPrices() {
		return TimeOfUsePrices.from(Instant.now(this.componentManager.getClock()), this.prices.get());
	}

	@Override
	public String debugLog() {
		return generateDebugLog(this, this.meta.getCurrency());
	}
}
