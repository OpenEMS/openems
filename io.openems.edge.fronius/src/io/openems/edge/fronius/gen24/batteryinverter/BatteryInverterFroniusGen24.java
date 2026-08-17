package io.openems.edge.fronius.gen24.batteryinverter;

import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.batteryinverter.api.HybridManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.fronius.enums.SetControlMode;
import io.openems.edge.fronius.gen24.dccharger.FroniusGen24DcCharger;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;

public interface BatteryInverterFroniusGen24
		extends HybridManagedSymmetricBatteryInverter, ManagedSymmetricBatteryInverter, SymmetricBatteryInverter,
		StartStoppable, ModbusComponent, ManagedSymmetricPvInverter, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		INITIALIZING(Doc.of(Level.WARNING)//
				.text("Initializing Sunspec Protocol")), //
		WRONG_BATTERY(Doc.of(Level.FAULT)//
				.text("Failed to run inverter. Battery is not a Fronius Gen24 battery component.")), //
		DEBUG_CONTROL_MODE(Doc.of(SetControlMode.values())//
				.persistencePriority(PersistencePriority.HIGH)), //
		CONFIGURED_CONTROL_MODE(Doc.of(ControlMode.values())//
				.persistencePriority(PersistencePriority.HIGH)), //

		DEBUG_W_MAX_LIM_PCT(Doc.of(OpenemsType.INTEGER)//
				.text("Last written WMaxLimPct value (SunSpec S123, scale 0-100)")), //

		DEBUG_W_MAX_LIM_ENA(Doc.of(OpenemsType.INTEGER)//
				.text("WMaxLim_Ena: 1=active, 0=disabled")), //

		/**
		 * Inverter Operating State.
		 *
		 * <p>
		 * SunSpec Model S103 {@code St} point - e.g.
		 * Off/Sleeping/Starting/MPPT/Throttled/Shutting Down/Fault/Standby.
		 */
		OPERATING_STATE(Doc.of(DefaultSunSpecModel.S103_St.values())//
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
	 * Internal method to set the 'nextValue' on {@link ChannelId#INITIALIZING}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setInitializing(boolean value) {
		this.getInitializingChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#OPERATING_STATE}.
	 *
	 * @return the Channel
	 */
	public default Channel<DefaultSunSpecModel.S103_St> getOperatingStateChannel() {
		return this.channel(ChannelId.OPERATING_STATE);
	}

	/**
	 * Gets the Inverter Operating State. See {@link ChannelId#OPERATING_STATE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<DefaultSunSpecModel.S103_St> getOperatingState() {
		return this.getOperatingStateChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#OPERATING_STATE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setOperatingState(DefaultSunSpecModel.S103_St value) {
		this.getOperatingStateChannel().setNextValue(value);
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
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#DEBUG_CONTROL_MODE} Channel.
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
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#CONFIGURED_CONTROL_MODE} Channel.
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
	 * Gets the SunSpec Channel S160Module2DCW.
	 * 
	 * @return the Channel
	 * @throws OpenemsException if the Channel is not present
	 */
	public Channel<Float> getModule2DcwChannel() throws OpenemsException;

	/**
	 * Returns true if the SunSpec initialization is completed.
	 *
	 * @return true if initialized
	 */
	public boolean isInitialized();

	/**
	 * Registers a {@link FroniusGen24DcCharger} with this BatteryInverter. Called
	 * by OSGi via a dynamic multiple {@literal @Reference} when a matching Charger
	 * component activates - the Charger itself holds no reference back to the
	 * BatteryInverter (matches the pattern used by GoodWe and FENECON
	 * Commercial40).
	 *
	 * @param charger the Charger
	 */
	public void addCharger(FroniusGen24DcCharger charger);

	/**
	 * Unregisters a {@link FroniusGen24DcCharger} from this BatteryInverter.
	 *
	 * @param charger the Charger
	 */
	public void removeCharger(FroniusGen24DcCharger charger);

	// -------------------------------------------------------------------------
	// Conflict resolution: ManagedSymmetricPvInverter vs SymmetricBatteryInverter
	// Re-declaration as abstract forces the Impl to provide a unique implementation
	// -------------------------------------------------------------------------

	@Override
	IntegerReadChannel getActivePowerChannel();

	@Override
	IntegerReadChannel getReactivePowerChannel();

	@Override
	IntegerReadChannel getMaxApparentPowerChannel();

	@Override
	boolean isManaged();

	@Override
	void _setActivePower(Integer value);

	@Override
	void _setActivePower(int value);

	@Override
	void _setReactivePower(Integer value);

	@Override
	void _setReactivePower(int value);

	@Override
	void _setMaxApparentPower(Integer value);

	@Override
	void _setMaxApparentPower(int value);

	@Override
	Value<Integer> getActivePower();

	@Override
	Value<Integer> getReactivePower();

	@Override
	Value<Integer> getMaxApparentPower();

	/**
	 * Sets the debug value for WMaxLim percentage.
	 *
	 * @param value the value to set
	 */
	public default void _setDebugWMaxLimPct(int value) {
		this.channel(ChannelId.DEBUG_W_MAX_LIM_PCT).setNextValue(value);
	}

	/**
	 * Sets the debug value for WMaxLim enable flag.
	 *
	 * @param value the value to set
	 */
	public default void _setDebugWMaxLimEna(int value) {
		this.channel(ChannelId.DEBUG_W_MAX_LIM_ENA).setNextValue(value);
	}
}
