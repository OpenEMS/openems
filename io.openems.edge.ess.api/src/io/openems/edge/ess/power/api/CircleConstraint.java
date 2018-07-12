package io.openems.edge.ess.power.api;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.optim.linear.Relationship;

import com.google.common.base.Objects;

import io.openems.edge.ess.api.ManagedSymmetricEss;

/**
 * Defines constraints that force points to be inside a circle. The circle is
 * defined as a list of Constraints in the form of tangents to the circle.
 * 
 * This can be used for a MaxApparentPower constraint.
 */
public class CircleConstraint {

	private final static int CIRCLE_SECTIONS_PER_QUARTER = 1; // don't set higher than 90

	private final ManagedSymmetricEss ess;
	private final List<Constraint> constraints = new ArrayList<>();

	private Integer radius = null;

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

	public CircleConstraint(ManagedSymmetricEss ess) {
		this.ess = ess;
	}

	public CircleConstraint(ManagedSymmetricEss ess, int radius) {
		this(ess);
		this.setRadius(radius);
	}

	/**
	 * Disable these constraints
	 */
	public void disable() {
		this.setRadius(null);
	}

	public synchronized void setRadius(Integer radius) {
		if (Objects.equal(radius, this.radius)) {
			// unchanged -> nothing to do
			return;
		}

		// store radius
		this.radius = radius;

		// remove all existing constraints from power
		this.constraints.forEach(constraint -> {
			this.ess.getPower().removeConstraint(constraint);
		});
		this.constraints.clear();

		if (radius == null) {
			// no new radius given -> stop
			return;
		}

		double degreeDelta = 90.0 / CIRCLE_SECTIONS_PER_QUARTER;
		Point p1 = this.getPointOnCircle(radius, 0);

		for (double degree = degreeDelta; Math.floor(degree) <= 360; degree += degreeDelta) {
			Point p2 = this.getPointOnCircle(radius, degree);

			Relationship relationship;
			if (Math.floor(degree) <= 180) {
				relationship = Relationship.GEQ;
			} else {
				relationship = Relationship.LEQ;
			}

			Constraint constraint = this.getConstraintThroughPoints(p1, p2, relationship);
			this.ess.getPower().addConstraint(constraint);
			this.constraints.add(constraint);

			// set p2 -> p1 for next loop
			p1 = p2;
		}
	}

	private Point getPointOnCircle(double radius, double degree) {
		return new Point(Math.cos(Math.toRadians(degree)) * radius, Math.sin(Math.toRadians(degree)) * radius);
	}

	private Constraint getConstraintThroughPoints(Point p1, Point p2, Relationship relationship) {
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

		return new Constraint(ConstraintType.STATIC, //
				new Coefficient[] { //
						new Coefficient(this.ess, Phase.ALL, Pwr.ACTIVE, coefficient1), //
						new Coefficient(this.ess, Phase.ALL, Pwr.REACTIVE, coefficient2) //
				}, relationship, constraintValue);
	}
}
