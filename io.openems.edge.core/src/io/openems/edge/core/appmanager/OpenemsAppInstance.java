package io.openems.edge.core.appmanager;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.dependency.Dependency;

/**
 * An {@link OpenemsAppInstance} is one instance of an {@link OpenemsApp} with a
 * specific configuration.
 */
public class OpenemsAppInstance {

	public final String appId;
	public final String alias;
	public final UUID instanceId;
	public final JsonObject properties;
	public final List<Dependency> dependencies;

	public OpenemsAppInstance(String appId, String alias, UUID instanceId, JsonObject properties,
			List<Dependency> dependencies) {
		this.appId = appId;
		this.alias = alias;
		this.instanceId = instanceId;
		this.properties = properties;
		this.dependencies = dependencies;
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
				.add("properties", this.properties) // TODO define if the field is editable
				.onlyIf(dependencies != null && !dependencies.isEmpty(), j -> j.add("dependencies", //
						dependencies.stream().map(t -> t.toJsonObject()).collect(JsonUtils.toJsonArray())))
				.build();
	}

}
