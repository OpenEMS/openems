package io.openems.edge.bridge.http.api;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.bridge.http.api.BridgeHttp.Endpoint;

public interface EndpointFetcher {

	/**
	 * Creates a {@link Runnable} to execute a request with the given parameters.
	 * 
	 * @param endpoint the {@link Endpoint} to fetch
	 * 
	 * @return the result of the {@link Endpoint}
	 * @throws OpenemsNamedException on error
	 */
	public HttpResponse<String> fetchEndpoint(Endpoint endpoint) throws HttpError;

	/**
	 * Executes an HTTP request for the given endpoint and returns the raw response
	 * body as a byte array.
	 * 
	 * @param endpoint the {@link Endpoint} to fetch
	 * @return the raw response body as a byte array
	 * @throws OpenemsNamedException on error
	 */
	public byte[] fetchEndpointRaw(Endpoint endpoint) throws HttpError;

}
