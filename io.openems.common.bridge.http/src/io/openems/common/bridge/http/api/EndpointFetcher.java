package io.openems.common.bridge.http.api;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.DebugMode;

public interface EndpointFetcher {

	/**
	 * Creates a {@link Runnable} to execute a request with the given parameters.
	 * 
	 * @param endpoint the {@link BridgeHttp.Endpoint} to fetch
	 * @param mode     the {@link DebugMode}
	 * 
	 * @return the result of the {@link BridgeHttp.Endpoint}
	 * @throws OpenemsNamedException on error
	 */
	public HttpResponse<String> fetchEndpoint(BridgeHttp.Endpoint endpoint, DebugMode mode) throws HttpError;

}
