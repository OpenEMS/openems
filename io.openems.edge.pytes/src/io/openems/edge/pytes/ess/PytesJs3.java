package io.openems.edge.pytes.ess;

import static io.openems.common.channel.AccessMode.READ_ONLY;
import static io.openems.common.types.OpenemsType.INTEGER;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.pytes.battery.PytesBattery;
import io.openems.edge.pytes.dccharger.PytesDcCharger;
import io.openems.edge.pytes.enums.Appendix2;
import io.openems.edge.pytes.enums.StandardWorkingMode;

public interface PytesJs3 extends OpenemsComponent, EventHandler {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		STARTER_BATTERY_VOLTAGE(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.VOLT)),

		INVERTED_RATED_APPARENT_POWER(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.VOLT_AMPERE)),

		// Add details and comments beyond this line
		SAFETY_VERSION(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
		),

		// Add details and comments beyond this line

		HMI_SUB_VERSION(Doc.of(INTEGER) // Version number of the HMI
		// .accessMode(READ_ONLY)
		),

		/*
		 * Add Alarm code data to distinguishing displayed Alarm code For external fan
		 * failure, each bit indicates the status of one fan; In conjunction with the
		 * 33095 register address, it is used for subdivided fault information display.
		 * Example: 33095 register read information is 0x1020,
		 */

		ALARM_CODE_DATA(Doc.of(INTEGER)
		// .accessMode(READ_ONLY)
		),

		DC_BUS_VOLTAGE(Doc.of(INTEGER).accessMode(READ_ONLY).unit(Unit.MILLIVOLT)),

		DC_BUS_HALF_VOLTAGE(Doc.of(INTEGER)
		// .accessMode(READ_ONLY)
		),

		APPARENT_POWER(Doc.of(INTEGER)
		// .accessMode(READ_ONLY)
		),

		INVERTER_CURRENT_STATUS(Doc.of(Appendix2.values()).accessMode(AccessMode.READ_ONLY)),

		LEAD_ACID_BATTERY_TEMP(Doc.of(INTEGER)
		// .accessMode(READ_ONLY)
		),

		FUNCTION_STATUS(Doc.of(INTEGER)
		// .accessMode(READ_ONLY)
		),

		CURRENT_DRM_CODE_STATUS(Doc.of(INTEGER)
		// .accessMode(READ_ONLY)
		),

		INVERTER_CABINET_TEMP(Doc.of(INTEGER)
		// .accessMode(READ_ONLY)
		),

		LIMITED_POWER_ACTUAL_VALUE(Doc.of(INTEGER)
		// .accessMode(READ_ONLY)
		),

		PF_ADJUSTMENT_ACTUAL_VALUE(Doc.of(INTEGER)
		// .accessMode(READ_ONLY)
		),

		LIMITED_REACTIVE_POWER(Doc.of(INTEGER)
		// .accessMode(READ_ONLY)
		),

		INVERTER_MODULE_TEMP2(Doc.of(INTEGER)
		// .accessMode(READ_ONLY)
		),

		VOLT_VAR_VREF_RT_VALUES(Doc.of(INTEGER)
		// .accessMode(READ_ONLY)
		),

		BMS_CHARGING_VOLTAGE_LIMIT(Doc.of(INTEGER)
		// .accessMode(READ_ONLY)
		),

		BATTERY_BMS_STATUS(Doc.of(INTEGER)
		// .accessMode(READ_ONLY)
		),

		INVERTER_INITIAL_SETTING_STATE(Doc.of(INTEGER)
		// .accessMode(READ_ONLY)
		),

		BATCH_UPGRADE_BOWL(Doc.of(INTEGER)
		// .accessMode(READ_ONLY)
		),

		FCAS_MODE_RUNNING_STATUS(Doc.of(INTEGER)
		// .accessMode(READ_ONLY)
		),

		SETTING_FLAG_BIT(Doc.of(INTEGER)
		// .accessMode(READ_ONLY)
		),

		FAULT_CODE_01(Doc.of(INTEGER)
		// .accessMode(READ_ONLY)
		),

		FAULT_CODE_02(Doc.of(INTEGER)
		// .accessMode(READ_ONLY)
		),

		FAULT_CODE_03(Doc.of(INTEGER)
		// .accessMode(READ_ONLY)
		),

		FAULT_CODE_04(Doc.of(INTEGER)
		// .accessMode(READ_ONLY)
		),

		FAULT_CODE_05(Doc.of(INTEGER)
		// .accessMode(READ_ONLY)
		),

		FAULT_CODE_06(Doc.of(INTEGER)
		// .accessMode(READ_ONLY)
		),

		FAULT_CODE_07(Doc.of(INTEGER)
		// .accessMode(READ_ONLY)
		),

		OPERATING_STATUS(Doc.of(INTEGER)
		// .accessMode(READ_ONLY)
		),

		OPERATING_MODE(Doc.of(INTEGER)
		// .accessMode(READ_ONLY)
		),

		WORKING_MODE_RUNNING_STATUS(Doc.of(INTEGER)
		// .accessMode(READ_ONLY)
		),

		STORAGE_CONTROL_SWITCHING_VALUE(Doc.of(INTEGER)
		// .accessMode(READ_ONLY)
		),

		VOLTAGE_L1(Doc.of(OpenemsType.INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.MILLIVOLT)),

		VOLTAGE_L2(Doc.of(OpenemsType.INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.MILLIVOLT)),

		VOLTAGE_L3(Doc.of(OpenemsType.INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.MILLIVOLT)),

		CURRENT_L1(Doc.of(OpenemsType.INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.MILLIAMPERE)),

		CURRENT_L2(Doc.of(OpenemsType.INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.MILLIAMPERE)),

		CURRENT_L3(Doc.of(OpenemsType.INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.MILLIAMPERE)),

		FREQUENCY(Doc.of(OpenemsType.INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.MILLIHERTZ)),

		SET_REMOTE_CONTROL_AC_GRID_PORT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.WRITE_ONLY)), //

		SET_REMOTE_CONTROL_MODE(Doc.of(OpenemsType.INTEGER) // 0 OFF， 1 ON with 'system grid connection point'， 2 ON
															// with 'Inverter AC grid port'
				.accessMode(AccessMode.WRITE_ONLY)),

		SET_REMOTE_DISPATCH_SWITCH(Doc.of(OpenemsType.INTEGER) // 0 OFF, 1 ON
				.accessMode(AccessMode.WRITE_ONLY)),

		SET_REMOTE_DISPATCH_TIMEOUT(Doc.of(OpenemsType.INTEGER) // Timeout in minutes for remote dispatch (failsafe
																// timer)
				.accessMode(AccessMode.WRITE_ONLY)),

		SET_REMOTE_DISPATCH_SYSTEM_LIMIT_SWITCH(Doc.of(OpenemsType.INTEGER) // 0 Disable system import/export limit, 1
																			// Enable
				.accessMode(AccessMode.WRITE_ONLY)),

		SET_REMOTE_DISPATCH_SYSTEM_IMPORT_LIMIT(Doc.of(OpenemsType.INTEGER) // System import limit; 0xFFFF = default /
																			// ignore
				.accessMode(AccessMode.WRITE_ONLY)),

		SET_REMOTE_DISPATCH_SYSTEM_EXPORT_LIMIT(Doc.of(OpenemsType.INTEGER) // System export limit; 0xFFFF = default /
																			// ignore
				.accessMode(AccessMode.WRITE_ONLY)),

		SET_REMOTE_DISPATCH_REALTIME_CONTROL_SWITCH(Doc.of(OpenemsType.INTEGER) // 1 Ignore register value, 2 Battery
																				// charge/discharge control, 3 Grid
																				// import/export control, 4 Grid power
																				// control
				.accessMode(AccessMode.WRITE_ONLY)),

		SET_REMOTE_DISPATCH_REALTIME_CONTROL_POWER(Doc.of(OpenemsType.INTEGER) // S32 value (two registers), unit 10 W;
																				// positive = charge/import, negative =
																				// discharge/export
				.accessMode(AccessMode.WRITE_ONLY)),

		SET_REMOTE_DISPATCH_REALTIME_CONTROL_FUNCTION_SWITCH(Doc.of(OpenemsType.INTEGER) // Bitfield: PV shutdown, DO
																							// control, allow grid
																							// charge, off-grid battery
																							// standby
				.accessMode(AccessMode.WRITE_ONLY)),

		STANDARD_WORKING_MODE(Doc.of(StandardWorkingMode.values()).accessMode(AccessMode.READ_ONLY)),

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

	// Set remote dispatch switch
	public default void setRemoteDispatchSwitch(int value) throws OpenemsNamedException {
		this.getSetRemoteDispatchSwitchChannel().setNextWriteValue(value);
	}

	public default IntegerWriteChannel getSetRemoteDispatchSwitchChannel() {
		return this.channel(ChannelId.SET_REMOTE_DISPATCH_SWITCH);
	}

	// Set remote dispatch timeout
	public default void setRemoteDispatchTimeout(int value) throws OpenemsNamedException {
		this.getSetRemoteDispatchTimeoutChannel().setNextWriteValue(value);
	}

	public default IntegerWriteChannel getSetRemoteDispatchTimeoutChannel() {
		return this.channel(ChannelId.SET_REMOTE_DISPATCH_TIMEOUT);
	}

	// Set remote dispatch system limit switch
	public default void setRemoteDispatchSystemLimitSwitch(int value) throws OpenemsNamedException {
		this.getSetRemoteDispatchSystemLimitSwitchChannel().setNextWriteValue(value);
	}

	public default IntegerWriteChannel getSetRemoteDispatchSystemLimitSwitchChannel() {
		return this.channel(ChannelId.SET_REMOTE_DISPATCH_SYSTEM_LIMIT_SWITCH);
	}

	// Set remote dispatch system import limit
	public default void setRemoteDispatchSystemImportLimit(int value) throws OpenemsNamedException {
		this.getSetRemoteDispatchSystemImportLimitChannel().setNextWriteValue(value);
	}

	public default IntegerWriteChannel getSetRemoteDispatchSystemImportLimitChannel() {
		return this.channel(ChannelId.SET_REMOTE_DISPATCH_SYSTEM_IMPORT_LIMIT);
	}

	// Set remote dispatch system export limit
	public default void setRemoteDispatchSystemExportLimit(int value) throws OpenemsNamedException {
		this.getSetRemoteDispatchSystemExportLimitChannel().setNextWriteValue(value);
	}

	public default IntegerWriteChannel getSetRemoteDispatchSystemExportLimitChannel() {
		return this.channel(ChannelId.SET_REMOTE_DISPATCH_SYSTEM_EXPORT_LIMIT);
	}

	// Set realtime control switch
	public default void setRemoteDispatchRealtimeControlSwitch(int value) throws OpenemsNamedException {
		this.getSetRemoteDispatchRealtimeControlSwitchChannel().setNextWriteValue(value);
	}

	public default IntegerWriteChannel getSetRemoteDispatchRealtimeControlSwitchChannel() {
		return this.channel(ChannelId.SET_REMOTE_DISPATCH_REALTIME_CONTROL_SWITCH);
	}

	// Set realtime control power (S32 value)
	public default void setRemoteDispatchRealtimeControlPower(int value) throws OpenemsNamedException {
		this.getSetRemoteDispatchRealtimeControlPowerChannel().setNextWriteValue(value);
	}

	public default IntegerWriteChannel getSetRemoteDispatchRealtimeControlPowerChannel() {
		return this.channel(ChannelId.SET_REMOTE_DISPATCH_REALTIME_CONTROL_POWER);
	}

	// Set realtime control function switch
	public default void setRemoteDispatchRealtimeControlFunctionSwitch(int value) throws OpenemsNamedException {
		this.getSetRemoteDispatchRealtimeControlFunctionSwitchChannel().setNextWriteValue(value);
	}

	public default IntegerWriteChannel getSetRemoteDispatchRealtimeControlFunctionSwitchChannel() {
		return this.channel(ChannelId.SET_REMOTE_DISPATCH_REALTIME_CONTROL_FUNCTION_SWITCH);
	}

	// Set remote control mode
	public default void setRemoteControlMode(int value) throws OpenemsNamedException {
		this.getSetRemoteControlModeChannel().setNextWriteValue(value);
	}

	public default IntegerWriteChannel getSetRemoteControlModeChannel() {
		return this.channel(ChannelId.SET_REMOTE_CONTROL_MODE);
	}

	// Set power setpoint
	public default void setRemoteControlPower(int value) throws OpenemsNamedException {
		this.getSetRemoteControlPowerChannel().setNextWriteValue(value);
	}

	public default IntegerWriteChannel getSetRemoteControlPowerChannel() {
		return this.channel(ChannelId.SET_REMOTE_CONTROL_AC_GRID_PORT_POWER);
	}

	/**
	 * Adds Battery to ESS hybrid system.
	 * 
	 * @param battery link to Pytes battery
	 */
	public void addBattery(PytesBattery battery);

	/**
	 * Removes link to battery.
	 * 
	 * @param PytesBattery battery
	 */
	public void removeBattery(PytesBattery battery);

	/**
	 * Adds DC-charger to ESS hybrid system. Represents PV production
	 * 
	 * @param charger link to DC charger(s)
	 */
	public void addCharger(PytesDcCharger charger);

	/**
	 * Removes link to pv DC charger.
	 * 
	 * @param charger charger
	 */
	public void removeCharger(PytesDcCharger charger);

	/**
	 * returns ModbusBrdigeId from config.
	 * 
	 * @return ModbusBrdigeId from config
	 */
	public String getModbusBridgeId();

	public Integer getUnitId();
}
