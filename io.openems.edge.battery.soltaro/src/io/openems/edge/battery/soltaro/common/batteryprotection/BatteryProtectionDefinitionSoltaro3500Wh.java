package io.openems.edge.battery.soltaro.common.batteryprotection;

import io.openems.edge.common.linecharacteristic.PolyLine;

/**
 * Soltaro Battery-Protection for modules with 3.5 kWh.
 */
public class BatteryProtectionDefinitionSoltaro3500Wh extends AbstractBatteryProtectionDefinitionSoltaro {

	@Override
	public PolyLine getChargeVoltageToPercent() {
		return PolyLine.create() //
				.addPoint(3000, 0.1) //
				.addPoint(Math.nextUp(3000), 1) //
				.addPoint(3350, 1) //
				.addPoint(3450, 0.9999) //
				.addPoint(3600, 0.01) //
				.addPoint(Math.nextDown(3650), 0.01) //
				.addPoint(3650, 0) //
				.build();
	}
}