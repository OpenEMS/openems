package io.openems.core.utilities.power;

import com.vividsolutions.jts.geom.Geometry;

public interface PowerChangeListener {
	void powerChanged(Geometry allowedPower);
}
