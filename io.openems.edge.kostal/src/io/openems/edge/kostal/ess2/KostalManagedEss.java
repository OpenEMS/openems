package io.openems.edge.kostal.ess2;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.kostal.ess2.charger.KostalDcCharger;

public interface KostalManagedEss extends OpenemsComponent {

	public static enum ChannelId
			implements
				io.openems.edge.common.channel.ChannelId {

		/**
		 * Charge Power Wanted is the activePower wanted from controllers and
		 * internal. Channel for applyPower()-Method
		 *
		 * <p>
		 * negative values for charging
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		CHARGE_POWER_WANTED(Doc.of(OpenemsType.INTEGER) // Charge/Discharge-Power
														// wanted from
														// controllers
				.unit(Unit.WATT)), // defined in external file

		/**
		 * Power from Grid. Used to calculate pv production.
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		GRID_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)), //

		/**
		 * Scaling factor for grid power.
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: None
		 * <li>Range: 0..100
		 * </ul>
		 */
		GRID_POWER_SCALE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.persistencePriority(PersistencePriority.HIGH)), // defined in
																	// external
																	// file

		/**
		 * Battery charge power (DC) setpoint
		 * 
		 * <p>
		 * Reads the charge power
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * </ul>
		 */
		CHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY)),

		/**
		 * Charge Power READ Channel always positive.
		 * 
		 * <p>
		 * Reads the charge power
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * </ul>
		 */
		MAX_CHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY)),

		/**
		 * Dicharge Power READ Channel always positive.
		 * 
		 * <p>
		 * Reads the charge power
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * </ul>
		 */
		MAX_DISCHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY)),

		/**
		 * AC-Power produced by the ESS. Either for grid or consumption.
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		POWER_AC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * Scale factor for AC-Power.
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: Scale factor
		 * <li>
		 * </ul>
		 */
		POWER_AC_SCALE(Doc.of(OpenemsType.INTEGER) //

				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * DC-Power of the inverter.
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		POWER_DC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * Scale for the DC-Power value.
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: Scale
		 * <li>
		 * </ul>
		 */
		POWER_DC_SCALE(Doc.of(OpenemsType.INTEGER) //
				.persistencePriority(PersistencePriority.HIGH)), //

		/**
		 * Charge Power WRITE Channel always positive
		 * 
		 * <p>
		 * Tells the ESS the charge power.
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * </ul>
		 */
		SET_MAX_CHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY)),

		/**
		 * Discharge Power WRITE Channel always positive
		 * 
		 * <p>
		 * Tells the ESS the charge power.
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * </ul>
		 */
		SET_MAX_DISCHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY)),

		// IntegerWriteChannels
		SET_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.unit(Unit.WATT)); //

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
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#CHARGE_POWER_WANTED} Channel.
	 *
	 * @param value
	 *            the next value
	 */
	public default void _setChargePowerWanted(Integer value) {
		this.getChargePowerWantedChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#CHARGE_POWER_WANTED} Channel.
	 *
	 * @param i
	 *            the next value
	 */
	public default void _setChargePower(Integer value) {
		System.out.println("setChargePower called... setting value: " + value);
		this.getSetChargePowerChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#REMOTE_CONTROL_COMMAND_MODE} Channel.
	 *
	 * @param value
	 *            the next value
	 * @throws OpenemsNamedException
	 *             throws named exception
	 */
	public default void _setMaxChargePower(Integer value)
			throws OpenemsNamedException {
		this.getSetMaxChargePowerChannel().setNextWriteValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#REMOTE_CONTROL_COMMAND_MODE} Channel.
	 *
	 * @param value
	 *            the next value for max. Discharge Power
	 * @throws OpenemsNamedException
	 *             throws named exception
	 */
	public default void _setMaxDischargePower(Integer value)
			throws OpenemsNamedException {
		this.getSetMaxDischargePowerChannel().setNextWriteValue(value);
	}

	/**
	 * Not sure if necessary.
	 * 
	 * @param charger
	 *            Adds charger component
	 */
	public void addCharger(KostalDcCharger charger);

	/**
	 * Is the Energy Storage System On-Grid? See {@link ChannelId#CONTROL_MODE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getAcPower() {
		return this.getAcPowerChannel().value();
	}

	// ######################
	/**
	 * Gets the Channel for {@link ChannelId#CONTROL_MODE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getAcPowerChannel() {
		return this.channel(ChannelId.POWER_AC);
	}

	/**
	 * Is the Energy Storage System On-Grid? See {@link ChannelId#CONTROL_MODE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getAcPowerScale() {
		return this.getAcPowerScaleChannel().value();
	}

	// ######################
	/**
	 * Gets the Channel for {@link ChannelId#CONTROL_MODE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getAcPowerScaleChannel() {
		return this.channel(ChannelId.POWER_AC_SCALE);
	}

	/**
	 * Gets the Active Power in [W]. Negative values for Charge; positive for
	 * Discharge. See {@link ChannelId#ACTIVE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getChargePowerWanted() {
		return this.getChargePowerWantedChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#ACTIVE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getChargePowerWantedChannel() {
		return this.channel(ChannelId.CHARGE_POWER_WANTED);
	}

	// ######################

	/**
	 * DC Power Channel {@link ChannelId#POWER_DC_SCALE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDcPower() {
		return this.getDcPowerChannel().value();
	}

	// ######################
	/**
	 * Gets the Channel for {@link ChannelId#POWER_DC}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDcPowerChannel() {
		return this.channel(ChannelId.POWER_DC);
	}

	/**
	 * Is the Energy Storage System On-Grid? See {@link ChannelId#CONTROL_MODE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDcPowerScale() {
		return this.getDcPowerScaleChannel().value();
	}

	// ######################
	/**
	 * Gets the Channel for {@link ChannelId#CONTROL_MODE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDcPowerScaleChannel() {
		return this.channel(ChannelId.POWER_DC_SCALE);
	}

	/**
	 * Gets the DC Discharge Power in [W]. Negative values for Charge; positive
	 * for Discharge. See {@link ChannelId#GRID_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getGridPower() {
		return this.getGridPowerChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#GRID_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getGridPowerChannel() {
		return this.channel(ChannelId.GRID_POWER);
	}

	/**
	 * Gets the DC Discharge Power in [W]. Negative values for Charge; positive
	 * for Discharge. See {@link ChannelId#GRID_POWER_SCALE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getGridPowerScale() {
		return this.getGridPowerScaleChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#GRID_POWER_SCALE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getGridPowerScaleChannel() {
		return this.channel(ChannelId.GRID_POWER_SCALE);
	}

	/**
	 * Is the Energy Storage System On-Grid? See
	 * {@link ChannelId#REMOTE_CONTROL_COMMAND_MODE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getMaxChargePower() {
		return this.getMaxChargePowerChannel().value();
	}

	// #############
	/**
	 * Gets the Channel for {@link ChannelId#REMOTE_CONTROL_COMMAND_MODE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getMaxChargePowerChannel() {
		return this.channel(ChannelId.MAX_CHARGE_POWER);
	}

	// ###########################
	/**
	 * Is the Energy Storage System On-Grid? See
	 * {@link ChannelId#REMOTE_CONTROL_COMMAND_MODE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getMaxDischargePower() {
		return this.getMaxDischargePowerChannel().value();
	}

	// #############
	/**
	 * Gets the Channel for {@link ChannelId#REMOTE_CONTROL_COMMAND_MODE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getMaxDischargePowerChannel() {
		return this.channel(ChannelId.MAX_DISCHARGE_POWER);
	}

	// ###########################
	/**
	 * Sets the Modbus-ID from Config.
	 * 
	 * @return Modbus-ID as String
	 */
	public String getModbusBridgeId();

	/**
	 * Gets the Channel for {@link ChannelId#REMOTE_CONTROL_COMMAND_MODE}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetMaxChargePowerChannel() {
		return this.channel(ChannelId.SET_MAX_CHARGE_POWER);
	}

	/**
	 * Gets the Channel for {@link ChannelId#REMOTE_CONTROL_COMMAND_MODE}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetMaxDischargePowerChannel() {
		return this.channel(ChannelId.SET_MAX_DISCHARGE_POWER);
	}

	/**
	 * Gets the Channel for {@link ChannelId#REMOTE_CONTROL_COMMAND_MODE}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetChargePowerChannel() {
		return this.channel(ChannelId.CHARGE_POWER);
	}

	/**
	 * Gets the Unit-ID for ESS.
	 * 
	 * @return the Unit-ID for the ESS
	 */
	public Integer getUnitId();

	/**
	 * Removes Charger.
	 * 
	 * @param charger
	 *            Removes given charger instance
	 */
	public void removeCharger(KostalDcCharger charger);

}