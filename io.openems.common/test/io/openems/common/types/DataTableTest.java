package io.openems.common.types;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

public class DataTableTest {

	@Test
	public void testValueSurvivesReserveRow() {
		final var table = new DataTable();
		table.reserveRow(10L, 2);
		table.put(10L, "component0/Channel", new JsonPrimitive(1));
		table.reserveRow(10L, 5);
		table.put(10L, "component0/Channel", new JsonPrimitive(2));
		table.put(10L, "component1/Channel", new JsonPrimitive(2));
		table.reserveRow(10L, 1);

		assertEquals(new JsonPrimitive(2), table.get(10L, "component0/Channel"));
		assertEquals(new JsonPrimitive(2), table.get(10L, "component1/Channel"));
	}

	@Test
	public void testGetLatestColumnValue() {
		final var table = new DataTable();
		table.put(10L, "component2/Channel", new JsonPrimitive(1));
		table.put(10L, "component1/Channel", new JsonPrimitive(1));
		table.put(30L, "component1/Channel", new JsonPrimitive(3));
		table.put(20L, "component1/Channel", new JsonPrimitive(2));
		table.put(50L, "component2/Channel", new JsonPrimitive(2));
		table.put(60L, "component2/Channel", JsonNull.INSTANCE);

		assertEquals(new JsonPrimitive(3), table.getLatestColumnValue("component1/Channel").orElse(null));
		assertEquals(new JsonPrimitive(2), table.getLatestColumnValue("component2/Channel").orElse(null));
		assertTrue(table.getLatestColumnValue("component3/Channel").isEmpty());
	}

	@Test
	public void testPutRow() {
		final var table = new DataTable();
		table.put(10L, "component0/OldChannel", new JsonPrimitive(1));

		final Map<String, JsonElement> row = new HashMap<>();
		row.put("component0/Channel", new JsonPrimitive(2));
		table.putRow(10L, row);

		row.put("component0/Channel", new JsonPrimitive(3));
		row.put("component0/AnotherChannel", new JsonPrimitive(4));

		assertNull(table.get(10L, "component0/OldChannel"));
		assertEquals(new JsonPrimitive(2), table.get(10L, "component0/Channel"));
		assertNull(table.get(10L, "component0/AnotherChannel"));
	}

	@Test
	public void testRowsAreOrderedByTimestamp() {
		final var table = new DataTable();
		table.reserveRow(30L, 1);
		table.reserveRow(10L, 1);
		table.reserveRow(20L, 1);
		table.put(30L, "component0/Channel", new JsonPrimitive(1));
		table.put(15L, "component0/Channel", new JsonPrimitive(2));
		table.put(1L, "component0/Channel", new JsonPrimitive(3));

		assertEquals(List.of(1L, 10L, 15L, 20L, 30L), new ArrayList<>(table.rowKeySet()));
	}
}
