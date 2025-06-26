package io.openems.edge.simulator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class DataContainer {

	private final HashMap<String, Integer> keys = new HashMap<>();
	private final List<Float[]> records = new ArrayList<>();
	private int currentIndex = -1;

	/**
	 * Gets the available keys.
	 *
	 * @return the Channel-Id
	 */
	public Set<String> getKeys() {
		return this.keys.keySet();
	}

	/**
	 * Sets the keys.
	 *
	 * @param keys the Channel-Id
	 */
	public void setKeys(String[] keys) {
		for (var i = 0; i < keys.length; i++) {
			this.keys.put(keys[i], i);
		}
	}

	/**
	 * Adds a Record to the end.
	 *
	 * @param record the record values
	 */
	public void addRecord(Float[] record) {
		this.records.add(record);
	}

	/**
	 * Gets the current record.
	 *
	 * @return the current record
	 */
	public Float[] getCurrentRecord() {
		if (this.currentIndex == -1) {
			this.currentIndex = 0;
		}
		return this.records.get(this.currentIndex);
	}

	/**
	 * Gets all values for the key. If no keys exist, get the value of all records.
	 *
	 * @param key the Channel-Id
	 * @return the record values
	 */
	public List<Float> getValues(String key) {
		var index = this.getIndex(key);
		if (index == null) {
			return List.of();
		}
		return this.records.stream() //
				.map(a -> a[index]) //
				.toList();
	}

	/**
	 * Gets the value for the key from the current record. If no keys exist, get the
	 * first value of the record.
	 *
	 * @param key the Channel-Id
	 * @return the record value
	 */
	public Optional<Float> getValue(String key) {
		var index = this.getIndex(key);
		if (index == null) {
			return Optional.empty();
		}
		var record = this.getCurrentRecord();
		if (index < record.length) {
			return Optional.ofNullable(record[index]);
		}
		return Optional.empty();
	}

	/**
	 * Gets the index of the kex.
	 * 
	 * @param key the Channel-Id
	 * @return the index; possibly null
	 */
	private Integer getIndex(String key) {
		if (this.keys.isEmpty()) {
			// no keys -> first value
			return 0;
		} else {
			// find index of key
			return this.keys.get(key);
		}
	}

	/**
	 * Switch to the next row of values.
	 */
	public void nextRecord() {
		this.currentIndex++;
		if (this.currentIndex >= this.records.size()) {
			this.currentIndex = 0;
		}
	}

	/**
	 * Rewinds the data to start again at the first record.
	 */
	public void rewind() {
		this.currentIndex = -1;
	}
}
