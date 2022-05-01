package io.openems.edge.core.appmanager;

import java.util.Objects;
import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.utils.JsonUtils;

/**
 * An {@link OpenemsAppInstance} is one instance of an {@link OpenemsApp} with a
 * specific configuration.
 */
public class OpenemsAppInstance {

	public final String appId;
	public final String alias;
	public final UUID instanceId;
	public final JsonObject properties;

	public OpenemsAppInstance(String appId, String alias, UUID instanceId, JsonObject properties) {
		this.appId = appId;
		this.alias = alias;
		this.instanceId = instanceId;
		this.properties = properties;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || this.getClass() != obj.getClass()) {
			return false;
		}
		var other = (OpenemsAppInstance) obj;
		return Objects.equals(this.instanceId, other.instanceId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.instanceId);
	}

	/**
	 * Gets this {@link OpenemsAppInstance} as {@link JsonObject}.
	 *
	 * @return the {@link JsonObject}
	 */
	public JsonObject toJsonObject() {
		return JsonUtils.buildJsonObject() //
				.addProperty("appId", this.appId) //
				.addProperty("alias", this.alias) //
				.addProperty("instanceId", this.instanceId.toString()) //
				.add("properties", this.properties) //
				.build();
	}

}
