package io.openems.common.bridge.http.authentication;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpExecutor;
import io.openems.common.bridge.http.api.EndpointFetcher;
import io.openems.common.bridge.http.api.HttpAuthorization;
import io.openems.common.bridge.http.api.HttpBridgeServiceDefinition;
import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.types.HttpStatus;

public record HttpBridgeAuthenticationServiceDefinition(//
		Supplier<CompletableFuture<String>> tokenSupplier, //
		Predicate<Throwable> sessionExpiredPredicate, //
		Function<String, String> authenticationHeaderFunction //
) implements HttpBridgeServiceDefinition<HttpBridgeAuthenticationService> {

	public static final Predicate<Throwable> DEFAULT_SESSION_EXPIRED_PREDICATE//
			= t -> t instanceof HttpError.ResponseError httpError
					&& httpError.status.code() == HttpStatus.UNAUTHORIZED.code();

	public static final Function<String, String> DEFAULT_AUTHENTICATION_HEADER_FUNCTION//
			= t -> HttpAuthorization.bearer(t);

	public HttpBridgeAuthenticationServiceDefinition(Supplier<CompletableFuture<String>> tokenSupplier) {
		this(tokenSupplier, DEFAULT_SESSION_EXPIRED_PREDICATE, DEFAULT_AUTHENTICATION_HEADER_FUNCTION);
	}

	public HttpBridgeAuthenticationServiceDefinition(Supplier<CompletableFuture<String>> tokenSupplier,
			Function<String, String> authenticationHeaderFunction) {
		this(tokenSupplier, DEFAULT_SESSION_EXPIRED_PREDICATE, authenticationHeaderFunction);
	}

	@Override
	public HttpBridgeAuthenticationService create(//
			BridgeHttp bridgeHttp, //
			BridgeHttpExecutor executor, //
			EndpointFetcher endpointFetcher //
	) {
		return new HttpBridgeAuthenticationService(bridgeHttp, this.tokenSupplier(), this.sessionExpiredPredicate(),
				this.authenticationHeaderFunction());
	}
}
