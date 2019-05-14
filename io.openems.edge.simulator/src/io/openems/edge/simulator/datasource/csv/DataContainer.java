package io.openems.edge.simulator.datasource.csv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class DataContainer {

	private HashMap<String, Integer> keys = new HashMap<>();
	private List<float[]> records = new ArrayList<>();
	private int currentIndex = 0;

	/**
	 * Gets the available keys.
	 * 
	 * @return the Channel-Id
	 */
	public Set<String> getKeys() {
		return keys.keySet();
	}

	/**
	 * Sets the keys.
	 * 
	 * @param keys the Channel-Id
	 */
	public void setKeys(String[] keys) {
		for (int i = 0; i < keys.length; i++) {
			this.keys.put(keys[i], i);
		}
	}

	/**
	 * Adds a Record to the end.
	 * 
	 * @param record the record values
	 */
	public void addRecord(float[] record) {
		this.records.add(record);
	}

	/**
	 * Gets the current record.
	 * 
	 * @return the current record
	 */
	public float[] getCurrentRecord() throws IndexOutOfBoundsException {
		return this.records.get(currentIndex);
	}

	/**
	 * Gets the value for the key from the current record.
	 * 
	 * @param key the Channel-Id
	 * @return the record value; or null
	 */
	public Float getValue(String key) {
		Integer keyIndex = this.keys.get(key);
		if (keyIndex == null) {
			return null;
		}
		try {
			return this.getCurrentRecord()[this.keys.get(key)];
		} catch (IndexOutOfBoundsException e) {
			return null;
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
}
