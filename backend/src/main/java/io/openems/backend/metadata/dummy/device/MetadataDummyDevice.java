package io.openems.backend.metadata.dummy.device;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetadataDummyDevice {

	private final Logger log = LoggerFactory.getLogger(MetadataDummyDevice.class);

	private final int id;
	private final String apikey;

	public MetadataDummyDevice(String name, String comment, String producttype, String role, int id, String apikey) {
		this.id = id;
		this.apikey = apikey;
	}

	public String getApikey() {
		return apikey;
	}

	public String getState() {
		return "active";
	}

	// @Override
	// public void setOpenemsConfig(JsonObject j) {
	// log.info("Metadata Dummy. Would set OpenEMS config: " + j.toString());
	// }
	//
	// @Override
	// public void setState(String state) {
	// log.info("Metadata Dummy. Would set state: " + state);
	// }
	//
	// @Override
	// public void setSoc(int value) {
	// log.info("Metadata Dummy. Would set SOC: " + value);
	// }
	//
	// @Override
	// public void setLastMessage() {
	// log.info("Metadata Dummy. Would set LastMessage");
	// }
	//
	// @Override
	// public void setLastUpdate() {
	// log.debug("Metadata Dummy. Would set LastUpdate");
	// }
	//
	// @Override
	// public void setIpV4(String value) {
	// log.info("Metadata Dummy. Would set IPv4: " + value);
	// }
	//
	// @Override
	// public void writeObject() throws OpenemsException {
	// log.debug("Metadata Dummy. Would write object");
	// }
}
