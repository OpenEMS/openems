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
import io.openems.edge.solaredge.enums.ChargeDischargeMode;
//import io.openems.edge.solaredge.charger.SolaredgeDcChargerImpl;

import io.openems.edge.solaredge.enums.AcChargePolicy;
import io.openems.edge.solaredge.enums.BatteryStatus;

import io.openems.edge.solaredge.charger.SolaredgeDcCharger;


public interface SolarEdgeHybridEss extends OpenemsComponent {
	
	
	public Integer getUnitId();

	public String getModbusBridgeId();

	public void addCharger(SolaredgeDcCharger charger);
	
	public void removeCharger(SolaredgeDcCharger charger);
	
	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {	
		
		// StorEdge Control and Status Block
		/*
		Storage Control Mode is used to set the StorEdge system operating mode: 
			0 – Disabled 
			1 – Maximize Self Consumption – requires a SolarEdge Electricity meter on the grid or load connection point 
			2 – Time of Use (Profile programming) – requires a SolarEdge Electricity meter on the grid or load connection point 
			3 – Backup Only (applicable only for systems support backup functionality) 
			4 – Remote Control – the battery charge/discharge state is controlled by an external controller 		
		*/
		CONTROL_MODE(Doc.of(ControlMode.values())
				.accessMode(AccessMode.READ_ONLY)),// defined in external file
		SET_CONTROL_MODE(Doc.of(ControlMode.values())
				.accessMode(AccessMode.WRITE_ONLY)),  // defined in external file
		
		
		
		MAX_CHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY)), //		
		SET_MAX_CHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		
		MAX_DISCHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY)), //
		SET_MAX_DISCHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		
		SET_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY)), //

		/*
		 * Storage AC Charge Limit 
		 * is used to set the AC charge limit according to the policy set in the previous register. Either fixed in kWh or 
			percentage is set (e.g. 100KWh or 70%). Relevant only for Storage AC Charge Policy = 2 or 3	
		 * */
		MAX_CHARGE_LIMIT(Doc.of(OpenemsType.INTEGER) // Percent or kWh
				.unit(Unit.PERCENT) //
				.persistencePriority(PersistencePriority.HIGH)),		
		SET_MAX_CHARGE_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		
		SET_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		
		

		/*
		 * Storage AC Charge Policy is used to enable charging for AC and the limit of yearly AC charge (if applicable).  
		0 – Disable 
		1 – Always allowed – needed for AC coupling operation. Allows unlimited charging from the AC. When used with Maximize 
			self-consumption, only excess power is used for charging (charging from the grid is not allowed).  
		2 – Fixed Energy Limit – allows AC charging with a fixed yearly (Jan 1 to Dec 31) limit (needed for meeting ITC regulation in 
			the US) 
		3 – Percent of Production - allows AC charging with a % of system production year to date limit (needed for meeting ITC 
			regulation in the US) 
		 *  
		 *  **/
		STORAGE_CHARGE_POLICY(Doc.of(AcChargePolicy.values())
				.accessMode(AccessMode.READ_ONLY)),  // defined in external file
		SET_STORAGE_CHARGE_POLICY(Doc.of(AcChargePolicy.values())
				.accessMode(AccessMode.WRITE_ONLY)),  // defined in external file		

		/* Charge/Discharge default Mode  / Remote Control Command Mode
		Storage Charge/Discharge default Mode sets the default mode of operation when Remote Control Command Timeout has expired. 
		The supported Charge/Discharge Modes are as follows: 
		0 – Off 
		1 – Charge excess PV power only.  
			Only PV excess power not going to AC is used for charging the battery. Inverter NominalActivePowerLimit (or the 
			inverter rated power whichever is lower) sets how much power the inverter is producing to the AC. In this mode, 
			the battery cannot be discharged. If the PV power is lower than NominalActivePowerLimit the AC production will 
			be equal to the PV power. 
		2 – Charge from PV first, before producing power to the AC. 
			The Battery charge has higher priority than AC production. First charge the battery then produce AC. 
			If StorageRemoteCtrl_ChargeLimit is lower than PV excess power goes to AC according to 
			NominalActivePowerLimit. If NominalActivePowerLimit is reached and battery StorageRemoteCtrl_ChargeLimit is 
			reached, PV power is curtailed. 
		3 – Charge from PV+AC according to the max battery power. 
			Charge from both PV and AC with priority on PV power. 
			If PV production is lower than StorageRemoteCtrl_ChargeLimit, the battery will be charged from AC up to 
			NominalActivePow-erLimit. In this case AC power = StorageRemoteCtrl_ChargeLimit- PVpower.  
			If PV power is larger than StorageRemoteCtrl_ChargeLimit the excess PV power will be directed to the AC up to the 
			Nominal-ActivePowerLimit beyond which the PV is curtailed. 
		4 – Maximize export – discharge battery to meet max inverter AC limit. 
			AC power is maintained to NominalActivePowerLimit, using PV power and/or battery power. If the PV power is not 
			sufficient, battery power is used to complement AC power up to StorageRemoteCtrl_DishargeLimit. In this mode, 
			charging excess power will occur if there is more PV than the AC limit.  
		5 – Discharge to meet loads consumption. Discharging to the grid is not allowed. 
		7 – Maximize self-consumption 		
		*/
		REMOTE_CONTROL_COMMAND_MODE(Doc.of(ChargeDischargeMode.values())
				.accessMode(AccessMode.READ_ONLY)),  // defined in external file
		SET_REMOTE_CONTROL_COMMAND_MODE(Doc.of(ChargeDischargeMode.values())
				.accessMode(AccessMode.WRITE_ONLY)),  // defined in external file		
		
		CHARGE_DISCHARGE_MODE(Doc.of(ChargeDischargeMode.values())
				.accessMode(AccessMode.READ_ONLY)),  // defined in external file
		SET_CHARGE_DISCHARGE_MODE(Doc.of(ChargeDischargeMode.values())
				.accessMode(AccessMode.WRITE_ONLY)),  // defined in external file
		
		
		CHARGE_DISCHARGE_DEFAULT_MODE(Doc.of(ChargeDischargeMode.values())
				.accessMode(AccessMode.READ_ONLY)),  // defined in external file
		SET_CHARGE_DISCHARGE_DEFAULT_MODE(Doc.of(ChargeDischargeMode.values())
				.accessMode(AccessMode.WRITE_ONLY)),  // defined in external file
		
		
		BATTERY_STATUS(Doc.of(BatteryStatus.values())
				.accessMode(AccessMode.READ_ONLY)), 
		
		
		/*
		 * 	Storage Backup Reserved Setting sets the percentage of reserved battery SOE to be used for backup purposes. Relevant only for 
			inverters with backup functionality. 
		 * */
		STORAGE_BACKUP_LIMIT(Doc.of(OpenemsType.INTEGER) // Percent. Only relevant for backup systems
				.unit(Unit.PERCENT) //
				.persistencePriority(PersistencePriority.HIGH)),	
		
		SET_STORAGE_BACKUP_LIMIT(Doc.of(OpenemsType.INTEGER) // Percent. Only relevant for backup systems
				.unit(Unit.PERCENT) //
				.persistencePriority(PersistencePriority.HIGH)),		

		
		/* Remote Control Command Timeout sets the operating timeframe for the charge/discharge command sets in Remote Control 
		 * */
		REMOTE_CONTROL_TIMEOUT(Doc.of(OpenemsType.INTEGER)  
				.accessMode(AccessMode.READ_ONLY)
				.unit(Unit.SECONDS) //
				.persistencePriority(PersistencePriority.HIGH)),
		
		SET_REMOTE_CONTROL_TIMEOUT(Doc.of(OpenemsType.INTEGER) // Remote Control Command Timeout sets the operating timeframe for the charge/discharge command sets in Remote Control 
				.accessMode(AccessMode.WRITE_ONLY)
				.unit(Unit.SECONDS) //
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
