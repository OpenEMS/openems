package io.openems.edge.ess.power.api;

import org.apache.commons.math3.optim.linear.LinearConstraint;
import org.apache.commons.math3.optim.linear.LinearObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;

import io.openems.common.utils.IntUtils;
import io.openems.common.utils.IntUtils.Round;
import io.openems.edge.ess.api.ManagedSymmetricEss;

public class Utils {

	/**
	 * Round values to accuracy of inverter; following this logic:
	 *
	 * On Discharge (Power > 0)
	 *
	 * <ul>
	 * <li>if SoC > 50 %: round up (more discharge)
	 * <li>if SoC < 50 %: round down (less discharge)
	 * </ul>
	 *
	 * On Charge (Power < 0)
	 *
	 * <ul>
	 * <li>if SoC > 50 %: round down (less charge)
	 * <li>if SoC < 50 %: round up (more discharge)
	 * </ul>
	 */
	public static int roundToInverterPrecision(ManagedSymmetricEss ess, double value) {
		Round round = Round.DOWN;
		int precision = ess.getPowerPrecision();
		int soc = ess.getSoc().value().orElse(0);

		if (value > 0 && soc > 50 || value < 0 && soc < 50) {
			round = Round.UP;
		}

		return IntUtils.roundToPrecision((float) value, round, precision);
	}

	private static String coefficientsToString(double[] coefficients) {
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < coefficients.length; i++) {
			double x = coefficients[i];
			b.append(String.format("%+.1f*", x));
			switch (i % 2) {
			case 0:
				b.append("p");
				break;
			case 1:
				b.append("q");
				break;
			}
			b.append((i / 2) % 3 + 1);
			if (i < coefficients.length - 1) {
				b.append(" ");
			}
		}
		return b.toString();
	}

	public static String linearConstraintToString(LinearConstraint constraint, String note) {
		StringBuilder b = new StringBuilder();
		b.append(String.format("%-30s ", note));
		double[] coefficients = constraint.getCoefficients().toArray();
		b.append(coefficientsToString(coefficients));
		b.append(" " + constraint.getRelationship().toString() + " " + String.format("%.1f", constraint.getValue()));
		return b.toString();
	}

	public static String objectiveFunctionToString(LinearObjectiveFunction function, GoalType goalType) {
		StringBuilder b = new StringBuilder();
		b.append(String.format("%-30s ", "Objective Function"));
		double[] coefficients = function.getCoefficients().toArray();
		b.append(coefficientsToString(coefficients));
		b.append(" -> " + goalType.toString());
		return b.toString();
	}

	public static String solutionToString(double[] solution) {
		StringBuilder b = new StringBuilder();
		for (int i = 0; i < solution.length; i++) {
			double x = solution[i];
			switch (i % 2) {
			case 0:
				b.append("p");
				break;
			case 1:
				b.append("q");
				break;
			}
			b.append((i / 2) % 3 + 1 + String.format(" = % .1f", x));
			if (i < solution.length - 1) {
				b.append(String.format("%n"));
			}
		}
		return b.toString();
	}

}
