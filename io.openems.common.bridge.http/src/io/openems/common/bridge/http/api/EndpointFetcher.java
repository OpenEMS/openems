package io.openems.common.bridge.http.api;

import io.openems.common.types.DebugMode;

public interface EndpointFetcher {

	/**
	 * Fetches the given {@link BridgeHttp.Endpoint}.
	 * 
	 * @param endpoint    the {@link BridgeHttp.Endpoint} to fetch
	 * @param mode        the {@link DebugMode}
	 * @param eventRaiser the {@link BridgeHttpEventRaiser} to raise events on
	 * 
	 * @return the result of the {@link BridgeHttp.Endpoint}
	 * @throws HttpError on error
	 */
	public HttpResponse<String> fetchEndpoint(//
			BridgeHttp.Endpoint endpoint, //
			DebugMode mode, //
			BridgeHttpEventRaiser eventRaiser //
	) throws HttpError;

}
