package io.openems.edge.ess.power.symmetric;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import io.openems.edge.ess.power.PowerException;

/**
 * Power has to be between CosPhi inductive and capacitive
 *
 */
public class MaxCosPhiLimitation extends Limitation {

	private Geometry polygon = null;
	private Double cosPhi = null;

	public MaxCosPhiLimitation(SymmetricPower power) {
		super(power);
	}

	public MaxCosPhiLimitation setMaxCosPhi(Double cosPhi) {
		if (cosPhi != this.cosPhi) {
			if (cosPhi != null) {
				int maxApparentPower = this.power.getMaxApparentPower();
				double m = Math.tan(Math.acos(cosPhi));
				double y = m * maxApparentPower;
				Coordinate[] coordinates = new Coordinate[] { new Coordinate(ZERO), new Coordinate(maxApparentPower, y),
						new Coordinate(maxApparentPower, y * -1), new Coordinate(ZERO),
						new Coordinate(maxApparentPower * -1, y * -1), new Coordinate(maxApparentPower * -1, y),
						new Coordinate(ZERO) };
				this.polygon = Utils.FACTORY.createPolygon(coordinates);
			} else {
				this.polygon = null;
			}
			this.cosPhi = cosPhi;
			this.emitOnChangeEvent();
		}
		return this;
	}

	@Override
	public Geometry applyLimit(Geometry geometry) throws PowerException {
		if (this.polygon != null) {
			Geometry newGeometry = geometry.intersection(this.polygon);
			if (newGeometry.isEmpty()) {
				throw new PowerException("MaxCosPhiLimitation [CosPhi <= " + this.cosPhi
						+ "] is too restrictive! There needs to be at least one point after the limitation.");
			}
			return newGeometry;
		}
		return geometry;
	}

	@Override
	public String toString() {
		return "MaxCosPhiLimitation [polygon=" + polygon + ", cosPhi=" + cosPhi + "]";
	}

}
