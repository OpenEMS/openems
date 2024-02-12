package io.openems.edge.bridge.http;

import io.openems.edge.bridge.http.api.BridgeHttp.Endpoint;

public interface UrlFetcher {

	/**
	 * Creates a {@link Runnable} to execute a request with the given parameters.
	 * 
	 * @param endpoint the {@link Endpoint} to fetch
	 * 
	 * @return the {@link Runnable} to run to execute the fetch
	 * @throws Exception on error
	 */
	public String fetchEndpoint(Endpoint endpoint) throws Exception;

}
