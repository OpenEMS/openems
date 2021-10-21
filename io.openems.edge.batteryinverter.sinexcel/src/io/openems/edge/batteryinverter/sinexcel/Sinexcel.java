package io.openems.edge.batteryinverter.sinexcel;

import java.util.Optional;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.OffGridBatteryInverter;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.batteryinverter.sinexcel.enums.SinexcelState;
import io.openems.edge.batteryinverter.sinexcel.statemachine.StateMachine.State;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.sum.GridMode;

public interface Sinexcel extends OffGridBatteryInverter, ManagedSymmetricBatteryInverter, SymmetricBatteryInverter,
		OpenemsComponent, StartStoppable, ModbusSlave {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		STATE_MACHINE(Doc.of(State.values()) //
				.text("Current State of State-Machine")), //
		RUN_FAILED(Doc.of(Level.FAULT) //
				.text("Running the Logic failed")), //

		CLEAR_FAILURE_CMD(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		SET_INTERN_DC_RELAY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.unit(Unit.NONE)),

		SET_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		SET_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //

		DEBUG_DISCHARGE_MAX_A(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE)), //

		DEBUG_CHARGE_MAX_A(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE)), //

		CHARGE_MAX_A(new IntegerDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.unit(Unit.AMPERE) //
				.onInit(new IntegerWriteChannel.MirrorToDebugChannel(Sinexcel.ChannelId.DEBUG_CHARGE_MAX_A))), //

		DISCHARGE_MAX_A(new IntegerDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.unit(Unit.AMPERE) //
				.onInit(new IntegerWriteChannel.MirrorToDebugChannel(Sinexcel.ChannelId.DEBUG_DISCHARGE_MAX_A))), //

		TOPPING_CHARGE_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.unit(Unit.VOLT)),
		FLOAT_CHARGE_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.unit(Unit.VOLT)),

		DEBUG_DIS_MIN_V(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)), //
		DEBUG_CHA_MAX_V(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)), //
		DISCHARGE_MIN_V(new IntegerDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.unit(Unit.VOLT) //
				.onInit(new IntegerWriteChannel.MirrorToDebugChannel(Sinexcel.ChannelId.DEBUG_DIS_MIN_V))), //
		CHARGE_MAX_V(new IntegerDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.unit(Unit.VOLT) //
				.onInit(new IntegerWriteChannel.MirrorToDebugChannel(Sinexcel.ChannelId.DEBUG_CHA_MAX_V))), //

		SET_ON_GRID_MODE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		SET_OFF_GRID_MODE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //

		INVOUTVOLT_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)), //
		INVOUTVOLT_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)),
		INVOUTVOLT_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)), //
		INVOUTCURRENT_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE)), //
		INVOUTCURRENT_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE)), //
		INVOUTCURRENT_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE)), //

		TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)), //

		DC_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //

		ANALOG_DC_CHARGE_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOVOLT_AMPERE)),
		ANALOG_DC_DISCHARGE_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOVOLT_AMPERE)),

		FREQUENCY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.HERTZ)), //

		DC_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE)), //
		DC_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)), //

		SINEXCEL_STATE(Doc.of(SinexcelState.values())), //

		SERIAL(Doc.of(OpenemsType.STRING) //
				.unit(Unit.NONE)), //
		MODEL(Doc.of(OpenemsType.STRING) //
				.unit(Unit.NONE)), //

		VERSION(Doc.of(OpenemsType.STRING) //
				.unit(Unit.NONE)), //

		ANTI_ISLANDING(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.unit(Unit.ON_OFF)),

		POWER_CHANGE_MODE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)),

		GRID_EXISTENCE_DETECTION_ON(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.unit(Unit.ON_OFF)),

		EMS_TIMEOUT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.unit(Unit.NONE)),
		BMS_TIMEOUT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.unit(Unit.NONE)),

		SET_START_COMMAND(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //

		SET_STOP_COMMAND(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //

		// EVENT Bitfield 32
		STATE_0(Doc.of(Level.FAULT) //
				.text("Ground fault")), //
		STATE_1(Doc.of(Level.WARNING) //
				.text("DC over Voltage")), //
		STATE_2(Doc.of(OpenemsType.BOOLEAN) //
				.text("AC disconnect open")), //
		STATE_3(Doc.of(Level.WARNING) //
				.text("DC disconnect open")), //
		STATE_4(Doc.of(Level.WARNING) //
				.text("Grid shutdown")), //
		STATE_5(Doc.of(Level.WARNING) //
				.text("Cabinet open")), //
		// Automatic Standby-Mode is activated after giving a active-power setpoint of
		// zero for a while.
		AUTOMATIC_STANDBY_MODE(Doc.of(Level.INFO) //
				.text("Automatic Standby-Mode")), //
		STATE_7(Doc.of(Level.WARNING) //
				.text("Over temperature")), //
		STATE_8(Doc.of(Level.WARNING) //
				.text("AC Frequency above limit")), //
		STATE_9(Doc.of(Level.WARNING) //
				.text("AC Frequnecy under limit")), //
		STATE_10(Doc.of(Level.WARNING) //
				.text("AC Voltage above limit")), //
		STATE_11(Doc.of(Level.WARNING) //
				.text("AC Voltage under limit")), //
		STATE_12(Doc.of(Level.WARNING) //
				.text("Blown String fuse on input")), //
		STATE_13(Doc.of(Level.WARNING) //
				.text("Under temperature")), //
		STATE_14(Doc.of(Level.WARNING) //
				.text("Generic Memory or Communication error (internal)")), //
		STATE_15(Doc.of(Level.FAULT) //
				.text("Hardware test failure")), //

		// FAULT LIST
		STATE_16(Doc.of(Level.FAULT) //
				.text("Fault Status")), //
		STATE_17(Doc.of(Level.WARNING) //
				.text("Alert Status")), //
		STATE_19(Doc.of(OpenemsType.BOOLEAN) //
				.text("On Grid") //
				.onInit(c -> { //
					BooleanReadChannel channel = (BooleanReadChannel) c;
					Sinexcel self = (Sinexcel) channel.getComponent();
					channel.onChange((oldValue, newValue) -> {
						Optional<Boolean> value = newValue.asOptional();
						if (!value.isPresent()) {
							self._setGridMode(GridMode.UNDEFINED);
						} else {
							if (value.get()) {
								self._setGridMode(GridMode.ON_GRID);
							} else {
								self._setGridMode(GridMode.OFF_GRID);
							}
						}
					});
				})),

		STATE_20(Doc.of(Level.INFO) //
				.text("Off Grid")), //
		STATE_21(Doc.of(Level.WARNING) //
				.text("AC OVP")), //
		STATE_22(Doc.of(Level.WARNING) //
				.text("AC UVP")), //
		STATE_23(Doc.of(Level.WARNING) //
				.text("AC OFP")), //
		STATE_24(Doc.of(Level.WARNING) //
				.text("AC UFP")), //
		STATE_25(Doc.of(Level.WARNING) //
				.text("Grid Voltage Unbalance")), //
		STATE_26(Doc.of(Level.WARNING) //
				.text("Grid Phase reserve")), //
		STATE_27(Doc.of(Level.INFO) //
				.text("Islanding")), //
		STATE_28(Doc.of(Level.WARNING) //
				.text("On/ Off Grid Switching Error")), //
		STATE_29(Doc.of(Level.WARNING) //
				.text("Output Grounding Error")), //
		STATE_30(Doc.of(Level.WARNING) //
				.text("Output Current Abnormal")), //
		STATE_31(Doc.of(Level.WARNING) //
				.text("Grid Phase Lock Fails")), //
		STATE_32(Doc.of(Level.WARNING) //
				.text("Internal Air Over-Temp")), //
		STATE_33(Doc.of(Level.WARNING) //
				.text("Zeitueberschreitung der Netzverbindung")), //
		STATE_34(Doc.of(Level.INFO) //
				.text("EPO")), //
		STATE_35(Doc.of(Level.FAULT) //
				.text("HMI Parameters Fault")), //
		STATE_36(Doc.of(Level.WARNING) //
				.text("DSP Version Error")), //
		STATE_37(Doc.of(Level.WARNING) //
				.text("CPLD Version Error")), //
		STATE_38(Doc.of(Level.WARNING) //
				.text("Hardware Version Error")), //
		STATE_39(Doc.of(Level.WARNING) //
				.text("Communication Error")), //
		STATE_40(Doc.of(Level.WARNING) //
				.text("AUX Power Error")), //
		STATE_41(Doc.of(Level.FAULT) //
				.text("Fan Failure")), //
		STATE_42(Doc.of(Level.WARNING) //
				.text("BUS Over Voltage")), //
		STATE_43(Doc.of(Level.WARNING) //
				.text("BUS Low Voltage")), //
		STATE_44(Doc.of(Level.WARNING) //
				.text("BUS Voltage Unbalanced")), //
		STATE_45(Doc.of(Level.WARNING) //
				.text("AC Soft Start Failure")), //
		STATE_46(Doc.of(Level.WARNING) //
				.text("Reserved")), //
		STATE_47(Doc.of(Level.WARNING) //
				.text("Output Voltage Abnormal")), //
		STATE_48(Doc.of(Level.WARNING) //
				.text("Output Current Unbalanced")), //
		STATE_49(Doc.of(Level.WARNING) //
				.text("Over Temperature of Heat Sink")), //
		STATE_50(Doc.of(Level.WARNING) //
				.text("Output Overload")), //
		STATE_51(Doc.of(Level.WARNING) //
				.text("Reserved")), //
		STATE_52(Doc.of(Level.WARNING) //
				.text("AC Breaker Short-Circuit")), //
		STATE_53(Doc.of(Level.WARNING) //
				.text("Inverter Start Failure")), //
		STATE_54(Doc.of(Level.WARNING) //
				.text("AC Breaker is open")), //
		STATE_55(Doc.of(Level.WARNING) //
				.text("EE Reading Error 1")), //
		STATE_56(Doc.of(Level.WARNING) //
				.text("EE Reading Error 2")), //
		STATE_57(Doc.of(Level.FAULT) //
				.text("SPD Failure  ")), //
		STATE_58(Doc.of(Level.WARNING) //
				.text("Inverter over load")), //
		STATE_59(Doc.of(OpenemsType.BOOLEAN) //
				.text("DC Charging")), //
		STATE_60(Doc.of(OpenemsType.BOOLEAN) //
				.text("DC Discharging")), //
		STATE_61(Doc.of(Level.INFO) //
				.text("Battery fully charged")), //
		STATE_62(Doc.of(Level.INFO) //
				.text("Battery empty")), //
		STATE_63(Doc.of(Level.FAULT) //
				.text("Fault Status")), //
		STATE_64(Doc.of(Level.WARNING) //
				.text("Alert Status")), //
		STATE_65(Doc.of(Level.WARNING) //
				.text("DC input OVP")), //
		STATE_66(Doc.of(Level.WARNING) //
				.text("DC input UVP")), //
		STATE_67(Doc.of(Level.WARNING) //
				.text("DC Groundig Error")), //
		STATE_68(Doc.of(Level.WARNING) //
				.text("BMS alerts")), //
		STATE_69(Doc.of(Level.FAULT) //
				.text("DC Soft-Start failure")), //
		STATE_70(Doc.of(Level.WARNING) //
				.text("DC relay short-circuit")), //
		STATE_71(Doc.of(Level.WARNING) //
				.text("DC relay short open")), //
		STATE_72(Doc.of(Level.WARNING) //
				.text("Battery power over load")), //
		STATE_73(Doc.of(Level.FAULT) //
				.text("BUS start fails")), //
		STATE_74(Doc.of(Level.WARNING) //
				.text("DC OCP"));

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
	 * Gets the Channel for {@link ChannelId#SET_ON_GRID_MODE}.
	 *
	 * @return the Channel
	 */
	public default BooleanWriteChannel getSetOnGridModeChannel() {
		return this.channel(ChannelId.SET_ON_GRID_MODE);
	}

	/**
	 * Gets the Set-On-Grid-Mode. See {@link ChannelId#SET_ON_GRID_MODE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getSetOnGridMode() {
		return this.getSetOnGridModeChannel().value();
	}

	/**
	 * Sets a the On-Grid-Mode. See {@link ChannelId#SET_ON_GRID_MODE}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setOnGridMode(Boolean value) throws OpenemsNamedException {
		this.getSetOnGridModeChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_OFF_GRID_MODE}.
	 *
	 * @return the Channel
	 */
	public default BooleanWriteChannel getSetOffGridModeChannel() {
		return this.channel(ChannelId.SET_OFF_GRID_MODE);
	}

	/**
	 * Gets the Set-Off-Grid-Mode. See {@link ChannelId#SET_OFF_GRID_MODE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getSetOffGridMode() {
		return this.getSetOffGridModeChannel().value();
	}

	/**
	 * Sets a the Off-Grid-Mode. See {@link ChannelId#SET_OFF_GRID_MODE}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setOffGridMode(Boolean value) throws OpenemsNamedException {
		this.getSetOffGridModeChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_START_COMMAND}.
	 *
	 * @return the Channel
	 */
	public default BooleanWriteChannel getSetStartCommandChannel() {
		return this.channel(ChannelId.SET_START_COMMAND);
	}

	/**
	 * Sends a START command to the inverter. See
	 * {@link ChannelId#SET_START_COMMAND}.
	 * 
	 * @throws OpenemsNamedException on error
	 */
	public default void setStartCommand() throws OpenemsNamedException {
		this.getSetStartCommandChannel().setNextWriteValue(true); // true = START
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_STOP_COMMAND}.
	 *
	 * @return the Channel
	 */
	public default BooleanWriteChannel getSetStopCommandChannel() {
		return this.channel(ChannelId.SET_STOP_COMMAND);
	}

	/**
	 * Sends a STOP command to the inverter. See {@link ChannelId#SET_STOP_COMMAND}.
	 * 
	 * @throws OpenemsNamedException on error
	 */
	public default void setStopCommand() throws OpenemsNamedException {
		this.getSetStopCommandChannel().setNextWriteValue(true); // true = STOP
	}

	/**
	 * Gets the Channel for {@link ChannelId#EMS_TIMEOUT}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getEmsTimeoutChannel() {
		return this.channel(ChannelId.EMS_TIMEOUT);
	}

	/**
	 * Gets the EMS Timeout. See {@link ChannelId#EMS_TIMEOUT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getEmsTimeout() {
		return this.getEmsTimeoutChannel().value();
	}

	/**
	 * Sets a the EMS Timeout. See {@link ChannelId#EMS_TIMEOUT}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setEmsTimeout(Integer value) throws OpenemsNamedException {
		this.getEmsTimeoutChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#BMS_TIMEOUT}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getBmsTimeoutChannel() {
		return this.channel(ChannelId.BMS_TIMEOUT);
	}

	/**
	 * Gets the EMS Timeout. See {@link ChannelId#BMS_TIMEOUT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBmsTimeout() {
		return this.getBmsTimeoutChannel().value();
	}

	/**
	 * Sets a the EMS Timeout. See {@link ChannelId#BMS_TIMEOUT}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setBmsTimeout(Integer value) throws OpenemsNamedException {
		this.getBmsTimeoutChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#GRID_EXISTENCE_DETECTION_ON}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getGridExistenceDetectionOnChannel() {
		return this.channel(ChannelId.GRID_EXISTENCE_DETECTION_ON);
	}

	/**
	 * See {@link ChannelId#GRID_EXISTENCE_DETECTION_ON}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getGridExistenceDetectionOn() {
		return this.getGridExistenceDetectionOnChannel().value();
	}

	/**
	 * See {@link ChannelId#GRID_EXISTENCE_DETECTION_ON}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setGridExistenceDetectionOn(Integer value) throws OpenemsNamedException {
		this.getGridExistenceDetectionOnChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#POWER_CHANGE_MODE}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getPowerChangeModeChannel() {
		return this.channel(ChannelId.POWER_CHANGE_MODE);
	}

	/**
	 * See {@link ChannelId#POWER_CHANGE_MODE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getPowerChangeMode() {
		return this.getPowerChangeModeChannel().value();
	}

	/**
	 * See {@link ChannelId#POWER_CHANGE_MODE}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setPowerChangeMode(Integer value) throws OpenemsNamedException {
		this.getPowerChangeModeChannel().setNextWriteValue(value);
	}
}
