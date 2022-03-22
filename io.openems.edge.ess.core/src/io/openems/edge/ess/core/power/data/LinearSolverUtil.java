package io.openems.edge.ess.core.power.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.optim.linear.LinearConstraint;
import org.apache.commons.math3.optim.linear.LinearObjectiveFunction;

import io.openems.edge.ess.power.api.Coefficients;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.LinearCoefficient;

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
		List<LinearConstraint> result = new ArrayList<>();
		for (Constraint c : constraints) {
			if (c.getValue().isPresent()) {
				var cos = generateEmptyCoefficientsArray(coefficients.getNoOfCoefficients());
				for (LinearCoefficient co : c.getCoefficients()) {
					// TODO verify, that ESS is enabled
					cos[co.getCoefficient().getIndex()] = co.getValue();
				}
				org.apache.commons.math3.optim.linear.Relationship relationship = null;
				switch (c.getRelationship()) {
				case EQUALS:
					relationship = org.apache.commons.math3.optim.linear.Relationship.EQ;
					break;
				case GREATER_OR_EQUALS:
					relationship = org.apache.commons.math3.optim.linear.Relationship.GEQ;
					break;
				case LESS_OR_EQUALS:
					relationship = org.apache.commons.math3.optim.linear.Relationship.LEQ;
					break;
				}
				result.add(new LinearConstraint(cos, relationship, c.getValue().get()));
			}
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
