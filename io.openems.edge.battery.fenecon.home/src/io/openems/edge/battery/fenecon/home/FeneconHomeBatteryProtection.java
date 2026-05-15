package io.openems.edge.battery.fenecon.home;

import java.util.function.IntSupplier;

import io.openems.edge.battery.fenecon.home.statemachine.StateMachine;
import io.openems.edge.battery.protection.BatteryProtectionDefinition;

public abstract class FeneconHomeBatteryProtection implements BatteryProtectionDefinition {

	private final IntSupplier forceChargeDischargeCurrent;
	private final StateMachine stateMachine;

	public FeneconHomeBatteryProtection(IntSupplier forceChargeDischargeCurrent, StateMachine stateMachine) {
		this.forceChargeDischargeCurrent = forceChargeDischargeCurrent;
		this.stateMachine = stateMachine;
	}

	/**
	 * Creates a {@link BatteryProtectionDefinition} for the given type.
	 * 
	 * @param type                 the {@link BatteryFeneconHomeHardwareType}
	 * @param forceCurrentSupplier supplier for forced charge/discharge current
	 * @param stateMachine         Battery state machine
	 * @return a {@link BatteryProtectionDefinition}
	 */
	public static BatteryProtectionDefinition createProtection(BatteryFeneconHomeHardwareType type,
			IntSupplier forceCurrentSupplier, StateMachine stateMachine) {
		return switch (type) {
		case BATTERY_52 -> new FeneconHomeBatteryProtection52(forceCurrentSupplier, stateMachine);
		case BATTERY_64 -> new FeneconHomeBatteryProtection64(forceCurrentSupplier, stateMachine);
		};
	}

	@Override
	public IntSupplier getForceChargeDischargeCurrent() {
		return this.forceChargeDischargeCurrent;
	}

	@Override
	public boolean isChargeAllowed() {
		return this.stateMachine.isChargeAllowed();
	}

	@Override
	public boolean isDischargeAllowed() {
		return this.stateMachine.isDischargeAllowed();
	}
}