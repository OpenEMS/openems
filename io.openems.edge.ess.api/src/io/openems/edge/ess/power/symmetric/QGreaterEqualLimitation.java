package io.openems.edge.ess.power.symmetric;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import io.openems.edge.ess.power.PowerException;

public class QGreaterEqualLimitation extends Limitation {

	private Geometry rect = null;
	private Integer q = null;

	public QGreaterEqualLimitation(SymmetricPower power) {
		super(power);
	}

	public QGreaterEqualLimitation setQ(Integer q) {
		if (q != this.q) {
			if (q != null) {
				long pMin = power.getMaxApparentPower() * -1 - 1;
				long pMax = power.getMaxApparentPower() + 1;
				long qMin = q;
				long qMax = power.getMaxApparentPower() + 1;
				Coordinate[] coordinates = new Coordinate[] { new Coordinate(pMin, qMax), new Coordinate(pMin, qMin),
						new Coordinate(pMax, qMin), new Coordinate(pMax, qMax), new Coordinate(pMin, qMax) };
				this.rect = Utils.FACTORY.createPolygon(coordinates);
			} else {
				this.rect = null;
			}
			this.q = q;
			this.emitOnChangeEvent();
		}
		return this;
	}

	@Override
	public Geometry applyLimit(Geometry geometry) throws PowerException {
		if (this.rect != null) {
			Geometry newGeometry = geometry.intersection(this.rect);
			if (newGeometry.isEmpty()) {
				throw new PowerException("QGreaterEqualLimitation [q >= " + this.q
						+ "] is too restrictive! There needs to be at least one point after the limitation.");
			}
			return newGeometry;
		}
		return geometry;
	}

	@Override
	public String toString() {
		return "QGreaterEqualLimitation [q=" + q + "]";
	}

}
