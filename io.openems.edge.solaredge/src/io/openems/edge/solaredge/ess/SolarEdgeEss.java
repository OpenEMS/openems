package io.openems.edge.solaredge.ess;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.FloatReadChannel;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.solaredge.charger.SolarEdgeCharger;
import io.openems.edge.solaredge.enums.AcChargePolicy;
import io.openems.edge.solaredge.enums.BatteryStatus;
import io.openems.edge.solaredge.enums.CommandMode;
import io.openems.edge.solaredge.enums.MeterCommunicateStatus;
import io.openems.edge.solaredge.enums.SeControlMode;

public interface SolarEdgeEss extends OpenemsComponent, SymmetricEss {

	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		WRONG_PHASE_CONFIGURED(Doc.of(Level.WARNING) //
				.text("Configured Phase does not match the Model")), //
		SMART_MODE_NOT_WORKING_WITH_PID_FILTER(Doc.of(Level.WARNING) //
				.text("SMART mode does not work correctly with active PID filter")),
		NO_SMART_METER_DETECTED(Doc.of(Level.WARNING) //
				.text("No SolarEdge Smart Meter detected. Only REMOTE mode can work correctly")),
		REMOTE_CONTROL_NOT_ENABLED(Doc.of(Level.WARNING) //
				.text("Storage Control Mode is not set to Remote Control. Please configure inverter using SetApp/LCD")),
		AC_CHARGE_NOT_ENABLED(Doc.of(Level.WARNING) //
				.text("Storage AC Charge Policy is not set to Always allowed. Please configure inverter using SetApp/LCD")),
		PV_EXPORT_LIMIT_FAILED(Doc.of(Level.FAULT) //
				.text("PV-Export Limit failed")), //
		DISABLED_PV_EXPORT_LIMIT_FAILED(Doc.of(Level.WARNING) //
				.text("PV-Export Limit is disabled: PV-Export Limit failed")), //
		
		SERIAL_NUMBER(Doc.of(OpenemsType.STRING) //
				.persistencePriority(PersistencePriority.HIGH)),
		
		/**
		 * Read/Set Active Export Power Limit.
		 *
		 * <ul>
		 * <li>Interface: FeedToGridLimitEss
		 * <li>Type: Integer
		 * <li>Unit: W
		 * </ul>
		 */
		ACTIVE_EXPORT_POWER_LIMIT(new IntegerDoc() //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.MEDIUM) //
				.onInit(channel -> { //
					// on each Write to the channel -> set the value
					((IntegerWriteChannel) channel).onSetNextWrite(value -> {
						channel.setNextValue(value);
					});
				})),		
		
		/**
		 * Storage Control Mode is used to set the StorEdge system operating mode.
		 * <ul>
		 * <li>0 – Disabled
		 * <li>1 – Maximize Self Consumption – requires a SolarEdge Electricity meter
		 * on the grid or load connection point
		 * <li>2 – Time of Use (Profile programming) – requires a SolarEdge Electricity meter
		 * on the grid or load connection point
		 * <li>3 – Backup Only (applicable only for systems support backup functionality)
		 * <li>4 – Remote Control – the battery charge/discharge state is controlled by an
		 * external controller
		 * </ul>
		 */
		STORAGE_CONTROL_MODE(Doc.of(SeControlMode.values()).accessMode(AccessMode.READ_ONLY)),	
		
		/**
		 * Defines the AC charge policy for the storage system.
		 * <ul>
		 * <li>0 - Disable: No AC charging allowed.
		 * <li>1 - Always allowed: Essential for AC coupling operation. Enables
		 * unlimited charging from AC. In 'Maximize Self-Consumption' mode, charging
		 * occurs only with excess power; grid charging is prohibited.
		 * <li>2 - Fixed Energy Limit: Allows AC charging up to a fixed yearly limit
		 * (Jan 1 to Dec 31), crucial for ITC regulation compliance in the US.
		 * <li>3 - Percent of Production: Permits AC charging up to a percentage of the
		 * system's year-to-date production, also for ITC regulation in the US.
		 * </ul>
		 */
		STORAGE_AC_CHARGE_POLICY(Doc.of(AcChargePolicy.values()).accessMode(AccessMode.READ_ONLY)),

		/**
		 * Storage AC Charge Limit is used to set the AC charge limit according to the
		 * policy set in the previous register. Either fixed in kWh or percentage is set
		 * (e.g. 100KWh or 70%). Relevant only for Storage AC Charge Policy = 2 or 3
		 */
		STORAGE_AC_CHARGE_LIMIT(Doc.of(OpenemsType.INTEGER) // Percent or kWh
				.unit(Unit.PERCENT) //
				.persistencePriority(PersistencePriority.HIGH).accessMode(AccessMode.READ_ONLY)), // defined in external

		/**
		 * Storage Backup Reserved Setting sets the percentage of reserved battery SOE
		 * to be used for backup purposes. Relevant only for inverters with backup
		 * functionality.
		 * 
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: Percent
		 * <li>
		 * </ul>
		 */
		STORAGE_BACKUP_RESERVED_SETTING(Doc.of(OpenemsType.INTEGER) // Percent. Relevant only for inverters with backup functionality.
				.unit(Unit.PERCENT) //
				.persistencePriority(PersistencePriority.HIGH).accessMode(AccessMode.READ_ONLY)),	
		
		/**
		 * Charge/Discharge default Mode / Remote Control Command Mode Storage
		 * Charge/Discharge default Mode sets the default mode of operation when Remote
		 * Control Command Timeout has expired. The supported Charge/Discharge Modes are
		 * as follows: 0 – Off 1 – Charge excess PV power only. Only PV excess power not
		 * going to AC is used for charging the battery. Inverter
		 * NominalActivePowerLimit (or the inverter rated power whichever is lower) sets
		 * how much power the inverter is producing to the AC. In this mode, the battery
		 * cannot be discharged. If the PV power is lower than NominalActivePowerLimit
		 * the AC production will be equal to the PV power. 2 – Charge from PV first,
		 * before producing power to the AC. The Battery charge has higher priority than
		 * AC production. First charge the battery then produce AC. If
		 * StorageRemoteCtrl_ChargeLimit is lower than PV excess power goes to AC
		 * according to NominalActivePowerLimit. If NominalActivePowerLimit is reached
		 * and battery StorageRemoteCtrl_ChargeLimit is reached, PV power is curtailed.
		 * 3 – Charge from PV+AC according to the max battery power. Charge from both PV
		 * and AC with priority on PV power. If PV production is lower than
		 * StorageRemoteCtrl_ChargeLimit, the battery will be charged from AC up to
		 * NominalActivePow-erLimit. In this case AC power =
		 * StorageRemoteCtrl_ChargeLimit- PVpower. If PV power is larger than
		 * StorageRemoteCtrl_ChargeLimit the excess PV power will be directed to the AC
		 * up to the Nominal-ActivePowerLimit beyond which the PV is curtailed. 4 –
		 * Maximize export – discharge battery to meet max inverter AC limit. AC power
		 * is maintained to NominalActivePowerLimit, using PV power and/or battery
		 * power. If the PV power is not sufficient, battery power is used to complement
		 * AC power up to StorageRemoteCtrl_DishargeLimit. In this mode, charging excess
		 * power will occur if there is more PV than the AC limit. 5 – Discharge to meet
		 * loads consumption. Discharging to the grid is not allowed. 7 – Maximize
		 * self-consumption
		 */
		STORAGE_CHARGE_DISCHARGE_DEFAULT_MODE(Doc.of(CommandMode.values()).accessMode(AccessMode.READ_ONLY)),	

		/**
		 * Remote Control Command Timeout sets the operating timeframe for the
		 * charge/discharge command sets in Remote Control.
		 */
		DEBUG_REMOTE_CONTROL_COMMAND_TIMEOUT(Doc.of(OpenemsType.INTEGER).unit(Unit.SECONDS)), //
		REMOTE_CONTROL_COMMAND_TIMEOUT(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE).unit(Unit.SECONDS) //
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_REMOTE_CONTROL_COMMAND_TIMEOUT)), //), //

		/**
		 * Charge/Discharge default Mode / Remote Control Command Mode Storage
		 * Charge/Discharge default Mode sets the default mode of operation when Remote
		 * Control Command Timeout has expired. The supported Charge/Discharge Modes are
		 * as follows: 0 – Off 1 – Charge excess PV power only. Only PV excess power not
		 * going to AC is used for charging the battery. Inverter
		 * NominalActivePowerLimit (or the inverter rated power whichever is lower) sets
		 * how much power the inverter is producing to the AC. In this mode, the battery
		 * cannot be discharged. If the PV power is lower than NominalActivePowerLimit
		 * the AC production will be equal to the PV power. 2 – Charge from PV first,
		 * before producing power to the AC. The Battery charge has higher priority than
		 * AC production. First charge the battery then produce AC. If
		 * StorageRemoteCtrl_ChargeLimit is lower than PV excess power goes to AC
		 * according to NominalActivePowerLimit. If NominalActivePowerLimit is reached
		 * and battery StorageRemoteCtrl_ChargeLimit is reached, PV power is curtailed.
		 * 3 – Charge from PV+AC according to the max battery power. Charge from both PV
		 * and AC with priority on PV power. If PV production is lower than
		 * StorageRemoteCtrl_ChargeLimit, the battery will be charged from AC up to
		 * NominalActivePow-erLimit. In this case AC power =
		 * StorageRemoteCtrl_ChargeLimit- PVpower. If PV power is larger than
		 * StorageRemoteCtrl_ChargeLimit the excess PV power will be directed to the AC
		 * up to the Nominal-ActivePowerLimit beyond which the PV is curtailed. 4 –
		 * Maximize export – discharge battery to meet max inverter AC limit. AC power
		 * is maintained to NominalActivePowerLimit, using PV power and/or battery
		 * power. If the PV power is not sufficient, battery power is used to complement
		 * AC power up to StorageRemoteCtrl_DishargeLimit. In this mode, charging excess
		 * power will occur if there is more PV than the AC limit. 5 – Discharge to meet
		 * loads consumption. Discharging to the grid is not allowed. 7 – Maximize
		 * self-consumption
		 */
		DEBUG_REMOTE_CONTROL_COMMAND_MODE(Doc.of(CommandMode.values())),
		REMOTE_CONTROL_COMMAND_MODE(Doc.of(CommandMode.values())
				.accessMode(AccessMode.READ_WRITE)
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_REMOTE_CONTROL_COMMAND_MODE)), //

		/**
		 * Maximum Charge Power Channel. Always positive. Reads and Writes the charge power
		 * Control mode and charge policy have to be set
		 * <ul>
		 * <li>Interface: SolarEdgeEss
		 * <li>Type: Integer
		 * <li>Unit: W
		 * </ul>
		 */
		DEBUG_REMOTE_CONTROL_COMMAND_CHARGE_LIMIT(Doc.of(OpenemsType.INTEGER)),
		REMOTE_CONTROL_COMMAND_CHARGE_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE)
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_REMOTE_CONTROL_COMMAND_CHARGE_LIMIT)),

		/**
		 * Maximum Discharge Power Channel. Always positive. Reads and writes the discharge power.
		 * <ul>
		 * <li>Interface: SolarEdgeEss
		 * <li>Type: Integer
		 * <li>Unit: W
		 * </ul>
		 */
		DEBUG_REMOTE_CONTROL_COMMAND_DISCHARGE_LIMIT(Doc.of(OpenemsType.INTEGER)),
		REMOTE_CONTROL_COMMAND_DISCHARGE_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE)
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_REMOTE_CONTROL_COMMAND_DISCHARGE_LIMIT)),

		/**
		 * Battery 1 Max Charge Continues Power. Varies with SoC.
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		BATTERY1_MAX_CHARGE_CONTINUES_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.LOW)),
		
		/**
		 * Battery 1 Max Discharge Continues Power. Varies with SoC.
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		BATTERY1_MAX_DISCHARGE_CONTINUES_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.LOW)),		
		
		/**
		 * Battery 1 Max Charge Peak Power. Varies with SoC. ?????
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		BATTERY1_MAX_CHARGE_PEAK_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.LOW)), // defined in external file
		
		/**
		 * Battery 1 Max Discharge Peak Power. Varies with SoC. ?????
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		BATTERY1_MAX_DISCHARGE_PEAK_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.LOW)),
		
		/**
		 * Battery 1 Average Temperature.
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: °C
		 * <li>
		 * </ul>
		 */
		BATTERY1_AVG_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS) //
				.persistencePriority(PersistencePriority.LOW)),		
		
		/**
		 * Battery 1 Max Temperature.
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		BATTERY1_MAX_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS) //
				.persistencePriority(PersistencePriority.LOW)),		

		/**
		 * Battery 1 Actual Voltage.
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: V
		 * <li>
		 * </ul>
		 */
		BATTERY1_ACTUAL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.persistencePriority(PersistencePriority.LOW)),
		
		/**
		 * Battery 1 Actual Current to or from the battery.
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: mA
		 * <li>
		 * </ul>
		 */
		BATTERY1_ACTUAL_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.persistencePriority(PersistencePriority.LOW)),
		
		/**
		 * Battery 1 Actual Charge/Discharge Power.
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: positive values for Charge; negative for Discharge
		 * <li>This is the instantaneous power to or from the battery
		 * </ul>
		 */
		BATTERY1_ACTUAL_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.LOW)),	

		/**
		 * Battery 1 Lifetime Export Energy Counter. "Lifetime" resets every night. Channel not
		 * really useful!
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: Wh
		 * <li>
		 * </ul>
		 */
		BATTERY1_LIFETIME_EXPORT_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.LOW)),

		/**
		 * Battery 1 Lifetime Import Energy Counter. "Lifetime" resets every night. No useful
		 * information!
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: Wh
		 * <li>
		 * </ul>
		 */
		BATTERY1_LIFETIME_IMPORT_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.LOW)),		

		/**
		 * Battery 1 Max. Capacity.
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: Wh
		 * <li>
		 * </ul>
		 */
		BATTERY1_MAX_CAPACITY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.LOW)),		
		
		/**
		 * Battery 1 State Of Health.
		 *
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: Integer
		 * <li>Unit: Percent
		 * <li>
		 * </ul>
		 */
		SOH(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.persistencePriority(PersistencePriority.LOW)),		

		/**
		 * Batter 1 Status. SE_BATT_STATUS_OFF(0, "Off"), //
		 * SE_BATT_STATUS_STBY(1, "Standby"), // SE_BATT_STATUS_INIT(2, "Init"), //
		 * SE_BATT_STATUS_CHARGE(3, "Charge"), // SE_BATT_STATUS_DISCHARGE(4,
		 * "Discharge"), // SE_BATT_STATUS_FAULT(5, "Fault"), // // 6 doesn´t exist
		 * SE_BATT_STATUS_IDLE(7, "Idle"); //
		 * <ul>
		 * <li>Interface: Ess
		 * <li>Type: enum
		 * <li>Unit:
		 * <li>
		 * </ul>
		 */
		BATTERY1_STATUS(Doc.of(BatteryStatus.values()).accessMode(AccessMode.READ_ONLY)),		

		/**
		 * Inverter Actual DC Power.
		 *
		 * <ul>
		 * <li>Interface: SolarEdgeEss
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>
		 * </ul>
		 */
		INVERTER_ACTIVE_DC_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)),
		
		/**
		 * Inverter Max Apparent Power.
		 *
		 * <ul>
		 * <li>Interface: SolarEdgeEss
		 * <li>Type: Float
		 * <li>Unit: Percent
		 * <li>
		 * </ul>
		 */
		INVERTER_MAX_APPARENT_POWER(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.LOW)),	

		/**
		 * Power Control Fixed Power Limit.
		 *
		 * <ul>
		 * <li>Interface: SolarEdgeEss
		 * <li>Type: Float
		 * <li>Unit: Percent
		 * <li>
		 * </ul>
		 */
		INVERTER_POWER_LIMIT(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.PERCENT) //
				.persistencePriority(PersistencePriority.LOW)),	

		/**
		 * Advanced Power Control Enabled.
		 *
		 * <ul>
		 * <li>Interface: SolarEdgeEss
		 * <li>Type: Integer
		 * <li>
		 * </ul>
		 */
		ADVANCED_PWR_CONTROL_EN(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),		
		
		/**
		 * Export Control Mode.
		 *
		 * <ul>
		 * <li>Interface: SolarEdgeEss
		 * <li>Type: Integer
		 * <li>
		 * </ul>
		 */
		EXPORT_CONTROL_MODE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),
		
		/**
		 * Export Control Limit Mode.
		 *
		 * <ul>
		 * <li>Interface: SolarEdgeEss
		 * <li>Type: Integer
		 * <li>
		 * </ul>
		 */		
		EXPORT_CONTROL_LIMIT_MODE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),	
		
		/**
		 * Export Control Site Limit.
		 *
		 * <ul>
		 * <li>Interface: SolarEdgeEss
		 * <li>Type: Float
		 * <li>Unit: Watt
		 * <li>
		 * </ul>
		 */
		EXPORT_CONTROL_SITE_LIMIT(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)
				.accessMode(AccessMode.READ_WRITE)),	

		/**
		 * Active Production Energy.
		 *
		 * <ul>
		 * <li>Interface: SolarEdgeEss
		 * <li>Type: Long
		 * <li>Unit: CUMULATED_WATT_HOURS
		 * <li>Range: only positive values
		 * </ul>
		 */
		ACTIVE_PRODUCTION_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.CUMULATED_WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)),		
				
		/**
		 * DC-Voltage produced by the ESS. Either for grid or consumption.
		 *
		 * <ul>
		 * <li>Interface: SolarEdgeEss
		 * <li>Type: Integer
		 * <li>Unit: mV
		 * <li>
		 * </ul>
		 */
		VOLTAGE_DC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.persistencePriority(PersistencePriority.LOW)),

		/*
		 * 
		 * METER_COMMUNICATE_STATUS
		 * 
		 */
		METER_COMMUNICATE_STATUS(Doc.of(MeterCommunicateStatus.values())), //	
		
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
	 * Gets the Channel for {@link ChannelId#ACTIVE_EXPORT_POWER_LIMIT}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getActiveExportPowerLimitChannel() {
		return this.channel(ChannelId.ACTIVE_EXPORT_POWER_LIMIT);
	}
	
	/**
	 * Gets the Active Export Power Limit in [W]. See {@link ChannelId#ACTIVE_EXPORT_POWER_LIMIT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getActiveExportPowerLimit() {
		return this.getActiveExportPowerLimitChannel().value();
	}

	/**
	 * Sets the Active Export Power Limit in [W]. See {@link ChannelId#ACTIVE_EXPORT_POWER_LIMIT}.
	 *
	 * @param value the Integer value
	 * @throws OpenemsNamedException on error
	 */
	public default void setActiveExportPowerLimit(Integer value) throws OpenemsNamedException {
		this.getActiveExportPowerLimitChannel().setNextWriteValue(value);
	}

	/**
	 * Sets the Active Export Power Limit in [W]. See {@link ChannelId#ACTIVE_EXPORT_POWER_LIMIT}.
	 *
	 * @param value the int value
	 * @throws OpenemsNamedException on error
	 */
	public default void setActiveExportPowerLimit(int value) throws OpenemsNamedException {
		this.getActiveExportPowerLimitChannel().setNextWriteValue(value);
	}	

	/**
	 * Gets the Channel for {@link ChannelId#STORAGE_CONTROL_MODE}.
	 *
	 * @return the Channel
	 */
	public default Channel<SeControlMode> getStorageControlModeChannel() {
		return this.channel(ChannelId.STORAGE_CONTROL_MODE);
	}	

	/**
	 * See {@link ChannelId#STORAGE_CONTROL_MODE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default SeControlMode getStorageControlMode() {
		return this.getStorageControlModeChannel().value().asEnum();
	}	

	/**
	 * Gets the Channel for {@link ChannelId#STORAGE_AC_CHARGE_POLICY}.
	 *
	 * @return the Channel
	 */
	public default Channel<AcChargePolicy> getStorageAcChargePolicyChannel() {
		return this.channel(ChannelId.STORAGE_AC_CHARGE_POLICY);
	}

	/**
	 * AC charge policy {@link ChannelId#STORAGE_AC_CHARGE_POLICY}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default AcChargePolicy getStorageAcChargePolicy() {
		return this.getStorageAcChargePolicyChannel().value().asEnum();
	}
	
	/**
	 * Gets the Channel for {@link ChannelId#STORAGE_AC_CHARGE_LIMIT}.
	 *
	 * @return the Channel
	 */
	public default Channel<AcChargePolicy> getStorageAcChargeLimitChannel() {
		return this.channel(ChannelId.STORAGE_AC_CHARGE_LIMIT);
	}

	/**
	 * AC charge policy {@link ChannelId#STORAGE_AC_CHARGE_LIMIT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default SeControlMode getStorageAcChargeLimit() {
		return this.getStorageAcChargeLimitChannel().value().asEnum();
	}

	/**
	 * Gets the Channel for {@link ChannelId#STORAGE_BACKUP_RESERVED_SETTING}.
	 *
	 * @return the Channel
	 */
	public default Channel<AcChargePolicy> getStorageBackupReservedSettingChannel() {
		return this.channel(ChannelId.STORAGE_BACKUP_RESERVED_SETTING);
	}

	/**
	 * See {@link ChannelId#STORAGE_BACKUP_RESERVED_SETTING}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default SeControlMode getStorageBackupReservedSetting() {
		return this.getStorageBackupReservedSettingChannel().value().asEnum();
	}

	/**
	 * Gets the Channel for {@link ChannelId#CHARGE_DISCHARGE_DEFAULT_MODE}.
	 *
	 * @return the Channel
	 */
	public default Channel<CommandMode> getStorageChargeDischargeDefaultModeChannel() {
		return this.channel(ChannelId.STORAGE_CHARGE_DISCHARGE_DEFAULT_MODE);
	}

	/**
	 * See {@link ChannelId#CHARGE_DISCHARGE_DEFAULT_MODE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default SeControlMode getStorageChargeDischargeDefaultMode() {
		return this.getStorageChargeDischargeDefaultModeChannel().value().asEnum();
	}

	/**
	 * Gets the Channel for {@link ChannelId#BATTERY1_MAX_CHARGE_CONTINUES_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getBattery1MaxChargeContinuesPowerChannel() {
		return this.channel(ChannelId.BATTERY1_MAX_CHARGE_CONTINUES_POWER);
	}

	/**
	 * See {@link ChannelId#BATTERY1_MAX_CHARGE_CONTINUES_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBattery1MaxChargeContinuesPower() {
		return this.getBattery1MaxChargeContinuesPowerChannel().value();
	}
	
	
	/**
	 * Gets the Channel for {@link ChannelId#BATTERY1_MAX_DISCHARGE_CONTINUES_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getBattery1MaxDischargeContinuesPowerChannel() {
		return this.channel(ChannelId.BATTERY1_MAX_DISCHARGE_CONTINUES_POWER);
	}

	/**
	 * See {@link ChannelId#BATTERY1_MAX_DISCHARGE_CONTINUES_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBattery1MaxDischargeContinuesPower() {
		return this.getBattery1MaxDischargeContinuesPowerChannel().value();
	}
	
	/**
	 * Gets the Channel for {@link ChannelId#BATTERY1_MAX_CHARGE_PEAK_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getBattery1MaxChargePeakPowerChannel() {
		return this.channel(ChannelId.BATTERY1_MAX_CHARGE_PEAK_POWER);
	}

	/**
	 * See {@link ChannelId#BATTERY1_MAX_CHARGE_PEAK_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBattery1MaxChargePeakPower() {
		return this.getBattery1MaxChargePeakPowerChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#BATTERY1_MAX_DISCHARGE_PEAK_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getBattery1MaxDischargePeakPowerChannel() {
		return this.channel(ChannelId.BATTERY1_MAX_DISCHARGE_PEAK_POWER);
	}

	/**
	 * See {@link ChannelId#BATTERY1_MAX_DISCHARGE_PEAK_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBattery1MaxDischargePeakPower() {
		return this.getBattery1MaxDischargePeakPowerChannel().value();
	}
	
	/**
	 * Gets the Channel for {@link ChannelId#BATTERY1_ACTUAL_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getBattery1ActualPowerChannel() {
		return this.channel(ChannelId.BATTERY1_ACTUAL_POWER);
	}

	/**
	 * AC-Power produced by ESS. See {@link ChannelId#BATTERY1_ACTUAL_POWER}
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBattery1ActualPower() {
		return this.getBattery1ActualPowerChannel().value();
	}	
	
	/**
	 * Gets the Channel for {@link ChannelId#BATTERY1_LIFETIME_EXPORT_ENERGY}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getBattery1LifetimeExportEnergyChannel() {
		return this.channel(ChannelId.BATTERY1_LIFETIME_EXPORT_ENERGY);
	}

	/**
	 * Gets the Actual Energy in [Wh_Σ]. See
	 * {@link ChannelId#BATTERY1_LIFETIME_EXPORT_ENERGY}
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Long> getBattery1LifetimeExportEnergy() {
		return this.getBattery1LifetimeExportEnergyChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#BATTERY1_LIFETIME_IMPORT_ENERGY}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getBattery1LifetimeImportEnergyChannel() {
		return this.channel(ChannelId.BATTERY1_LIFETIME_IMPORT_ENERGY);
	}

	/**
	 * See {@link ChannelId#BATTERY1_LIFETIME_IMPORT_ENERGY}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Long> getBattery1LifetimeImportEnergy() {
		return this.getBattery1LifetimeImportEnergyChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#INVERTER_POWER_LIMIT}.
	 *
	 * @return the Channel
	 */
	public default FloatReadChannel getInverterPowerLimitChannel() {
		return this.channel(ChannelId.INVERTER_POWER_LIMIT);
	}

	/**
	 * See {@link ChannelId#INVERTER_POWER_LIMIT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Float> getInverterPowerLimit() {
		return this.getInverterPowerLimitChannel().value();
	}
	
	/**
	 * Gets the Channel for {@link ChannelId#ACTIVE_PRODUCTION_ENERGY}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getActiveProductionEnergyChannel() {
		return this.channel(ChannelId.ACTIVE_PRODUCTION_ENERGY);
	}

	/**
	 * See {@link ChannelId#ACTIVE_PRODUCTION_ENERGY}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Long> getActiveProductionEnergy() {
		return this.getActiveProductionEnergyChannel().value();
	}	
	
	/**
	 * Adds DC-charger to ESS hybrid system. Represents PV production
	 * 
	 * @param charger link to DC charger(s)
	 */
	public void addCharger(SolarEdgeCharger charger);

	/**
	 * Removes link to pv DC charger.
	 * 
	 * @param charger charger
	 */
	public void removeCharger(SolarEdgeCharger charger);

	/**
	 * returns ModbusBrdigeId from config.
	 * 
	 * @return ModbusBrdigeId from config
	 */
	public String getModbusBridgeId();

	/**
	 * returns UnitId for ESS from config.
	 * 
	 * @return UnitId for ESS from config
	 */
	public Integer getUnitId();
	
	
	/**
	 * Gets the PV production from chargers ACTUAL_POWER. Returns null if the PV
	 * production is not available.
	 *
	 * @return production power
	 */
	public Integer getPvProduction();

	/**
	 * Gets Surplus Power.
	 *
	 * @return {@link Integer}
	 */
	public Integer getSurplusPower();

	/**
	 * Used for Modbus/TCP Api Controller. Provides a Modbus table for the Channels
	 * of this Component.
	 *
	 * @param accessMode filters the Modbus-Records that should be shown
	 * @return the {@link ModbusSlaveNatureTable}
	 */
	public default ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {
		return ModbusSlaveNatureTable.of(SolarEdgeEss.class, accessMode, 100) //
				.build();
	}
	
}
