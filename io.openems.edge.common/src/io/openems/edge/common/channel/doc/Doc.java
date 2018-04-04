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

	Unit getUnit() {
		return this.unit;
	}

	/*
	 * Options
	 */
	protected TreeMap<Integer, String> options = new TreeMap<Integer, String>();

	public Doc option(int value, String option) {
		this.options.put(value, option);
		return this;
	}

}
