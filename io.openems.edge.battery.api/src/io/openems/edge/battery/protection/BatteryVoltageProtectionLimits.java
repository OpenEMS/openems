package io.openems.edge.battery.protection;

import java.util.function.BiFunction;
import java.util.function.Consumer;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.common.type.TypeUtils;

public final class BatteryVoltageProtectionLimits<T extends Battery & BatteryVoltageProtection> {

	private final T battery;
	private final BatteryVoltageSpecification specification;

	public BatteryVoltageProtectionLimits(BatteryVoltageSpecification specification, T battery) {
		this.battery = battery;
		this.specification = specification;
	}

	public record BatteryVoltageSpecification(int cells, //
			int modules, //
			int maximumCellVoltageLimit, //
			int minimumCellVoltageLimit, //
			int maximumCellVoltageOperationLimit, //
			int minimumCellVoltageOperationLimit, //
			int maximumChargeVoltage, //
			int minimumDischargeVoltage//
	) {
	}

	/**
	 * Updates the battery voltage protection values based on the given limit
	 * parameters.
	 */
	public void updateLimits() {
		this.updateDynamicVoltageLimits(TypeUtils::sum, true, this.battery::_setChargeMaxVoltage, Math::min,
				((BatteryVoltageProtection) this.battery)::_setBvpChargeBms);
		this.updateDynamicVoltageLimits(TypeUtils::subtract, false, this.battery::_setDischargeMinVoltage, Math::max,
				((BatteryVoltageProtection) this.battery)::_setBvpDischargeBms);
	}

	/**
	 * Dynamically calculates battery voltage extremes and operational voltage limit
	 * in every cycle.
	 * 
	 * @param voltageAdjustmentMethod sum for Charge and subtract for discharge
	 * @param isCharging              charge-discharge direction true for charge
	 * @param setBatteryMethod        for charge
	 *                                {@link Battery#_setChargeMaxVoltage(int)} and
	 *                                for discharge
	 *                                {@link Battery#_setDischargeMinVoltage(int)}
	 * @param minMax                  {@link Math#min(double, double) or
	 *                                Math#max(double, double)}
	 * @param setBvpMethod            {@link BatteryVoltageProtection#_setBvpChargeBms(Integer)}
	 *                                or
	 *                                {@link BatteryVoltageProtection#_setBvpDischargeBms(Integer)}
	 */
	private void updateDynamicVoltageLimits(BiFunction<Integer, Integer, Integer> voltageAdjustmentMethod,
			boolean isCharging, Consumer<Integer> setBatteryMethod, BiFunction<Integer, Integer, Integer> minMax,
			Consumer<Integer> setBvpMethod) {
		final var cells = this.specification.cells();
		final var modules = this.specification.modules();
		final var batteryVoltage = TypeUtils.multiply(this.battery.getVoltage().orElse(0), 1000);

		if (!this.battery.isStarted()) {
			setBatteryMethod.accept(null);
			setBvpMethod.accept(null);
			return;
		}

		// Calculate the battery Charge_Max_Voltage, Discharge_Min_Voltage
		var result = isCharging //
				? TypeUtils.subtract(this.specification.maximumCellVoltageLimit(),
						this.battery.getMaxCellVoltage().orElse(0))//
				: TypeUtils.subtract(this.battery.getMinCellVoltage().orElse(0),
						this.specification.minimumCellVoltageLimit());
		var packVoltage = TypeUtils.multiply(result, cells, modules);
		var sum = voltageAdjustmentMethod.apply(batteryVoltage, packVoltage);
		setBatteryMethod.accept(TypeUtils.divide(sum, 1000));

		// Calculate the battery operational limit Bvp_Charge_Bms. Bvp_Discharge_Bms
		var bvpLimit = //
				isCharging //
						? TypeUtils.subtract(this.specification.maximumCellVoltageOperationLimit(),
								this.battery.getMaxCellVoltage().orElse(0))//
						: TypeUtils.subtract(this.specification.minimumCellVoltageOperationLimit(),
								this.battery.getMinCellVoltage().orElse(0));

		var bvpOperationLimit = minMax.apply(//
				isCharging //
						? this.specification.maximumChargeVoltage() //
						: this.specification.minimumDischargeVoltage(), //
				TypeUtils.sum(//
						batteryVoltage, //
						TypeUtils.multiply(//
								bvpLimit, //
								cells, //
								modules))
						/ 1000);
		setBvpMethod.accept(bvpOperationLimit);
	}

}
