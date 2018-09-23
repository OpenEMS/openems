package io.openems.edge.ess.core.power;

import java.util.ArrayList;
import java.util.Collection;
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
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.coefficient.Coefficient;
import io.openems.edge.ess.power.api.coefficient.LinearCoefficient;
import io.openems.edge.ess.power.api.inverter.Inverter;

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
		long startTimestamp = System.nanoTime();

		List<Inverter> disabledInverters = new ArrayList<>();
		PointValuePair solution = solveWithout(disabledInverters);

		List<Inverter> allInverters = new ArrayList<>(data.getInverters());
		for (Inverter inverter : allInverters) {
			disabledInverters.add(inverter);

			try {
				solution = solveWithout(disabledInverters);
				
			} catch (NoFeasibleSolutionException | UnboundedSolutionException e) {
				disabledInverters.remove(inverter);
				System.out.println("Following inverters can be safely disabled:");
				for (Inverter disabledInverter : disabledInverters) {
					System.out.println(disabledInverter.toString());
				}
				System.out.println("Disabling " + inverter + " was too much: " + e.getMessage());
				break;
			}
		}
		
		System.out.println("--");
		this.applySolution(solution);

//
//		try {
//
//			// announce success
//			this.onSolved.accept(true);
//
//		} catch (NoFeasibleSolutionException | UnboundedSolutionException e) {
//			// announce failure
//			this.onSolved.accept(false);
//
//			System.out.println("Unable to solve under the following constraints:");
//			for (Constraint c : data.getConstraints(disabledInverters)) {
//				System.out.println(c);
//			}
//		}

		System.out.println("Elapsed Time: " + (System.nanoTime() - startTimestamp) / 1_000_000 + " ms");
	}

	private PointValuePair solveWithout(List<Inverter> disabledInverters)
			throws NoFeasibleSolutionException, UnboundedSolutionException {
		List<LinearConstraint> constraints = Solver.getLinearConstraints(this.data, disabledInverters);
		LinearObjectiveFunction objectiveFunction = Solver.getObjectiveFunction(this.data);

		SimplexSolver solver = new SimplexSolver();
		return solver.optimize( //
				objectiveFunction, //
				new LinearConstraintSet(constraints), //
				GoalType.MINIMIZE, //
				PivotSelectionRule.BLAND);
	}

	private void applySolution(PointValuePair solution) {
		List<Coefficient> cos = new ArrayList<>(this.data.getCoefficients().getAll());
		cos.sort((c1, c2) -> {
			if (c1.getPwr() != c2.getPwr()) {
				return c1.getPwr().compareTo(c2.getPwr());
			}
			return c1.toString().compareTo(c2.toString());
		});
		for (Coefficient c : cos) {
			if (c.getPwr() != Pwr.REACTIVE) {
				System.out.println(c.toString() + ": " + solution.getPoint()[c.getIndex()]);
			}
		}
	}

	public static List<LinearConstraint> getLinearConstraints(Data data, Collection<Inverter> disabledInverters) {
		List<LinearConstraint> result = new ArrayList<>();
		for (Constraint c : data.getConstraints(disabledInverters)) {
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
