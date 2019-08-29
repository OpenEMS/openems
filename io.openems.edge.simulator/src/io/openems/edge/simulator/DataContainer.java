package io.openems.edge.simulator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
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
	public float[] getCurrentRecord() {
		return this.records.get(currentIndex);
	}

	/**
	 * Gets the value for the key from the current record.
	 * 
	 * @param key the Channel-Id
	 * @return the record value
	 */
	public Optional<Float> getValue(String key) {
		Integer index = this.keys.get(key);
		if (index == null) {
			return Optional.empty();
		}
		float[] record = this.getCurrentRecord();
		if (index < record.length) {
			return Optional.of(record[index]);
		} else {
			return Optional.empty();
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
		this.currentIndex = 0;
	}
}
