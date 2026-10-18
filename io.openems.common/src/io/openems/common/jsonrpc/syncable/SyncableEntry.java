package io.openems.common.jsonrpc.syncable;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.buildJsonObject;

import java.time.Instant;

import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.serialization.JsonObjectPath;
import io.openems.common.jsonrpc.serialization.JsonSerializer;

public record SyncableEntry<VALUE>(SyncableKey<VALUE> key, VALUE value, Instant lastChange) {

	/**
	 * Creates the json serializer for {@link SyncableEntry}.
	 *
	 * @return Json serializer
	 */
	public static JsonSerializer<SyncableEntry<?>> serializer() {
		return jsonObjectSerializer(//
				json -> {
					var key = json.getObject("key", SyncableKey.keySerializer());
					return deserializeSyncableEntry(json, key);
				}, //
				obj -> {
					if (obj == null) {
						return null;
					}

					return serializeSyncableEntry(obj);
				});
	}

	private static <T> JsonObject serializeSyncableEntry(SyncableEntry<T> entry) {
		return buildJsonObject() //
				.add("key", entry.key(), SyncableKey.keySerializer()) //
				.add("value", entry.value(), entry.key().getValueSerializer()) //
				.addProperty("lastChange", entry.lastChange()) //
				.build();
	}

	private static <T> SyncableEntry<T> deserializeSyncableEntry(JsonObjectPath json, SyncableKey<T> key) {
		return new SyncableEntry<>(//
				key, //
				json.getObject("value", key.getValueSerializer()), //
				json.getInstant("lastChange"));
	}

}
