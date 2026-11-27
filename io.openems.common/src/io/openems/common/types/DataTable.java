package io.openems.common.types;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import com.google.common.collect.ForwardingTable;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * A Table that stores timestamped data for JSON-RPC notifications. The table is
 * implemented as a custom table with a TreeMap for the row keys (timestamps)
 * and a HashMap for the column keys (channel addresses).
 *
 * <p>
 * This table is row-sorted by timestamp, allowing for efficient retrieval of
 * data in chronological order.
 */
public class DataTable extends ForwardingTable<Long, String, JsonElement> {

	private final TreeMap<Long, Map<String, JsonElement>> rows = new TreeMap<>();
	private final Table<Long, String, JsonElement> data = Tables.newCustomTable(this.rows,
			() -> HashMap.newHashMap(50));

	/**
	 * Reserves a row for the given timestamp, sized for the expected number of
	 * columns, so that filling it does not resize the HashMap.
	 *
	 * @param timestamp       the timestamp epoch in milliseconds
	 * @param expectedColumns the expected number of channels in this row
	 */
	public void reserveRow(long timestamp, int expectedColumns) {
		this.rows.compute(timestamp, (ignore, existingRow) -> {
			if (existingRow == null) {
				return HashMap.newHashMap(expectedColumns);
			}
			if (existingRow.size() < expectedColumns) {
				final var newRow = HashMap.<String, JsonElement>newHashMap(expectedColumns);
				newRow.putAll(existingRow);
				return newRow;
			}
			return existingRow;
		});
	}

	/**
	 * Puts a new row for the given timestamp. If a row already exists for this
	 * timestamp, it will be replaced.
	 *
	 * @param timestamp the timestamp epoch in milliseconds
	 * @param data      the new row data as a Map of channel addresses to values
	 */
	public void putRow(long timestamp, Map<String, JsonElement> data) {
		final var newRow = HashMap.<String, JsonElement>newHashMap(data.size());
		newRow.putAll(data);
		this.rows.put(timestamp, newRow);
	}

	/**
	 * Returns the latest value for the given column key, if it exists. The latest
	 * value is determined by the highest timestamp in the table.
	 *
	 * @param columnKey the column key (channel address) to retrieve the latest value for
	 * @return an Optional containing the latest value as a JsonPrimitive, or an empty Optional if no value exists.
	 */
	public Optional<JsonPrimitive> getLatestColumnValue(String columnKey) {
		return this.rows.descendingMap().values().stream() //
				.map(row -> row.get(columnKey)) //
				.filter(JsonPrimitive.class::isInstance) //
				.map(JsonPrimitive.class::cast) //
				.findFirst();
	}

	@Override
	protected Table<Long, String, JsonElement> delegate() {
		return this.data;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof DataTable && super.equals(obj);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}
}
