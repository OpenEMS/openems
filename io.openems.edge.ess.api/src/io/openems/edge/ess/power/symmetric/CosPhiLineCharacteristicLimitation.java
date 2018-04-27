package io.openems.edge.ess.power.symmetric;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.operation.distance.DistanceOp;
import com.vividsolutions.jts.operation.distance.GeometryLocation;

import io.openems.edge.ess.power.PowerException;

public class CosPhiLineCharacteristicLimitation extends Limitation {

	private final TreeMap<Long, Double> characteristic = new TreeMap<>();

	private Geometry line = null;
	private Long xNull = null;
	private Long yNull = null;

	public CosPhiLineCharacteristicLimitation(SymmetricPower power) {
		super(power);
	}

	public CosPhiLineCharacteristicLimitation setCosPhi(Long xNull, Long yNull, TreeMap<Long, Double> characteristic) {
		if (this.characteristic.equals(characteristic) || this.xNull != xNull || this.yNull != yNull) {
			if (characteristic != null && !characteristic.isEmpty() && xNull != null && yNull != null) {
				long maxApparentPower = this.power.getMaxApparentPower();
				boolean isFirs = true;
				List<Coordinate> coordinates = new ArrayList<>();
				Double y = null;
				for (Entry<Long, Double> point : characteristic.entrySet()) {
					double cosPhi = point.getValue();
					double m = Math.tan(Math.acos(Math.abs(cosPhi)));
					double x = point.getKey();
					double t = yNull - m * xNull;
					if ((x < 0 && cosPhi > 0) || (x > 0 && cosPhi > 0)) {
						m *= -1;
					}
					y = m * x + t;
					if (isFirs) {
						coordinates.add(new Coordinate(maxApparentPower * -1, y));
						isFirs = false;
					}
					coordinates.add(new Coordinate(x, y));
				}
				if (y != null) {
					coordinates.add(new Coordinate(maxApparentPower, y));
				}
				this.line = Utils.FACTORY.createLineString(coordinates.toArray(new Coordinate[coordinates.size()]));
			} else {
				this.line = null;
			}
			this.characteristic.clear();
			this.characteristic.putAll(characteristic);
			this.xNull = xNull;
			this.yNull = yNull;
			this.emitOnChangeEvent();
		}
		return this;
	}

	@Override
	protected Geometry applyLimit(Geometry geometry) throws PowerException {
		if (this.line != null) {
			Geometry newGeometry = geometry.intersection(this.line);
			if (newGeometry.isEmpty()) {
				DistanceOp distance = new DistanceOp(geometry, this.line);
				GeometryLocation[] locations = distance.nearestLocations();
				for (GeometryLocation location : locations) {
					if (!location.getGeometryComponent().equals(this.line)) {
						return Utils.FACTORY.createPoint(location.getCoordinate());
					}
				}
			} else {
				return newGeometry;
			}
		}
		return geometry;
	}

	@Override
	public String toString() {
		return "CosPhiLineCharacteristicLimitation [x=" + xNull + ", y=" + yNull + "]";
	}

}
