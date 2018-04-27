package io.openems.edge.ess.power.symmetric;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.util.GeometricShapeFactory;

import io.openems.edge.ess.power.PowerException;

public class SMaxLimitation extends Limitation {

	private Geometry circle = null;
	private Integer sMax = null;

	public SMaxLimitation(SymmetricPower power) {
		super(power);
	}

	public SMaxLimitation setSMax(Integer sMax, int xNull, int yNull) {
		if (sMax != this.sMax) {
			if (sMax == null) {
				this.circle = null;

			} else if (sMax == 0) {
				this.circle = Utils.FACTORY.createPoint(new Coordinate(xNull, yNull));

			} else {
				GeometricShapeFactory shapeFactory = new GeometricShapeFactory(Utils.FACTORY);
				shapeFactory.setCentre(new Coordinate(xNull, yNull));
				shapeFactory.setSize(sMax * 2);
				shapeFactory.setNumPoints(32);
				this.circle = shapeFactory.createCircle();
			}
			this.sMax = sMax;
			notifyListeners();
		}
		return this;
	}

	@Override
	public Geometry applyLimit(Geometry geometry) throws PowerException {
		if (this.circle != null) {
			Geometry newGeometry = geometry.intersection(this.circle);
			if (newGeometry.isEmpty()) {
				throw new PowerException("SMaxLimitation [ApparentPower <= " + this.sMax
						+ "] is too restrictive! There needs to be at least one point after the limitation.");
			}
			return newGeometry;
		}
		return geometry;
	}

	@Override
	public String toString() {
		return "SMaxLimitation [sMax=" + sMax + "]";
	}
}
