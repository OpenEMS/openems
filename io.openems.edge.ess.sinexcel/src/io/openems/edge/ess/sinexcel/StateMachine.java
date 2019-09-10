package io.openems.edge.ess.sinexcel;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.EnumReadChannel;

public class StateMachine {

	protected final EssSinexcel parent;

	public StateMachine(EssSinexcel parent) throws OpenemsNamedException {
		this.parent = parent;
		
	}

	public void run() throws IllegalArgumentException, OpenemsNamedException {

		/*
		 * UNDEFINED(-1, "Undefined"), // OFF(1, "Off"), // SLEEPING(2, "Sleeping"), //
		 * STARTING(3, "Starting"), // MPPT(4, "MPPT"), // THROTTLED(5, "Throttled"), //
		 * SHUTTINGDOWN(6, "Shutting Down"), // FAULT(7, "Fault"), // STANDBY(8,
		 * "Standby"), // STARTED(9, "Started");
		 */

		// To soft start if there is any error or manual change in sinexcel state
		CurrentState currentState = getSinexcelState();

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
			//this.parent.softStart(false);
		default:
			break;

		}

	}

	protected CurrentState getSinexcelState() {
		EnumReadChannel currentState = this.parent.channel(SinexcelChannelId.SINEXCEL_STATE);
		CurrentState curState = currentState.value().asEnum();
		System.out.println("[Current State is : " + curState.toString() + "]");
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