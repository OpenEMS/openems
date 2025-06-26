package io.openems.edge.ess.power.api;

@FunctionalInterface
public interface OnSolved {

	/**
	 * OnSolved callback.
	 * 
	 * @param isSolved could the problem be solved?
	 * @param duration solve duration in seconds
	 * @param strategy applied {@link SolverStrategy}
	 */
	public void accept(boolean isSolved, int duration, SolverStrategy strategy);

}
