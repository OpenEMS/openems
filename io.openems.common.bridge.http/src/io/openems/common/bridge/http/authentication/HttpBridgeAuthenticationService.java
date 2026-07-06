package io.openems.common.bridge.http.authentication;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpEventDefinition;
import io.openems.common.bridge.http.api.BridgeHttpEventListener;
import io.openems.common.bridge.http.api.HttpBridgeService;
import io.openems.common.bridge.http.api.HttpBridgeServiceDefinition;
import io.openems.common.bridge.http.api.HttpHeader;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.function.Disposable;
import io.openems.common.types.DebugMode;

public class HttpBridgeAuthenticationService implements HttpBridgeService, BridgeHttp {

	private final Logger log = LoggerFactory.getLogger(HttpBridgeAuthenticationService.class);

	private final BridgeHttp bridgeHttp;
	private final Supplier<CompletableFuture<String>> tokenSupplier;
	private final Predicate<Throwable> sessionExpiredPredicate;
	private final Function<String, String> authenticationHeaderFunction;

	private volatile CompletableFuture<String> loginFuture = CompletableFuture.failedFuture(new RuntimeException());

	public HttpBridgeAuthenticationService(//
			BridgeHttp bridgeHttp, //
			Supplier<CompletableFuture<String>> tokenSupplier, Predicate<Throwable> sessionExpiredPredicate, //
			Function<String, String> authenticationHeaderFunction) {
		this.bridgeHttp = bridgeHttp;
		this.tokenSupplier = tokenSupplier;
		this.sessionExpiredPredicate = sessionExpiredPredicate;
		this.authenticationHeaderFunction = authenticationHeaderFunction;
	}

	@Override
	public void close() {
		// empty
	}

	@Override
	public <T> Disposable subscribeEvent(//
			BridgeHttpEventDefinition<T> eventDefinition, //
			BridgeHttpEventListener<T> listener //
	) {
		return this.bridgeHttp.subscribeEvent(eventDefinition, listener);
	}

	@Override
	public void setMaximumPoolSize(int maximumPoolSize) {
		this.bridgeHttp.setMaximumPoolSize(maximumPoolSize);
	}

	@Override
	public void setDebugMode(DebugMode debugMode) {
		this.bridgeHttp.setDebugMode(debugMode);
	}

	@Override
	public DebugMode getDebugMode() {
		return this.bridgeHttp.getDebugMode();
	}

	@Override
	public <T extends HttpBridgeService> T createService(//
			HttpBridgeServiceDefinition<T> serviceDefinition //
	) {
		return this.bridgeHttp.createService((bridgeHttp, executor, endpointFetcher) -> {
			return serviceDefinition.create(this, executor, endpointFetcher);
		});
	}

	@Override
	public CompletableFuture<HttpResponse<String>> request(Endpoint endpoint) {
		return this.sendAuthenticatedRequest(endpoint);
	}

	@Override
	public Map<String, Long> getMetrics() {
		return this.bridgeHttp.getMetrics();
	}

	@Override
	public <T> void raiseEvent(BridgeHttpEventDefinition<T> eventDefinition, T eventData) {
		this.bridgeHttp.raiseEvent(eventDefinition, eventData);
	}

	private CompletableFuture<String> authenticateAsAdmin() {
		var currentFuture = this.loginFuture;
		if (!currentFuture.isDone()) {
			return currentFuture;
		}

		synchronized (this) {
			currentFuture = this.loginFuture;
			if (!currentFuture.isDone()) {
				return currentFuture;
			}

			return this.loginFuture = this.tokenSupplier.get().orTimeout(5, TimeUnit.MINUTES) //
					.whenComplete((s, throwable) -> {
						if (throwable != null) {
							this.raiseEvent(HttpBridgeAuthenticationEvents.AUTHENTICATION_FAILED, null);
							return;
						}
						this.raiseEvent(HttpBridgeAuthenticationEvents.AUTHENTICATION_SUCCESS, null);
					});
		}
	}

	private CompletableFuture<HttpResponse<String>> sendAuthenticatedRequest(Endpoint endpoint) {
		return this.loginFuture.exceptionallyCompose(t -> this.authenticateAsAdmin()) //
				.thenCompose(token -> this.bridgeHttp
						.request(addAuthToken(endpoint, this.authenticationHeaderFunction.apply(token)))
						.exceptionallyCompose(throwable -> {

							// retry once if authentication failed
							if (this.sessionExpiredPredicate.test(throwable)) {
								this.log.info("Session expired, re-authenticating as admin and retrying request",
										throwable);
								return this.authenticateAsAdmin() //
										.thenCompose(s -> this.bridgeHttp.request(addAuthToken(endpoint,
												this.authenticationHeaderFunction.apply(token))));
							}

							return CompletableFuture.failedFuture(throwable);
						}));
	}

	private static Endpoint addAuthToken(Endpoint endpoint, String authToken) {
		return endpoint.toBuilder() //
				.setHeader(HttpHeader.authorization(authToken)) //
				.build();
	}

}
