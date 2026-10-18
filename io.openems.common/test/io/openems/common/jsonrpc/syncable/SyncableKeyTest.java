package io.openems.common.jsonrpc.syncable;

import java.lang.reflect.AccessFlag;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Comparator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.openems.common.types.BiddingZone;

class SyncableKeyTest {
	@Test
	void testKeySerializing() throws Exception {
		var keyTypes = SyncableKey.class.getPermittedSubclasses();
		for (Class<?> keyType : keyTypes) {
			var syncableKey = this.createSyncableKey(keyType);
			var serialized = SyncableKey.keySerializer().serialize(syncableKey);
			var deserialized = SyncableKey.keySerializer().deserialize(serialized);
			Assertions.assertEquals(syncableKey, deserialized,
					"Serialization round-trip failed for " + keyType.getSimpleName());
		}
	}

	private SyncableKey<?> createSyncableKey(Class<?> keyType) throws Exception {
		var constructor = Arrays.stream(keyType.getDeclaredConstructors())
				.filter(c -> c.accessFlags().contains(AccessFlag.PUBLIC))
				.max(Comparator.comparingInt(Constructor::getParameterCount))
				.orElseThrow(() -> new IllegalStateException("No public constructor for " + keyType.getSimpleName()));

		Object[] values = Arrays.stream(constructor.getParameterTypes()).map(this::createDummyValue)
				.toArray(Object[]::new);
		return (SyncableKey<?>) constructor.newInstance(values);
	}

	private Object createDummyValue(Class<?> type) {
		if (type == String.class) {
			return "Dummy";
		} else if (type == Integer.class) {
			return 0;
		} else if (type == Long.class) {
			return 0L;
		} else if (type == Double.class) {
			return 0.0;
		} else if (type == Float.class) {
			return 0.0F;
		} else if (type == Boolean.class) {
			return false;
		} else if (type == BiddingZone.class) {
			return BiddingZone.GERMANY;
		} else {
			throw new IllegalStateException("No dummy value available for type " + type.getSimpleName());
		}
	}
}
