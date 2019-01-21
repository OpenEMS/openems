package io.openems.edge.ess.mr.gridcon.enums;

import io.openems.edge.common.channel.doc.OptionsEnum;

/*
 * see manual (Betriebsanleitung Feldbus Konfiguration (Anybus-Modul)) page 15
 */
public enum Command implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	PLAY(1, "Start active filter"), //
	PAUSE(2, "Set outgoing current of ACF to zero"), //
	ACKNOWLEDGE(4, "Achnowledge errors"), //
	STOP(8, "Switch off");

	int value;
	String name;

	private Command(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override
	public int getValue() {
		return value;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}
