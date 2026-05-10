package io.openems.edge.battery.bmw;

import java.time.Duration;
import java.time.Instant;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceScope;
import org.osgi.service.component.annotations.ServiceScope;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttp.Endpoint;
import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.time.DelayTimeProvider;
import io.openems.common.bridge.http.time.DelayTimeProviderChain;
import io.openems.common.bridge.http.time.HttpBridgeTimeService;
import io.openems.common.bridge.http.time.HttpBridgeTimeServiceDefinition;
import io.openems.common.types.HttpStatus;

@Component(scope = ServiceScope.SINGLETON, service = BmwToken.class)
public class BmwToken {
	private static final int FETCH_TOKEN_DELAY = 30;
	private static final long DEFAULT_TOKEN_LIFETIME_HOURS = 24;

	private String token;
	private Instant tokenExpiration;
	private HttpBridgeTimeService timeService;

	@Activate
	public BmwToken(@Reference(scope = ReferenceScope.PROTOTYPE_REQUIRED) BridgeHttp http) {
		this.timeService = http.createService(HttpBridgeTimeServiceDefinition.INSTANCE);
	}

	/**
	 * Fetch the token.
	 * 
	 * @param endpoint the endpoint
	 */
	public synchronized void fetchToken(Endpoint endpoint) {
		if (this.isTokenValid()) {
			return; // Token is still valid, no need to refresh
		}

		this.timeService.subscribeJsonTime(new BmwTokenDelayProvider(), endpoint, (a, b) -> {
			if (a == null) {
				return;
			}
			this.parseAndSetToken(a.data().getAsJsonObject());
		});
	}

	public String getToken() {
		return this.token;
	}

	/**
	 * Parse token and lifetime from JSON response and set internal state.
	 * 
	 * @param jsonObject the JSON response containing token information
	 */
	private void parseAndSetToken(com.google.gson.JsonObject jsonObject) {
		this.token = jsonObject.getAsJsonPrimitive("jwtToken").getAsString();

		var lifetimeHours = DEFAULT_TOKEN_LIFETIME_HOURS;
		if (jsonObject.has("lifetimeHours")) {
			lifetimeHours = jsonObject.getAsJsonPrimitive("lifetimeHours").getAsLong();
		} else if (jsonObject.has("expiresIn")) {
			lifetimeHours = jsonObject.getAsJsonPrimitive("expiresIn").getAsLong() / 3600;
		}

		// Set expiration time with 1 hour buffer
		this.tokenExpiration = Instant.now().plusSeconds(lifetimeHours * 3600 - 3600);
	}

	/**
	 * Check if the current token is still valid.
	 * 
	 * @return true if token exists and has not expired
	 */
	private boolean isTokenValid() {
		return this.token != null && this.tokenExpiration != null && Instant.now().isBefore(this.tokenExpiration);
	}

	private static final class BmwTokenDelayProvider implements DelayTimeProvider {

		@Override
		public Delay onFirstRunDelay() {
			return DelayTimeProviderChain.fixedDelay(Duration.ofSeconds(FETCH_TOKEN_DELAY)).getDelay();
		}

		@Override
		public Delay onErrorRunDelay(HttpError error) {
			return DelayTimeProviderChain.fixedDelay(Duration.ofSeconds(FETCH_TOKEN_DELAY)).getDelay();
		}

		@Override
		public Delay onSuccessRunDelay(HttpResponse<String> result) {
			if (result.status() != HttpStatus.OK) {
				return DelayTimeProviderChain.fixedDelay(Duration.ofSeconds(FETCH_TOKEN_DELAY)).getDelay();
			}
			return DelayTimeProviderChain.runNeverAgain().getDelay();
		}
	}
}