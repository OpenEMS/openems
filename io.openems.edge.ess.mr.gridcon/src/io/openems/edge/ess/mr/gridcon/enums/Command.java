package io.openems.edge.ess.mr.gridcon.enums;

import io.openems.edge.common.channel.doc.OptionsEnum;

public enum Command implements OptionsEnum { // see manual(Betriebsanleitung Feldbus Konfiguration (Anybus-Modul)) page 15
	PLAY(1, "Start active filter"),
	PAUSE(2, "Set outgoing current of ACF to zero"),
	ACKNOWLEDGE(4, "Achnowledge errors"),
	STOP(8, "Switch off");

	int value;
	String option;

	private Command(int value, String option) {
		this.value = value;
		this.option = option;
	}

	@Override
	public int getValue() {
		return value;
	}

	@Override
	public String getOption() {
		return option;
	}
}
