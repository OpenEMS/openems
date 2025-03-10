package io.openems.edge.ess.sma.stpxx3se.batteryinverter;

import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.batteryinverter.api.HybridManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.ess.sma.enums.SetControlMode;

public interface BatteryInverterSmaStpSe extends HybridManagedSymmetricBatteryInverter, ManagedSymmetricBatteryInverter,
		SymmetricBatteryInverter, StartStoppable, ModbusComponent, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		INITIALIZING(Doc.of(Level.WARNING) //
				.text("Initializing Sunspec Protocol")), //
		WRONG_BATTERY(Doc.of(Level.FAULT) //
				.text("Failed to run inverter. Battery is not SMA battery.")), //
		SMART_MODE_NOT_WORKING_WITH_PID_FILTER(Doc.of(Level.WARNING) //
				.text("SMART mode does not work correctly with active PID filter")), //
		DEBUG_CONTROL_MODE(Doc.of(SetControlMode.values()) //
				.persistencePriority(PersistencePriority.HIGH)), //
		CONFIGURED_CONTROL_MODE(Doc.of(ControlMode.values()) //
				.persistencePriority(PersistencePriority.HIGH)), //
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
	 * Gets the Channel for {@link ChannelId#INITIALIZING}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getInitializingChannel() {
		return this.channel(ChannelId.INITIALIZING);
	}
	
	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#INITIALIZING} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setInitializing(boolean value) {
		this.getInitializingChannel().setNextValue(value);
	}
	
	/**
	 * Gets the Channel for {@link ChannelId#WRONG_BATTERY}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getWrongBatteryChannel() {
		return this.channel(ChannelId.WRONG_BATTERY);
	}
	
	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#WRONG_BATTERY}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setWrongBattery(boolean value) {
		this.getWrongBatteryChannel().setNextValue(value);
	}
	
	/**
	 * Gets the Channel for {@link ChannelId#DEBUG_CONTROL_MODE}.
	 *
	 * @return the Channel
	 */
	public default Channel<SetControlMode> getDebugControlModeChannel() {
		return this.channel(ChannelId.DEBUG_CONTROL_MODE);
	}
	
	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#DEBUG_CONTROL_MODE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDebugControlMode(SetControlMode value) {
		this.getDebugControlModeChannel().setNextValue(value);
	}
	
	/**
	 * Gets the Channel for {@link ChannelId#CONFIGURED_CONTROL_MODE}.
	 *
	 * @return the Channel
	 */
	public default Channel<ControlMode> getConfiguredControlModeChannel() {
		return this.channel(ChannelId.CONFIGURED_CONTROL_MODE);
	}
	
	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#CONFIGURED_CONTROL_MODE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setConfiguredControlMode(ControlMode value) {
		this.getConfiguredControlModeChannel().setNextValue(value);
	}
 
	/**
	 * Gets the SunSpec Channel S160Module1DCW.
	 * 
	 * @return the Channel
	 * @throws OpenemsException if the Channel is not present
	 */
	public Channel<Float> getModule1DcwChannel() throws OpenemsException;

	/**
	 * Gets the SunSpec Channel S160Module1DCA.
	 * 
	 * @return the Channel
	 * @throws OpenemsException if the Channel is not present
	 */
	public Channel<Float> getModule1DcaChannel() throws OpenemsException;

	/**
	 * Gets the SunSpec Channel S160Module1DCV.
	 * 
	 * @return the Channel
	 * @throws OpenemsException if the Channel is not present
	 */
	public Channel<Float> getModule1DcvChannel() throws OpenemsException;

	/**
	 * Gets the SunSpec Channel S160Module2DCW.
	 * 
	 * @return the Channel
	 * @throws OpenemsException if the Channel is not present
	 */
	public Channel<Float> getModule2DcwChannel() throws OpenemsException;

	/**
	 * Gets the SunSpec Channel S160Module2DCA.
	 * 
	 * @return the Channel
	 * @throws OpenemsException if the Channel is not present
	 */
	public Channel<Float> getModule2DcaChannel() throws OpenemsException;

	/**
	 * Gets the SunSpec Channel S160Module2DCV.
	 * 
	 * @return the Channel
	 * @throws OpenemsException if the Channel is not present
	 */
	public Channel<Float> getModule2DcvChannel() throws OpenemsException;

	/**
	 * Asks if the SunSpec initialization is completed.
	 * 
	 * @return true, if the SunSpec initialization is completed
	 */
	public boolean isInitialized();
}
