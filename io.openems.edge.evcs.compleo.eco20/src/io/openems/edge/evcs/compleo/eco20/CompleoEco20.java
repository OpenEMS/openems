package io.openems.edge.evcs.compleo.eco20;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanDoc;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

public interface CompleoEco20 extends OpenemsComponent {
    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

	CABLE_CURRENT_LIMIT(Doc.of(OpenemsType.INTEGER) //
		.unit(Unit.AMPERE) //
		.persistencePriority(PersistencePriority.LOW) //
		.text("Cable max current")),
	VOLTAGE_L1(Doc.of(OpenemsType.INTEGER) //
		.unit(Unit.VOLT) //
		.persistencePriority(PersistencePriority.LOW) //
		.text("Voltage on L1")),
	VOLTAGE_L2(Doc.of(OpenemsType.INTEGER) //
		.unit(Unit.VOLT) //
		.persistencePriority(PersistencePriority.LOW) //
		.text("Voltage on L2")),
	VOLTAGE_L3(Doc.of(OpenemsType.INTEGER) //
		.unit(Unit.VOLT) //
		.persistencePriority(PersistencePriority.LOW) //
		.text("Voltage on L3")),
	FIRMWARE_VERSION(Doc.of(OpenemsType.LONG) //
		.unit(Unit.NONE) //
		.persistencePriority(PersistencePriority.HIGH)), //
	REACTIVE_POWER(Doc.of(OpenemsType.LONG) //
		.unit(Unit.VOLT_AMPERE) //
		.persistencePriority(PersistencePriority.LOW) //
		.text("reactive power")),
	APPARENT_POWER(Doc.of(OpenemsType.LONG) //
		.unit(Unit.VOLT_AMPERE_REACTIVE) //
		.persistencePriority(PersistencePriority.LOW) //
		.text("apparent power")),
	FREQUENCY(Doc.of(OpenemsType.INTEGER) //
		.unit(Unit.HERTZ) //
		.persistencePriority(PersistencePriority.LOW) //
		.text("Frequency")),
	MAX_CURRENT_L1(Doc.of(OpenemsType.INTEGER) //
		.unit(Unit.AMPERE) //
		.persistencePriority(PersistencePriority.LOW) //
		.text("Max current on L1")),
	MAX_CURRENT_L2(Doc.of(OpenemsType.INTEGER) //
		.unit(Unit.AMPERE) //
		.persistencePriority(PersistencePriority.LOW) //
		.text("Max current on L2")),
	MAX_CURRENT_L3(Doc.of(OpenemsType.INTEGER) //
		.unit(Unit.AMPERE) //
		.persistencePriority(PersistencePriority.LOW) //
		.text("Max current on L3")),

	SET_CHARGING_CURRENT(Doc.of(OpenemsType.INTEGER) //
		.unit(Unit.MILLIAMPERE) //
		.persistencePriority(PersistencePriority.HIGH) //
		.text("current")),
	DEFAULT_CHARGING_CURRENT(Doc.of(OpenemsType.INTEGER) //
		.unit(Unit.MILLIAMPERE).accessMode(AccessMode.READ_WRITE) //
		.persistencePriority(PersistencePriority.HIGH) //
		.text("current setpoint")),

	DEBUG_MODIFY_CHARGING_STATION_AVAILABILTY(new BooleanDoc() //
		.unit(Unit.ON_OFF)),
	/**
	 * Disable/enable pilotsignal between evcs and charging station.
	 *
	 * <ul>
	 * <li>Type: Boolean
	 * <li>Range: On/Off
	 * </ul>
	 */
	MODIFY_CHARGING_STATION_AVAILABILTY(new BooleanDoc() //
		.unit(Unit.ON_OFF) //
		.accessMode(AccessMode.READ_WRITE) //
		.onInit(new BooleanWriteChannel.MirrorToDebugChannel(DEBUG_MODIFY_CHARGING_STATION_AVAILABILTY))), //
	DEBUG_ENABLE_CHARGING(new BooleanDoc() //
		.unit(Unit.ON_OFF)),
	ENABLE_CHARGING(new BooleanDoc() //
		.unit(Unit.ON_OFF) //
		.accessMode(AccessMode.READ_WRITE) //
		.onInit(new BooleanWriteChannel.MirrorToDebugChannel(DEBUG_ENABLE_CHARGING))), //
	START_STOP_TIMER(Doc.of(OpenemsType.INTEGER) //
		.unit(Unit.SECONDS)), //
	PILOT_SIGNAL_DEACTIVATION_TIMER(Doc.of(OpenemsType.INTEGER) //
		.unit(Unit.SECONDS)) //
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
     * Gets the Channel for {@link ChannelId#CABLE_CURRENT_LIMIT}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getCableCurrentLimitChannel() {
	return this.channel(ChannelId.CABLE_CURRENT_LIMIT);
    }

    /**
     * Gets the Channel for {@link ChannelId#SET_CHARGING_CURRENT}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getSetChargingCurrentChannel() {
	return this.channel(ChannelId.SET_CHARGING_CURRENT);
    }

    /**
     * Gets the set charging current of the EVCS in [mA]. See
     * {@link ChannelId#SET_CHARGING_CURRENT}.
     *
     * @return the channel {@link Value}
     */
    public default Value<Integer> getSetChargingCurrent() {
	return this.getSetChargingCurrentChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#DEFAULT_CHARGING_CURRENT}.
     *
     * @return the Channel
     */
    public default WriteChannel<Integer> getDefaultChargingCurrentChannel() {
	return this.channel(ChannelId.DEFAULT_CHARGING_CURRENT);
    }

    /**
     * Sets the default charging current of the EVCS in [mA]. See
     * {@link ChannelId#DEFAULT_CHARGING_CURRENT}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    public default void setDefaultChargingCurrent(Integer value) throws OpenemsNamedException {
	this.getDefaultChargingCurrentChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#MODIFY_CHARGING_STATION_AVAILABILTY}.
     *
     * @return the Channel
     */
    public default BooleanWriteChannel getModifyChargingStationAvailabilityChannel() {
	return this.channel(ChannelId.MODIFY_CHARGING_STATION_AVAILABILTY);
    }

    /**
     * Disables or enables the pilot signal. See
     * {@link ChannelId#MODIFY_CHARGING_STATION_AVAILABILTY}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    public default void setModifyChargingStationAvailability(Boolean value) throws OpenemsNamedException {
	this.getModifyChargingStationAvailabilityChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#ENABLE_CHARGING}.
     *
     * @return the Channel
     */
    public default BooleanWriteChannel getEnableChargingChannel() {
	return this.channel(ChannelId.ENABLE_CHARGING);
    }

    /**
     * Enables or disables charging. See {@link ChannelId#ENABLE_CHARGING}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    public default void setEnableCharging(Boolean value) throws OpenemsNamedException {
	this.getEnableChargingChannel().setNextWriteValue(value);
    }

}
