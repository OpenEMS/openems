package io.openems.edge.timeofusetariff.luox;

import static io.openems.common.utils.StringUtils.isNullOrBlank;
import static io.openems.edge.common.oauth.OAuthUtils.generateCodeChallenge;
import static io.openems.edge.common.oauth.OAuthUtils.generateCodeVerifier;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.utils.ThreadPoolUtils;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.oauth.ConnectionState;
import io.openems.edge.common.oauth.OAuthBackend;
import io.openems.edge.common.oauth.OAuthCore;
import io.openems.edge.common.oauth.OAuthProvider;
import io.openems.edge.common.oauth.jsonrpc.InitiateOAuthConnect;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "TimeOfUseTariff.LUOX.Energy", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class TimeOfUseTariffLuoxImpl extends AbstractOpenemsComponent
		implements OpenemsComponent, TimeOfUseTariffLuox, OAuthProvider, TimeOfUseTariff {

	private static final int MAX_REFRESH_TRIES = 5;

	private record InitiatedConnectState(String state, String codeVerifier) {
	}

	private final Logger log = LoggerFactory.getLogger(TimeOfUseTariffLuoxImpl.class);
	private final AtomicReference<TimeOfUsePrices> prices = new AtomicReference<>(TimeOfUsePrices.EMPTY_PRICES);

	@Reference
	private ConfigurationAdmin configurationAdmin;

	@Reference
	private OAuthBackend authBackend;

	@Reference
	private ComponentManager componentManager;

	@Reference
	private BridgeHttpFactory bridgeHttpFactory;
	@Reference
	private HttpBridgeLuoxService.HttpBridgeLuoxServiceDefinition httpBridgeLuoxServiceDefinition;
	private BridgeHttp bridgeHttp;

	private OAuthBackend.OAuthClientBackendRegistration oAuthClientBackendRegistration;
	private String refreshToken;
	private InitiatedConnectState lastConnectionStateInitiator;

	private ScheduledExecutorService executorService;

	public TimeOfUseTariffLuoxImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				OAuthProvider.ChannelId.values(), //
				TimeOfUseTariffLuox.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this._setOAuthConnectionState(ConnectionState.NOT_CONNECTED);
		this.executorService = Executors.newScheduledThreadPool(0, Thread.ofVirtual().factory());

		this.oAuthClientBackendRegistration = new OAuthBackend.OAuthClientBackendRegistration(
				config.backendOAuthClientIdentifier(), List.of("openid", "profile"));
		this.refreshToken = config.refreshToken();

		if (!config.enabled()) {
			return;
		}

		this.bridgeHttp = this.bridgeHttpFactory.get();
		this.bridgeHttp.setDebugMode(config.debugMode());

		if (isNullOrBlank(config.accessToken())) {
			this._setOAuthConnectionState(ConnectionState.NOT_CONNECTED);
			return;
		}

		final var luoxService = this.bridgeHttp.createService(this.httpBridgeLuoxServiceDefinition);

		luoxService.queryContractDetails(config.accessToken(), this::refreshTokens, this::onServerError)
				.thenAccept(contractDetailsResponse -> {
					this._setOAuthConnectionState(ConnectionState.CONNECTED);
					this._setStatusServerError(false);

					if (contractDetailsResponse.details().isEmpty()) {
						return;
					}
					final var contractId = contractDetailsResponse.details().getFirst().saasContractId();
					luoxService.subscribeToPrices(config.accessToken(), contractId, this::refreshTokens,
							this::onServerError, response -> {
								this._setStatusServerError(false);
								this.prices.set(response.toTimeOfUsePrices());
							});
				});
	}

	@Deactivate
	@Override
	protected void deactivate() {
		super.deactivate();
		this.bridgeHttpFactory.unget(this.bridgeHttp);
		this.bridgeHttp = null;
		ThreadPoolUtils.shutdownAndAwaitTermination(this.executorService, 0);
	}

	@Override
	public TimeOfUsePrices getPrices() {
		return TimeOfUsePrices.from(ZonedDateTime.now(this.componentManager.getClock()), this.prices.get());
	}

	@Override
	public OAuthCore.OAuthMetaInfo getMetaInfo() {
		return new OAuthCore.OAuthMetaInfo(//
				this.id(), //
				this.alias(), //
				this.alias() //
		);
	}

	@Override
	public CompletableFuture<InitiateOAuthConnect.Response> initiateConnect() {
		final var codeVerifier = generateCodeVerifier();
		final var codeChallenge = generateCodeChallenge(codeVerifier);

		final var state = UUID.randomUUID().toString();
		this.lastConnectionStateInitiator = new InitiatedConnectState(state, codeVerifier);

		return this.authBackend.getInitMetadata(this.oAuthClientBackendRegistration.identifier())
				.thenApply(metadata -> {
					if (metadata == null) {
						throw new RuntimeException(
								"No metadata found for identifier " + this.oAuthClientBackendRegistration.identifier());
					}
					return new InitiateOAuthConnect.Response(//
							metadata.authenticationUrl(), //
							metadata.clientId(), //
							metadata.redirectUrl(), //
							this.oAuthClientBackendRegistration.scopes(), //
							state, //
							codeChallenge, //
							"S256" //
					);
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

		return this.authBackend.fetchTokensFromCode(this.oAuthClientBackendRegistration, code,
				this.lastConnectionStateInitiator.codeVerifier()).handle((tokens, error) -> {
					if (error != null) {
						this.log.error("Unable to fetch tokens", error);
						this._setOAuthConnectionState(ConnectionState.NOT_CONNECTED);
						return null;
					}
					this.updateConfiguration(tokens.accessToken(), tokens.refreshToken());
					this._setOAuthConnectionState(ConnectionState.CONNECTED);

					return null;
				});
	}

	@Override
	public void disconnect() {
		this.updateConfiguration("", "");
		this._setOAuthConnectionState(ConnectionState.NOT_CONNECTED);
	}

	private void refreshTokens() {
		this.refreshTokens(1);
	}

	private void refreshTokens(int tryCount) {
		this.authBackend.fetchTokensFromRefreshToken(this.oAuthClientBackendRegistration, this.refreshToken)
				.whenComplete((oAuthTokens, throwable) -> {
					if (throwable != null) {
						this.log.warn("Unable to refresh tokens on try {}/{}", tryCount, MAX_REFRESH_TRIES, throwable);

						if (tryCount < MAX_REFRESH_TRIES) {
							this.executorService.schedule(() -> {
								this.refreshTokens(tryCount + 1);
							}, 5, TimeUnit.MINUTES);
							return;
						}

						this.updateConfiguration("", "");
						this._setOAuthConnectionState(ConnectionState.EXPIRED);
						return;
					}

					this.updateConfiguration(oAuthTokens.accessToken(), oAuthTokens.refreshToken());
				});
	}

	private void onServerError() {
		this._setStatusServerError(true);
	}

	private void updateConfiguration(String accessToken, String refreshToken) {
		try {
			this.log.info("Update tokens");
			final var configuration = this.configurationAdmin.getConfiguration(this.servicePid(), "?");

			final var props = configuration.getProperties();
			if (accessToken != null) {
				props.put("accessToken", accessToken);
			}
			if (refreshToken != null) {
				props.put("refreshToken", refreshToken);
			}
			configuration.update(props);
		} catch (IOException e) {
			this.log.warn("Unable to update configuration", e);
		}
	}

}
