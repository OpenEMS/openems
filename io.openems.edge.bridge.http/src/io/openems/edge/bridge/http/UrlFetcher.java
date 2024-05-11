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

}
