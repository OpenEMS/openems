package io.openems.edge.battery.soltaro.common.batteryprotection;

import io.openems.edge.common.linecharacteristic.PolyLine;

/**
 * Soltaro Battery-Protection for modules with 3.5 kWh.
 */
public class BatteryProtectionDefinitionSoltaro3500Wh extends AbstractBatteryProtectionDefinitionSoltaro {

	// Over voltage Protection
	@Override
	public PolyLine getChargeVoltageToPercent() {
		return PolyLine.create() //
				.addPoint(3010, 0.1) //
				.addPoint(Math.nextUp(3010), 1) //
				.addPoint(Math.nextDown(3460), 1) //
				.addPoint(3460, 0.99) //
				.addPoint(3560, 0.02) //
				.addPoint(3605, 0.01) //
				.addPoint(Math.nextDown(3615), 0.01) //
				.addPoint(3615, 0) //
				.build();
	}

}