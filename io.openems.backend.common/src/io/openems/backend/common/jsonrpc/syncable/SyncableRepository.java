package io.openems.backend.common.jsonrpc.syncable;

import java.util.List;

import com.google.common.collect.ImmutableList;

import io.openems.common.event.Subscribable;
import io.openems.common.jsonrpc.syncable.SyncableEntry;
import io.openems.common.jsonrpc.syncable.SyncableKey;

public interface SyncableRepository {
	/**
	 * Returns all currently stored static entries.
	 *
	 * @return immutable list of all static entries
	 */
	ImmutableList<SyncableEntry<?>> getAllStaticEntries();

	/**
	 * Returns the stored static entry for the given key.
	 *
	 * @param <VALUE> the value type of the key/entry
	 * @param key     the syncable key
	 * @return the matching static entry
	 */
	<VALUE> SyncableEntry<VALUE> getStaticEntry(SyncableKey<VALUE> key);

	/**
	 * Adds or updates a static entry.
	 *
	 * @param entry the entry to store
	 */
	void setStaticEntry(SyncableEntry<?> entry);

	/**
	 * Removes the static entry for the given key.
	 *
	 * @param key the key of the entry to remove
	 */
	void removeStaticEntry(SyncableKey<?> key);

	/**
	 * Replaces all static entries with the provided list.
	 *
	 * @param entries the new full set of static entries
	 */
	void replaceStaticEntries(List<SyncableEntry<?>> entries);

	/**
	 * Returns the event emitter for repository change events. You can subscribe to
	 * it to get notifications about repository changes.
	 *
	 * @return change event emitter
	 */
	Subscribable<ChangeEvent> getChangeEvent();

	public sealed interface ChangeEvent {
		record EntryUpdated(SyncableEntry<?> entry) implements ChangeEvent {
		}

		record EntryRemoved(SyncableEntry<?> removedEntry) implements ChangeEvent {
		}

		record AllEntriesReplaced() implements ChangeEvent {
		}
	}
}
