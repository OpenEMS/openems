package io.openems.edge.ess.power.api;

import io.openems.common.exceptions.OpenemsException;

public class PowerException extends OpenemsException {

	private static final long serialVersionUID = 1L;

	public enum Type {
		NO_FEASIBLE_SOLUTION, UNBOUNDED_SOLUTION
	}

	private final Type type;

	private Constraint reason = null;

	public PowerException(Type type) {
		super(type == Type.NO_FEASIBLE_SOLUTION ? "No Feasible Solution" : "Unbounded Solution");
		this.type = type;
	}

	public Type getType() {
		return this.type;
	}

	public void setReason(Constraint reason) {
		this.reason = reason;
	}

	@Override
	public String getMessage() {
		return super.getMessage() + (this.reason != null ? " [" + this.reason.toString() + "]" : "");
	}
}
