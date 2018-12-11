package io.openems.edge.ess.power.api;

@FunctionalInterface
public interface OnSolved {

	public void accept(boolean isSolved, int duration, SolverStrategy strategy);

}
