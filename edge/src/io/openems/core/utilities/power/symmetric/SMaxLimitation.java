package io.openems.core.utilities.power.symmetric;

public class SMaxLimitation extends Limitation {
	//MOVED TO OSGi
	//	private Geometry circle;
	//	private Long sMax;
	//
	//	public SMaxLimitation(SymmetricPower power) {
	//		super(power);
	//	}
	//
	//	public void setSMax(Long sMax, Long xNull, Long yNull) {
	//		if (sMax != this.sMax) {
	//			if (sMax != null) {
	//				GeometricShapeFactory shapeFactory = SymmetricPowerImpl.getShapefactory();
	//				shapeFactory.setCentre(new Coordinate(xNull,yNull));
	//				shapeFactory.setSize(sMax*2);
	//				shapeFactory.setNumPoints(32);
	//				this.circle = shapeFactory.createCircle();
	//			} else {
	//				this.circle = null;
	//			}
	//			this.sMax = sMax;
	//			notifyListeners();
	//		}
	//	}
	//
	//	@Override
	//	public Geometry applyLimit(Geometry geometry) throws PowerException {
	//		if (this.circle != null) {
	//			Geometry newGeometry = geometry.intersection(this.circle);
	//			if (newGeometry.isEmpty()) {
	//				throw new PowerException(
	//						"The ApparentPower limitation is too small! There needs to be at least one point after the limitation.");
	//			}
	//			return newGeometry;
	//		}
	//		return geometry;
	//	}
	//
	//	@Override
	//	public String toString() {
	//		return "No apparentpower greater than "+sMax+".";
	//	}
}
