package io.openems.edge.ess.generic.common.essprotection;

import static io.openems.common.utils.IntUtils.maxInt;
import static io.openems.common.utils.IntUtils.minInt;
import static java.lang.Math.max;

import java.time.Clock;
import java.util.function.DoubleBinaryOperator;
import java.util.function.IntBinaryOperator;

import com.google.common.annotations.VisibleForTesting;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.protection.BatteryVoltageProtection;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.common.filter.PT1Filter;

public class EpVoltageRegulationHandler implements EssProtectionHandler {

	public static final int VOLTAGE_CONTROL_FILTER_TIME_CONSTANT = 10_000; // [milliseconds]

	private final PT1Filter pt1FilterChargeMaxCurrentVoltLimit;
	private final PT1Filter pt1FilterDischargeMaxCurrentVoltLimit;

	public EpVoltageRegulationHandler() {
		this.pt1FilterChargeMaxCurrentVoltLimit = new PT1Filter(VOLTAGE_CONTROL_FILTER_TIME_CONSTANT);
		this.pt1FilterDischargeMaxCurrentVoltLimit = new PT1Filter(VOLTAGE_CONTROL_FILTER_TIME_CONSTANT);
	}

	@VisibleForTesting
	public EpVoltageRegulationHandler(Clock clock) {
		this.pt1FilterChargeMaxCurrentVoltLimit = new PT1Filter(clock, VOLTAGE_CONTROL_FILTER_TIME_CONSTANT);
		this.pt1FilterDischargeMaxCurrentVoltLimit = new PT1Filter(clock, VOLTAGE_CONTROL_FILTER_TIME_CONSTANT);
	}

	@Override
	public EssProtectionLimits calculateEssProtectionLimits(Battery battery, SymmetricBatteryInverter inverter) {
		var chargeMaxCurrent = calculateMaxCurrent(battery, inverter, this.pt1FilterChargeMaxCurrentVoltLimit,
				Math::min, (a, b) -> a - b, true);
		var dischargeMaxCurrent = calculateMaxCurrent(battery, inverter, this.pt1FilterDischargeMaxCurrentVoltLimit,
				Math::max, (a, b) -> a + b, false);
		return new EssProtectionLimits(chargeMaxCurrent, dischargeMaxCurrent);
	}

	protected static Integer calculateMaxCurrent(Battery battery, SymmetricBatteryInverter inverter,
			PT1Filter pt1Filter, IntBinaryOperator dcLimit, DoubleBinaryOperator deltaChargeCurrentMethod,
			boolean invert) {
		var regulationValues = RegulationValues.from(battery, inverter);
		if (regulationValues == null) {
			return null;
		}

		final var batteryLimit = invert //
				? minInt(regulationValues.chargeMaxVoltage, regulationValues.bvpChargeBms) //
				: maxInt(regulationValues.dischargeMinVoltage, regulationValues.bvpDischargeBms);

		final var inverterLimit = invert //
				? regulationValues.inverterDcMaxVoltage
				: regulationValues.inverterDcMinVoltage;
		final var limitVoltage = dcLimit.applyAsInt(//
				batteryLimit, //
				inverterLimit);

		var subtractLimit = regulationValues.voltage - limitVoltage;
		var voltageDifference = invert //
				? -subtractLimit //
				: subtractLimit;

		var resistance = regulationValues.innerResistance / 1000.;
		final var deltaChargeCurrent = voltageDifference / resistance;
		var maxCurrentVoltLimit = deltaChargeCurrentMethod.applyAsDouble(deltaChargeCurrent, regulationValues.current);
		return pt1Filter.applyPT1Filter(max(maxCurrentVoltLimit, -5.0));
	}

	private record RegulationValues(//
			boolean isBatteryStarted, //
			int voltage, //
			int current, //
			int chargeMaxVoltage, //
			int dischargeMinVoltage, //
			int innerResistance, //
			Integer bvpChargeBms, // nullable
			Integer bvpDischargeBms, // nullable
			int inverterDcMinVoltage, //
			int inverterDcMaxVoltage) {
		private static RegulationValues from(Battery battery, SymmetricBatteryInverter inverter) {
			var isBatteryStarted = battery.isStarted();
			var voltage = battery.getVoltage().get();
			var current = battery.getCurrent().get();
			var chargeMaxVoltage = battery.getChargeMaxVoltage().get();
			var dischargeMinVoltage = battery.getDischargeMinVoltage().get();
			var innerResistance = battery.getInnerResistance().get();
			var bvpChargeBms = battery instanceof BatteryVoltageProtection b ? b.getBvpChargeBms().get() : null;
			var bvpDischargeBms = battery instanceof BatteryVoltageProtection b ? b.getBvpDischargeBms().get() : null;
			var inverterDcMinVoltage = inverter.getDcMinVoltage().get();
			var inverterDcMaxVoltage = inverter.getDcMaxVoltage().get();
			if (!isBatteryStarted //
					|| voltage == null//
					|| current == null //
					|| chargeMaxVoltage == null//
					|| dischargeMinVoltage == null//
					|| innerResistance == null//
					|| inverterDcMinVoltage == null //
					|| inverterDcMaxVoltage == null//
			) {
				return null;
			}
			return new RegulationValues(isBatteryStarted, voltage, current, chargeMaxVoltage, dischargeMinVoltage,
					innerResistance, bvpChargeBms, bvpDischargeBms, inverterDcMinVoltage, inverterDcMaxVoltage);
		}
	}
}
