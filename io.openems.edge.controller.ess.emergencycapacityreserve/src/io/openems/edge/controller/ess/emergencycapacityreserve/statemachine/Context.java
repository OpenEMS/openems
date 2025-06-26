package io.openems.edge.controller.ess.emergencycapacityreserve.statemachine;

import io.openems.edge.common.statemachine.AbstractContext;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.ess.emergencycapacityreserve.ControllerEssEmergencyCapacityReserve;
import io.openems.edge.controller.ess.emergencycapacityreserve.statemachine.StateMachine.State;

public class Context extends AbstractContext<ControllerEssEmergencyCapacityReserve> {

	protected final Sum sum;
	protected final boolean isEssChargeFromGridAllowed;

	/**
	 * MaxApparentPower is guaranteed to be not-null in any State other than
	 * NO_LIMIT.
	 */
	public final Integer maxApparentPower;
	/**
	 * SoC is guaranteed to be not-null in any State other than NO_LIMIT.
	 */
	protected final Integer soc;
	protected final int reserveSoc;

	private Float targetPower;
	private float rampPower;
	private State lastActiveState;
	private State previousState;

	public Context(ControllerEssEmergencyCapacityReserve emergencyCapacityReserve, Sum sum, Integer maxApparentPower,
			Integer soc, int reserveSoc, boolean isEssChargeFromGridAllowed) {
		super(emergencyCapacityReserve);
		this.sum = sum;
		this.maxApparentPower = maxApparentPower;
		this.soc = soc;
		this.reserveSoc = reserveSoc;
		this.isEssChargeFromGridAllowed = isEssChargeFromGridAllowed;
	}

	public Float getTargetPower() {
		return this.targetPower;
	}

	protected void setTargetPower(Float targetPower) {
		this.targetPower = targetPower;
	}

	protected void setTargetPower(Integer targetPower) {
		this.targetPower = targetPower == null ? null : targetPower.floatValue();
	}

	public float getRampPower() {
		return this.rampPower;
	}

	public State getLastActiveState() {
		return this.lastActiveState == null ? State.UNDEFINED : this.lastActiveState;
	}

	public State getPreviousState() {
		return this.previousState == null ? State.UNDEFINED : this.previousState;
	}

	protected void setRampPower(Double rampPower) {
		this.rampPower = rampPower == null ? null : rampPower.floatValue();
	}

	protected void setRampPower(float rampPower) {
		this.rampPower = rampPower;
	}

	protected void setRampPower(int rampPower) {
		this.rampPower = rampPower;
	}

	public void setLastActiveState(State state) {
		this.lastActiveState = state;
	}

	public void setPreviousState(State state) {
		this.previousState = state;
	}

}