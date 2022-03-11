package io.openems.edge.core.appmanager;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.utils.JsonUtils;

/**
 * An {@link OpenemsAppInstance} is one instance of an {@link OpenemsApp} with a
 * specific configuration.
 */
public class OpenemsAppInstance {

	public final String appId;
	public final UUID instanceId;
	public final JsonObject properties;

	public OpenemsAppInstance(String appId, UUID instanceId, JsonObject properties) {
		this.appId = appId;
		this.instanceId = instanceId;
		this.properties = properties;
	}

	/**
	 * Gets this {@link OpenemsAppInstance} as {@link JsonObject}.
	 *
	 * @return the {@link JsonObject}
	 */
	public JsonObject toJsonObject() {
		return JsonUtils.buildJsonObject() //
				.addProperty("appId", this.appId) //
				.addProperty("instanceId", this.instanceId.toString()) //
				.add("properties", this.properties) //
				.build();
	}

}
