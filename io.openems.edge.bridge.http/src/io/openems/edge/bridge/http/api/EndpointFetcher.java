package io.openems.edge.bridge.http.api;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.DebugMode;
import io.openems.edge.bridge.http.api.BridgeHttp.Endpoint;

public interface EndpointFetcher {

	/**
	 * Creates a {@link Runnable} to execute a request with the given parameters.
	 * 
	 * @param endpoint the {@link Endpoint} to fetch
	 * @param mode     the {@link DebugMode}
	 * 
	 * @return the result of the {@link Endpoint}
	 * @throws OpenemsNamedException on error
	 */
	public HttpResponse<String> fetchEndpoint(Endpoint endpoint, DebugMode mode) throws HttpError;

}
