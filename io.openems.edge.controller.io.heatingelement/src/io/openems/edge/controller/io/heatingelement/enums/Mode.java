package io.openems.edge.controller.io.heatingelement.enums;

public enum Mode {

	MANUAL_ON, // uses Default Level
	MANUAL_OFF, // switches all relays off
	AUTOMATIC, // uses surplus power and configured guarantees
	FAST_HEAT;// immediately activates all configured relays
}
