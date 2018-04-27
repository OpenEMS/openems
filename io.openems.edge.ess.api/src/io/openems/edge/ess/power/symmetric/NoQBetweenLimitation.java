package io.openems.edge.ess.power.symmetric;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import io.openems.edge.ess.power.PowerException;

/**
 * Do not allow ReactivePower between two values
 *
 */
public class NoQBetweenLimitation extends Limitation {

	private Geometry rect = null;
	private Integer qMin = null;
	private Integer qMax = null;

	public NoQBetweenLimitation(SymmetricPower power) {
		super(power);
	}

	public NoQBetweenLimitation setQ(Integer qMin, Integer qMax) {
		if (qMin != this.qMin || qMax != this.qMax) {
			if (qMin != null && qMax != null) {
				long pMin = power.getMaxApparentPower() * -1;
				long pMax = power.getMaxApparentPower();
				Coordinate[] coordinates = new Coordinate[] { new Coordinate(pMin, qMax - 0.1),
						new Coordinate(pMin, qMin + 0.1), new Coordinate(pMax, qMin + 0.1),
						new Coordinate(pMax, qMax - 0.1), new Coordinate(pMin, qMax - 0.1) };
				this.rect = Utils.FACTORY.createPolygon(coordinates);
			} else {
				this.rect = null;
			}
			this.qMin = qMin;
			this.qMax = qMax;
			this.emitOnChangeEvent();
		}
		return this;
	}

	@Override
	protected Geometry applyLimit(Geometry geometry) throws PowerException {
		if (this.rect != null) {
			Geometry newGeometry = geometry.difference(this.rect);
			if (newGeometry.isEmpty()) {
				throw new PowerException("NoQBetweenLimitation [q < " + this.qMin + " || q > " + this.qMax
						+ "] is too restrictive! There needs to be at least one point after the limitation.");
			}
			return newGeometry;
		}
		return geometry;
	}

	@Override
	public String toString() {
		return "NoQBetweenLimitation [qMin=" + qMin + ", qMax=" + qMax + "]";
	}

}
