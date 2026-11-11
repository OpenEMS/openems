package io.openems.common.jsonrpc.syncable;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

class SyncableEntryTest {

	@Test
	void testSerializer() {
		var entry = new SyncableEntry<>(new SyncableKey.TimeDataTransferAllowed(), true, Instant.now());
		final var serializedJson = SyncableEntry.serializer().serialize(entry);
		final var deserializedEntry = SyncableEntry.serializer().deserialize(serializedJson);
		assertEquals(entry, deserializedEntry);
	}

	@Test
	void testCompareAndGetChanges() {
		final var time = Instant.now();
		final List<SyncableEntry<?>> oldList = List.of(//
				new SyncableEntry<>(new SyncableKey.TimeDataTransferAllowed(), true, time), //
				new SyncableEntry<>(new SyncableKey.CurrentTimeInUnixTimestamp(), 1787929464L, time) //
		);
		final List<SyncableEntry<?>> newList = List.of(//
				new SyncableEntry<>(new SyncableKey.TimeDataTransferAllowed(), false, time), //
				new SyncableEntry<>(new SyncableKey.CurrentTimeInUnixTimestamp(), 1787929464L, time) //
		);
		final var expectedDifference = List.of(//
				new SyncableEntry.SyncableEntryChange(new SyncableKey.TimeDataTransferAllowed(), oldList.getFirst(),
						newList.getFirst()));

		final var differences = SyncableEntry.compareAndGetChanges(oldList, newList);
		assertEquals(expectedDifference, differences);
	}
}
