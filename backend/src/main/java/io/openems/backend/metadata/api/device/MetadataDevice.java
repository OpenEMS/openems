package io.openems.backend.metadata.api.device;

import java.util.Optional;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.Device;

public interface MetadataDevice extends Device {

	@Override
	public default Optional<Integer> getIdOpt() {
		return Device.parseNumberFromName(this.getName());
	}

	String getName();

	String getComment();

	String getState();

	String getProductType();

	JsonObject getOpenemsConfig();

	void setOpenemsConfig(JsonObject j);

	void setState(String active);

	void setSoc(int value);

	void setLastMessage();

	void setLastUpdate();

	void setIpV4(String value);

	void writeObject() throws OpenemsException;

	public default JsonObject toJsonObject() {
		JsonObject j = new JsonObject();
		j.addProperty("name", this.getName());
		j.addProperty("comment", this.getComment());
		j.addProperty("id", this.getIdOpt().orElse(0));
		// j.add("openemsConfig", this.getOpenemsConfig());
		j.addProperty("productType", this.getProductType());
		j.addProperty("state", this.getState());
		return j;
	}
}