package io.openems.backend.metadata.odoo.odoo;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpExecutor;
import io.openems.common.bridge.http.api.EndpointFetcher;
import io.openems.common.bridge.http.api.HttpBridgeServiceDefinition;

public record HttpBridgeOdooServiceDefinition(Credentials credentials)
		implements HttpBridgeServiceDefinition<HttpBridgeOdooService> {

	@Override
	public HttpBridgeOdooService create(//
			BridgeHttp bridgeHttp, //
			BridgeHttpExecutor executor, //
			EndpointFetcher endpointFetcher //
	) {
		return new HttpBridgeOdooService(this.credentials, bridgeHttp);
	}

}
