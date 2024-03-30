package io.openems.edge.bridge.http;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.bridge.http.api.BridgeHttp.Endpoint;

public interface UrlFetcher {

	/**
	 * Creates a {@link Runnable} to execute a request with the given parameters.
	 * 
	 * @param endpoint the {@link Endpoint} to fetch
	 * 
	 * @return the result of the {@link Endpoint}
	 * @throws OpenemsNamedException on error
	 */
	public String fetchEndpoint(Endpoint endpoint) throws OpenemsNamedException;

	/**
	 * Executes an HTTP request for the given endpoint and returns the raw response
	 * body as a byte array.
	 * 
	 * @param endpoint the {@link Endpoint} to fetch
	 * @return the raw response body as a byte array
	 * @throws OpenemsNamedException on error
	 */
	public byte[] fetchEndpointRaw(Endpoint endpoint) throws OpenemsNamedException;

}
