package io.openems.core.utilities.power;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Stopwatch;
import com.vividsolutions.jts.densify.Densifier;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.util.AffineTransformation;

public class Test {

	public static void main(String[] args) throws PowerException {
		Stopwatch sw = Stopwatch.createStarted();
		SymmetricPowerImpl power = new SymmetricPowerImpl(100000, null, null);
		SymmetricPowerImpl power2 = new SymmetricPowerImpl(100000, null, null);
		System.out.println(sw.elapsed().toMillis());
		sw.reset();
		sw.start();
		QGreaterEqualLimitation limit1 = new QGreaterEqualLimitation(power);
		limit1.setQ(-1100L);
		power.applyLimitation(limit1);
		NoPBetweenLimitation limit2 = new NoPBetweenLimitation(power);
		limit2.setP(0L, 5000L);
		power2.applyLimitation(limit2);
		//		CosPhiLineLimitation limit = new CosPhiLineLimitation(power);
		//		limit.setCosPhi(0.8, true, 0L, 0L);
		//		power2.applyLimitation(limit);
		// MaxCosPhiLimitation limit = new MaxCosPhiLimitation(power);
		// limit.setMaxCosPhi(0.8);
		// power.applyLimitation(limit);
		//		CosPhiLineCharacteristicLimitation limit = new CosPhiLineCharacteristicLimitation(power);
		//		TreeMap<Long, Double> characteristic = new TreeMap<>();
		//		characteristic.put(-2000L, -0.75);
		//		characteristic.put(0L, 1.0);
		//		characteristic.put(2000L, -0.9);
		//		limit.setCosPhi(0L, 0L, characteristic);
		//		power.applyLimitation(limit);
		System.out.println(sw.elapsed().toMillis());
		sw.reset();
		System.out.println(power.getMaxP());
		System.out.println(power.getMinP());
		System.out.println(power.getMaxQ());
		System.out.println(power.getMinQ());
		System.out.println(power.getAsSVG());
		sw.start();
		Geometry g = getUnionAround(power.getGeometry(), power2.getGeometry());
		System.out.println(sw.elapsed().toMillis());
		System.out.println(SymmetricPowerImpl.getAsSVG(g));
	}

	private static Geometry getUnionAround(Geometry g1,Geometry g2) {
		GeometryFactory factory = new GeometryFactory();
		Geometry g1dens = Densifier.densify(g1, 10000);
		Geometry g2dens = Densifier.densify(g2, 10000);
		List<Geometry> geometries = new ArrayList<>();
		geometries.add(g1);
		for (Coordinate c : g1dens.getCoordinates()) {
			geometries.add(AffineTransformation.translationInstance(c.x, c.y).transform(g2));
		}
		geometries.add(g2);
		for (Coordinate c : g2dens.getCoordinates()) {
			geometries.add(AffineTransformation.translationInstance(c.x, c.y).transform(g1));
		}
		GeometryCollection collection = new GeometryCollection(geometries.toArray(new Geometry[geometries.size()]), factory);
		return collection.union();
	}

}
