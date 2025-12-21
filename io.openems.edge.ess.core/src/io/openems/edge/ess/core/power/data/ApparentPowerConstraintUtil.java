package io.openems.edge.ess.core.power.data;

import java.util.ArrayList;
import java.util.List;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.DoubleUtils;
import io.openems.edge.common.type.Phase.SingleOrAllPhase;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Coefficients;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.LinearCoefficient;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

public class ApparentPowerConstraintUtil {

	private static final int CIRCLE_SECTIONS_PER_QUARTER = 2; // don't set higher than 90

	private static class Point {
		protected final double x;
		protected final double y;

		Point(double x, double y) {
			this.x = x;
			this.y = y;
		}

		@Override
		public String toString() {
			return "Point [x=" + Math.round(this.x) + ", y=" + Math.round(this.y) + "]";
		}
	}

	private ApparentPowerConstraintUtil() {
	}

	/**
	 * Generate Constraints for ApparentPower.
	 *
	 * @param coefficients  the {@link Coefficients}
	 * @param essId         the Id of the {@link ManagedSymmetricEss}
	 * @param phase         the {@link SingleOrAllPhase}
	 * @param apparentPower the apparent power in [VA]
	 * @return a list of {@link Constraint}s
	 * @throws OpenemsException on error
	 */
	public static List<Constraint> generateConstraints(Coefficients coefficients, String essId, SingleOrAllPhase phase,
			double apparentPower) throws OpenemsException {
		var result = new ArrayList<Constraint>();
		if (apparentPower > 0) {
			// Calculate 'Apparent-Power Circle'
			var degreeDelta = 90.0 / CIRCLE_SECTIONS_PER_QUARTER;
			var p1 = getPointOnCircle(apparentPower, 0);

			for (var degree = degreeDelta; Math.floor(degree) <= 360; degree += degreeDelta) {
				var p2 = getPointOnCircle(apparentPower, degree);

				final var relationship = Math.floor(degree) <= 180 //
						? Relationship.GREATER_OR_EQUALS //
						: Relationship.LESS_OR_EQUALS;

				var constraint = getConstraintThroughPoints(coefficients, essId, phase, p1, p2, relationship);
				result.add(constraint);

				// set p2 -> p1 for next loop
				p1 = p2;
			}

		} else {
			// Add Active-/Reactive-Power = 0 constraints
			result.add(ConstraintUtil.createSimpleConstraint(coefficients, //
					essId + ": Max Apparent Power", essId, phase, Pwr.ACTIVE, Relationship.EQUALS, 0));
			result.add(ConstraintUtil.createSimpleConstraint(coefficients, //
					essId + ": Max Apparent Power", essId, phase, Pwr.REACTIVE, Relationship.EQUALS, 0));
		}

		return result;
	}

	private static Point getPointOnCircle(double radius, double degree) {
		return new Point(Math.cos(Math.toRadians(degree)) * radius, Math.sin(Math.toRadians(degree)) * radius);
	}

	private static Constraint getConstraintThroughPoints(Coefficients coefficients, String essId,
			SingleOrAllPhase phase, Point p1, Point p2, Relationship relationship) throws OpenemsException {
		/**
		 * Build the LinearConstraint.
		 *
		 * <pre>
		 *  We use the formula:
		 *  y = ((y2-y1)/(x2-x1)) * x + ((x2*y1-x1*y2)/(x2-x1))
		 * </pre>
		 */
		var deltaX = p2.x - p1.x;

		// Check for division by zero - if points have same x-coordinate, create
		// vertical constraint
		if (DoubleUtils.isCloseToZero(deltaX)) {
			// Vertical line: x = constant, so we constrain active power directly
			var constraintValue = p1.x;
			return new Constraint(essId + ": Max Apparent Power", new LinearCoefficient[] { //
					new LinearCoefficient(coefficients.of(essId, phase, Pwr.ACTIVE), 1.0) //
			}, relationship, constraintValue);
		}

		var constraintValue = -1 * (p1.y * p2.x - p2.y * p1.x) / deltaX;
		var coefficient1 = (p2.y - p1.y) / deltaX;
		double coefficient2 = -1;

		return new Constraint(essId + ": Max Apparent Power", new LinearCoefficient[] { //
				new LinearCoefficient(coefficients.of(essId, phase, Pwr.ACTIVE), coefficient1), //
				new LinearCoefficient(coefficients.of(essId, phase, Pwr.REACTIVE), coefficient2) //
		}, relationship, constraintValue);
	}
}
