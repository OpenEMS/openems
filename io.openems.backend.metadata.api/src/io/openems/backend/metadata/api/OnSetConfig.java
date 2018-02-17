package io.openems.backend.metadata.api;

import com.google.gson.JsonObject;

public interface OnSetConfig {
	public void call(JsonObject config);
}
