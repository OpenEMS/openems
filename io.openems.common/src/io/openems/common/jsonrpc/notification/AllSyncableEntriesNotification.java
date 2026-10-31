package io.openems.common.jsonrpc.notification;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.jsonrpc.syncable.SyncableEntry;
import io.openems.common.utils.JsonUtils;

public class AllSyncableEntriesNotification extends JsonrpcNotification {
	public static final String METHOD = "syncable/allentries";

	/**
	 * Creates an {@link AllSyncableEntriesNotification} from a generic
	 * {@link JsonrpcNotification} by deserializing the payload.
	 *
	 * @param n the source notification
	 * @return the parsed {@link AllSyncableEntriesNotification}
	 * @throws OpenemsError.OpenemsNamedException if {@code entries} is missing or
	 *                                            cannot be deserialized
	 */
	public static AllSyncableEntriesNotification from(JsonrpcNotification n) throws OpenemsError.OpenemsNamedException {
		final var entriesSerializer = SyncableEntry.serializer().toListSerializer();

		var j = n.getParams();
		var entries = entriesSerializer.deserialize(JsonUtils.getAsJsonArray(j, "entries"));

		return new AllSyncableEntriesNotification(entries);
	}

	private final List<SyncableEntry<?>> entries;

	public AllSyncableEntriesNotification(List<SyncableEntry<?>> entries) {
		super(METHOD);
		this.entries = entries;
	}

	public List<SyncableEntry<?>> getEntries() {
		return this.entries;
	}

	@Override
	public JsonObject getParams() {
		final var entriesSerializer = SyncableEntry.serializer().toListSerializer();

		return JsonUtils.buildJsonObject() //
				.add("entries", this.entries, entriesSerializer) //
				.build();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof AllSyncableEntriesNotification other //
				&& this.entries.size() == other.entries.size() //
				&& IntStream.range(0, this.entries.size())
						.allMatch(i -> this.entries.get(i).equals(other.entries.get(i)));
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.entries);
	}
}
