package io.openems.backend.metadata.dummy;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.browserwebsocket.session.BrowserSession;
import io.openems.backend.browserwebsocket.session.BrowserSessionData;
import io.openems.backend.metadata.api.MetadataSingleton;
import io.openems.backend.metadata.api.device.MetadataDeviceModel;
import io.openems.backend.metadata.dummy.device.MetadataDummyDeviceModel;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.session.SessionData;
import io.openems.common.types.Device;

public class MetadataDummySingleton implements MetadataSingleton {
	private final Logger log = LoggerFactory.getLogger(MetadataDummySingleton.class);

	private MetadataDummyDeviceModel deviceModel;

	public MetadataDummySingleton() {
		this.deviceModel = new MetadataDummyDeviceModel();
	}

	/**
	 * Returns static device data
	 *
	 * @return
	 * @throws OpenemsException
	 */
	@Override
	public void getInfoWithSession(BrowserSession session) throws OpenemsException {
		SessionData sessionData = session.getData();
		BrowserSessionData data = (BrowserSessionData) sessionData;
		data.setUserId(0);
		// Allow access to all available devices
		List<Device> deviceInfos = new ArrayList<>();
		for (Device device : this.deviceModel.getAllDevices()) {
			deviceInfos.add(device);
		}
		data.setDevices(deviceInfos);
		session.setValid();
		return;
	}

	@Override
	public MetadataDeviceModel getDeviceModel() {
		return deviceModel;
	}
}
