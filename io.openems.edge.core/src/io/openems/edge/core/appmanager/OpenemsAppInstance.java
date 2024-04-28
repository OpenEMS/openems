package io.openems.edge.core.appmanager;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.toJsonArray;
import static java.util.Collections.emptyList;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
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
		this.appId = Objects.requireNonNull(appId);
		this.alias = alias;
		this.instanceId = Objects.requireNonNull(instanceId);
		this.properties = properties;
		this.dependencies = dependencies == null ? emptyList() : dependencies;
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
	 * Returns a {@link JsonSerializer} for a {@link OpenemsAppInstance}.
	 * 
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<OpenemsAppInstance> serializer() {
		return jsonObjectSerializer(OpenemsAppInstance.class, //
				json -> new OpenemsAppInstance(//
						json.getString("appId"), //
						json.getString("alias"), //
						json.getStringPath("instanceId").getAsUuid(), //
						json.getJsonObject("properties"), //
						// TODO add optional methods
						json.getList("dependencies", Dependency.serializer())), //
				obj -> JsonUtils.buildJsonObject() //
						.addProperty("appId", obj.appId) //
						.addProperty("alias", obj.alias != null ? obj.alias : "") //
						.addProperty("instanceId", obj.instanceId.toString()) //
						.add("properties", obj.properties == null ? new JsonObject() : obj.properties) //
						.onlyIf(obj.dependencies != null && !obj.dependencies.isEmpty(), b -> b //
								.add("dependencies", obj.dependencies.stream() //
										.map(Dependency.serializer()::serialize) //
										.collect(toJsonArray()))) //
						.build());
	}

	/**
	 * Gets this {@link OpenemsAppInstance} as {@link JsonObject}.
	 *
	 * @return the {@link JsonObject}
	 */
	public JsonElement toJsonObject() {
		return serializer().serialize(this);
	}

}
