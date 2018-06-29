package io.openems.edge.ess.power;

import org.apache.commons.math3.optim.linear.NoFeasibleSolutionException;
import org.apache.commons.math3.optim.linear.UnboundedSolutionException;

import io.openems.common.exceptions.OpenemsException;

public class PowerException extends OpenemsException {

	private static final long serialVersionUID = 1L;

	private enum Type {
		NO_FEASIBLE_SOLUTION, UNBOUNDED_SOLUTION
	}

	private final Type type;

	public PowerException(NoFeasibleSolutionException e) {
		super("No Feasible Solution");
		this.type = Type.NO_FEASIBLE_SOLUTION;
	}

	public PowerException(NoFeasibleSolutionException e, AbstractConstraint constraint) {
		super("No Feasible Solution: " + Utils.abstractConstraintToString(constraint));
		this.type = Type.NO_FEASIBLE_SOLUTION;
	}

	public PowerException(UnboundedSolutionException e) {
		super("Unbounded Solution");
		this.type = Type.UNBOUNDED_SOLUTION;
	}

	public Type getType() {
		return type;
	}
}
