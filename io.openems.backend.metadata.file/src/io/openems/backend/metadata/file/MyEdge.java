package io.openems.backend.metadata.file;

import com.google.gson.JsonObject;
import io.openems.backend.common.metadata.Edge;

public class MyEdge extends Edge {

	private final String apikey;
	private final String setupPassword;
	private JsonObject settings;

	public MyEdge(MetadataFile parent, String id, String apikey, String setupPassword, String comment, String version,
			String producttype, JsonObject settings) {
		super(parent, id, comment, version, producttype, null);
		this.apikey = apikey;
		this.setupPassword = setupPassword;
		this.settings = settings;
	}

	public String getApikey() {
		return this.apikey;
	}

	public String getSetupPassword() {
		return this.setupPassword;
	}

	public JsonObject getSettings() {
		return this.settings;
	}

	public void setSettings(JsonObject settings) {
		this.settings = settings;
	}
}
