package io.openems.edge.ess.core.power;

import org.apache.commons.math3.optim.PointValuePair;

import io.openems.edge.ess.power.api.SolverStrategy;

class SolveSolution {

	private final SolverStrategy solvedBy;
	private final PointValuePair points;

	public SolveSolution(SolverStrategy solvedBy, PointValuePair points) {
		this.solvedBy = solvedBy;
		this.points = points;
	}

	public PointValuePair getPoints() {
		return this.points;
	}

	public SolverStrategy getSolvedBy() {
		return this.solvedBy;
	}

}