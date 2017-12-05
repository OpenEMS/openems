package io.openems.backend.metadata.file;

import java.io.File;
import java.io.IOException;

import com.google.common.collect.LinkedHashMultimap;

import io.openems.backend.browserwebsocket.session.BrowserSession;
import io.openems.backend.browserwebsocket.session.BrowserSessionData;
import io.openems.backend.metadata.api.MetadataSingleton;
import io.openems.backend.metadata.api.device.MetadataDeviceModel;
import io.openems.backend.metadata.file.device.MetadataFileDeviceModel;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.session.SessionData;
import io.openems.common.types.DeviceImpl;

public class MetadataFileSingleton implements MetadataSingleton {
	// private final Logger log = LoggerFactory.getLogger(MetadataDummySingleton.class);

	private MetadataFileDeviceModel deviceModel;

	public MetadataFileSingleton(File file) throws IOException {
		this.deviceModel = new MetadataFileDeviceModel(file);
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
		// Devices can have the same name, that's why we use a Multimap.
		LinkedHashMultimap<String, DeviceImpl> deviceMap = LinkedHashMultimap.create();
		for (DeviceImpl device : this.deviceModel.getAllDevices()) {
			deviceMap.put(device.getName(), device);
		}
		data.setDevices(deviceMap);
		return;
	}

	@Override
	public MetadataDeviceModel getDeviceModel() {
		return deviceModel;
	}
}
