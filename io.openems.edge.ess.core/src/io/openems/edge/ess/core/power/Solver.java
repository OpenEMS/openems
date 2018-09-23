package io.openems.edge.ess.core.power;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.linear.LinearConstraint;
import org.apache.commons.math3.optim.linear.LinearConstraintSet;
import org.apache.commons.math3.optim.linear.LinearObjectiveFunction;
import org.apache.commons.math3.optim.linear.NoFeasibleSolutionException;
import org.apache.commons.math3.optim.linear.PivotSelectionRule;
import org.apache.commons.math3.optim.linear.SimplexSolver;
import org.apache.commons.math3.optim.linear.UnboundedSolutionException;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;

import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.coefficient.Coefficient;
import io.openems.edge.ess.power.api.coefficient.LinearCoefficient;

/*
 * TODO:
 * - Inverter abstrahieren
 * - Inverter nach Gewichtung sortieren
 * - Nach und nach hintere Inverter entfernen und nochmal lösen (mit Puffer) -> noOfRequiredEss
 * - Vorherige Gewichtung anwenden
 * - Lernrate Richtung noOfRequiredEss anwenden
 * - Gerundete Werte anwenden und neu solven
 * - applyPower auf alle Ess ohne MetaEss
 * - Zeitmessung
 * - getActivePowerExtrema()
 * - isSolvable()
 */

public class Solver {

	private final Data data;
	private Consumer<Boolean> onSolved = (wasSolved) -> {
	};

	public Solver(Data data) {
		this.data = data;

	}

	public void onSolved(Consumer<Boolean> onSolved) {
		this.onSolved = onSolved;
	}

	public boolean isSolvable() {
		return false;
	}

	public int getActivePowerExtrema(GoalType maximize) {
		return 0;
	}

	public void solve() {
		List<LinearConstraint> constraints = Solver.getConstraints(this.data);
		LinearObjectiveFunction objectiveFunction = Solver.getObjectiveFunction(this.data);

		try {
			SimplexSolver solver = new SimplexSolver();
			PointValuePair solution = solver.optimize( //
					objectiveFunction, //
					new LinearConstraintSet(constraints), //
					GoalType.MINIMIZE, //
					PivotSelectionRule.BLAND);

			// announce success
			this.onSolved.accept(true);

			this.applySolution(solution);
		} catch (NoFeasibleSolutionException | UnboundedSolutionException e) {
			// announce failure
			this.onSolved.accept(false);

			System.out.println("Unable to solve under the following constraints:");
			for (Constraint c : data.getConstraints()) {
				System.out.println(c);
			}
		}
	}

	private void applySolution(PointValuePair solution) {
		for (Coefficient c : this.data.getCoefficients().getAll()) {
			System.out.println(c.toString() + ": " + solution.getPoint()[c.getIndex()]);
		}
	}

	public static List<LinearConstraint> getConstraints(Data data) {
		List<LinearConstraint> result = new ArrayList<>();
		for (Constraint c : data.getConstraints()) {
			if (c.getValue().isPresent()) {
				double[] cos = Solver.getEmptyCoefficients(data);
				for (LinearCoefficient co : c.getCoefficients()) {
					cos[co.getCoefficient().getIndex()] = co.getValue();
				}
				result.add(new LinearConstraint(cos, c.getRelationship(), c.getValue().get()));
			}
		}
		return result;
	}

	public static LinearObjectiveFunction getObjectiveFunction(Data data) {
		double[] cos = Solver.getEmptyCoefficients(data);
		for (int i = 0; i < cos.length; i++) {
			cos[i] = 1;
		}
		return new LinearObjectiveFunction(cos, 0);
	}

	public static double[] getEmptyCoefficients(Data data) {
		return new double[data.getCoefficients().getNoOfCoefficients()];
	}
}
