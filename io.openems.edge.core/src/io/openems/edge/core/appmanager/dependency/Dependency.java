package io.openems.edge.core.appmanager.dependency;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
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

	/**
	 * Gets the {@link Dependency} as a {@link JsonObject}.
	 *
	 * @return the {@link JsonObject}
	 */
	public JsonObject toJsonObject() {
		return JsonUtils.buildJsonObject() //
				.addProperty("key", this.key) //
				.addProperty("instanceId", this.instanceId.toString()) //
				.build();
	}

	/**
	 * Returns a {@link JsonSerializer} for a {@link Dependency}.
	 * 
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<Dependency> serializer() {
		return jsonObjectSerializer(Dependency.class, //
				json -> new Dependency(//
						json.getString("key"), //
						json.getStringPath("instanceId").getAsUuid()), //
				obj -> JsonUtils.buildJsonObject() //
						.addProperty("key", obj.key) //
						.addProperty("instanceId", obj.instanceId.toString()) //
						.build());
	}

}
