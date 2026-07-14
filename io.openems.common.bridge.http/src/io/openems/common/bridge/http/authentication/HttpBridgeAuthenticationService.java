package io.openems.common.bridge.http.authentication;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
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
	private final Map<HttpBridgeServiceDefinition<?>, HttpBridgeServiceDefinition<?>> mappedDefinitions = new ConcurrentHashMap<>();

	private final Supplier<CompletableFuture<HttpHeader>> authHeaderSupplier;
	private final Predicate<Throwable> sessionExpiredPredicate;

	private volatile CompletableFuture<HttpHeader> loginFuture = CompletableFuture.failedFuture(new RuntimeException());

	public HttpBridgeAuthenticationService(//
			BridgeHttp bridgeHttp, //
			Supplier<CompletableFuture<HttpHeader>> authHeaderSupplier, //
			Predicate<Throwable> sessionExpiredPredicate //
	) {
		this.bridgeHttp = bridgeHttp;
		this.authHeaderSupplier = authHeaderSupplier;
		this.sessionExpiredPredicate = sessionExpiredPredicate;
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
		return this.bridgeHttp.createService(this.getMappedServiceDefinition(serviceDefinition));
	}

	@SuppressWarnings("unchecked")
	private <T extends HttpBridgeService> HttpBridgeServiceDefinition<T> getMappedServiceDefinition(
			HttpBridgeServiceDefinition<T> serviceDefinition //
	) {
		return (HttpBridgeServiceDefinition<T>) this.mappedDefinitions.computeIfAbsent(serviceDefinition,
				s -> (ignoredHttpBridge, executor, endpointFetcher) -> serviceDefinition.create(this, executor,
						endpointFetcher));
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

	private CompletableFuture<HttpHeader> authenticateAsAdmin() {
		var currentFuture = this.loginFuture;
		if (!currentFuture.isDone()) {
			return currentFuture;
		}

		synchronized (this) {
			currentFuture = this.loginFuture;
			if (!currentFuture.isDone()) {
				return currentFuture;
			}

			return this.loginFuture = this.authHeaderSupplier.get().orTimeout(5, TimeUnit.MINUTES) //
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
				.thenCompose(header -> this.bridgeHttp.request(addAuthHeader(endpoint, header))
						.exceptionallyCompose(throwable -> {

							// retry once if authentication failed
							if (this.sessionExpiredPredicate.test(throwable)) {
								this.log.info("Session expired, re-authenticating and retrying request", throwable);
								return this.authenticateAsAdmin() //
										.thenCompose(s -> this.bridgeHttp.request(addAuthHeader(endpoint, s)));
							}

							return CompletableFuture.failedFuture(throwable);
						}));
	}

	private static Endpoint addAuthHeader(Endpoint endpoint, HttpHeader authToken) {
		return endpoint.toBuilder() //
				.setHeader(authToken) //
				.build();
	}

}
