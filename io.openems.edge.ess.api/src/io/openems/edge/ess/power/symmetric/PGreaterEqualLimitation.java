package io.openems.edge.ess.power.symmetric;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import io.openems.edge.ess.power.PowerException;

public class PGreaterEqualLimitation extends Limitation {

	private Geometry rect = null;
	private Integer p = null;

	public PGreaterEqualLimitation(SymmetricPower power) {
		super(power);
	}

	public PGreaterEqualLimitation setP(Integer p) {
		if (p != this.p) {
			if (p != null) {
				long pMin = p;
				long pMax = power.getMaxApparentPower() + 1;
				long qMin = power.getMaxApparentPower() * -1 - 1;
				long qMax = power.getMaxApparentPower() + 1;
				Coordinate[] coordinates = new Coordinate[] { new Coordinate(pMin, qMax), new Coordinate(pMin, qMin),
						new Coordinate(pMax, qMin), new Coordinate(pMax, qMax), new Coordinate(pMin, qMax) };
				this.rect = Utils.FACTORY.createPolygon(coordinates);
			} else {
				this.rect = null;
			}
			this.p = p;
			this.emitOnChangeEvent();
		}
		return this;
	}

	@Override
	public Geometry applyLimit(Geometry geometry) throws PowerException {
		if (this.rect != null) {
			Geometry newGeometry = geometry.intersection(this.rect);
			if (newGeometry.isEmpty()) {
				throw new PowerException("PGreaterEqualLimitation [p >= " + this.p
						+ "] is too restrictive! There needs to be at least one point after the limitation.");
			}
			return newGeometry;
		}
		return geometry;
	}

	@Override
	public String toString() {
		return "PGreaterEqualLimitation [p=" + p + "]";
	}
}
