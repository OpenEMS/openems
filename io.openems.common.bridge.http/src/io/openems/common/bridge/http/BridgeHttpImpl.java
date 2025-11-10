package io.openems.common.bridge.http;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceScope;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpExecutor;
import io.openems.common.bridge.http.api.EndpointFetcher;
import io.openems.common.bridge.http.api.HttpBridgeService;
import io.openems.common.bridge.http.api.HttpBridgeServiceDefinition;
import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.types.DebugMode;

@Component(//
		scope = ServiceScope.PROTOTYPE //
)
public class BridgeHttpImpl implements BridgeHttp {

	private final Logger log = LoggerFactory.getLogger(BridgeHttpImpl.class);

	private final EndpointFetcher urlFetcher;
	private final BridgeHttpExecutor pool;
	private final Map<HttpBridgeServiceDefinition<?>, HttpBridgeService> services = new ConcurrentHashMap<>();

	private DebugMode debugMode = DebugMode.OFF;

	@Activate
	public BridgeHttpImpl(//
			@Reference final EndpointFetcher urlFetcher, //
			@Reference(scope = ReferenceScope.PROTOTYPE_REQUIRED) final BridgeHttpExecutor pool //
	) {
		super();
		this.urlFetcher = urlFetcher;
		this.pool = pool;
	}

	@Override
	@SuppressWarnings("unchecked")
	public synchronized <T extends HttpBridgeService> T createService(
			HttpBridgeServiceDefinition<T> serviceDefinition) {
		final var existingService = this.services.get(serviceDefinition);
		if (existingService != null) {
			return (T) existingService;
		}
		final var service = serviceDefinition.create(this, this.pool, this.urlFetcher);
		this.services.put(serviceDefinition, service);
		return service;
	}

	/**
	 * Deactivate method.
	 */
	@Deactivate
	public void deactivate() {
		for (var value : this.services.values()) {
			try {
				value.close();
			} catch (Exception e) {
				this.log.warn("Error during deactivation of HttpBridgeService {}", value, e);
			}
		}
	}

	@Override
	public void setDebugMode(DebugMode debugMode) {
		this.debugMode = debugMode;
	}

	@Override
	public DebugMode getDebugMode() {
		return this.debugMode;
	}

	@Override
	public CompletableFuture<HttpResponse<String>> request(Endpoint endpoint) {
		final var future = new CompletableFuture<HttpResponse<String>>();
		this.pool.execute(() -> {
			try {
				final var result = this.urlFetcher.fetchEndpoint(endpoint, this.debugMode);
				future.complete(result);
			} catch (HttpError e) {
				future.completeExceptionally(e);
			} catch (Exception e) {
				future.completeExceptionally(new HttpError.UnknownError(e));
			}
		});
		return future;
	}

}
