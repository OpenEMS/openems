package io.openems.edge.battery.fenecon.f2b.bmw;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Debounce;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.fenecon.f2b.BatteryFeneconF2b;
import io.openems.edge.battery.fenecon.f2b.bmw.enums.ContactorDiagnosticStatus;
import io.openems.edge.battery.fenecon.f2b.bmw.enums.CoolingApproval;
import io.openems.edge.battery.fenecon.f2b.bmw.enums.CoolingRequest;
import io.openems.edge.battery.fenecon.f2b.bmw.enums.CoolingValveErrorState;
import io.openems.edge.battery.fenecon.f2b.bmw.enums.CoolingValveRequest;
import io.openems.edge.battery.fenecon.f2b.bmw.enums.GoRunningSubState;
import io.openems.edge.battery.fenecon.f2b.bmw.enums.HeatingRequest;
import io.openems.edge.battery.fenecon.f2b.bmw.enums.HvContactorStatus;
import io.openems.edge.battery.fenecon.f2b.bmw.enums.InsulationMeasurement;
import io.openems.edge.battery.fenecon.f2b.bmw.enums.InsulationMeasurementStatus;
import io.openems.edge.battery.fenecon.f2b.bmw.enums.OperationState;
import io.openems.edge.battery.fenecon.f2b.bmw.enums.RequestCharging;
import io.openems.edge.battery.fenecon.f2b.bmw.statemachine.StateMachine.State;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.DoubleDoc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;

public interface BatteryFeneconF2bBmw extends BatteryFeneconF2b, Battery, OpenemsComponent, StartStoppable {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		// T_SEC_COU_REL, Time in seconds since 1.1.2016 [0:00:00].
		// "This value is used as a time stamp for events and in the error memory
		TIMESTAMP_FOR_EVENTS_AND_ERROR_MEMORY(Doc.of(OpenemsType.LONG)//
				.unit(Unit.SECONDS)//
				.accessMode(AccessMode.READ_WRITE)), // [0...4294967294s]

		// RQ_DCSW_HVSTO_CLO, Request to close HV contactors
		HV_CONTACTOR(Doc.of(OpenemsType.BOOLEAN)//
				.accessMode(AccessMode.READ_WRITE)), //

		// CTR_MEASMT_ISL
		INSULATION_MEASUREMENT(Doc.of(InsulationMeasurement.values())//
				.accessMode(AccessMode.READ_WRITE)), //

		// RLS_COOL_HVSTO, Cooling approval, if requested by the battery
		COOLING_APPROVAL(Doc.of(CoolingApproval.values())//
				.accessMode(AccessMode.READ_WRITE)), //

		// RQ_CSOV_HVSTO
		REQUEST_COOLING_VALVE(Doc.of(CoolingValveRequest.values())//
				.accessMode(AccessMode.READ_WRITE)), //

		// RLS_ID_PWRCOS_FN_1
		ALLOCATES_BATTERY_HEATING_POWER(Doc.of(HeatingRequest.values())//
				.accessMode(AccessMode.READ_WRITE)), //

		// RLS_PWR_HV_FN_1
		HEATING_RELEASED_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.accessMode(AccessMode.READ_WRITE)), // [0 ... 40940 W]

		// RQ_CHGRDI
		REQUEST_CHARGING(Doc.of(RequestCharging.values())//
				.accessMode(AccessMode.READ_WRITE)), //

		// FRC_PWR_CHGNG, Forecast of power charging
		PREDICTED_CHARGING_POWER(Doc.of(OpenemsType.LONG)//
				.unit(Unit.WATT)//
				.accessMode(AccessMode.READ_WRITE)), // [0 ... 102350 W]

		// FRC_AVR_HVSTO_PWR
		PREDICTION_OF_THE_AVERAGE_EXPECTED_POWER_LOAD(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.accessMode(AccessMode.WRITE_ONLY)), // [0 ... 204,70 kW]

		// IDENT_HVSTO, Hardware identification number of the high voltage battery
		// system
		HARDWARE_IDENTIFICATION_NUMBER(Doc.of(OpenemsType.LONG)//
				.accessMode(AccessMode.READ_ONLY)), //

		// Read Channels

		UNDERVOLTAGE_AT_OUTPUT(Doc.of(Level.FAULT)//
				.accessMode(AccessMode.READ_ONLY)), //

		// ST_DCSW_HVSTO
		HV_CONTACTOR_STATUS(Doc.of(HvContactorStatus.values())//
				.accessMode(AccessMode.READ_ONLY)), //

		// Error Channels

		// ST_PRCHRG_LOKD_HVSTO
		CAT1_PRECHARGE_SYSTEM_IS_LOCKED(Doc.of(Level.WARNING)//
				.debounce(5, Debounce.TRUE_VALUES_IN_A_ROW_TO_SET_TRUE)//
				.accessMode(AccessMode.READ_ONLY)), //

		CAT1_FAULT(Doc.of(Level.FAULT)//
				.debounce(5, Debounce.TRUE_VALUES_IN_A_ROW_TO_SET_TRUE)//
				.text("CAT1 precharge system is locked")//
				.accessMode(AccessMode.READ_ONLY)), //
		// RQ_SER_HVSTO, Error category 3: No limitation but service needed
		CAT3_NO_LIMITATIONS_BUT_SERVICE_NEEDED(Doc.of(Level.WARNING)//
				.debounce(5, Debounce.TRUE_VALUES_IN_A_ROW_TO_SET_TRUE)//
				.accessMode(AccessMode.READ_ONLY)), //

		// ERR_EMMOD_HVSTO, Error category 4: At least one de_rating is active
		CAT4_AT_LEAST_ONE_DERATING_ACTIVE(Doc.of(Level.WARNING)//
				.debounce(5, Debounce.TRUE_VALUES_IN_A_ROW_TO_SET_TRUE)//
				.accessMode(AccessMode.READ_ONLY)), //

		// RQ_OPC_CHG_HVSTO, Error category 5: The power of the batteries will be
		// limited.The error turns into CAT6 automatically after 10s
		CAT5_BATTERY_POWER_WILL_BE_LIMITED(Doc.of(Level.WARNING)//
				.debounce(5, Debounce.TRUE_VALUES_IN_A_ROW_TO_SET_TRUE)//
				.accessMode(AccessMode.READ_ONLY)), //

		CAT5_FAULT(Doc.of(Level.FAULT)//
				.debounce(5, Debounce.TRUE_VALUES_IN_A_ROW_TO_SET_TRUE)//
				.text("CAT5 battery power will be limited")//
				.accessMode(AccessMode.READ_ONLY)), //

		// "RQ_OPN_DCSW_HVSTO_FAST, Error category 6: Zero current request and
		// contactors will open after 2s (emergency stop requested, set load to zero)
		CAT6_ZERO_CURRENT_REQUEST(Doc.of(Level.WARNING)//
				.debounce(5, Debounce.TRUE_VALUES_IN_A_ROW_TO_SET_TRUE)//
				.accessMode(AccessMode.READ_ONLY)), //

		CAT6_FAULT(Doc.of(Level.FAULT)//
				.debounce(5, Debounce.TRUE_VALUES_IN_A_ROW_TO_SET_TRUE)//
				.text("CAT6 Zero current request")//
				.accessMode(AccessMode.READ_ONLY)), //

		// RQ_OPN_DCSW_HVSTO_ILY, Error category 7: Contactors will open immediately
		CAT7_EMERGENCY_CONTACTOR_OPEN(Doc.of(Level.WARNING)//
				.debounce(5, Debounce.TRUE_VALUES_IN_A_ROW_TO_SET_TRUE)//
				.accessMode(AccessMode.READ_ONLY)), //

		CAT7_FAULT(Doc.of(Level.FAULT)//
				.debounce(5, Debounce.TRUE_VALUES_IN_A_ROW_TO_SET_TRUE)//
				.text("CAT7 emergency contactor open")//
				.accessMode(AccessMode.READ_ONLY)), //

		// Battery Measurement

		// MB_U_HV_CELLSUM , Summed cell voltages
		SUMMED_CELL_VOLTAGES(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.VOLT)//
				.accessMode(AccessMode.READ_ONLY)), // [0 ... 8,188 V]

		// U_MIN_DCHG_HVSTO
		BATTERY_DISCHARGE_MIN_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.VOLT)//
				.accessMode(AccessMode.READ_ONLY)),
		BATTERY_CHARGE_MAX_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.VOLT)//
				.accessMode(AccessMode.READ_ONLY)),
		BATTERY_CHARGE_MAX_CURRENT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.AMPERE)//
				.accessMode(AccessMode.READ_ONLY)),
		BATTERY_DISCHARGE_MAX_CURRENT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.AMPERE)//
				.accessMode(AccessMode.READ_ONLY)),

		// AVLB_PWR_SRT_DCHG_HVSTO, Maximum allowed discharge power for the next second
		ALLOWED_DISCHARGE_POWER(Doc.of(OpenemsType.LONG)//
				.unit(Unit.WATT)//
				.accessMode(AccessMode.READ_ONLY)), // [0… 196602 W]

		// AVLB_PWR_LT_DCHG_HVSTO,Maximum allowed discharge power for next 5s
		MAX_ALLOWED_DISCHARGE_POWER(Doc.of(OpenemsType.LONG)//
				.unit(Unit.WATT)//
				.accessMode(AccessMode.READ_ONLY)), // [0 … 196602 W]

		// AVLB_PWR_LT_CHG_HVSTO, Maximum allowed discharge power for the next second
		MAX_ALLOWED_CHARGE_POWER(Doc.of(OpenemsType.LONG)//
				.unit(Unit.WATT)//
				.accessMode(AccessMode.READ_ONLY)), //

		// AVLB_PWR_SRT_CHG_HVSTO,Maximum allowed discharge power for next 5s
		ALLOWED_CHARGE_POWER(Doc.of(OpenemsType.LONG)//
				.unit(Unit.WATT)//
				.accessMode(AccessMode.READ_ONLY)), // [0 … 196596 W]

		// ST_MEASMT_ISL,Current status of the insulation measurement function
		INSULATION_MEASUREMENT_STATUS(Doc.of(InsulationMeasurementStatus.values())//
				.accessMode(AccessMode.READ_ONLY)), //

		// AVL_ISRE
		INSULATION_RESISTANCE(Doc.of(OpenemsType.LONG)//
				.unit(Unit.KILOOHM)//
				.accessMode(AccessMode.READ_ONLY)), // [0...2047kOhm]

		// ST_WARN_ISL_BN_HV,The insulation value is below the warning value (250 kOhm)
		INSULATION_VALUE_WARNING(Doc.of(Level.WARNING)//
				.accessMode(AccessMode.READ_ONLY)), // [0 ... 2047kOhm]

		MAX_BALANCING_TIME(Doc.of(OpenemsType.LONG)//
				.unit(Unit.MINUTE)//
				.accessMode(AccessMode.READ_ONLY)), //

		// RQ_COOL_HVSTO,Cooling request: In case the EES is requesting cooling, cooling
		// should be granted in order to maximize EES life. Cooling is granted for each
		// EES by setting the signal 'RLS_COOL_HVSTO' to the correct value" (01h for
		// cooling granted).
		COOLING_REQUEST(Doc.of(CoolingRequest.values())//
				.accessMode(AccessMode.READ_WRITE)//
				.persistencePriority(PersistencePriority.HIGH)//
				.<BatteryFeneconF2bBmwImpl>onChannelChange(//
						BatteryFeneconF2bBmwImpl::updateCoolingRequest)), //

		// ST_STOR_AIC_COOL_RQMT, Battery Cooling necessity expressed as average
		// thermal_power loss
		AVERAGE_COOLING_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.persistencePriority(PersistencePriority.HIGH)//
				.accessMode(AccessMode.READ_ONLY)), // [0...5040W]

		// AVL_TEMP_HTEX_HVSTO, Actual temperature of the cooling plate in the battery
		COOLING_PLATE_TEMPERATURE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)//
				.accessMode(AccessMode.READ_ONLY)), // [-50...204°C]

		// ST_CSOV_HVSTO, Error state of the cooling valve
		COOLING_VALVE_ERROR_STATE(Doc.of(CoolingValveErrorState.values())//
				.accessMode(AccessMode.READ_ONLY)), //

		// INQY_ID_PWRCOS_FN_1 ,This signal shows the heating request
		HEATING_REQUEST(Doc.of(HeatingRequest.values())//
				.accessMode(AccessMode.READ_ONLY)), //

		// INQY_PWR_HV_FN_1, This signals shows the power, which is send to HV
		BATTERY_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.accessMode(AccessMode.READ_ONLY)), //

		// AVL_PWR_HT_HVSTO
		HEATING_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.accessMode(AccessMode.READ_ONLY)), // [0 ... 40940W]

		// ENC_HVSTO_MAX
		PREDICTED_DISCHARGE_ENERGY(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.KILOWATT_HOURS)//
				.accessMode(AccessMode.READ_ONLY)), // [0..81,88kWh]

		// PRD_ENERG_CHGCOND_2_COS
		PREDICTED_AVERAGE_ENERGY(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.KILOWATT_HOURS)//
				.accessMode(AccessMode.READ_ONLY)), //

		// PRD_ENERG_CHGTAR_2_HVSTO, The amount of energy in kWh to receive the target
		// SoC
		PREDICTED_ENERGY_TO_RECEIVE_THE_TARGET_SOC(Doc.of(OpenemsType.LONG)//
				.unit(Unit.KILOWATT_HOURS)//
				.accessMode(AccessMode.READ_ONLY)), //

		UNLIMITED_SOC(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.THOUSANDTH)//
				.accessMode(AccessMode.READ_ONLY)), //

		// CHGCOND_HVSTO
		BATTERY_SOC(new DoubleDoc() //
				.accessMode(AccessMode.READ_ONLY) //
				.<BatteryFeneconF2bBmwImpl>onChannelChange(//
						BatteryFeneconF2bBmwImpl::updateSoc)), //

		LINK_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_ONLY)//
				.<BatteryFeneconF2bBmwImpl>onChannelChange(//
						BatteryFeneconF2bBmwImpl::updateBatteryVoltage)), //

		LINK_VOLTAGE_HIGH_RES(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.VOLT)//
				.accessMode(AccessMode.READ_ONLY)), //

		BATTERY_CURRENT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.AMPERE)//
				.accessMode(AccessMode.READ_ONLY)), //

		// RQ_CHGCOND_HVSTO_MIN, Minimum state of charge (SoC)
		MIN_SOC(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PERCENT)//
				.accessMode(AccessMode.READ_ONLY)), //

		// RQ_CHGCOND_HVSTO_MAX, Maximum state of charge (SoC)
		MAX_SOC(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.PERCENT)//
				.accessMode(AccessMode.READ_ONLY)), //

		// ST_WARN_OTMP_HVSTO, Status of HV-Warn Concept
		HV_WARN_CONCEPT_STATUS(Doc.of(OpenemsType.BOOLEAN)//
				.accessMode(AccessMode.READ_ONLY)), //

		// BNLM_ACTIVE
		BATTERY_EMERGENCY_MODE(Doc.of(Level.FAULT)//
				.accessMode(AccessMode.READ_ONLY)), //

		// ST_ERR_LOKG_HVSTO
		INTERLOCK_LOOP_STATUS(Doc.of(Level.FAULT)//
				.accessMode(AccessMode.READ_ONLY)), //

		// ST_ERR_DCSW_HVSTO, Is one or both contactors welded or not
		CONTACTORS_DIAGNOSTIC_STATUS(Doc.of(ContactorDiagnosticStatus.values())//
				.accessMode(AccessMode.READ_ONLY)), //

		// ST_ERR_ISL_INTL_BN_HV, Insulation error:The insulation value for open
		// contactors is below the error value (200 kOhm)
		OPEN_CONTACTOR_INSULATION_ERROR(Doc.of(Level.WARNING)//
				.accessMode(AccessMode.READ_ONLY)), //

		// ST_ERR_ISL_EXTN_BN_HV, Insulation error: The insulation value for closed
		// contactors is below the error value (200 kOhm)
		CLOSED_CONTACTOR_INSULATION_ERROR(Doc.of(Level.WARNING)//
				.accessMode(AccessMode.READ_ONLY)), //
		// Time since the last change of modbus register T_SEC_COU_REL
		CAN_TIMEOUT_LIM_CHG_DCHG_HVSTO(Doc.of(OpenemsType.BOOLEAN)//
				.accessMode(AccessMode.READ_ONLY)), //
		CAN_TIMEOUT_STAT_HVSTO_2(Doc.of(OpenemsType.BOOLEAN)//
				.accessMode(AccessMode.READ_ONLY)), //
		CAN_TIMEOUT_ST_HVSTO_1(Doc.of(OpenemsType.BOOLEAN)//
				.accessMode(AccessMode.READ_ONLY)), //
		CAN_SIGNAL_INVALID_AVL_U_HVSTO(Doc.of(OpenemsType.BOOLEAN)//
				.accessMode(AccessMode.READ_ONLY)), //
		DEEP_DISCHARGE_PROTECTION_LIMIT_SOC(Doc.of(Level.FAULT) //
				.text("Deep discharge protection of \"limit SoC\" triggered!")), //
		DEEP_DISCHARGE_PROTECTION_VOLTAGE_CONTROL(Doc.of(Level.FAULT) //
				.text("Deep discharge protection of \"voltage control\" triggered!")), //
		GO_RUNNING_STATE_MACHINE(Doc.of(GoRunningSubState.values()) //
				.text("Current State of GoRunning State-Machine")), //
		GO_STOPPED_STATE_MACHINE(Doc.of(OpenemsType.INTEGER) //
				.text("Current State of GoStopped State-Machine")), //
		CHARGE_MAX_CURRENT_VOLT_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.AMPERE)), //
		DISCHARGE_MAX_CURRENT_VOLT_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.AMPERE)), //
		OPERATION_STATE(Doc.of(OperationState.values())//
				.persistencePriority(PersistencePriority.HIGH)), //
		ZERO_CURRENT_REQUEST_FAILED(Doc.of(OpenemsType.BOOLEAN)//
				.text("Opened contactors under load, because zero current request was ignored")//
				.persistencePriority(PersistencePriority.HIGH)), //
		HV_CONTACTORS_STUCK(Doc.of(OpenemsType.BOOLEAN)//
				.text("Hv Contactors no battery reaction for open/close request")//
				.persistencePriority(PersistencePriority.HIGH)), //
		HV_CONTACTORS_OPEN_IN_RUNNING(Doc.of(OpenemsType.BOOLEAN)//
				.text("Running: Hv Contactors opened while battery in Running State is.")//
				.persistencePriority(PersistencePriority.HIGH)), //
		STATE_MACHINE(Doc.of(State.values()) //
				.text("Current State of State-Machine")), //
		RUN_FAILED(Doc.of(Level.FAULT) //
				.text("Running the Logic failed")), //

		// BMW specific Balancing
		SET_BALANCING_TARGET_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)//
				.persistencePriority(PersistencePriority.HIGH)//
				.accessMode(AccessMode.READ_WRITE)), //
		SET_BALANCING_CONDITIONS_FULFILLED(Doc.of(OpenemsType.INTEGER)//
				.persistencePriority(PersistencePriority.HIGH)//
				.accessMode(AccessMode.READ_WRITE)), //
		SET_OCV_REACHED_AT_ALL_THE_BATTERIES(Doc.of(OpenemsType.INTEGER)//
				.persistencePriority(PersistencePriority.HIGH)//
				.accessMode(AccessMode.READ_WRITE)), //
		SET_BALANCING_RUNNING(Doc.of(OpenemsType.INTEGER)//
				.persistencePriority(PersistencePriority.HIGH)//
				.accessMode(AccessMode.READ_WRITE)), //
		BALANCING_MIN_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.MILLIVOLT)//
				.persistencePriority(PersistencePriority.HIGH)//
				.accessMode(AccessMode.READ_ONLY)), //
		OCV_REACHED(Doc.of(OpenemsType.BOOLEAN)//
				.persistencePriority(PersistencePriority.HIGH)//
				.accessMode(AccessMode.READ_ONLY)), //
		BALANCING_STILL_RUNNING(Doc.of(OpenemsType.BOOLEAN)//
				.persistencePriority(PersistencePriority.HIGH)//
				.accessMode(AccessMode.READ_ONLY)), //
		BALANCING_CONDITION(Doc.of(OpenemsType.BOOLEAN)//
				.persistencePriority(PersistencePriority.HIGH)//
				.accessMode(AccessMode.READ_ONLY)), //
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
	 * A helper method in determining when to begin heating the battery. The serial
	 * cluster will begin to heat up the batteries if the all battery temperatures
	 * is below 10 degrees and they are all started.
	 * 
	 * @param value true for start heating.
	 */
	public void setHeatingTarget(boolean value);

	/**
	 * Start the battery heating.
	 * 
	 * @throws OpenemsNamedException on error.
	 */
	public default void startHeating() {
		this.setHeatingTarget(true);
	}

	/**
	 * Stop the battery heating.
	 * 
	 * @throws OpenemsNamedException on error.
	 */
	public default void stopHeating() {
		this.setHeatingTarget(false);
	}

	/**
	 * Gets the heating target which set by {@link #setHeating()} method.
	 * 
	 * @return heating target.
	 */
	public boolean getHeatingTarget();

	/**
	 * Gets the main contactor target which set by {@link #setHvContactor(Boolean)}
	 * method.
	 * 
	 * @return main contactor target.
	 */
	public boolean isHvContactorUnlocked();

	/**
	 * Gets the target Start/Stop mode from configuration or StartStop-Channel.
	 *
	 * @return {@link StartStop}
	 */
	public StartStop getStartStopTarget();

	/**
	 * Gets the Channel for {@link ChannelId#UNLIMITED_SOC}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getUnlimitedSocChannel() {
		return this.channel(ChannelId.UNLIMITED_SOC);
	}

	/**
	 * Gets the UnlimitedSoc, see {@link ChannelId#UNLIMITED_SOC}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getUnlimitedSoc() {
		return this.getUnlimitedSocChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#LINK_VOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getLinkVoltageChannel() {
		return this.channel(ChannelId.LINK_VOLTAGE);
	}

	/**
	 * Gets the LinkVoltage, see {@link ChannelId#LINK_VOLTAGE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getLinkVoltage() {
		return this.getLinkVoltageChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#BATTERY_CURRENT}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getBatteryCurrentChannel() {
		return this.channel(ChannelId.BATTERY_CURRENT);
	}

	/**
	 * Gets the BatteryCurrent, see {@link ChannelId#BATTERY_CURRENT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBatteryCurrent() {
		return this.getBatteryCurrentChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#BATTERY_DISCHARGE_MAX_CURRENT}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getBatteryDischargeMaxCurrentChannel() {
		return this.channel(ChannelId.BATTERY_DISCHARGE_MAX_CURRENT);
	}

	/**
	 * Gets the BatteryDischargeMaxCurrent, see
	 * {@link ChannelId#BATTERY_DISCHARGE_MAX_CURRENT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBatteryDischargeMaxCurrent() {
		return this.getBatteryDischargeMaxCurrentChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#BATTERY_CHARGE_MAX_CURRENT}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getBatteryChargeMaxCurrentChannel() {
		return this.channel(ChannelId.BATTERY_CHARGE_MAX_CURRENT);
	}

	/**
	 * Gets the BatteryChargeMaxCurrent, see
	 * {@link ChannelId#BATTERY_CHARGE_MAX_CURRENT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBatteryChargeMaxCurrent() {
		return this.getBatteryChargeMaxCurrentChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#BATTERY_DISCHARGE_MIN_VOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getBatteryDischargeMinVoltageChannel() {
		return this.channel(ChannelId.BATTERY_DISCHARGE_MIN_VOLTAGE);
	}

	/**
	 * Gets the BatteryDischargeMinVoltage, see
	 * {@link ChannelId#BATTERY_DISCHARGE_MIN_VOLTAGE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBatteryDischargeMinVoltage() {
		return this.getBatteryDischargeMinVoltageChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#BATTERY_CHARGE_MAX_VOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getBatteryChargeMaxVoltageChannel() {
		return this.channel(ChannelId.BATTERY_CHARGE_MAX_VOLTAGE);
	}

	/**
	 * Gets the BatteryChargeMaxCurrent, see
	 * {@link ChannelId#BATTERY_CHARGE_MAX_VOLTAGE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBatteryChargeMaxVoltage() {
		return this.getBatteryChargeMaxVoltageChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#LINK_VOLTAGE_HIGH_RES}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getLinkVoltageHighResChannel() {
		return this.channel(ChannelId.LINK_VOLTAGE_HIGH_RES);
	}

	/**
	 * Gets the LinkVoltageHighRes, see {@link ChannelId#LINK_VOLTAGE_HIGH_RES}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getLinkVoltageHighRes() {
		return this.getLinkVoltageHighResChannel().value();
	}

	/**
	 * Sets the LinkVoltageHighRes, see {@link ChannelId#LINK_VOLTAGE_HIGH_RES}.
	 * 
	 * @param value to set.
	 */
	public default void _setLinkVoltageHighRes(Integer value) {
		this.getLinkVoltageHighResChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#HV_CONTACTOR_STATUS}.
	 *
	 * @return the Channel
	 */
	public default Channel<HvContactorStatus> getHvContactorStatusChannel() {
		return this.channel(ChannelId.HV_CONTACTOR_STATUS);
	}

	/**
	 * Gets the HvContactorsStatus, see {@link ChannelId#HV_CONTACTOR_STATUS}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<HvContactorStatus> getHvContactorStatus() {
		return this.getHvContactorStatusChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#CONTACTORS_DIAGNOSTIC_STATUS}.
	 *
	 * @return the Channel
	 */
	public default Channel<ContactorDiagnosticStatus> getContactorsDiagnosticStatusChannel() {
		return this.channel(ChannelId.CONTACTORS_DIAGNOSTIC_STATUS);
	}

	/**
	 * Gets the ContactorsDiagnosticStatus, see
	 * {@link ChannelId#CONTACTORS_DIAGNOSTIC_STATUS}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default ContactorDiagnosticStatus getContactorsDiagnosticStatus() {
		return this.getContactorsDiagnosticStatusChannel().value().asEnum();
	}

	/**
	 * Gets the Channel for {@link ChannelId#COOLING_REQUEST}.
	 *
	 * @return the Channel
	 */
	public default Channel<CoolingRequest> getCoolingRequestChannel() {
		return this.channel(ChannelId.COOLING_REQUEST);
	}

	/**
	 * Gets the CoolingRequest, see {@link ChannelId#COOLING_REQUEST}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default CoolingRequest getCoolingRequest() {
		return this.getCoolingRequestChannel().value().asEnum();
	}

	/**
	 * Gets the Channel for {@link ChannelId#INSULATION_MEASUREMENT}.
	 *
	 * @return the Channel
	 */
	public default WriteChannel<InsulationMeasurement> getInsulationMeasurementChannel() {
		return this.channel(ChannelId.INSULATION_MEASUREMENT);
	}

	/**
	 * Writes the value to the {@link ChannelId#INSULATION_MEASUREMENT} Register.
	 *
	 * @param value the next value
	 * @throws OpenemsNamedException on error
	 */
	public default void setInsulationMeasurement(InsulationMeasurement value) throws OpenemsNamedException {
		this.getInsulationMeasurementChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#HEATING_POWER}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getHeatingPowerChannel() {
		return this.channel(ChannelId.HEATING_POWER);
	}

	/**
	 * Gets the HeatingPower, see {@link ChannelId#HEATING_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getHeatingPower() {
		return this.getHeatingPowerChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#HEATING_REQUEST}.
	 *
	 * @return the Channel
	 */
	public default Channel<HeatingRequest> getHeatingRequestChannel() {
		return this.channel(ChannelId.HEATING_REQUEST);
	}

	/**
	 * Gets the HeatingPower, see {@link ChannelId#HEATING_REQUEST}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<HeatingRequest> getHeatingRequest() {
		return this.getHeatingRequestChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#BATTERY_POWER}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getBatteryPowerChannel() {
		return this.channel(ChannelId.BATTERY_POWER);
	}

	/**
	 * Gets the HeatingPower, see {@link ChannelId#BATTERY_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBatteryPower() {
		return this.getBatteryPowerChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#CAT1_PRECHARGE_SYSTEM_IS_LOCKED}.
	 *
	 * @return the Channel
	 */
	public default Channel<Boolean> getCat1PrechargeSystemIsLockedChannel() {
		return this.channel(ChannelId.CAT1_PRECHARGE_SYSTEM_IS_LOCKED);
	}

	/**
	 * Gets the getCat1PrechargeSystemIsLocked, see
	 * {@link ChannelId#CAT1_PRECHARGE_SYSTEM_IS_LOCKED}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getCat1PrechargeSystemIsLocked() {
		return this.getCat1PrechargeSystemIsLockedChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#CAT5_FAULT}.
	 *
	 * @return the Channel
	 */
	public default Channel<Boolean> getCat1FaultChannel() {
		return this.channel(ChannelId.CAT1_FAULT);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#CAT1_FAULT}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setCat1Fault(boolean value) {
		this.getCat1FaultChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#CAT5_BATTERY_POWER_WILL_BE_LIMITED}.
	 *
	 * @return the Channel
	 */
	public default Channel<Boolean> getCat5BatteryPowerWillBeLimitedChannel() {
		return this.channel(ChannelId.CAT5_BATTERY_POWER_WILL_BE_LIMITED);
	}

	/**
	 * Gets the getCat5BatteryPowerWillBeLimited, see
	 * {@link ChannelId#CAT5_BATTERY_POWER_WILL_BE_LIMITED}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getCat5BatteryPowerWillBeLimited() {
		return this.getCat5BatteryPowerWillBeLimitedChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#CAT5_FAULT}.
	 *
	 * @return the Channel
	 */
	public default Channel<Boolean> getCat5FaultChannel() {
		return this.channel(ChannelId.CAT5_FAULT);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#CAT5_FAULT}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setCat5Fault(boolean value) {
		this.getCat5FaultChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#CAT6_ZERO_CURRENT_REQUEST}.
	 *
	 * @return the Channel
	 */
	public default Channel<Boolean> getCat6ZeroCurrentRequestChannel() {
		return this.channel(ChannelId.CAT6_ZERO_CURRENT_REQUEST);
	}

	/**
	 * Gets the getCat6ZeroCurrentRequest, see
	 * {@link ChannelId#CAT6_ZERO_CURRENT_REQUEST}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getCat6ZeroCurrentRequest() {
		return this.getCat6ZeroCurrentRequestChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#CAT6_FAULT}.
	 *
	 * @return the Channel
	 */
	public default Channel<Boolean> getCat6FaultChannel() {
		return this.channel(ChannelId.CAT6_FAULT);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#CAT6_FAULT}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setCat6Fault(boolean value) {
		this.getCat6FaultChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#CAT7_EMERGENCY_CONTACTOR_OPEN}.
	 *
	 * @return the Channel
	 */
	public default Channel<Boolean> getCat7EmergencyContactorOpenChannel() {
		return this.channel(ChannelId.CAT7_EMERGENCY_CONTACTOR_OPEN);
	}

	/**
	 * Gets the getCat7EmergencyContactorOpen, see
	 * {@link ChannelId#CAT7_EMERGENCY_CONTACTOR_OPEN}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getCat7EmergencyContactorOpen() {
		return this.getCat7EmergencyContactorOpenChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#CAT7_FAULT}.
	 *
	 * @return the Channel
	 */
	public default Channel<Boolean> getCat7FaultChannel() {
		return this.channel(ChannelId.CAT7_FAULT);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#CAT7_FAULT}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setCat7Fault(boolean value) {
		this.getCat7FaultChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#STATE_MACHINE}.
	 *
	 * @return the Channel
	 */
	public default Channel<State> getStateMachineChannel() {
		return this.channel(ChannelId.STATE_MACHINE);
	}

	/**
	 * Gets the {@link StateChannel} for {@link ChannelId#STATE_MACHINE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<State> getStateMachine() {
		return this.getStateMachineChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#STATE_MACHINE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setStateMachine(State value) {
		this.getStateMachineChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#RUN_FAILED}.
	 *
	 * @return the Channel
	 */
	public default Channel<Boolean> getRunFailedChannel() {
		return this.channel(ChannelId.RUN_FAILED);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#RUN_FAILED}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setRunFailed(boolean value) {
		this.getRunFailedChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#ZERO_CURRENT_REQUEST_FAILED}.
	 *
	 * @return the Channel
	 */
	public default Channel<Boolean> getZeroCurrentRequestFailedChannel() {
		return this.channel(ChannelId.ZERO_CURRENT_REQUEST_FAILED);
	}

	/**
	 * Gets the {@link ChannelId#ZERO_CURRENT_REQUEST_FAILED}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getZeroCurrentRequestFailed() {
		return this.getZeroCurrentRequestFailedChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#ZERO_CURRENT_REQUEST_FAILED} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setZeroCurrentRequestFailed(Boolean value) {
		this.getZeroCurrentRequestFailedChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#HV_CONTACTORS_STUCK}.
	 *
	 * @return the Channel
	 */
	public default Channel<Boolean> getHvContactorsStuckFailedChannel() {
		return this.channel(ChannelId.HV_CONTACTORS_STUCK);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#HV_CONTACTORS_STUCK} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setHvContactorsStuckFailed(boolean value) {
		this.getHvContactorsStuckFailedChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#HV_CONTACTORS_OPEN_IN_RUNNING}.
	 *
	 * @return the Channel
	 */
	public default Channel<Boolean> getHvContactorsOpenInRunningChannel() {
		return this.channel(ChannelId.HV_CONTACTORS_OPEN_IN_RUNNING);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#HV_CONTACTORS_OPEN_IN_RUNNING} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setHvContactorsOpenInRunning(Boolean value) {
		this.getHvContactorsOpenInRunningChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#DISCHARGE_MAX_CURRENT_VOLT_LIMIT}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getDischargeMaxCurrentVoltLimitChannel() {
		return this.channel(ChannelId.DISCHARGE_MAX_CURRENT_VOLT_LIMIT);
	}

	/**
	 * Gets the {@link ChannelId#DISCHARGE_MAX_CURRENT_VOLT_LIMIT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDischargeMaxCurrentVoltLimit() {
		return this.getDischargeMaxCurrentVoltLimitChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#DISCHARGE_MAX_CURRENT_VOLT_LIMIT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDischargeMaxCurrentVoltLimit(Integer value) {
		this.getDischargeMaxCurrentVoltLimitChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#CHARGE_MAX_CURRENT_VOLT_LIMIT}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getChargeMaxCurrentVoltaLimitChannel() {
		return this.channel(ChannelId.CHARGE_MAX_CURRENT_VOLT_LIMIT);
	}

	/**
	 * Gets the {@link ChannelId#CHARGE_MAX_CURRENT_VOLT_LIMIT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getChargeMaxCurrentVoltaLimit() {
		return this.getChargeMaxCurrentVoltaLimitChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#CHARGE_MAX_CURRENT_VOLT_LIMIT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setChargeMaxCurrentVoltaLimitChannel(Integer value) {
		this.getChargeMaxCurrentVoltaLimitChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#COOLING_APPROVAL}.
	 *
	 * @return the Channel {@link Channel}
	 */
	public default WriteChannel<CoolingApproval> getCoolingApprovalChannel() {
		return this.channel(ChannelId.COOLING_APPROVAL);
	}

	/**
	 * Writes the value to the {@link ChannelId#COOLING_APPROVAL} Register.
	 *
	 * @param value the next value
	 * @throws OpenemsNamedException on error
	 */
	public default void setCoolingApproval(CoolingApproval value) throws OpenemsNamedException {
		this.getCoolingApprovalChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#REQUEST_CHARGING}.
	 *
	 * @return the Channel {@link Channel}
	 */
	public default WriteChannel<RequestCharging> getRequestChargingChannel() {
		return this.channel(ChannelId.REQUEST_CHARGING);
	}

	/**
	 * Writes the value to the {@link ChannelId#REQUEST_CHARGING} Register.
	 *
	 * @param value the next value
	 * @throws OpenemsNamedException on error
	 */
	public default void setRequestCharging(RequestCharging value) throws OpenemsNamedException {
		this.getRequestChargingChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#PREDICTED_CHARGING_POWER}.
	 *
	 * @return the Channel {@link Channel}
	 */
	public default WriteChannel<Long> getPredictedChargingPowerChannel() {
		return this.channel(ChannelId.PREDICTED_CHARGING_POWER);
	}

	/**
	 * Writes the value to the {@link ChannelId#PREDICTED_CHARGING_POWER} Register.
	 *
	 * @param value the next value
	 * @throws OpenemsNamedException on error
	 */
	public default void setPredictedChargingPower(Long value) throws OpenemsNamedException {
		this.getPredictedChargingPowerChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#ALLOCATES_BATTERY_HEATING_POWER}.
	 *
	 * @return the Channel {@link Channel}
	 */
	public default WriteChannel<HeatingRequest> getAllocatesBatteryHeatingPowerChannel() {
		return this.channel(ChannelId.ALLOCATES_BATTERY_HEATING_POWER);
	}

	/**
	 * Writes the value to the {@link ChannelId#ALLOCATES_BATTERY_HEATING_POWER}
	 * Register.
	 *
	 * @param value the next value
	 * @throws OpenemsNamedException on error
	 */
	public default void setAllocatesBatteryHeatingPower(HeatingRequest value) throws OpenemsNamedException {
		this.getAllocatesBatteryHeatingPowerChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#HEATING_RELEASED_POWER}.
	 *
	 * @return the Channel {@link Channel}
	 */
	public default WriteChannel<Integer> getHeatingReleasedPowerChannel() {
		return this.channel(ChannelId.HEATING_RELEASED_POWER);
	}

	/**
	 * Writes the value to the {@link ChannelId#HEATING_RELEASED_POWER} Register.
	 *
	 * @param value the next value
	 * @throws OpenemsNamedException on error
	 */
	public default void setHeatingReleasedPower(Integer value) throws OpenemsNamedException {
		this.getHeatingReleasedPowerChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_BALANCING_TARGET_VOLTAGE}.
	 *
	 * @return the Channel {@link Channel}
	 */
	public default WriteChannel<Integer> getBalancingTargetVoltageChannel() {
		return this.channel(ChannelId.SET_BALANCING_TARGET_VOLTAGE);
	}

	/**
	 * See {@link ChannelId#SET_BALANCING_TARGET_VOLTAGE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBalancingTargetVoltage() {
		return this.getBalancingTargetVoltageChannel().value();
	}

	/**
	 * See {@link ChannelId#SET_BALANCING_TARGET_VOLTAGE}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setBalancingTargetVoltage(Integer value) throws OpenemsNamedException {
		this.getBalancingTargetVoltageChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_BALANCING_CONDITIONS_FULFILLED}.
	 *
	 * @return the Channel {@link Channel}
	 */
	public default WriteChannel<Integer> getBalancingConditionsFullfilledChannel() {
		return this.channel(ChannelId.SET_BALANCING_CONDITIONS_FULFILLED);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_BALANCING_CONDITIONS_FULFILLED}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBalancingConditionsFullfilled() {
		return this.getBalancingConditionsFullfilledChannel().value();
	}

	/**
	 * Writes the value to the {@link ChannelId#SET_BALANCING_CONDITIONS_FULFILLED}
	 * Register.
	 *
	 * @param value the next value
	 * @throws OpenemsNamedException on error
	 */
	public default void setBalancingConditionsFullfilled(Integer value) throws OpenemsNamedException {
		this.getBalancingConditionsFullfilledChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_OCV_REACHED_AT_ALL_THE_BATTERIES}.
	 *
	 * @return the Channel {@link Channel}
	 */
	public default WriteChannel<Integer> getOcvReachedAtAllTheBatteriesChannel() {
		return this.channel(ChannelId.SET_OCV_REACHED_AT_ALL_THE_BATTERIES);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_OCV_REACHED_AT_ALL_THE_BATTERIES}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getOcvReachedAtAllTheBatteries() {
		return this.getOcvReachedAtAllTheBatteriesChannel().value();
	}

	/**
	 * Writes the value to the
	 * {@link ChannelId#SET_OCV_REACHED_AT_ALL_THE_BATTERIES} Register.
	 *
	 * @param value the next value
	 * @throws OpenemsNamedException on error
	 */
	public default void setOcvReachedAtAllTheBatteries(Integer value) throws OpenemsNamedException {
		this.getOcvReachedAtAllTheBatteriesChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_BALANCING_RUNNING}.
	 *
	 * @return the Channel {@link Channel}
	 */
	public default WriteChannel<Integer> getBalancingRunningChannel() {
		return this.channel(ChannelId.SET_BALANCING_RUNNING);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_BALANCING_RUNNING}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBalancingRunning() {
		return this.getBalancingRunningChannel().value();
	}

	/**
	 * Writes the value to the {@link ChannelId#SET_BALANCING_RUNNING} Register.
	 *
	 * @param value the next value
	 * @throws OpenemsNamedException on error
	 */
	public default void setBalancingRunning(Integer value) throws OpenemsNamedException {
		this.getBalancingRunningChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#BALANCING_MIN_CELL_VOLTAGE}.
	 *
	 * @return the Channel {@link Channel}
	 */
	public default Channel<Integer> getBalancingMinCellVoltageChannel() {
		return this.channel(ChannelId.BALANCING_MIN_CELL_VOLTAGE);
	}

	/**
	 * Gets the {@link ChannelId#BALANCING_MIN_CELL_VOLTAGE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBalancingMinCellVoltage() {
		return this.getBalancingMinCellVoltageChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#OCV_REACHED}.
	 *
	 * @return the Channel {@link Channel}
	 */
	public default Channel<Boolean> getOcvReachedChannel() {
		return this.channel(ChannelId.OCV_REACHED);
	}

	/**
	 * Gets the {@link ChannelId#OCV_REACHED}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getOcvReached() {
		return this.getOcvReachedChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#BALANCING_STILL_RUNNING}.
	 *
	 * @return the Channel {@link Channel}
	 */
	public default Channel<Boolean> getBalancingStillRunningChannel() {
		return this.channel(ChannelId.BALANCING_STILL_RUNNING);
	}

	/**
	 * Gets the {@link ChannelId#BALANCING_STILL_RUNNING}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getBalancingStillRunning() {
		return this.getBalancingStillRunningChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#BALANCING_CONDITION}.
	 *
	 * @return the Channel {@link Channel}
	 */
	public default Channel<Boolean> getBalancingConditionChannel() {
		return this.channel(ChannelId.BALANCING_CONDITION);
	}

	/**
	 * Gets the {@link ChannelId#BALANCING_CONDITION}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getBalancingCondition() {
		return this.getBalancingConditionChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#HV_CONTACTOR}.
	 *
	 * @return the Channel {@link Channel}
	 */
	public default BooleanWriteChannel getHvContactorChannel() {
		return this.channel(ChannelId.HV_CONTACTOR);
	}

	/**
	 * See {@link ChannelId#HV_CONTACTOR}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getHvContactor() {
		return this.getHvContactorChannel().value();
	}

	/**
	 * See {@link ChannelId#HV_CONTACTOR}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setHvContactor(boolean value) throws OpenemsNamedException {
		this.getHvContactorChannel().setNextWriteValue(value);
	}
}
