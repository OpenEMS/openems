package io.openems.api.bridge;

public class BridgeEvent {
	public enum Position {
		BEFOREREADREQUIRED, BEFOREREADOTHER1, BEFOREWRITE, BEFOREREADOTHER2
	}

	private final Position position;

	public BridgeEvent(Position position) {
		this.position = position;
	}

	public Position getPosition() {
		return position;
	}

}
