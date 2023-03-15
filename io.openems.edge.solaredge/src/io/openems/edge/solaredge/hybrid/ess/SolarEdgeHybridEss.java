package io.openems.edge.solaredge.hybrid.ess;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
//import io.openems.edge.common.sum.GridMode;
//import io.openems.edge.ess.api.SymmetricEss.ChannelId;
import io.openems.edge.solaredge.enums.ControlMode;
import io.openems.edge.solaredge.enums.StorageChargePolicy;
import io.openems.edge.solaredge.enums.ChargeDischargeMode;
import io.openems.edge.solaredge.enums.BatteryStatus;

public interface SolarEdgeHybridEss extends OpenemsComponent {
	
	
	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {	
		
		// StorEdge Control and Status Block
		// IntegerWriteChannel
		SET_MAX_CHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		SET_MAX_DISCHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		
		SET_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		SET_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		
		// ControlMode, i.e. local functions or remote controlled. Has to be set to 4
		CONTROL_MODE(Doc.of(ControlMode.values())
				.accessMode(AccessMode.READ_ONLY)),// defined in external file
		
		SET_CONTROL_MODE(Doc.of(ControlMode.values())
				.accessMode(AccessMode.WRITE_ONLY)),  // defined in external file
		
		
		STORAGE_CHARGE_POLICY(Doc.of(StorageChargePolicy.values())
				.accessMode(AccessMode.READ_ONLY)),  // defined in external file
		SET_STORAGE_CHARGE_POLICY(Doc.of(StorageChargePolicy.values())
				.accessMode(AccessMode.WRITE_ONLY)),  // defined in external file
		
		CHARGE_DISCHARGE_MODE(Doc.of(ChargeDischargeMode.values())
				.accessMode(AccessMode.READ_ONLY)),  // defined in external file
		SET_CHARGE_DISCHARGE_MODE(Doc.of(ChargeDischargeMode.values())
				.accessMode(AccessMode.WRITE_ONLY)),  // defined in external file
		
		
		BATTERY_STATUS(Doc.of(BatteryStatus.values())
				.accessMode(AccessMode.READ_ONLY)), 
		
		MAX_CHARGE_LIMIT(Doc.of(OpenemsType.INTEGER) // Percent or kWh
				.unit(Unit.PERCENT) //
				.persistencePriority(PersistencePriority.HIGH)),		

		STORAGE_BACKUP_LIMIT(Doc.of(OpenemsType.INTEGER) // Percent. Only relevant for backup systems
				.unit(Unit.PERCENT) //
				.persistencePriority(PersistencePriority.HIGH)),	
		/*

		// IntegerReadChannels
		ORIGINAL_ALLOWED_CHARGE_POWER(new IntegerDoc() //
				.onInit(channel -> { //
					// on each Update to the channel -> set the ALLOWED_CHARGE_POWER value with a
					// delta of max 500
					channel.onUpdate(newValue -> {
						IntegerReadChannel currentValueChannel = channel.getComponent()
								.channel(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER);
						var originalValue = newValue.asOptional();
						var currentValue = currentValueChannel.value().asOptional();
						int value;
						if (!originalValue.isPresent() && !currentValue.isPresent()) {
							value = 0;
						} else if (originalValue.isPresent() && !currentValue.isPresent()) {
							value = originalValue.get();
						} else if (!originalValue.isPresent() && currentValue.isPresent()) {
							value = currentValue.get();
						} else {
							value = Math.max(originalValue.get(), currentValue.get() - 500);
						}
						currentValueChannel.setNextValue(value);
					});
				})), //

		ORIGINAL_ALLOWED_DISCHARGE_POWER(new IntegerDoc() //
				.onInit(channel -> { //
					// on each Update to the channel -> set the ALLOWED_DISCHARGE_POWER value with a
					// delta of max 500
					channel.onUpdate(newValue -> {
						IntegerReadChannel currentValueChannel = channel.getComponent()
								.channel(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER);
						var originalValue = newValue.asOptional();
						var currentValue = currentValueChannel.value().asOptional();
						int value;
						if (!originalValue.isPresent() && !currentValue.isPresent()) {
							value = 0;
						} else if (originalValue.isPresent() && !currentValue.isPresent()) {
							value = originalValue.get();
						} else if (!originalValue.isPresent() && currentValue.isPresent()) {
							value = currentValue.get();
						} else {
							value = Math.min(originalValue.get(), currentValue.get() + 500);
						}
						currentValueChannel.setNextValue(value);
					});
				})), //			
				
		*/
		
		/**
		 * Available Energy
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		BATT_ACTUAL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.persistencePriority(PersistencePriority.LOW)),
		
		/**
		 * Available Energy
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		BATT_ACTUAL_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.persistencePriority(PersistencePriority.LOW)),
				
		
		/**
		 * Available Energy
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		AVAIL_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.LOW)),
		
		/**
		 * Available Energy
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		RATED_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.LOW)),		
		
		/**
		 * Available Energy
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		BATT_LIFETIME_EXPORT_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.LOW)),		
		
		/**
		 * Available Energy
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		BATT_LIFETIME_IMPORT_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.LOW)),			
		
		/**
		 * Available Energy
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		MAX_CHARGE_CONTINUES_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.LOW)),		
		
		/**
		 * Available Energy
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		MAX_DISCHARGE_CONTINUES_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.LOW)),			

		/**
		 * Available Energy
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		MAX_CHARGE_PEAK_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.LOW)),		
		
		/**
		 * Available Energy
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		MAX_DISCHARGE_PEAK_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.LOW)),			

		/**
		 * Available Energy
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		BATT_AVG_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS) //
				.persistencePriority(PersistencePriority.LOW)),			
		/**
		 * Available Energy
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		BATT_MAX_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS) //
				.persistencePriority(PersistencePriority.LOW)),			
		
		/**
		 * State Of Health
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		SOH(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.persistencePriority(PersistencePriority.LOW)),
		
		/**
		 * Power from Grid. Used to calculate pv production
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
				.persistencePriority(PersistencePriority.HIGH)),
		
		/**
		 * Scaling factor for grid power
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
				.persistencePriority(PersistencePriority.HIGH));		
		
		/**
		 * State of Charge.
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: %
		 * <li>Range: 0..100
		 * </ul>
		 */
//		CONSUMPTION_POWER(Doc.of(OpenemsType.INTEGER) //
//				.unit(Unit.WATT) //
//				.persistencePriority(PersistencePriority.HIGH));
		
		
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
	 * Gets the Channel for {@link ChannelId#ESS_SOC}.
	 *
	 * @return the Channel
	 */
//	public default IntegerReadChannel getEssSocChannel() {
//		return this.channel(ChannelId.ESS_SOC);
//	}

	/**
	 * Gets the Average of all Energy Storage System State of Charge in [%], range
	 * 0..100 %. See {@link ChannelId#ESS_SOC}.
	 *
	 * @return the Channel {@link Value}
	 */
//	public default Value<Integer> getEssSoc() {
//		return this.getEssSocChannel().value();
//	}
	

	/**
	 * Gets the Channel for {@link ChannelId#DC_DISCHARGE_POWER}.
	 *
	 * @return the Channel
	 */
//	public default IntegerReadChannel getConsumptionPowerChannel() {
//		return this.channel(ChannelId.CONSUMPTION_POWER);
//	}

	/**
	 * Gets the DC Discharge Power in [W]. Negative values for Charge; positive for
	 * Discharge. See {@link ChannelId#DC_DISCHARGE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
//	public default Value<Integer> getConsumptionPower() {
//		return this.getConsumptionPowerChannel().value();
//	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#DC_DISCHARGE_POWER} Channel.
	 *
	 * @param value the next value
	 */
//	public default void _setConsumptionPower(Integer value) {
//		this.getConsumptionPowerChannel().setNextValue(value);
//	}	
	
	
	/**
	 * Gets the Channel for {@link ChannelId#DC_DISCHARGE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getGridPowerScaleChannel() {
		return this.channel(ChannelId.GRID_POWER_SCALE);
	}

	/**
	 * Gets the DC Discharge Power in [W]. Negative values for Charge; positive for
	 * Discharge. See {@link ChannelId#DC_DISCHARGE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getGridPowerScale() {
		return this.getGridPowerScaleChannel().value();
	}
	
	
	/**
	 * Gets the Channel for {@link ChannelId#DC_DISCHARGE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getGridPowerChannel() {
		return this.channel(ChannelId.GRID_POWER);
	}

	/**
	 * Gets the DC Discharge Power in [W]. Negative values for Charge; positive for
	 * Discharge. See {@link ChannelId#DC_DISCHARGE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getGridPower() {
		return this.getGridPowerChannel().value();
	}
	
	/**
	 * Gets the Channel for {@link ChannelId#GRID_MODE}.
	 *
	 * @return the Channel
	 */
	public default Channel<ControlMode> getControlModeChannel() {
		return this.channel(ChannelId.CONTROL_MODE);
	}

	/**
	 * Is the Energy Storage System On-Grid? See {@link ChannelId#GRID_MODE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default ControlMode getControlMode() {
		return this.getControlModeChannel().value().asEnum();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#GRID_MODE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setControlMode(ControlMode value) {
		this.getControlModeChannel().setNextValue(value);
	}

	

}
