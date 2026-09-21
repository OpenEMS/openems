package io.openems.edge.phoenixcontact.plcnext.ess;

import io.openems.common.bridge.http.dummy.DummyBridgeHttp;

public class PlcNextDummyBridgeHttp extends DummyBridgeHttp {

	final String accessToken;

	public PlcNextDummyBridgeHttp(String accessToken) {
		this.accessToken = accessToken;
	}
}
