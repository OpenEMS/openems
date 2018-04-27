package io.openems.core.utilities.power.symmetric;

public class CosPhiLineCharacteristicLimitation extends Limitation {
	// MOVED TO OSGi
	//	private final TreeMap<Long, Double> characteristic = new TreeMap<>();
	//	private Long xNull;
	//	private Long yNull;
	//	private Geometry line;
	//
	//	public CosPhiLineCharacteristicLimitation(SymmetricPower power) {
	//		super(power);
	//	}
	//
	//	public void setCosPhi(Long xNull, Long yNull, TreeMap<Long, Double> characteristic) {
	//		if (this.characteristic.equals(characteristic) || this.xNull != xNull || this.yNull != yNull) {
	//			if (characteristic != null && !characteristic.isEmpty() && xNull != null && yNull != null) {
	//				long maxApparentPower = power.getMaxApparentPower();
	//				boolean isFirs = true;
	//				List<Coordinate> coordinates = new ArrayList<>();
	//				Double y = null;
	//				for (Entry<Long, Double> point : characteristic.entrySet()) {
	//					double cosPhi = point.getValue();
	//					double m = Math.tan(Math.acos(Math.abs(cosPhi)));
	//					double x = point.getKey();
	//					double t = yNull - m * xNull;
	//					if ((x < 0 && cosPhi > 0) || (x > 0 && cosPhi > 0)) {
	//						m *= -1;
	//					}
	//					y = m * x + t;
	//					if (isFirs) {
	//						coordinates.add(new Coordinate(maxApparentPower * -1, y));
	//						isFirs = false;
	//					}
	//					coordinates.add(new Coordinate(x, y));
	//				}
	//				if (y != null) {
	//					coordinates.add(new Coordinate(maxApparentPower, y));
	//				}
	//				line = SymmetricPowerImpl.getFactory()
	//						.createLineString(coordinates.toArray(new Coordinate[coordinates.size()]));
	//			} else {
	//				line = null;
	//			}
	//			this.characteristic.clear();
	//			this.characteristic.putAll(characteristic);
	//			this.xNull = xNull;
	//			this.yNull = yNull;
	//			notifyListeners();
	//		}
	//	}
	//
	//	@Override
	//	protected Geometry applyLimit(Geometry geometry) throws PowerException {
	//		if (line != null) {
	//			Geometry newGeometry = geometry.intersection(line);
	//			if (newGeometry.isEmpty()) {
	//				DistanceOp distance = new DistanceOp(geometry, line);
	//				GeometryLocation[] locations = distance.nearestLocations();
	//				for (GeometryLocation location : locations) {
	//					if (!location.getGeometryComponent().equals(line)) {
	//						return SymmetricPowerImpl.getFactory().createPoint(location.getCoordinate());
	//					}
	//				}
	//			} else {
	//				return newGeometry;
	//			}
	//		}
	//		return geometry;
	//	}
	//
	//	@Override
	//	public String toString() {
	//		// TODO
	//		return null;
	//	}

}
