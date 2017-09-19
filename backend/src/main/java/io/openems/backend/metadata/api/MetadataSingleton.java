package io.openems.backend.metadata.api;

import io.openems.backend.browserwebsocket.session.BrowserSession;
import io.openems.backend.metadata.api.device.MetadataDeviceModel;
import io.openems.common.exceptions.OpenemsException;

public interface MetadataSingleton {
	public void getInfoWithSession(BrowserSession session) throws OpenemsException;

	public MetadataDeviceModel getDeviceModel();
}
