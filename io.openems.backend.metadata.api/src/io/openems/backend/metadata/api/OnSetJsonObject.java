package io.openems.backend.metadata.api;

import com.google.gson.JsonObject;

public interface OnSetJsonObject {
	public void call(JsonObject config);
}
