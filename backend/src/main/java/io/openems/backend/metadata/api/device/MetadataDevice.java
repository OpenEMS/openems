package io.openems.backend.metadata.api.device;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;

public interface MetadataDevice {

	final static Pattern NAME_NUMBER_PATTERN = Pattern.compile("[^0-9]+([0-9]+)$");

	Integer getId();

	public static Optional<Integer> parseNumberFromName(String name) {
		Matcher matcher = NAME_NUMBER_PATTERN.matcher(name);
		if (matcher.find()) {
			String nameNumberString = matcher.group(1);
			return Optional.ofNullable(Integer.parseInt(nameNumberString));
		}
		return Optional.empty();
	}

	public default Optional<Integer> getNameNumber() {
		return MetadataDevice.parseNumberFromName(this.getName());
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
		j.addProperty("id", this.getId());
		// j.add("openemsConfig", this.getOpenemsConfig());
		j.addProperty("productType", this.getProductType());
		j.addProperty("state", this.getState());
		return j;
	}
}