package io.openems.edge.batteryinverter.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

@ProviderType
public interface ManagedSymmetricBatteryInverter extends SymmetricBatteryInverter {

	public static class BatteryInverterData {
		public final Integer soc;
		public final Integer soh;
		public final Integer maxCellTemperature;
		public final Integer dischargeMinVoltage;
		public final Integer chargeMaxVoltage;
		public final Integer dischargeMaxCurrent;
		public final Integer chargeMaxCurrent;
		public final Integer setActivePower;
		public final Integer setReactivePower;

		public BatteryInverterData(Integer soc, Integer soh, Integer maxCellTemperature, Integer dischargeMinVoltage,
				Integer chargeMaxVoltage, Integer dischargeMaxCurrent, Integer chargeMaxCurrent, Integer setActivePower,
				Integer setReactivePower) {
			this.soc = soc;
			this.soh = soh;
			this.maxCellTemperature = maxCellTemperature;
			this.dischargeMinVoltage = dischargeMinVoltage;
			this.chargeMaxVoltage = chargeMaxVoltage;
			this.dischargeMaxCurrent = dischargeMaxCurrent;
			this.chargeMaxCurrent = chargeMaxCurrent;
			this.setActivePower = setActivePower;
			this.setReactivePower = setReactivePower;
		}

	}

	public static class BatteryInverterConstraint {
		public final String description;
		public final Phase phase;
		public final Pwr pwr;
		public final Relationship relationship;
		public final double value;

		public static BatteryInverterConstraint[] NO_CONSTRAINTS = new BatteryInverterConstraint[] {};

		public BatteryInverterConstraint(String description, Phase phase, Pwr pwr, Relationship relationship,
				double value) {
			this.description = description;
			this.phase = phase;
			this.pwr = pwr;
			this.relationship = relationship;
			this.value = value;
		}
	}

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	public static ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {
		return ModbusSlaveNatureTable.of(ManagedSymmetricBatteryInverter.class, accessMode, 100) //
				.build();
	}

	/**
	 * Apply the {@link BatteryInverterData}.
	 * 
	 * @param data the {@link BatteryInverterData}
	 * @throws OpenemsNamedException on error
	 */
	public void apply(BatteryInverterData data) throws OpenemsNamedException;

	/**
	 * Gets static Constraints for this Battery-Inverter. Override this method to
	 * provide specific Constraints for this Battery-Inverter on every Cycle.
	 * 
	 * @return the Constraints
	 * @throws OpenemsException on error
	 */
	public default BatteryInverterConstraint[] getStaticConstraints() throws OpenemsNamedException {
		return BatteryInverterConstraint.NO_CONSTRAINTS;
	}

	/**
	 * Gets the smallest positive power that can be set (in W, VA or var). Example:
	 * <ul>
	 * <li>FENECON Commercial 40 allows setting of power in 100 W steps. It should
	 * return 100.
	 * <li>KACO blueplanet gridsave 50 allows setting of power in 0.1 % of 52 VA. It
	 * should return 52 (= 52000 * 0.001)
	 * </ul>
	 * 
	 * @return the power precision
	 */
	public int getPowerPrecision();

}
