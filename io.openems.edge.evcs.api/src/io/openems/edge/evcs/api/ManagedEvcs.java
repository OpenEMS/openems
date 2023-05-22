package io.openems.edge.evcs.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.DoubleReadChannel;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StringWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusType;
import io.openems.edge.common.type.TypeUtils;

@ProviderType
public interface ManagedEvcs extends Evcs {

	/**
	 * Get the {@link EvcsPower}.
	 * 
	 * @return the {@link EvcsPower}
	 */
	public EvcsPower getEvcsPower();

	/**
	 * Minimal pause between two consecutive writes if the limit has not changed in
	 * seconds.
	 * 
	 * @return write interval in seconds
	 */
	public default int getWriteInterval() {
		return 30;
	}

	/**
	 * Minimum hardware limit in W given by the configuration.
	 * 
	 * <p>
	 * This value is taken as a fallback for the
	 * {@link ChannelId#FIXED_MINIMUM_HARDWARE_POWER} channel. As the configuration
	 * property should be in mA for AC chargers, make sure that the value is
	 * converted to W. Example: Math.round(current / 1000f) * DEFAULT_VOLTAGE *
	 * Phases.THREE_PHASE.getValue();
	 * 
	 * @return minimum hardware limit in W
	 */
	public int getConfiguredMinimumHardwarePower();

	/**
	 * Maximum hardware limit in W given by the configuration.
	 * 
	 * <p>
	 * This value is taken as a fallback for the
	 * {@link ChannelId#FIXED_MAXIMUM_HARDWARE_POWER} channel. As the configuration
	 * property should be in mA for AC chargers, make sure that the value is
	 * converted to W. Example: Math.round(current / 1000f) * DEFAULT_VOLTAGE *
	 * Phases.THREE_PHASE.getValue();
	 * 
	 * @return maximum hardware limit in W
	 */
	public int getConfiguredMaximumHardwarePower();

	/**
	 * Configuration if the debug mode is active or not.
	 * 
	 * @return boolean
	 */
	public boolean getConfiguredDebugMode();

	/**
	 * Command to send the given power, to the EVCS.
	 * 
	 * <p>
	 * Example:
	 * 
	 * <pre>
	 * {@code}
	 *  // Format the power to the required format and unit
	 * 
	 * 	String raw = "currtime " + current + " 1";
	 * 
	 * 	byte[] raw = s.getBytes();
	 * 	DatagramPacket packet = new DatagramPacket(raw, raw.length, ip, KebaKeContact.UDP_PORT);
	 * 	DatagramSocket datagrammSocket = null;
	 * 	try {
	 * 		datagrammSocket = new DatagramSocket();
	 * 		datagrammSocket.send(packet);
	 * 		return true;
	 * 	} catch (SocketException e) {
	 * 		this.logError(this.log, "Unable to open UDP socket for sending [" + s + "] to [" + ip.getHostAddress()
	 * 				+ "]: " + e.getMessage());
	 * 	} catch (IOException e) {
	 * 		this.logError(this.log,
	 * 				"Unable to send [" + s + "] UDP message to [" + ip.getHostAddress() + "]: " + e.getMessage());
	 * 	} finally {
	 * 		if (datagrammSocket != null) {
	 * 			datagrammSocket.close();
	 * 		}
	 * 	}
	 * 	return false;
	 * 
	 * </pre>
	 * 
	 * @param power Power that should be send in watt
	 * @return boolean if the power was applied to the EVCS
	 * @throws OpenemsException on error
	 */
	public boolean applyChargePowerLimit(int power) throws Exception;

	/**
	 * Command to pause a charge process of the EVCS.
	 * 
	 * <p>
	 * In most of the cases, it is sufficient to call the method
	 * {@link #applyChargePowerLimit(Integer)} with 0 as power but sometimes it
	 * needs extra commands to initiate a pause of charging.
	 * 
	 * @return boolean if the command was applied to the EVCS
	 * @throws OpenemsException on error
	 */
	public boolean pauseChargeProcess() throws Exception;

	/**
	 * Command to send the specified text to the EVCS display, if supported
	 * 
	 * <p>
	 * Writes to the display of the charging station, if the EVCS supports that
	 * feature - if not, return false.
	 * 
	 * @param text Text to display
	 * @return boolean if it was sent or not
	 * @throws OpenemsException on error
	 */
	public boolean applyDisplayText(String text) throws OpenemsException;

	/**
	 * Minimum time till a charging limit is taken by the EVCS in seconds.
	 * 
	 * @return time in seconds
	 */
	public int getMinimumTimeTillChargingLimitTaken();

	/**
	 * Get charge state handler.
	 * 
	 * @return charge state handler
	 */
	public ChargeStateHandler getChargeStateHandler();

	/**
	 * Log debug using {@link ManagedEvcs} Logger.
	 * 
	 * @param message message
	 */
	public void logDebug(String message);

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		/**
		 * Gets the smallest power steps that can be set (given in W).
		 *
		 * <p>
		 * Example:
		 * <ul>
		 * <li>KEBA-series allows setting of milli Ampere. It should return 0.23 W
		 * (0.001A * 230V).
		 * <li>Hardy Barth allows setting in Ampere. It should return 230 W (1A * 230V).
		 * </ul>
		 *
		 * <p>
		 * <ul>
		 * <li>Interface: ManagedEvcs
		 * <li>Writable
		 * <li>Type: Integer
		 * <li>Unit: W
		 * </ul>
		 */
		POWER_PRECISION(Doc.of(OpenemsType.DOUBLE) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH)), //

		/**
		 * Sets the charge power limit of the EVCS in [W].
		 *
		 * <p>
		 * Actual charge power depends on
		 * <ul>
		 * <li>whether the electric vehicle is connected at all and ready for charging
		 * <li>hardware limit of the charging station
		 * <li>limit of electric vehicle
		 * <li>limit of power line
		 * <li>...
		 * </ul>
		 *
		 * <p>
		 * Function:
		 * <ul>
		 * <li>Write Value should be sent to the EVCS and cleared afterwards
		 * <li>Read value should contain the currently valid loading target that was
		 * sent
		 * </ul>
		 *
		 * <ul>
		 * <li>Interface: ManagedEvcs
		 * <li>Writable
		 * <li>Type: Integer
		 * <li>Unit: W
		 * </ul>
		 */
		SET_CHARGE_POWER_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.HIGH) //
				.accessMode(AccessMode.READ_WRITE)), //

		/**
		 * Applies the configured filter in {@link EvcsPowerComponent} and sets a the
		 * charge power limit.
		 * 
		 * <p>
		 * The Filter is not used, when the limit is lower or equals the last limit.
		 * 
		 * <ul>
		 * <li>Interface: ManagedEvcs
		 * <li>Writable
		 * <li>Type: Integer
		 * <li>Unit: W
		 * </ul>
		 */
		SET_CHARGE_POWER_LIMIT_WITH_FILTER(new IntegerDoc() //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE).onInit(channel -> {
					((IntegerWriteChannel) channel).onSetNextWrite(value -> {

						if (value != null) {

							var evcs = (ManagedEvcs) channel.getComponent();

							// Set the limit directly if it is decreasing
							var currentTarget = evcs.getSetChargePowerLimit().orElse(0);
							if (value == null || value <= currentTarget || evcs.getEvcsPower() == null) {
								evcs.setChargePowerLimit(value);
								return;
							}

							var min = evcs.getMinimumHardwarePower().orElse(Evcs.DEFAULT_MINIMUM_HARDWARE_POWER);
							var max = evcs.getMaximumHardwarePower().orElse(Evcs.DEFAULT_MAXIMUM_HARDWARE_POWER);

							var increaseRate = evcs.getEvcsPower().getIncreaseRate();

							// Fit values into max value
							value = TypeUtils.fitWithin(min, max, value);
							currentTarget = TypeUtils.fitWithin(min, max, currentTarget);

							// Increase the last value with a ramp
							var filterOutput = evcs.getEvcsPower().getRampFilter().getFilteredValueAsInteger(
									currentTarget, value.floatValue(), evcs.getMaximumHardwarePower().orElse(22080),
									increaseRate);

							evcs.setChargePowerLimit(filterOutput);

							if (evcs instanceof ManagedEvcs) {
								((ManagedEvcs) evcs).logDebug("Filter: " + value + " -> " + filterOutput);
							}
						}
					});
				})), //

		/**
		 * Is true if the EVCS is in a EVCS-Cluster.
		 *
		 * <ul>
		 * <li>Interface: ManagedEvcs
		 * <li>Readable
		 * <li>Type: Boolean
		 * </ul>
		 */
		IS_CLUSTERED(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH)), //

		/**
		 * Which mode is used to set a charge power.
		 * 
		 * <p>
		 * Set internally by a controller. Used to prioritize between several evcs.
		 *
		 * <ul>
		 * <li>Interface: ManagedEvcs
		 * <li>Readable
		 * <li>Type: ChargeMode
		 * </ul>
		 */
		CHARGE_MODE(Doc.of(ChargeMode.values()) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH)), //

		/**
		 * Sets a Text that is shown on the display of the EVCS.
		 *
		 * <p>
		 * Be aware that the EVCS might not have a display or the text might be
		 * restricted.
		 *
		 * <ul>
		 * <li>Interface: ManagedEvcs
		 * <li>Writable
		 * <li>Type: String
		 * </ul>
		 */
		SET_DISPLAY_TEXT(Doc.of(OpenemsType.STRING) //
				.accessMode(AccessMode.READ_WRITE)),

		/**
		 * Sets a request for a charge power. The limit is not directly activated by
		 * this call.
		 *
		 * <ul>
		 * <li>Interface: ManagedEvcs
		 * <li>Writable
		 * <li>Type: Integer
		 * <li>Unit: W
		 * </ul>
		 */
		SET_CHARGE_POWER_REQUEST(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE)),

		/**
		 * Sets the energy limit for the current or next session in [Wh].
		 *
		 * <ul>
		 * <li>Interface: ManagedEvcs
		 * <li>Writable
		 * <li>Type: Integer
		 * <li>Unit: Wh
		 * </ul>
		 */
		SET_ENERGY_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.accessMode(AccessMode.READ_WRITE)),

		/**
		 * Charge Status.
		 * 
		 * <p>
		 * The Charge Status of the EVCS charging station. This is set by the
		 * ManagedEvcs.
		 * 
		 * <ul>
		 * <li>Interface: Evcs
		 * <li>Readable
		 * <li>Type: {@link ChargeState}
		 * </ul>
		 */
		CHARGE_STATE(Doc.of(ChargeState.values()) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH));

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
	 * Gets the Channel for {@link ChannelId#POWER_PRECISION}.
	 *
	 * @return the Channel
	 */
	public default DoubleReadChannel getPowerPrecisionChannel() {
		return this.channel(ChannelId.POWER_PRECISION);
	}

	/**
	 * Gets the power precision value of the EVCS in [W]. See
	 * {@link ChannelId#POWER_PRECISION}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Double> getPowerPrecision() {
		return this.getPowerPrecisionChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#POWER_PRECISION}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setPowerPrecision(Double value) {
		this.getPowerPrecisionChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#POWER_PRECISION}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setPowerPrecision(double value) {
		this.getPowerPrecisionChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_CHARGE_POWER_LIMIT}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetChargePowerLimitChannel() {
		return this.channel(ChannelId.SET_CHARGE_POWER_LIMIT);
	}

	/**
	 * Gets the set charge power limit of the EVCS in [W]. See
	 * {@link ChannelId#SET_CHARGE_POWER_LIMIT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getSetChargePowerLimit() {
		return this.getSetChargePowerLimitChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#SET_CHARGE_POWER_LIMIT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setSetChargePowerLimit(Integer value) {
		this.getSetChargePowerLimitChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#SET_CHARGE_POWER_LIMIT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setSetChargePowerLimit(int value) {
		this.getSetChargePowerLimitChannel().setNextValue(value);
	}

	/**
	 * Sets the charge power limit of the EVCS in [W]. See
	 * {@link ChannelId#SET_CHARGE_POWER_LIMIT}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setChargePowerLimit(Integer value) throws OpenemsNamedException {
		this.getSetChargePowerLimitChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_CHARGE_POWER_LIMIT_WITH_FILTER}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetChargePowerLimitWithFilterChannel() {
		return this.channel(ChannelId.SET_CHARGE_POWER_LIMIT_WITH_FILTER);
	}

	/**
	 * Gets the set charge power limit of the EVCS in [W] with applied filter. See
	 * {@link ChannelId#SET_CHARGE_POWER_LIMIT_WITH_FILTER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getSetChargePowerLimitWithFilter() {
		return this.getSetChargePowerLimitWithFilterChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#SET_CHARGE_POWER_LIMIT_WITH_FILTER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setSetChargePowerLimitWithFilter(Integer value) {
		this.getSetChargePowerLimitWithFilterChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#SET_CHARGE_POWER_LIMIT_WITH_FILTER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setSetChargePowerLimitWithFilter(int value) {
		this.getSetChargePowerLimitWithFilterChannel().setNextValue(value);
	}

	/**
	 * Sets the charge power limit of the EVCS in [W] with applied filter. See
	 * {@link ChannelId#SET_CHARGE_POWER_LIMIT_WITH_FILTER}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setChargePowerLimitWithFilter(Integer value) throws OpenemsNamedException {
		this.getSetChargePowerLimitWithFilterChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#IS_CLUSTERED}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getIsClusteredChannel() {
		return this.channel(ChannelId.IS_CLUSTERED);
	}

	/**
	 * Gets the Is true if the EVCS is in a EVCS-Cluster. See
	 * {@link ChannelId#IS_CLUSTERED}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getIsClustered() {
		return this.getIsClusteredChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#IS_CLUSTERED}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setIsClustered(boolean value) {
		this.getIsClusteredChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#CHARGE_MODE}.
	 *
	 * @return the Channel
	 */
	public default Channel<ChargeMode> getChargeModeChannel() {
		return this.channel(ChannelId.CHARGE_MODE);
	}

	/**
	 * Gets the ChargeMode of the Managed EVCS charging station. See
	 * {@link ChannelId#CHARGE_MODE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default ChargeMode getChargeMode() {
		return this.getChargeModeChannel().value().asEnum();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#CHARGE_MODE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setChargeMode(ChargeMode value) {
		this.getChargeModeChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_DISPLAY_TEXT}.
	 *
	 * @return the Channel
	 */
	public default StringWriteChannel getSetDisplayTextChannel() {
		return this.channel(ChannelId.SET_DISPLAY_TEXT);
	}

	/**
	 * Gets the Text that is shown on the display of the EVCS. See
	 * {@link ChannelId#SET_DISPLAY_TEXT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<String> getSetDisplayText() {
		return this.getSetDisplayTextChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#SET_DISPLAY_TEXT}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setSetDisplayText(String value) {
		this.getSetDisplayTextChannel().setNextValue(value);
	}

	/**
	 * Sets a Text that is shown on the display of the EVCS. See
	 * {@link ChannelId#SET_DISPLAY_TEXT}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setDisplayText(String value) throws OpenemsNamedException {
		this.getSetDisplayTextChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_CHARGE_POWER_REQUEST}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetChargePowerRequestChannel() {
		return this.channel(ChannelId.SET_CHARGE_POWER_REQUEST);
	}

	/**
	 * Gets the request for a charge power in [W]. The limit is not directly
	 * activated by this call.. See {@link ChannelId#SET_CHARGE_POWER_REQUEST}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getSetChargePowerRequest() {
		return this.getSetChargePowerRequestChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#SET_CHARGE_POWER_REQUEST} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setSetChargePowerRequest(Integer value) {
		this.getSetChargePowerRequestChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#SET_CHARGE_POWER_REQUEST} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setSetChargePowerRequest(int value) {
		this.getSetChargePowerRequestChannel().setNextValue(value);
	}

	/**
	 * Sets the request for a charge power in [W]. The limit is not directly
	 * activated by this call. See {@link ChannelId#SET_CHARGE_POWER_REQUEST}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setChargePowerRequest(Integer value) throws OpenemsNamedException {
		this.getSetChargePowerRequestChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_ENERGY_LIMIT}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetEnergyLimitChannel() {
		return this.channel(ChannelId.SET_ENERGY_LIMIT);
	}

	/**
	 * Gets the energy limit for the current or next session in [Wh].. See
	 * {@link ChannelId#SET_ENERGY_LIMIT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getSetEnergyLimit() {
		return this.getSetEnergyLimitChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#SET_ENERGY_LIMIT}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setSetEnergyLimit(Integer value) {
		this.getSetEnergyLimitChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#SET_ENERGY_LIMIT}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setSetEnergyLimit(int value) {
		this.getSetEnergyLimitChannel().setNextValue(value);
	}

	/**
	 * Sets the energy limit for the current or next session in [Wh]. See
	 * {@link ChannelId#SET_ENERGY_LIMIT}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setEnergyLimit(Integer value) throws OpenemsNamedException {
		this.getSetEnergyLimitChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#CHARGE_STATE}.
	 *
	 * @return the Channel
	 */
	public default EnumReadChannel getChargeStateChannel() {
		return this.channel(ChannelId.CHARGE_STATE);
	}

	/**
	 * Gets the current charge state. See {@link ChannelId#CHARGE_STATE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getChargeState() {
		return this.getChargeStateChannel().value();
	}

	/**
	 * Used for Modbus/TCP Api Controller. Provides a Modbus table for the Channels
	 * of this Component.
	 *
	 * @param accessMode filters the Modbus-Records that should be shown
	 * @return the {@link ModbusSlaveNatureTable}
	 */
	public static ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {
		return ModbusSlaveNatureTable.of(ManagedEvcs.class, accessMode, 100) //
				.channel(0, ChannelId.SET_CHARGE_POWER_LIMIT, ModbusType.UINT16) //
				.channel(1, ChannelId.SET_DISPLAY_TEXT, ModbusType.STRING16) //
				.channel(17, ChannelId.SET_ENERGY_LIMIT, ModbusType.UINT16) //
				// TODO: Add remaining channels
				.build();
	}
}
