package io.openems.edge.battery.protection;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.protection.currenthandler.ChargeMaxCurrentHandler;
import io.openems.edge.battery.protection.currenthandler.DischargeMaxCurrentHandler;
import io.openems.edge.battery.protection.force.AbstractForceChargeDischarge;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.type.TypeUtils;

/**
 * This utility class provides algorithms to calculate maximum allowed charge
 * and discharge currents for batteries.
 *
 * <p>
 * The logic uses:
 *
 * <ul>
 * <li>Allowed Current Limit provided by Battery Management System
 * <li>Voltage-to-Percent characteristics based on Min- and Max-Cell-Voltage
 * <li>Temperature-to-Percent characteristics based on Min- and
 * Max-Cell-Temperature
 * <li>Linear max increase limit (e.g. 0.5 A per second)
 * <li>Force Charge/Discharge mode (e.g. -1 A to enforce charge/discharge)
 * </ul>
 */
public class BatteryProtection {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Charge Current limit provided by the Battery/BMS.
		 *
		 * <ul>
		 * <li>Interface: BatteryProtection
		 * <li>Type: Integer
		 * <li>Unit: Ampere
		 * </ul>
		 */
		BP_CHARGE_BMS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.persistencePriority(PersistencePriority.MEDIUM)), //
		/**
		 * Discharge Current limit provided by the Battery/BMS.
		 *
		 * <ul>
		 * <li>Interface: BatteryProtection
		 * <li>Type: Integer
		 * <li>Unit: Ampere
		 * </ul>
		 */
		BP_DISCHARGE_BMS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.persistencePriority(PersistencePriority.MEDIUM)), //
		/**
		 * Charge minimum voltage.
		 *
		 * <ul>
		 * <li>Interface: BatteryProtection
		 * <li>Type: Integer
		 * <li>Unit: Volt
		 * </ul>
		 */
		BP_CHARGE_MIN_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.persistencePriority(PersistencePriority.MEDIUM)), //
		/**
		 * Discharge minimum voltage.
		 *
		 * <ul>
		 * <li>Interface: BatteryProtection
		 * <li>Type: Integer
		 * <li>Unit: Volt
		 * </ul>
		 */
		BP_DISCHARGE_MIN_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.persistencePriority(PersistencePriority.MEDIUM)), //
		/**
		 * Charge maximum voltage.
		 *
		 * <ul>
		 * <li>Interface: BatteryProtection
		 * <li>Type: Integer
		 * <li>Unit: Volt
		 * </ul>
		 */
		BP_CHARGE_MAX_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.persistencePriority(PersistencePriority.MEDIUM)), //
		/**
		 * Discharge maximum voltage.
		 *
		 * <ul>
		 * <li>Interface: BatteryProtection
		 * <li>Type: Integer
		 * <li>Unit: Volt
		 * </ul>
		 */
		BP_DISCHARGE_MAX_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.persistencePriority(PersistencePriority.MEDIUM)), //
		/**
		 * Charge Minimum Temperature.
		 *
		 * <ul>
		 * <li>Interface: BatteryProtection
		 * <li>Type: Integer
		 * <li>Unit: Degree Celsius
		 * </ul>
		 */
		BP_CHARGE_MIN_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS) //
				.persistencePriority(PersistencePriority.MEDIUM)), //
		/**
		 * Discharge Minimum Temperature.
		 *
		 * <ul>
		 * <li>Interface: BatteryProtection
		 * <li>Type: Integer
		 * <li>Unit: Degree Celsius
		 * </ul>
		 */
		BP_DISCHARGE_MIN_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS) //
				.persistencePriority(PersistencePriority.MEDIUM)), //
		/**
		 * Charge Maximum Temperature.
		 *
		 * <ul>
		 * <li>Interface: BatteryProtection
		 * <li>Type: Integer
		 * <li>Unit: Degree Celsius
		 * </ul>
		 */
		BP_CHARGE_MAX_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS) //
				.persistencePriority(PersistencePriority.MEDIUM)), //
		/**
		 * Discharge Maximum Temperature.
		 *
		 * <ul>
		 * <li>Interface: BatteryProtection
		 * <li>Type: Integer
		 * <li>Unit: Degree Celsius
		 * </ul>
		 */
		BP_DISCHARGE_MAX_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS) //
				.persistencePriority(PersistencePriority.MEDIUM)), //
		/**
		 * Charge Max-Increase Current limit.
		 *
		 * <ul>
		 * <li>Interface: BatteryProtection
		 * <li>Type: Integer
		 * <li>Unit: Ampere
		 * </ul>
		 */
		BP_CHARGE_INCREASE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.persistencePriority(PersistencePriority.MEDIUM)), //
		/**
		 * Discharge Max-Increase Current limit.
		 *
		 * <ul>
		 * <li>Interface: BatteryProtection
		 * <li>Type: Integer
		 * <li>Unit: Ampere
		 * </ul>
		 */
		BP_DISCHARGE_INCREASE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.persistencePriority(PersistencePriority.MEDIUM)), //
		/**
		 * Force-Discharge State.
		 *
		 * <ul>
		 * <li>Interface: BatteryProtection
		 * </ul>
		 */
		BP_FORCE_DISCHARGE(Doc.of(AbstractForceChargeDischarge.State.values()) //
				.persistencePriority(PersistencePriority.MEDIUM)), //
		/**
		 * Force-Charge State.
		 *
		 * <ul>
		 * <li>Interface: BatteryProtection
		 * </ul>
		 */
		BP_FORCE_CHARGE(Doc.of(AbstractForceChargeDischarge.State.values()) //
				.persistencePriority(PersistencePriority.MEDIUM)), //

		/**
		 * Battery Max Ever Current.
		 *
		 * <ul>
		 * <li>Interface: BatteryProtection
		 * </ul>
		 */
		BP_MAX_EVER_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.persistencePriority(PersistencePriority.MEDIUM)), //

		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public static class Builder {

		private final Battery battery;

		private ChargeMaxCurrentHandler chargeMaxCurrentHandler;
		private DischargeMaxCurrentHandler dischargeMaxCurrentHandler;

		protected Builder(Battery battery) {
			this.battery = battery;
		}

		/**
		 * Applies all values from a {@link BatteryProtectionDefinition}.
		 *
		 * @param def           the {@link BatteryProtectionDefinition}
		 * @param clockProvider a {@link ClockProvider}
		 * @return a {@link Builder}
		 */
		public Builder applyBatteryProtectionDefinition(BatteryProtectionDefinition def, ClockProvider clockProvider) {
			return this //
					.setChargeMaxCurrentHandler(
							ChargeMaxCurrentHandler.create(clockProvider, def.getInitialBmsMaxEverChargeCurrent()) //
									.setVoltageToPercent(def.getChargeVoltageToPercent()) //
									.setTemperatureToPercent(def.getChargeTemperatureToPercent()) //
									.setMaxIncreasePerSecond(def.getMaxIncreaseAmperePerSecond()) //
									.setForceDischarge(def.getForceDischargeParams()) //
									.build()) //
					.setDischargeMaxCurrentHandler(
							DischargeMaxCurrentHandler.create(clockProvider, def.getInitialBmsMaxEverDischargeCurrent()) //
									.setVoltageToPercent(def.getDischargeVoltageToPercent())
									.setTemperatureToPercent(def.getDischargeTemperatureToPercent()) //
									.setMaxIncreasePerSecond(def.getMaxIncreaseAmperePerSecond()) //
									.setForceCharge(def.getForceChargeParams()) //
									.build()) //
			;
		}

		/**
		 * Sets the {@link ChargeMaxCurrentHandler}.
		 *
		 * @param chargeMaxCurrentHandler the {@link ChargeMaxCurrentHandler}
		 * @return a {@link Builder}
		 */
		public Builder setChargeMaxCurrentHandler(ChargeMaxCurrentHandler chargeMaxCurrentHandler) {
			this.chargeMaxCurrentHandler = chargeMaxCurrentHandler;
			return this;
		}

		/**
		 * Sets the {@link DischargeMaxCurrentHandler}.
		 *
		 * @param dischargeMaxCurrentHandler the {@link DischargeMaxCurrentHandler}
		 * @return a {@link Builder}
		 */
		public Builder setDischargeMaxCurrentHandler(DischargeMaxCurrentHandler dischargeMaxCurrentHandler) {
			this.dischargeMaxCurrentHandler = dischargeMaxCurrentHandler;
			return this;
		}

		/**
		 * Builds the {@link BatteryProtection} instance.
		 *
		 * @return a {@link BatteryProtection}
		 */
		public BatteryProtection build() {
			return new BatteryProtection(this.battery, this.chargeMaxCurrentHandler, this.dischargeMaxCurrentHandler);
		}
	}

	/**
	 * Create a {@link BatteryProtection} using builder pattern.
	 *
	 * @param battery the {@link Battery}
	 * @return a {@link Builder}
	 */
	public static Builder create(Battery battery) {
		return new Builder(battery);
	}

	private final Battery battery;
	private final ChargeMaxCurrentHandler chargeMaxCurrentHandler;
	private final DischargeMaxCurrentHandler dischargeMaxCurrentHandler;

	protected BatteryProtection(Battery battery, ChargeMaxCurrentHandler chargeMaxCurrentHandler,
			DischargeMaxCurrentHandler dischargeMaxCurrentHandler) {
		TypeUtils.assertNull("BatteryProtection algorithm is missing data", battery, chargeMaxCurrentHandler,
				dischargeMaxCurrentHandler);
		this.battery = battery;
		this.chargeMaxCurrentHandler = chargeMaxCurrentHandler;
		this.dischargeMaxCurrentHandler = dischargeMaxCurrentHandler;

		this.battery.getChargeMaxCurrentChannel().setMetaInfo("Battery-Protection");
		this.battery.getDischargeMaxCurrentChannel().setMetaInfo("Battery-Protection");
	}

	/**
	 * Apply the logic on the {@link Battery}.
	 *
	 * <ul>
	 * <li>Set CHARGE_MAX_CURRENT Channel
	 * <li>Set DISCHARGE_MAX_CURRENT Channel
	 * <li>Set FORCE_DISCHARGE_ACTIVE State-Channel if Charge-Max-Current < 0
	 * <li>Set FORCE_CHARGE_ACTIVE State-Channel if Discharge-Max-Current < 0
	 * <li>SET
	 * </ul>
	 */
	public void apply() {
		// Use MaxCurrentHandlers to calculate max charge and discharge currents.
		// These methods also write debug information to BatteryProtection-Channels, so
		// it is feasible to always execute them, even if battery is not started.
		var chargeMaxCurrent = this.chargeMaxCurrentHandler.calculateCurrentLimit(this.battery);
		var dischargeMaxCurrent = this.dischargeMaxCurrentHandler.calculateCurrentLimit(this.battery);

		// Set max charge and discharge currents
		this.battery._setChargeMaxCurrent(chargeMaxCurrent);
		this.battery._setDischargeMaxCurrent(dischargeMaxCurrent);
	}
}
