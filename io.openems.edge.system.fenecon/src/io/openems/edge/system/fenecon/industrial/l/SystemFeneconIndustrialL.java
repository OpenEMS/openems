package io.openems.edge.system.fenecon.industrial.l;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.system.fenecon.industrial.l.envicool.BmsModeControl;
import io.openems.edge.system.fenecon.industrial.l.envicool.RuntimeControlMode;

public interface SystemFeneconIndustrialL extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		/*
		 * Envicool AC.
		 */
		PUMP_RUNNING_STATE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		COMPRESSOR_1_RUNNING_STATE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		COMPRESSOR_2_RUNNING_STATE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		WATER_OUTLET_TEMP(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.DEGREE_CELSIUS)), //
		WATER_INLET_TEMP(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.DEGREE_CELSIUS)), //
		WATER_OUTLET_PRESSURE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.BAR)), //
		WATER_INLET_PRESSURE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.BAR)), //
		HEATER_RUNNING_STATE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //

		COMPRESSOR_1_SYSTEM_FAILURE(Doc.of(Level.FAULT) //
				.accessMode(AccessMode.READ_ONLY)), //
		COMPRESSOR_1_GENERAL_ALARM(Doc.of(Level.FAULT) //
				.accessMode(AccessMode.READ_ONLY)), //
		SYSTEM_WATER_SHORTAGE_ALARM(Doc.of(Level.FAULT) //
				.accessMode(AccessMode.READ_ONLY)), //
		COMPRESSOR_1_CONDENSING_PRESSURE_TOO_HIGH_ALARM(Doc.of(Level.FAULT) //
				.accessMode(AccessMode.READ_ONLY)), //
		COMPRESSOR_2_CONDENSING_PRESSURE_TOO_HIGH_ALARM(Doc.of(Level.FAULT) //
				.accessMode(AccessMode.READ_ONLY)), //
		WATER_OUTLET_PRESSURE_HIGH_ALARM(Doc.of(Level.FAULT) //
				.accessMode(AccessMode.READ_ONLY)), //
		WATER_INLET_PRESSURE_WARNING(Doc.of(Level.FAULT) //
				.accessMode(AccessMode.READ_ONLY)), //
		POWER_FAILURE_WARNING(Doc.of(Level.FAULT) //
				.accessMode(AccessMode.READ_ONLY)), //
		COMPRESSOR_2_SYSTEM_FAILURE(Doc.of(Level.FAULT) //
				.accessMode(AccessMode.READ_ONLY)), //
		COMPRESSOR_2_GENERAL_ALARM(Doc.of(Level.FAULT) //
				.accessMode(AccessMode.READ_ONLY)), //

		COOLING_POINT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.unit(Unit.DEGREE_CELSIUS)), //
		MONITOR_SWITCH(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		PUMP_MANUAL_DEMAND_ENABLE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		RUNTIME_CONTROL_MODE(Doc.of(RuntimeControlMode.values()) //
				.accessMode(AccessMode.READ_WRITE)),
		ELECTRIC_HEATING_WORKING(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		HEATING_POINT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.unit(Unit.DEGREE_CELSIUS)),
		WATER_PRESSURE_OUTLET_TOO_HIGH(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.unit(Unit.BAR)),
		WATER_PRESSURE_INLET_TOO_HIGH(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.unit(Unit.BAR)),
		PUMP_ROTATING_SPEED(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.unit(Unit.NONE)), //
		BMS_MODE_CONTROL(Doc.of(BmsModeControl.values()) //
				.accessMode(AccessMode.READ_WRITE)), //
		MONITOR_AND_ISSUE_MAX_TEMP(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.unit(Unit.DEGREE_CELSIUS)), //
		MONITOR_AND_ISSUE_MIN_TEMP(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.unit(Unit.DEGREE_CELSIUS)), //
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
	 * Gets the Channel for {@link ChannelId#WATER_INLET_TEMP}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getWaterInletTempChannel() {
		return this.channel(ChannelId.WATER_INLET_TEMP);
	}

	/**
	 * Gets the Water Inlet Temp value [Degree-celcius].
	 * 
	 * @return the value of the channel
	 */
	public default Value<Integer> getWaterInletTemp() {
		return this.getWaterInletTempChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#COOLING_POINT}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getCoolingPointChannel() {
		return this.channel(ChannelId.COOLING_POINT);
	}

	/**
	 * Gets the Channel for {@link ChannelId#HEATING_POINT}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getHeatingPointChannel() {
		return this.channel(ChannelId.HEATING_POINT);
	}

	/**
	 * Gets the Channel for {@link ChannelId#RUNTIME_CONTROL_MODE}.
	 * 
	 * @return the Channel
	 */
	public default EnumWriteChannel getRunTimeControlModeChannel() {
		return this.channel(ChannelId.RUNTIME_CONTROL_MODE);
	}

	/**
	 * Gets the Channel for {@link ChannelId#BMS_MODE_CONTROL}.
	 * 
	 * @return the Channel
	 */
	public default EnumWriteChannel getBmsModeControlModeChannel() {
		return this.channel(ChannelId.BMS_MODE_CONTROL);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MONITOR_AND_ISSUE_MAX_TEMP}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getMonitorAndIssueMaxTempChannel() {
		return this.channel(ChannelId.MONITOR_AND_ISSUE_MAX_TEMP);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MONITOR_AND_ISSUE_MIN_TEMP}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getMonitorAndIssueMinTempChannel() {
		return this.channel(ChannelId.MONITOR_AND_ISSUE_MIN_TEMP);
	}

	/**
	 * Gets the Channel for {@link ChannelId#PUMP_RUNNING_STATE}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getPumpRunningStateChannel() {
		return this.channel(ChannelId.PUMP_RUNNING_STATE);
	}

	/**
	 * Gets the PUMP_RUNNING_STATE, see {@link ChannelId#PUMP_RUNNING_STATE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default boolean getPumpRunningState() {
		return this.getPumpRunningStateChannel().value().asOptional().get();
	}

	/**
	 * Gets the Channel for {@link ChannelId#HEATER_RUNNING_STATE}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getHeaterRunningStateChannel() {
		return this.channel(ChannelId.HEATER_RUNNING_STATE);
	}

	/**
	 * Gets the CommandActivateBalancing, see
	 * {@link ChannelId#HEATER_RUNNING_STATE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default boolean getHeaterRunningState() {
		return this.getHeaterRunningStateChannel().value().asOptional().get();
	}

	/**
	 * Gets the Channel for {@link ChannelId#COMPRESSOR_1_RUNNING_STATE}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getCompressor1RunningStateChannel() {
		return this.channel(ChannelId.COMPRESSOR_1_RUNNING_STATE);
	}

	/**
	 * Gets the CommandActivateBalancing, see
	 * {@link ChannelId#COMPRESSOR_1_RUNNING_STATE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default boolean getCompressor1RunningState() {
		return this.getCompressor1RunningStateChannel().value().asOptional().get();
	}

	/**
	 * Gets the Channel for {@link ChannelId#COMPRESSOR_2_RUNNING_STATE}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getCompressor2RunningStateChannel() {
		return this.channel(ChannelId.COMPRESSOR_2_RUNNING_STATE);
	}

	/**
	 * Gets the CommandActivateBalancing, see
	 * {@link ChannelId#COMPRESSOR_2_RUNNING_STATE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default boolean getCompressor2RunningState() {
		return this.getCompressor2RunningStateChannel().value().asOptional().get();
	}
}