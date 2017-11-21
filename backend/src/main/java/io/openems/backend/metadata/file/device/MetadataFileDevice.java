package io.openems.backend.metadata.file.device;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.backend.metadata.api.device.MetadataDevice;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.DeviceImpl;

public class MetadataFileDevice extends DeviceImpl implements MetadataDevice {

	private final Logger log = LoggerFactory.getLogger(MetadataFileDevice.class);

	private final int id;
	private final String apikey;

	public MetadataFileDevice(String name, String comment, String producttype, String role, int id, String apikey) {
		super(name, comment, producttype, role);
		this.id = id;
		this.apikey = apikey;
	}

	@Override
	public Optional<Integer> getIdOpt() {
		return Optional.of(this.id);
	}

	public String getApikey() {
		return apikey;
	}

	@Override
	public String getName() {
		return super.getName();
	}

	@Override
	public String getComment() {
		return super.getComment();
	}

	@Override
	public String getState() {
		return "active";
	}

	@Override
	public String getProductType() {
		return super.getProducttype();
	}

	@Override
	public JsonObject getOpenemsConfig() {
		return new JsonObject();
	}

	@Override
	public void setOpenemsConfig(JsonObject j) {
		log.info("Metadata Dummy. Would set OpenEMS config: " + j.toString());
	}

	@Override
	public void setState(String state) {
		log.info("Metadata Dummy. Would set state: " + state);
	}

	@Override
	public void setSoc(int value) {
		log.info("Metadata Dummy. Would set SOC: " + value);
	}

	@Override
	public void setLastMessage() {
		log.info("Metadata Dummy. Would set LastMessage");
	}

	@Override
	public void setLastUpdate() {
		log.debug("Metadata Dummy. Would set LastUpdate");
	}

	@Override
	public void setIpV4(String value) {
		log.info("Metadata Dummy. Would set IPv4: " + value);
	}

	@Override
	public void writeObject() throws OpenemsException {
		log.debug("Metadata Dummy. Would write object");
	}
}
