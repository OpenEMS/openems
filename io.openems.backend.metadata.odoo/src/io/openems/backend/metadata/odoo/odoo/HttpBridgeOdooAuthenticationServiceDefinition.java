package io.openems.backend.metadata.odoo.odoo;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpExecutor;
import io.openems.common.bridge.http.api.EndpointFetcher;
import io.openems.common.bridge.http.api.HttpBridgeServiceDefinition;

public record HttpBridgeOdooAuthenticationServiceDefinition(Credentials credentials, OdooHandler odooHandler)
		implements HttpBridgeServiceDefinition<HttpBridgeOdooAuthenticationService> {

	@Override
	public HttpBridgeOdooAuthenticationService create(BridgeHttp bridgeHttp, BridgeHttpExecutor executor,
			EndpointFetcher endpointFetcher) {
		return new HttpBridgeOdooAuthenticationService(this.credentials, bridgeHttp, this.odooHandler);
	}

}
