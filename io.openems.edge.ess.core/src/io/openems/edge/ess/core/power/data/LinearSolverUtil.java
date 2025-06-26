package io.openems.edge.ess.core.power.data;

import static org.apache.commons.math3.optim.linear.Relationship.EQ;
import static org.apache.commons.math3.optim.linear.Relationship.GEQ;
import static org.apache.commons.math3.optim.linear.Relationship.LEQ;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.optim.linear.LinearConstraint;
import org.apache.commons.math3.optim.linear.LinearObjectiveFunction;

import io.openems.edge.ess.power.api.Coefficients;
import io.openems.edge.ess.power.api.Constraint;

public class LinearSolverUtil {

	/**
	 * Gets all Constraints converted to Linear Constraints.
	 *
	 * @param coefficients the data object
	 * @param constraints  a list of Constraints
	 * @return a list of LinearConstraints
	 */
	public static List<LinearConstraint> convertToLinearConstraints(Coefficients coefficients,
			List<Constraint> constraints) {
		final var result = new ArrayList<LinearConstraint>();
		for (Constraint c : constraints) {
			final var value = c.getValue();
			if (value.isEmpty()) {
				continue;
			}

			final var cos = generateEmptyCoefficientsArray(coefficients.getNoOfCoefficients());
			for (var co : c.getCoefficients()) {
				var index = co.getCoefficient().getIndex();
				if (index >= cos.length) { // check for race conditions
					continue;
				}
				cos[index] = co.getValue();
			}

			final var relationship = switch (c.getRelationship()) {
			case EQUALS -> EQ;
			case GREATER_OR_EQUALS -> GEQ;
			case LESS_OR_EQUALS -> LEQ;
			};

			result.add(new LinearConstraint(cos, relationship, value.get()));
		}
		return result;
	}

	/**
	 * Gets an empty coefficients array required for linear solver.
	 *
	 * @param length the length of the array
	 * @return an array of '0' coefficients
	 */
	public static double[] generateEmptyCoefficientsArray(int length) {
		return new double[length];
	}

	/**
	 * Gets the linear objective function in the form 1*a + 1*b + 1*c + ...
	 *
	 * @param noOfCoefficients the number of coefficients of the objective function
	 * @return a {@link LinearObjectiveFunction}
	 */
	public static LinearObjectiveFunction getDefaultObjectiveFunction(int noOfCoefficients) {
		var cos = LinearSolverUtil.generateEmptyCoefficientsArray(noOfCoefficients);
		Arrays.fill(cos, 1);
		return new LinearObjectiveFunction(cos, 0);
	}
}
