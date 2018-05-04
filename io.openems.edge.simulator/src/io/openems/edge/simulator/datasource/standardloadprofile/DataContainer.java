package io.openems.edge.simulator.datasource.standardloadprofile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class DataContainer {

	private HashMap<String, Integer> keys = new HashMap<>();
	private List<Float[]> records = new ArrayList<>();
	private int currentIndex = 0;

	/**
	 * Gets the available keys
	 * 
	 * @return
	 */
	public Set<String> getKeys() {
		return keys.keySet();
	}

	/**
	 * Sets the keys
	 * 
	 * @param keys
	 */
	public void setKeys(String[] keys) {
		for (int i = 0; i < keys.length; i++) {
			this.keys.put(keys[i], i);
		}
	}

	/**
	 * Adds a Record to the end
	 * 
	 * @param record
	 */
	public void addRecord(Float[] record) {
		this.records.add(record);
	}

	/**
	 * Gets the current record
	 * 
	 * @return
	 */
	public Float[] getCurrentRecord() {
		return this.records.get(currentIndex);
	}

	/**
	 * Gets the value for the key from the current record
	 * 
	 * @param key
	 * @return
	 */
	public Float getValue(String key) {
		return this.getCurrentRecord()[this.keys.get(key)];
	}

	/**
	 * Switch to the next row of values
	 */
	public void nextRecord() {
		this.currentIndex++;
		if (this.currentIndex >= this.records.size()) {
			this.currentIndex = 0;
		}
	}
}
