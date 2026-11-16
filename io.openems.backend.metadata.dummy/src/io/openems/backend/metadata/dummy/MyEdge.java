package io.openems.backend.metadata.dummy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.JsonObject;

import io.openems.backend.common.metadata.Edge;

public class MyEdge extends Edge {

	private String apikey;
	private String setupPassword;
	private JsonObject settings;
	private String locationId;
	private List<String> groupIds = new ArrayList<>();

	public MyEdge(MetadataDummy parent, String id, String apikey, String setupPassword, String comment, String version,
			String producttype, JsonObject settings, String locationId, List<String> groupIds) {
		super(parent, id, comment, version, producttype, null);
		this.apikey = apikey;
		this.setupPassword = setupPassword;
		this.settings = settings;
		this.locationId = locationId;
		if (groupIds != null) {
			this.groupIds = new ArrayList<>(groupIds);
		}
	}

	public MyEdge(MetadataDummy parent, String id, String apikey, String setupPassword, String comment, String version,
			String producttype, JsonObject settings) {
		this(parent, id, apikey, setupPassword, comment, version, producttype, settings, null, Collections.emptyList());
	}

	public void update(String apikey, String setupPassword, String comment, JsonObject settings, String locationId,
			List<String> groupIds) {
		this.apikey = apikey;
		this.setupPassword = setupPassword;
		if (comment != null) {
			this.setComment(comment);
		}
		this.settings = settings;
		this.locationId = locationId;
		this.groupIds = groupIds != null ? new ArrayList<>(groupIds) : new ArrayList<>();
	}

	public void update(String apikey, String setupPassword, String comment, JsonObject settings) {
		this.update(apikey, setupPassword, comment, settings, this.locationId, this.groupIds);
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

	public String getLocationId() {
		return this.locationId;
	}

	public void setLocationId(String locationId) {
		this.locationId = locationId;
	}

	public List<String> getGroupIds() {
		return Collections.unmodifiableList(this.groupIds);
	}

	public void setGroupIds(List<String> groupIds) {
		this.groupIds = groupIds != null ? new ArrayList<>(groupIds) : new ArrayList<>();
	}
}
