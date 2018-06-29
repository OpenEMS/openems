package io.openems.edge.ess.power;

import java.util.Optional;

import org.apache.commons.math3.optim.linear.LinearConstraint;
import org.apache.commons.math3.optim.linear.Relationship;

/**
 * Defines a constraint that forces points to be inside a circle. The circle is
 * defined as LinearConstraints in the form of tangents to the circle.
 * 
 * This can be used for a MaxApparentPower constraint.
 */
public class CircleConstraint extends AbstractConstraint {

	private final static int CIRCLE_SECTIONS_PER_QUARTER = 1; // don't set higher than 90

	private final int pIndex;
	private final int qIndex;

	private Optional<Integer> radiusOpt;

	public CircleConstraint(int noOfCoefficients, int pIndex, int qIndex, Integer radius, String note) {
		super(noOfCoefficients, note);
		this.pIndex = pIndex;
		this.qIndex = qIndex;
		this.radiusOpt = Optional.ofNullable(radius);
	}

	public void setRadius(Integer radius) {
		this.radiusOpt = Optional.ofNullable(radius);
	}

	@Override
	public LinearConstraint[] getConstraints() {
		if (this.radiusOpt.isPresent()) {
			int radius = this.radiusOpt.get();
			LinearConstraint[] constraints = new LinearConstraint[CIRCLE_SECTIONS_PER_QUARTER * 4];
			double degreeDelta = 90.0 / CIRCLE_SECTIONS_PER_QUARTER;
			Point p1 = this.getPointOnCircle(radius, 0);

			int i = 0;
			for (double degree = degreeDelta; Math.floor(degree) <= 360; degree += degreeDelta) {
				Point p2 = this.getPointOnCircle(radius, degree);

				Relationship relationship;
				if (Math.floor(degree) <= 180) {
					relationship = Relationship.GEQ;
				} else {
					relationship = Relationship.LEQ;
				}

				constraints[i++] = this.getConstraintThroughPoints(p1, p2, relationship);

				// set p2 -> p1 for next loop
				p1 = p2;
			}
			return constraints;
		} else {
			return new LinearConstraint[] {};
		}
	}

	private Point getPointOnCircle(double radius, double degree) {
		return new Point(Math.cos(Math.toRadians(degree)) * radius, Math.sin(Math.toRadians(degree)) * radius);
	}

	private LinearConstraint getConstraintThroughPoints(Point p1, Point p2, Relationship relationship) {
		/**
		 * Build the LinearConstraint.
		 * 
		 * <pre>
		 *  We use the formula:
		 *  y = ((y2-y1)/(x2-x1)) * x + ((x2*y1-x1*y2)/(x2-x1))
		 * </pre>
		 */
		double constraintValue = -1 * (p1.y * p2.x - p2.y * p1.x) / (p2.x - p1.x);
		double coefficient1 = (p2.y - p1.y) / (p2.x - p1.x);
		double coefficient2 = -1;

		double[] coefficients = initializeCoefficients();
		coefficients[this.pIndex] = coefficient1;
		coefficients[this.qIndex] = coefficient2;

		return new LinearConstraint(coefficients, relationship, constraintValue);
	}

	private class Point {
		protected final double x;
		protected final double y;

		Point(double x, double y) {
			this.x = x;
			this.y = y;
		}

		@Override
		public String toString() {
			return "Point [x=" + Math.round(x) + ", y=" + Math.round(y) + "]";
		}
	}

}
