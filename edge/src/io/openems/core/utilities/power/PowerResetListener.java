package io.openems.core.utilities.power;

import com.vividsolutions.jts.geom.Geometry;

public interface PowerResetListener {

	Geometry afterPowerReset(Geometry allowedPower);

}
