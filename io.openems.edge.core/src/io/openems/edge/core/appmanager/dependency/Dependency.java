package io.openems.edge.core.appmanager.dependency;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.AppManager;

/**
 * Represents a dependency in the configuration of the {@link AppManager} of an
 * app.
 * 
 */
public class Dependency {

	public final String key;

	public final UUID instanceId;

	public Dependency(String key, UUID instanceId) {
		this.key = key;
		this.instanceId = instanceId;
	}

	public JsonObject toJsonObject() {
		return JsonUtils.buildJsonObject() //
				.addProperty("key", key) //
				.addProperty("instanceId", instanceId.toString()) //
				.build();
	}

}
