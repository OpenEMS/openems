package io.openems.edge.controller.ess.emergencycapacityreserve.statemachine;

import io.openems.edge.common.statemachine.AbstractContext;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.ess.emergencycapacityreserve.ControllerEssEmergencyCapacityReserve;

public class Context extends AbstractContext<ControllerEssEmergencyCapacityReserve> {

	protected final Sum sum;

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

	public Context(ControllerEssEmergencyCapacityReserve emergencyCapacityReserve, Sum sum, Integer maxApparentPower,
			Integer soc, int reserveSoc) {
		super(emergencyCapacityReserve);
		this.sum = sum;
		this.maxApparentPower = maxApparentPower;
		this.soc = soc;
		this.reserveSoc = reserveSoc;
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

	protected void setRampPower(Double rampPower) {
		this.rampPower = rampPower == null ? null : rampPower.floatValue();
	}

	protected void setRampPower(float rampPower) {
		this.rampPower = rampPower;
	}

	protected void setRampPower(int rampPower) {
		this.rampPower = rampPower;
	}

}