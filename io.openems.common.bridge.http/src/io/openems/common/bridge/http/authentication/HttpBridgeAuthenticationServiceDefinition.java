package io.openems.common.bridge.http.authentication;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpExecutor;
import io.openems.common.bridge.http.api.EndpointFetcher;
import io.openems.common.bridge.http.api.HttpBridgeServiceDefinition;
import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpHeader;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.types.HttpStatus;

public record HttpBridgeAuthenticationServiceDefinition<T>(//
		HttpBridgeAuthenticationServiceConfig<T> config //
) implements HttpBridgeServiceDefinition<HttpBridgeAuthenticationService<T>> {

	public static final BiPredicate<HttpResponse<String>, Throwable> DEFAULT_SESSION_EXPIRED_PREDICATE//
			= (response, t) -> t instanceof HttpError.ResponseError httpError
					&& httpError.status.code() == HttpStatus.UNAUTHORIZED.code();

	public record HttpBridgeAuthenticationServiceConfigHttpHeader(//
			Supplier<CompletableFuture<HttpHeader>> authHeaderSupplier, //
			BiPredicate<HttpResponse<String>, Throwable> sessionExpired//
	) implements HttpBridgeAuthenticationServiceConfig<HttpHeader> {

		@Override
		public CompletableFuture<HttpHeader> fetchAuthHeader() {
			return this.authHeaderSupplier.get();
		}

		@Override
		public BridgeHttp.Endpoint applyAuthentication(BridgeHttp.Endpoint endpoint, HttpHeader authParams) {
			return endpoint.toBuilder() //
					.setHeader(authParams) //
					.build();
		}

		@Override
		public boolean isSessionExpired(HttpResponse<String> response, Throwable error) {
			return this.sessionExpired.test(response, error);
		}
	}

	/**
	 * Creates a simple {@link HttpBridgeAuthenticationServiceDefinition} for a
	 * {@link HttpHeader}.
	 * 
	 * @param authHeaderSupplier the header supplier
	 * @return the {@link HttpBridgeAuthenticationServiceDefinition}
	 */
	public static HttpBridgeAuthenticationServiceDefinition<HttpHeader> of(//
			Supplier<CompletableFuture<HttpHeader>> authHeaderSupplier //
	) {
		return new HttpBridgeAuthenticationServiceDefinition<>(new HttpBridgeAuthenticationServiceConfigHttpHeader(
				authHeaderSupplier, DEFAULT_SESSION_EXPIRED_PREDICATE));
	}

	@Override
	public HttpBridgeAuthenticationService<T> create(//
			BridgeHttp bridgeHttp, //
			BridgeHttpExecutor executor, //
			EndpointFetcher endpointFetcher //
	) {
		return new HttpBridgeAuthenticationService<>(bridgeHttp, this.config());
	}
}
