package io.openems.core.utilities.power.symmetric;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.util.GeometricShapeFactory;

public class SMaxLimitation extends Limitation {

	protected final GeometricShapeFactory shapeFactory = new GeometricShapeFactory();
	private Geometry circle;
	private Long sMax;

	public SMaxLimitation(SymmetricPower power) {
		super(power);
	}

	public void setSMax(Long sMax, Long xNull, Long yNull) {
		if (sMax != this.sMax) {
			if (sMax != null) {
				shapeFactory.setCentre(new Coordinate(xNull,yNull));
				shapeFactory.setSize(sMax*2);
				shapeFactory.setNumPoints(32);
				this.circle = shapeFactory.createCircle();
			} else {
				this.circle = null;
			}
			this.sMax = sMax;
			notifyListeners();
		}
	}

	@Override
	public Geometry applyLimit(Geometry geometry) throws PowerException {
		if (this.circle != null) {
			Geometry newGeometry = geometry.intersection(this.circle);
			if (newGeometry.isEmpty()) {
				throw new PowerException(
						"The ApparentPower limitation is too small! There needs to be at least one point after the limitation.");
			}
			return newGeometry;
		}
		return geometry;
	}

	@Override
	public String toString() {
		return "No apparentpower greater than "+sMax+".";
	}
}
