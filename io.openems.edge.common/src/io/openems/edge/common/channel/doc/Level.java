package io.openems.edge.common.channel.doc;

/**
 * Severity/visibility Level
 */
public enum Level {
	INFO(1), WARNING(2), FAULT(3);
	
	private final int value;
	
	private Level(int value) {
		this.value = value;
	}
	
	public int getValue() {
		return value;
	}
}