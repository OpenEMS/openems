package io.openems.edge.ess.power.api;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.math3.optim.linear.LinearConstraint;
import org.apache.commons.math3.optim.linear.LinearObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;

public class Utils {

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

	public static String abstractConstraintToString(AbstractConstraint ac) {
		return Arrays.stream(ac.getConstraints()).map(c -> Utils.linearConstraintToString(c, ""))
				.collect(Collectors.joining(",")).trim();
	}
}
