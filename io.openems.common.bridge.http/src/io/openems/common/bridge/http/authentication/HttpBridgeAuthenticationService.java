package io.openems.common.bridge.http.authentication;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpEventDefinition;
import io.openems.common.bridge.http.api.BridgeHttpEventListener;
import io.openems.common.bridge.http.api.HttpBridgeService;
import io.openems.common.bridge.http.api.HttpBridgeServiceDefinition;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.function.Disposable;
import io.openems.common.types.DebugMode;

public class HttpBridgeAuthenticationService<C> implements HttpBridgeService, BridgeHttp {

	private final Logger log = LoggerFactory.getLogger(HttpBridgeAuthenticationService.class);

	private final BridgeHttp bridgeHttp;
	private final Map<HttpBridgeServiceDefinition<?>, HttpBridgeServiceDefinition<?>> mappedDefinitions = new ConcurrentHashMap<>();

	private final HttpBridgeAuthenticationServiceConfig<C> config;

	private volatile CompletableFuture<C> loginFuture = CompletableFuture.failedFuture(new RuntimeException());

	public HttpBridgeAuthenticationService(//
			BridgeHttp bridgeHttp, //
			HttpBridgeAuthenticationServiceConfig<C> config //
	) {
		this.bridgeHttp = bridgeHttp;
		this.config = config;
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

	private CompletableFuture<C> authenticate() {
		var currentFuture = this.loginFuture;
		if (!currentFuture.isDone()) {
			return currentFuture;
		}

		synchronized (this) {
			currentFuture = this.loginFuture;
			if (!currentFuture.isDone()) {
				return currentFuture;
			}

			this.loginFuture = this.config.fetchAuthHeader().orTimeout(5, TimeUnit.MINUTES) //
					.whenComplete((s, throwable) -> {
						if (throwable != null) {
							this.raiseEvent(HttpBridgeAuthenticationEvents.AUTHENTICATION_FAILED, null);
							return;
						}
						this.raiseEvent(HttpBridgeAuthenticationEvents.AUTHENTICATION_SUCCESS, null);
					});

			return this.loginFuture;
		}
	}

	private CompletableFuture<HttpResponse<String>> sendAuthenticatedRequest(Endpoint endpoint) {
		return this.loginFuture.exceptionallyCompose(t -> this.authenticate()) //
				.thenCompose(
						authParams -> this.bridgeHttp.request(this.config.applyAuthentication(endpoint, authParams)) //
								.thenCompose(response -> {
									// retry once if authentication failed for success response
									// may happen for apis which always return 200/OK and then have an error in the
									// body
									if (this.config.isSessionExpired(response, null)) {
										this.log.info("Session expired, re-authenticating and retrying request {}",
												response);
										return this.authenticate() //
												.thenCompose(t -> this.bridgeHttp
														.request(this.config.applyAuthentication(endpoint, t)));
									}

									return CompletableFuture.completedFuture(response);
								}) //
								.exceptionallyCompose(throwable -> {
									// retry once if authentication failed
									if (this.config.isSessionExpired(null,
											throwable instanceof CompletionException ce ? ce.getCause() : throwable)) {
										this.log.info("Session expired, re-authenticating and retrying request",
												throwable);
										return this.authenticate() //
												.thenCompose(t -> this.bridgeHttp
														.request(this.config.applyAuthentication(endpoint, t)));
									}

									return CompletableFuture.failedFuture(throwable);
								}));
	}

}
