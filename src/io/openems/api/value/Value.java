package io.openems.api.value;

public class Value {

	private int value = 0;

	public Value(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}
}
