package io.openems.common.bridge.http.authentication;

import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.function.Supplier;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpExecutor;
import io.openems.common.bridge.http.api.EndpointFetcher;
import io.openems.common.bridge.http.api.HttpBridgeServiceDefinition;
import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.types.HttpStatus;

public record HttpBridgeAuthenticationServiceDefinition(//
		Supplier<CompletableFuture<String>> tokenSupplier, //
		Predicate<Throwable> sessionExpiredPredicate //
) implements HttpBridgeServiceDefinition<HttpBridgeAuthenticationService> {

	public static final Predicate<Throwable> DEFAULT_SESSION_EXPIRED_PREDICATE//
			= t -> t instanceof HttpError.ResponseError httpError
					&& httpError.status.code() == HttpStatus.UNAUTHORIZED.code();

	public HttpBridgeAuthenticationServiceDefinition(Supplier<CompletableFuture<String>> tokenSupplier) {
		this(tokenSupplier, DEFAULT_SESSION_EXPIRED_PREDICATE);
	}

	@Override
	public HttpBridgeAuthenticationService create(//
			BridgeHttp bridgeHttp, //
			BridgeHttpExecutor executor, //
			EndpointFetcher endpointFetcher //
	) {
		return new HttpBridgeAuthenticationService(bridgeHttp, this.tokenSupplier(), this.sessionExpiredPredicate());
	}
}
