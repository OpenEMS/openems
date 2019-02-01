package io.openems.edge.ess.sinexcel;
import io.openems.edge.common.channel.doc.OptionsEnum;

public enum CurrentState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"),
	OFF(1, "Off"),
	SLEEPING(2, "Sleeping"),
	STARTING(3, "Starting"),
	MPPT(4, "MPPT"),
	THROTTLED(5, "Throttled"),
	SHUTTINGDOWN(6, "Shutting Down"),
	FAULT(7, "Fault"),
	STANDBY(8, "Stadnby"),
	STARTED(9, "Started");
	
	int value;
	String option;

	private CurrentState(int value, String option) {
		this.value = value;
		this.option = option;
	}

	@Override
	public int getValue() {
		return value;
	}

//	@Override
//	public String getOption() {
//		return option;
//	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return option;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}

}
