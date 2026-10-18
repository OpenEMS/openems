package io.openems.common.jsonrpc.syncable;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.buildJsonObject;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
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

	/**
	 * Compares two syncable entry lists by {@link SyncableKey} and returns all
	 * detected changes.
	 *
	 * <p>
	 * A change is reported if an entry is added, removed, or if the matching entry
	 * is not equal according to {@link Objects#equals(Object, Object)}.
	 *
	 * @param oldList the previous entries; may be {@code null}
	 * @param newList the current entries; must not be {@code null}
	 * @return an immutable list of detected changes
	 * @throws IllegalArgumentException if {@code newList} is {@code null}
	 */
	public static List<SyncableEntryChange> compareAndGetChanges(List<SyncableEntry<?>> oldList,
			List<SyncableEntry<?>> newList) {
		if (newList == null) {
			throw new IllegalArgumentException("newList must not be null");
		}

		final var oldByKey = oldList != null //
				? oldList.stream().collect(Collectors.toMap(SyncableEntry::key, e -> e))
				: Map.<SyncableKey<?>, SyncableEntry<?>>of();
		final var newByKey = newList.stream().collect(Collectors.toMap(SyncableEntry::key, e -> e));
		final var keys = Sets.union(oldByKey.keySet(), newByKey.keySet());

		final var changesBuilder = ImmutableList.<SyncableEntryChange>builder();
		for (var key : keys) {
			final var oldEntry = oldByKey.get(key);
			final var newEntry = newByKey.get(key);

			if (!Objects.equals(oldEntry, newEntry)) {
				changesBuilder.add(new SyncableEntryChange(key, oldEntry, newEntry));
			}
		}

		return changesBuilder.build();
	}

	public record SyncableEntryChange(SyncableKey<?> key, SyncableEntry<?> oldEntry, SyncableEntry<?> newEntry) {
	}

}
