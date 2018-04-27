package io.openems.edge.ess.power.symmetric;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import io.openems.edge.ess.power.PowerException;

/**
 * Do not allow ActivePower between two values
 */
public class NoPBetweenLimitation extends Limitation {

	private Geometry rect = null;
	private Integer pMin = null;
	private Integer pMax = null;

	public NoPBetweenLimitation(SymmetricPower power) {
		super(power);
	}

	public NoPBetweenLimitation setP(Integer pMin, Integer pMax) {
		if (pMin != this.pMin || pMax != this.pMax) {
			if (pMin != null && pMax != null) {
				long qMin = power.getMaxApparentPower() * -1;
				long qMax = power.getMaxApparentPower();
				Coordinate[] coordinates = new Coordinate[] { new Coordinate(pMin + 0.1, qMax),
						new Coordinate(pMin + 0.1, qMin), new Coordinate(pMax - 0.1, qMin),
						new Coordinate(pMax - 0.1, qMax), new Coordinate(pMin + 0.1, qMax) };
				this.rect = Utils.FACTORY.createPolygon(coordinates);
			} else {
				this.rect = null;
			}
			this.pMin = pMin;
			this.pMax = pMax;
			notifyListeners();
		}
		return this;
	}

	@Override
	protected Geometry applyLimit(Geometry geometry) throws PowerException {
		if (this.rect != null) {
			Geometry newGeometry = geometry.difference(this.rect);
			if (newGeometry.isEmpty()) {
				throw new PowerException("NoPBetweenLimitation [p < " + this.pMin + " || p > " + this.pMax
						+ "] is too restrictive! There needs to be at least one point after the limitation.");
			}
			return newGeometry;
		}
		return geometry;
	}

	@Override
	public String toString() {
		return "NoPBetweenLimitation [pMin=" + pMin + ", pMax=" + pMax + "]";
	}

}
