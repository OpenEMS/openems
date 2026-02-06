package io.openems.edge.battery.fenecon.home;

import java.util.function.IntSupplier;

import io.openems.edge.battery.protection.BatteryProtectionDefinition;

public abstract class FeneconHomeBatteryProtection implements BatteryProtectionDefinition {

	private final IntSupplier forceChargeDischargeCurrent;

	public FeneconHomeBatteryProtection(IntSupplier forceChargeDischargeCurrent) {
		this.forceChargeDischargeCurrent = forceChargeDischargeCurrent;
	}

	/**
	 * Creates a {@link BatteryProtectionDefinition} for the given type.
	 * 
	 * @param type                 the {@link BatteryFeneconHomeHardwareType}
	 * @param forceCurrentSupplier supplier for forced charge/discharge current
	 * @return a {@link BatteryProtectionDefinition}
	 */
	public static BatteryProtectionDefinition createProtection(BatteryFeneconHomeHardwareType type,
			IntSupplier forceCurrentSupplier) {
		return switch (type) {
		case BATTERY_52 -> new FeneconHomeBatteryProtection52(forceCurrentSupplier);
		case BATTERY_64 -> new FeneconHomeBatteryProtection64(forceCurrentSupplier);
		};
	}

	@Override
	public IntSupplier getForceChargeDischargeCurrent() {
		return this.forceChargeDischargeCurrent;
	}
}