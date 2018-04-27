package io.openems.edge.common.channel.doc;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class Doc {

	/*
	 * Description
	 */
	private String text = "";

	public Doc text(String text) {
		this.text = text;
		return this;
	}

	String getText() {
		return this.text;
	}

	/*
	 * Unit
	 */
	private Unit unit = Unit.NONE;

	public Doc unit(Unit unit) {
		this.unit = unit;
		return this;
	}

	public Unit getUnit() {
		return this.unit;
	}

	/*
	 * Options
	 */
	private BiMap<Integer, String> options = HashBiMap.create();

	public Doc option(int value, Enum<?> option) {
		this.options.put(value, option.name());
		return this;
	}

	public Doc option(int value, String option) {
		this.options.put(value, option);
		return this;
	}

	/**
	 * Get the Option value. Throws IllegalArgumentException if there is no option
	 * with that name
	 * 
	 * @param value
	 * @return
	 */
	public int getOption(String name) {
		Integer option = this.options.inverse().get(name);
		if (option == null) {
			throw new IllegalArgumentException(
					"Channel has no option [" + name + "]! Existing options: " + this.options.values());
		}
		return option;
	}

	/**
	 * Get the Option value. Throws IllegalArgumentException if there is no option
	 * with that name
	 * 
	 * @param value
	 * @return
	 */
	public int getOption(Enum<?> nameEnum) {
		return this.getOption(nameEnum.name());
	}

	/*
	 * Levels
	 */
	private Level level = Level.INFO;

	public Doc level(Level level) {
		this.level = level;
		return this;
	}

	public Level getLevel() {
		return level;
	}

	/*
	 * Verbose Debug mode
	 */
	private boolean debug = false;

	public Doc debug() {
		this.debug = true;
		return this;
	}

	public boolean isDebug() {
		return this.debug;
	}
}
