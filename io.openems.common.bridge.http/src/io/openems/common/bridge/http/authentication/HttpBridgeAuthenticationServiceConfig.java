package io.openems.common.bridge.http.authentication;

import java.util.concurrent.CompletableFuture;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.HttpResponse;

public interface HttpBridgeAuthenticationServiceConfig<T> {

	/**
	 * Fetches the authentication header to be used for requests.
	 * 
	 * @return the header to add to the request
	 */
	CompletableFuture<T> fetchAuthHeader();

	/**
	 * Applies the authentication params to the endpoint.
	 * 
	 * @param endpoint   the endpoint which is called
	 * @param authParams the auth params from {@link #fetchAuthHeader()}
	 * @return the new {@link io.openems.common.bridge.http.api.BridgeHttp.Endpoint}
	 *         with the auth params applied
	 */
	BridgeHttp.Endpoint applyAuthentication(BridgeHttp.Endpoint endpoint, T authParams);

	/**
	 * Checks if the session has expired based on the response and error. This is
	 * used to determine if a re-authentication is needed. Either
	 * {@link HttpResponse} or {@link Throwable} can be null, but not both.
	 * 
	 * @param response the request response, can be null if the request failed
	 * @param error    the request error, can be null if the request succeeded
	 * @return true if the session expired; else false
	 */
	boolean isSessionExpired(HttpResponse<String> response, Throwable error);

}