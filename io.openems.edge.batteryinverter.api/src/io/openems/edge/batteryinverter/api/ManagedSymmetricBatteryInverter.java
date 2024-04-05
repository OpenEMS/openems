package io.openems.edge.batteryinverter.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.startstop.StartStoppable;

/**
 * Represents a Symmetric Battery-Inverter that can be controlled.
 *
 * <p>
 * To indicate, that the Battery-Inverter is ready for charging/discharging, the
 * following Channels need to be set:
 *
 * <ul>
 * <li>StartStoppable.ChannelId.START_STOP must be set to 'START'
 * <li>No 'Fault'-StateChannels are set (i.e. 'OpenemsComponent.ChannelId.STATE'
 * is < 3)
 * </ul>
 */
@ProviderType
public interface ManagedSymmetricBatteryInverter extends SymmetricBatteryInverter, StartStoppable {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
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

	/**
	 * Used for Modbus/TCP Api Controller. Provides a Modbus table for the Channels
	 * of this Component.
	 *
	 * @param accessMode filters the Modbus-Records that should be shown
	 * @return the {@link ModbusSlaveNatureTable}
	 */
	public static ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {
		return ModbusSlaveNatureTable.of(ManagedSymmetricBatteryInverter.class, accessMode, 100) //
				.build();
	}

	/**
	 * Run the Battery-Inverter.
	 *
	 * <ul>
	 * <li>bring the Battery-Inverter in the desired state
	 * <li>apply information from {@link Battery} as required
	 * <li>set the target Active and Reactive Power.
	 * </ul>
	 *
	 * <p>
	 * This is called on ManagedSymmetricEss::applyPower()
	 *
	 * @param battery          the {@link Battery}
	 * @param setActivePower   the active power setpoint
	 * @param setReactivePower the reactive power setpoint
	 * @throws OpenemsNamedException on error
	 */
	public void run(Battery battery, int setActivePower, int setReactivePower) throws OpenemsNamedException;

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

	/**
	 * Gets a boolean if the battery inverter is managed or not.
	 *
	 * <p>
	 * Returns false if the battery inverter itself is not managed or in a read only
	 * mode.
	 *
	 * @return is managed or not
	 */
	public default boolean isManaged() {
		return true;
	}

	/**
	 * Gets a boolean if the battery inverter is able to build a micro-grid in
	 * off-grid.
	 *
	 * <p>
	 * Returns false if the battery inverter is not able to build a micro-grid.
	 *
	 * @return is managed or not
	 */
	public default boolean isOffGridPossible() {
		return false;
	}
}
