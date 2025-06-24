package io.openems.edge.core.serialnumber;

import static io.openems.common.utils.FunctionUtils.apply;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.openems.common.types.ChannelAddress;

public class SerialNumberSaverImplConfigDiffTest {

	@Test
	public void testApplyToNoChange() {
		final var diff = new SerialNumberStorageImpl.ConfigDiff(emptyList(), emptyList());

		final var data = apply(new HashMap<String, Map<String, JsonElement>>(), map -> {
			map.put("key", apply(new HashMap<>(), t -> t.put("value1Key", new JsonPrimitive("value"))));
		});
		diff.applyTo(data);

		assertEquals(1, data.size());
		assertEquals(1, data.get("key").size());
		assertEquals(new JsonPrimitive("value"), data.get("key").get("value1Key"));
	}

	@Test
	public void testApplyToElementAdded() {
		final var diff = new SerialNumberStorageImpl.ConfigDiff(List.of(//
				Map.entry(new ChannelAddress("key", "value2Key"), new JsonPrimitive("value2")) //
		), emptyList());

		final var data = apply(new HashMap<String, Map<String, JsonElement>>(), map -> {
			map.put("key", apply(new HashMap<>(), t -> t.put("value1Key", new JsonPrimitive("value"))));
		});
		diff.applyTo(data);

		assertEquals(1, data.size());
		assertEquals(2, data.get("key").size());
		assertEquals(new JsonPrimitive("value"), data.get("key").get("value1Key"));
		assertEquals(new JsonPrimitive("value2"), data.get("key").get("value2Key"));
	}

	@Test
	public void testApplyToElementAddedDifferentComponent() {
		final var diff = new SerialNumberStorageImpl.ConfigDiff(List.of(//
				Map.entry(new ChannelAddress("key2", "value1Key"), new JsonPrimitive("value")) //
		), emptyList());

		final var data = apply(new HashMap<String, Map<String, JsonElement>>(), map -> {
			map.put("key", apply(new HashMap<>(), t -> t.put("value1Key", new JsonPrimitive("value"))));
		});
		diff.applyTo(data);

		assertEquals(2, data.size());
		assertEquals(1, data.get("key2").size());
		assertEquals(new JsonPrimitive("value"), data.get("key").get("value1Key"));
	}

	@Test
	public void testApplyToElementChanged() {
		final var diff = new SerialNumberStorageImpl.ConfigDiff(List.of(//
				Map.entry(new ChannelAddress("key", "value1Key"), new JsonPrimitive("newValue")) //
		), emptyList());

		final var data = apply(new HashMap<String, Map<String, JsonElement>>(), map -> {
			map.put("key", apply(new HashMap<>(), t -> t.put("value1Key", new JsonPrimitive("value"))));
		});
		diff.applyTo(data);

		assertEquals(1, data.size());
		assertEquals(1, data.get("key").size());
		assertEquals(new JsonPrimitive("newValue"), data.get("key").get("value1Key"));
	}

	@Test
	public void testApplyToElementRemoved() {
		final var diff = new SerialNumberStorageImpl.ConfigDiff(emptyList(),
				List.of(new ChannelAddress("key", "value1Key")));

		final var data = apply(new HashMap<String, Map<String, JsonElement>>(), map -> {
			map.put("key", apply(new HashMap<>(), t -> t.put("value1Key", new JsonPrimitive("value"))));
		});
		diff.applyTo(data);

		assertEquals(0, data.size());
	}

	@Test
	public void testBetweenNoChange() {
		final var data1 = apply(new HashMap<String, Map<String, JsonElement>>(), map -> {
			map.put("key", apply(new HashMap<>(), t -> t.put("value1Key", new JsonPrimitive("value"))));
		});
		final var data2 = apply(new HashMap<String, Map<String, JsonElement>>(), map -> {
			map.put("key", apply(new HashMap<>(), t -> t.put("value1Key", new JsonPrimitive("value"))));
		});

		final var diff = SerialNumberStorageImpl.ConfigDiff.between(data1, data2);

		assertEquals(0, diff.addedOrModifiedValues().size());
		assertEquals(0, diff.removedValues().size());
	}

	@Test
	public void testBetweenValueChanged() {
		final var data1 = apply(new HashMap<String, Map<String, JsonElement>>(), map -> {
			map.put("key", apply(new HashMap<>(), t -> t.put("value1Key", new JsonPrimitive("value"))));
		});
		final var data2 = apply(new HashMap<String, Map<String, JsonElement>>(), map -> {
			map.put("key", apply(new HashMap<>(), t -> t.put("value1Key", new JsonPrimitive("newValue"))));
		});

		final var diff = SerialNumberStorageImpl.ConfigDiff.between(data1, data2);

		assertEquals(1, diff.addedOrModifiedValues().size());
		assertEquals(0, diff.removedValues().size());

		final var changedElement = diff.addedOrModifiedValues().get(0);
		assertEquals(new ChannelAddress("key", "value1Key"), changedElement.getKey());
		assertEquals(new JsonPrimitive("newValue"), changedElement.getValue());
	}

	@Test
	public void testBetweenValueRemoved() {
		final var data1 = apply(new HashMap<String, Map<String, JsonElement>>(), map -> {
			map.put("key", apply(new HashMap<>(), t -> {
				t.put("value1Key", new JsonPrimitive("value"));
				t.put("value2Key", new JsonPrimitive("value2"));
			}));
		});
		final var data2 = apply(new HashMap<String, Map<String, JsonElement>>(), map -> {
			map.put("key", apply(new HashMap<>(), t -> t.put("value1Key", new JsonPrimitive("value"))));
		});

		final var diff = SerialNumberStorageImpl.ConfigDiff.between(data1, data2);

		assertEquals(0, diff.addedOrModifiedValues().size());
		assertEquals(1, diff.removedValues().size());

		final var removedElement = diff.removedValues().get(0);
		assertEquals(new ChannelAddress("key", "value2Key"), removedElement);
	}

	@Test
	public void testBetweenValueRemovedWholeComponent() {
		final var data1 = apply(new HashMap<String, Map<String, JsonElement>>(), map -> {
			map.put("key", apply(new HashMap<>(), t -> {
				t.put("value1Key", new JsonPrimitive("value"));
			}));
			map.put("key2", apply(new HashMap<>(), t -> {
				t.put("value1Key", new JsonPrimitive("value"));
				t.put("value2Key", new JsonPrimitive("value"));
			}));
		});
		final var data2 = apply(new HashMap<String, Map<String, JsonElement>>(), map -> {
			map.put("key", apply(new HashMap<>(), t -> t.put("value1Key", new JsonPrimitive("value"))));
		});

		final var diff = SerialNumberStorageImpl.ConfigDiff.between(data1, data2);

		assertEquals(0, diff.addedOrModifiedValues().size());
		assertEquals(2, diff.removedValues().size());

		assertTrue(diff.removedValues().contains(new ChannelAddress("key2", "value1Key")));
		assertTrue(diff.removedValues().contains(new ChannelAddress("key2", "value2Key")));
	}

	@Test
	public void testBetweenValueAdded() {
		final var data1 = apply(new HashMap<String, Map<String, JsonElement>>(), map -> {
			map.put("key", apply(new HashMap<>(), t -> t.put("value1Key", new JsonPrimitive("value"))));
		});
		final var data2 = apply(new HashMap<String, Map<String, JsonElement>>(), map -> {
			map.put("key", apply(new HashMap<>(), t -> {
				t.put("value1Key", new JsonPrimitive("value"));
				t.put("value2Key", new JsonPrimitive("value2"));
			}));
		});

		final var diff = SerialNumberStorageImpl.ConfigDiff.between(data1, data2);

		assertEquals(1, diff.addedOrModifiedValues().size());
		assertEquals(0, diff.removedValues().size());

		final var addedElement = diff.addedOrModifiedValues().get(0);
		assertEquals(new ChannelAddress("key", "value2Key"), addedElement.getKey());
		assertEquals(new JsonPrimitive("value2"), addedElement.getValue());
	}

	@Test
	public void testBetweenValueAddedDifferentComponent() {
		final var data1 = apply(new HashMap<String, Map<String, JsonElement>>(), map -> {
			map.put("key", apply(new HashMap<>(), t -> t.put("value1Key", new JsonPrimitive("value"))));
		});
		final var data2 = apply(new HashMap<String, Map<String, JsonElement>>(), map -> {
			map.put("key", apply(new HashMap<>(), t -> t.put("value1Key", new JsonPrimitive("value"))));
			map.put("key2", apply(new HashMap<>(), t -> t.put("value1Key", new JsonPrimitive("value"))));
		});

		final var diff = SerialNumberStorageImpl.ConfigDiff.between(data1, data2);

		assertEquals(1, diff.addedOrModifiedValues().size());
		assertEquals(0, diff.removedValues().size());

		final var addedElement = diff.addedOrModifiedValues().get(0);
		assertEquals(new ChannelAddress("key2", "value1Key"), addedElement.getKey());
		assertEquals(new JsonPrimitive("value"), addedElement.getValue());
	}

}
