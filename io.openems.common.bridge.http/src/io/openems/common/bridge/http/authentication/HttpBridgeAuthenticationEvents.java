package io.openems.common.bridge.http.authentication;

import io.openems.common.bridge.http.api.BridgeHttpEventDefinition;

public final class HttpBridgeAuthenticationEvents {

	public static final BridgeHttpEventDefinition<Void> AUTHENTICATION_SUCCESS//
			= new BridgeHttpEventDefinition<>();

	public static final BridgeHttpEventDefinition<Void> AUTHENTICATION_FAILED//
			= new BridgeHttpEventDefinition<>();

	private HttpBridgeAuthenticationEvents() {
	}
}
