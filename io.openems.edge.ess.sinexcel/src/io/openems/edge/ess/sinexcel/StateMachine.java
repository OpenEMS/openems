package io.openems.edge.ess.sinexcel;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.EnumReadChannel;

public class StateMachine {

	protected final EssSinexcelImpl parent;

	public StateMachine(EssSinexcelImpl parent) throws OpenemsNamedException {
		this.parent = parent;

	}

	public void run() throws IllegalArgumentException, OpenemsNamedException {
		// To soft start if there is any error or manual change in sinexcel state
		CurrentState currentState = this.getSinexcelState();

		switch (currentState) {
		case UNDEFINED:
		case SLEEPING:
		case MPPT:
		case THROTTLED:
		case STARTED:
			this.parent.softStart(true);
			break;
		case SHUTTINGDOWN:
		case FAULT:
		case STANDBY:
		case OFF:
			// this.parent.softStart(false);
			break;
		default:
			break;
		}
	}

	private CurrentState getSinexcelState() {
		EnumReadChannel currentState = this.parent.channel(EssSinexcel.ChannelId.SINEXCEL_STATE);
		CurrentState curState = currentState.value().asEnum();
		return curState;
	}

	/*
	 * public enum State implements OptionsEnum { UNDEFINED(-1, "Undefined"), //
	 * ONGRID(1, "On-Grid"), // OFFGRID(2, "Off-Grid"), // ERROR(3, "Error");
	 * 
	 * private final int value; private final String name;
	 * 
	 * private State(int value, String name) { this.value = value; this.name = name;
	 * }
	 * 
	 * @Override public int getValue() { return value; }
	 * 
	 * @Override public String getName() { return name; }
	 * 
	 * @Override public OptionsEnum getUndefined() { return UNDEFINED; } }
	 */

}