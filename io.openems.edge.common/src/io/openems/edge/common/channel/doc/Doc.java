package io.openems.edge.common.channel.doc;

import java.util.TreeMap;

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
	private TreeMap<Integer, String> options = new TreeMap<Integer, String>();

	public Doc option(int value, String option) {
		this.options.put(value, option);
		return this;
	}

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
