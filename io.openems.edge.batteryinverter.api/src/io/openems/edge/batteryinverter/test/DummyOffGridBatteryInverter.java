package io.openems.edge.batteryinverter.test;

import io.openems.edge.batteryinverter.api.OffGridBatteryInverter;

public class DummyOffGridBatteryInverter extends DummyManagedSymmetricBatteryInverter
		implements OffGridBatteryInverter {

	public DummyOffGridBatteryInverter(String id) {
		super(id);
	}

	@Override
	public void setTargetGridMode(TargetGridMode targetGridMode) {
		System.out.println("setTargetGridMode: " + targetGridMode);
	}

}
