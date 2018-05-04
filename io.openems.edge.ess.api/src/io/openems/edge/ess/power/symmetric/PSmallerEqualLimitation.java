package io.openems.edge.ess.power.symmetric;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

import io.openems.edge.ess.power.PowerException;

public class PSmallerEqualLimitation extends Limitation {

	private Geometry rect = null;
	private Integer p = null;

	public PSmallerEqualLimitation(SymmetricPower power) {
		super(power);
	}

	public PSmallerEqualLimitation setP(Integer p) {
		if (p == this.p || (p != null && p.equals(this.p))) {
			return this;
		}

		if (p != null) {
			long pMin = power.getMaxApparentPower() * -1 - 1;
			long pMax = p + 1;
			long qMin = power.getMaxApparentPower() * -1 - 1;
			long qMax = power.getMaxApparentPower() + 1;
			this.rect = Utils.FACTORY.toGeometry(new Envelope(pMin, pMax, qMin, qMax));
		} else {
			this.rect = null;
		}
		this.p = p;
		this.emitOnChangeEvent();
		return this;
	}

	@Override
	public Geometry applyLimit(Geometry geometry) throws PowerException {
		if (this.rect != null) {
			Geometry newGeometry = geometry.intersection(this.rect);
			if (newGeometry.isEmpty()) {
				throw new PowerException("PSmallerEqualLimitation [p <= " + this.p
						+ "] is too restrictive! There needs to be at least one point after the limitation.");
			}
			return newGeometry;
		}
		return geometry;
	}

	@Override
	public String toString() {
		return "PSmallerEqualLimitation [p=" + p + "]";
	}

}
