package io.openems.edge.evcs.schneider.parking.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evcs.schneider.parking.RemoteCommand;

/**
 * This Provides the Channels for the Schneider EVCS.
 */

public interface Schneider extends OpenemsComponent {

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * The state of the Charging Station.
         * See CPWState enum for more info.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: Integer
         * <li>Unit: Na
         * </ul>
         */
        CPW_STATE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),
        /**
         * Last state of the Charging Station.
         * See LastChargeStatus enum for more info.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: Integer
         * <li>Unit: Na
         * </ul>
         */
        LAST_CHARGE_STATUS(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),
        /**
         * Return Value of the Remote Command.
         * Return the Command Code if valid or 0x8000 if an error occurred.
         * See RemoteCommand enum for more info.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: Integer
         * <li>Unit: Na
         * </ul>
         */
        REMOTE_COMMAND_STATUS(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),
        /**
         * First register for Error Status of EVCS.
         * See EVCSEError enum for more info.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: Integer
         * <li>Unit: Na
         * </ul>
         */
        ERROR_STATUS_MSB(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),
        /**
         * Second register for Error Status of EVCS.
         * See EVCSEError enum for more info.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: Integer
         * <li>Unit: Na
         * </ul>
         */
        ERROR_STATUS_LSB(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),
        /**
         * The charge time is the time during the session while the contactor is closed.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: Integer
         * <li>Unit: Na
         * </ul>
         */
        CHARGE_TIME(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),
        CHARGE_TIME_2(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),
        /**
         * Write Channel for the Remote Command.
         * See RemoteCommand enum for more info.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: Integer
         * <li>Unit: Na
         * </ul>
         */
        REMOTE_COMMAND(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        /**
         * The Maximum Current that can be distributed to the EVCS.
         * Either 16 or 32 A.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: Integer
         * <li>Unit: Ampere
         * </ul>
         */
        MAX_INTENSITY_SOCKET(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE).accessMode(AccessMode.READ_WRITE)),
        /**
         * Only operably on a master board if load balancing is enabled.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: Integer
         * <li>Unit: Unknown
         * </ul>
         */
        STATIC_MAX_INTENSITY_CLUSTER(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        /**
         * Current current load currently drawn on Phase 1.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: float
         * <li>Unit: Ampere
         * </ul>
         */
        STATION_INTENSITY_PHASE_X(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE).accessMode(AccessMode.READ_WRITE)),
        /**
         * Current current load currently drawn on Phase 2.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: float
         * <li>Unit: Ampere
         * </ul>
         */
        STATION_INTENSITY_PHASE_2(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE).accessMode(AccessMode.READ_WRITE)),
        /**
         * Current current load currently drawn on Phase 3.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: float
         * <li>Unit: Ampere
         * </ul>
         */
        STATION_INTENSITY_PHASE_3(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE).accessMode(AccessMode.READ_WRITE)),
        /**
         * TBD.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: Integer
         * <li>Unit: Watt Hours
         * </ul>
         */
        STATION_ENERGY_MSB(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_WRITE)),
        /**
         * TBD.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: Integer
         * <li>Unit: Watt Hours
         * </ul>
         */
        STATION_ENERGY_LSB(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_WRITE)),
        /**
         * Total Power Consumption of the EVCS.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: Float
         * <li>Unit: Kilo Watt
         * </ul>
         */
        STATION_POWER_TOTAL(Doc.of(OpenemsType.FLOAT).unit(Unit.KILOWATT).accessMode(AccessMode.READ_WRITE)),
        /**
         * Current current load currently drawn on Phase 1.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: float
         * <li>Unit: Ampere
         * </ul>
         */
        STATION_INTENSITY_PHASE_X_READ(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
        /**
         * Current current load currently drawn on Phase 2.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: float
         * <li>Unit: Ampere
         * </ul>
         */
        STATION_INTENSITY_PHASE_2_READ(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
        /**
         * Current current load currently drawn on Phase 3.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: float
         * <li>Unit: Ampere
         * </ul>
         */
        STATION_INTENSITY_PHASE_3_READ(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
        /**
         * TBD.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: Integer
         * <li>Unit: Watt Hours
         * </ul>
         */
        STATION_ENERGY_MSB_READ(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY)),
        /**
         * TBD.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: Integer
         * <li>Unit: Watt Hours
         * </ul>
         */
        STATION_ENERGY_LSB_READ(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY)),
        /**
         * Total Power Consumption of the EVCS.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: Float
         * <li>Unit: Kilo Watt
         * </ul>
         */
        STATION_POWER_TOTAL_READ(Doc.of(OpenemsType.FLOAT).unit(Unit.KILOWATT).accessMode(AccessMode.READ_ONLY)),
        /**
         * Current Voltage between L1 and L2.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: Float
         * <li>Unit: Volt
         * </ul>
         */
        STN_METER_L1_L2_VOLTAGE(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)),
        /**
         * Current Voltage between L2 and L3.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: Float
         * <li>Unit: Volt
         * </ul>
         */
        STN_METER_L2_L3_VOLTAGE(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)),
        /**
         * Current Voltage between L3 and L1.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: Float
         * <li>Unit: Volt
         * </ul>
         */
        STN_METER_L3_L1_VOLTAGE(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)),
        /**
         * Current Voltage between L1 and N.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: Float
         * <li>Unit: Volt
         * </ul>
         */
        STN_METER_L1_N_VOLTAGE(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)),
        /**
         * Current Voltage between L2 and N.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: Float
         * <li>Unit: Volt
         * </ul>
         */
        STN_METER_L2_N_VOLTAGE(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)),
        /**
         * Current Voltage between L3 and N.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: Float
         * <li>Unit: Volt
         * </ul>
         */
        STN_METER_L3_N_VOLTAGE(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)),
        /**
         * Keep Alive Register for the Remote Managment System. Has to be set to 1 every Cycle.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: Integer
         * <li>Unit: Na
         * </ul>
         */
        REMOTE_CONTROLLER_LIFE_BIT(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        /**
         * Register that contains the Information if the EVCS is currently running in Degraded Mode.
         * Value == 2 if Degraded Mode is active
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: Integer
         * <li>Unit: Na
         * </ul>
         */
        DEGRADED_MODE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),

        /**
         * Time between start and stop of the current Charging process. Start with RFID and ends with RFID or command.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: Integer
         * <li>Unit:
         * </ul>
         */
        SESSION_TIME(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),
        SESSION_TIME_2(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY));
        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }


    /**
     * Gets the Channel for {@link ChannelId#CPW_STATE}.
     *
     * @return the Channel
     */
    default Channel<Integer> getCPWStateChannel() {
        return this.channel(ChannelId.CPW_STATE);
    }

    /**
     * Gets the Value of {@link ChannelId#CPW_STATE}.
     *
     * @return the value
     */
    default int getCPWState() {
        Channel<Integer> channel = this.getCPWStateChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0));
    }

    /**
     * Gets the Channel for {@link ChannelId#LAST_CHARGE_STATUS}.
     *
     * @return the Channel
     */
    default Channel<Integer> getLastChargeStatusChannel() {
        return this.channel(ChannelId.LAST_CHARGE_STATUS);
    }

    /**
     * Gets the Value of {@link ChannelId#LAST_CHARGE_STATUS}.
     *
     * @return the value
     */
    default int getLastChargeStatus() {
        Channel<Integer> channel = this.getLastChargeStatusChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0));
    }

    /**
     * Gets the Channel for {@link ChannelId#REMOTE_COMMAND_STATUS}.
     *
     * @return the Channel
     */
    default Channel<Integer> getRemoteCommandStatusChannel() {
        return this.channel(ChannelId.REMOTE_COMMAND_STATUS);
    }

    /**
     * Gets the Value of {@link ChannelId#REMOTE_COMMAND_STATUS}.
     *
     * @return the value
     */
    default int getRemoteCommandStatus() {
        Channel<Integer> channel = this.getRemoteCommandStatusChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0));
    }

    /**
     * Gets the Channel for {@link ChannelId#ERROR_STATUS_MSB}.
     *
     * @return the Channel
     */
    default Channel<Integer> getErrorStatusMSBChannel() {
        return this.channel(ChannelId.ERROR_STATUS_MSB);
    }

    /**
     * Gets the Value of {@link ChannelId#ERROR_STATUS_MSB}.
     *
     * @return the value
     */
    default int getErrorStatusMSB() {
        Channel<Integer> channel = this.getErrorStatusMSBChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0));
    }

    /**
     * Gets the Channel for {@link ChannelId#ERROR_STATUS_LSB}.
     *
     * @return the Channel
     */
    default Channel<Integer> getErrorStatusLSBChannel() {
        return this.channel(ChannelId.ERROR_STATUS_LSB);
    }

    /**
     * Gets the Value of {@link ChannelId#ERROR_STATUS_LSB}.
     *
     * @return the value
     */
    default int getErrorStatusLSB() {
        Channel<Integer> channel = this.getErrorStatusLSBChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0));
    }

    /**
     * Gets the Channel for {@link ChannelId#CHARGE_TIME}.
     *
     * @return the Channel
     */
    default Channel<Integer> getChargeTimeChannel() {
        return this.channel(ChannelId.CHARGE_TIME);
    }

    /**
     * Gets the Value of {@link ChannelId#CHARGE_TIME}.
     *
     * @return the value
     */
    default int getChargeTime() {
        Channel<Integer> channel = this.getChargeTimeChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0));
    }

    /**
     * Gets the Channel for {@link ChannelId#CHARGE_TIME_2}.
     *
     * @return the Channel
     */
    default Channel<Integer> getChargeTime2Channel() {
        return this.channel(ChannelId.CHARGE_TIME_2);
    }

    /**
     * Gets the Value of {@link ChannelId#CHARGE_TIME_2}.
     *
     * @return the value
     */
    default int getChargeTime2() {
        Channel<Integer> channel = this.getChargeTime2Channel();
        return channel.value().orElse(channel.getNextValue().orElse(0));
    }

    /**
     * Gets the Channel for {@link ChannelId#REMOTE_COMMAND}.
     *
     * @return the Channel
     */
    default WriteChannel<Integer> getRemoteCommandChannel() {
        return this.channel(ChannelId.REMOTE_COMMAND);
    }

    /**
     * Sets a command into the RemoteCommand register. See
     * {@link ChannelId#REMOTE_COMMAND}.
     *
     * @param command the next write value
     * @throws OpenemsError.OpenemsNamedException on error
     */
    default void setRemoteCommand(RemoteCommand command) throws OpenemsError.OpenemsNamedException {
        this.getRemoteCommandChannel().setNextWriteValue(command.getValue());
    }

    /**
     * Gets the Value of {@link ChannelId#REMOTE_COMMAND}.
     *
     * @return the value
     */
    default int getSetRemoteCommand() {
        WriteChannel<Integer> channel = this.getRemoteCommandChannel();
        return channel.value().orElse(channel.getNextWriteValue().orElse(0));
    }

    /**
     * Gets the Channel for {@link ChannelId#MAX_INTENSITY_SOCKET}.
     *
     * @return the Channel
     */
    default WriteChannel<Integer> getMaxIntensitySocketChannel() {
        return this.channel(ChannelId.MAX_INTENSITY_SOCKET);
    }

    /**
     * Sets the Maximum Load in A (either 16 A or 32 A). See
     * {@link ChannelId#MAX_INTENSITY_SOCKET}.
     *
     * @param value the next write value
     * @throws OpenemsError.OpenemsNamedException on error
     */
    default void setMaxIntensitySocket(int value) throws OpenemsError.OpenemsNamedException {
        this.getMaxIntensitySocketChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Value of {@link ChannelId#MAX_INTENSITY_SOCKET}.
     *
     * @return the value
     */
    default int getSetMaxIntensitySocket() {
        WriteChannel<Integer> channel = this.getMaxIntensitySocketChannel();
        return channel.value().orElse(channel.getNextWriteValue().orElse(channel.getNextValue().orElse(channel.value().orElse(0))));
    }

    /**
     * Gets the Channel for {@link ChannelId#STATIC_MAX_INTENSITY_CLUSTER}.
     *
     * @return the Channel
     */
    default WriteChannel<Integer> getStaticMaxIntensityClusterChannel() {
        return this.channel(ChannelId.STATIC_MAX_INTENSITY_CLUSTER);
    }

    /**
     * TBD. See
     * {@link ChannelId#STATIC_MAX_INTENSITY_CLUSTER}.
     *
     * @param value the next write value
     * @throws OpenemsError.OpenemsNamedException on error
     */
    default void setStaticMaxIntensityCluster(int value) throws OpenemsError.OpenemsNamedException {
        this.getStaticMaxIntensityClusterChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Value of {@link ChannelId#STATIC_MAX_INTENSITY_CLUSTER}.
     *
     * @return the value
     */
    default int getSetStaticMaxIntensityCluster() {
        WriteChannel<Integer> channel = this.getStaticMaxIntensityClusterChannel();
        return channel.value().orElse(channel.getNextWriteValue().orElse(0));
    }

    /**
     * Gets the Channel for {@link ChannelId#STATION_INTENSITY_PHASE_X}.
     *
     * @return the Channel
     */
    default WriteChannel<Float> getStationIntensityPhaseXChannel() {
        return this.channel(ChannelId.STATION_INTENSITY_PHASE_X);
    }

    /**
     * Gets the Channel for {@link ChannelId#STATION_INTENSITY_PHASE_X_READ}.
     *
     * @return the Channel
     */
    default Channel<Float> getStationIntensityPhaseXReadChannel() {
        return this.channel(ChannelId.STATION_INTENSITY_PHASE_X_READ);
    }

    /**
     * Gets the Value of {@link ChannelId#STATION_INTENSITY_PHASE_X_READ}.
     *
     * @return the value
     */
    default float getStationIntensityPhaseX() {
        Channel<Float> channel = this.getStationIntensityPhaseXReadChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0.f));
    }

    /**
     * Sets the Limit for Phase 1. See
     * {@link ChannelId#STATION_INTENSITY_PHASE_X}.
     *
     * @param value the next write value
     * @throws OpenemsError.OpenemsNamedException on error
     */
    default void setStationIntensityPhaseX(float value) throws OpenemsError.OpenemsNamedException {
        this.getStationIntensityPhaseXChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Value of {@link ChannelId#STATION_INTENSITY_PHASE_X}.
     *
     * @return the value
     */
    default float getSetStationIntensityPhaseX() {
        WriteChannel<Float> channel = this.getStationIntensityPhaseXChannel();
        return channel.value().orElse(channel.getNextWriteValue().orElse(0.f));
    }

    /**
     * Gets the Channel for {@link ChannelId#STATION_INTENSITY_PHASE_2}.
     *
     * @return the Channel
     */
    default WriteChannel<Float> getStationIntensityPhase2Channel() {
        return this.channel(ChannelId.STATION_INTENSITY_PHASE_2);
    }

    /**
     * Gets the Channel for {@link ChannelId#STATION_INTENSITY_PHASE_2_READ}.
     *
     * @return the Channel
     */
    default Channel<Float> getStationIntensityPhase2ReadChannel() {
        return this.channel(ChannelId.STATION_INTENSITY_PHASE_2_READ);
    }

    /**
     * Gets the Value of {@link ChannelId#STATION_INTENSITY_PHASE_2_READ}.
     *
     * @return the value
     */
    default float getStationIntensityPhase2() {
        Channel<Float> channel = this.getStationIntensityPhase2ReadChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0.f));
    }

    /**
     * Sets the Limit for Phase 2. See
     * {@link ChannelId#STATION_INTENSITY_PHASE_2}.
     *
     * @param value the next write value
     * @throws OpenemsError.OpenemsNamedException on error
     */
    default void setStationIntensityPhase2(float value) throws OpenemsError.OpenemsNamedException {
        this.getStationIntensityPhase2Channel().setNextWriteValue(value);
    }

    /**
     * Gets the Value of {@link ChannelId#STATION_INTENSITY_PHASE_2}.
     *
     * @return the value
     */
    default float getSetStationIntensityPhase2() {
        WriteChannel<Float> channel = this.getStationIntensityPhase2Channel();
        return channel.value().orElse(channel.getNextWriteValue().orElse(0.f));
    }

    /**
     * Gets the Channel for {@link ChannelId#STATION_INTENSITY_PHASE_3}.
     *
     * @return the Channel
     */
    default WriteChannel<Float> getStationIntensityPhase3Channel() {
        return this.channel(ChannelId.STATION_INTENSITY_PHASE_3);
    }

    /**
     * Gets the Channel for {@link ChannelId#STATION_INTENSITY_PHASE_3_READ}.
     *
     * @return the Channel
     */
    default Channel<Float> getStationIntensityPhase3ReadChannel() {
        return this.channel(ChannelId.STATION_INTENSITY_PHASE_3_READ);
    }

    /**
     * Gets the Value of {@link ChannelId#STATION_INTENSITY_PHASE_3_READ}.
     *
     * @return the value
     */
    default float getStationIntensityPhase3() {
        Channel<Float> channel = this.getStationIntensityPhase3ReadChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0.f));
    }

    /**
     * Sets the Limit for Phase 3. See
     * {@link ChannelId#STATION_INTENSITY_PHASE_3}.
     *
     * @param value the next write value
     * @throws OpenemsError.OpenemsNamedException on error
     */
    default void setStationIntensityPhase3(float value) throws OpenemsError.OpenemsNamedException {
        this.getStationIntensityPhase3Channel().setNextWriteValue(value);
    }

    /**
     * Gets the Value of {@link ChannelId#STATION_INTENSITY_PHASE_3}.
     *
     * @return the value
     */
    default float getSetStationIntensityPhase3() {
        WriteChannel<Float> channel = this.getStationIntensityPhase3Channel();
        return channel.value().orElse(channel.getNextWriteValue().orElse(0.f));
    }

    /**
     * Gets the Channel for {@link ChannelId#STATION_ENERGY_MSB}.
     *
     * @return the Channel
     */
    default WriteChannel<Integer> getStationEnergyMSBChannel() {
        return this.channel(ChannelId.STATION_ENERGY_MSB);
    }

    /**
     * Gets the Channel for {@link ChannelId#STATION_ENERGY_MSB_READ}.
     *
     * @return the Channel
     */
    default Channel<Integer> getStationEnergyMSBReadChannel() {
        return this.channel(ChannelId.STATION_ENERGY_MSB_READ);
    }

    /**
     * Gets the Value of {@link ChannelId#STATION_ENERGY_MSB_READ}.
     *
     * @return the value
     */
    default int getStationEnergyMSB() {
        Channel<Integer> channel = this.getStationEnergyMSBReadChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0));
    }

    /**
     * TBD. See
     * {@link ChannelId#STATION_ENERGY_MSB}.
     *
     * @param value the next write value
     * @throws OpenemsError.OpenemsNamedException on error
     */
    default void setStationEnergyMSB(int value) throws OpenemsError.OpenemsNamedException {
        this.getStationEnergyMSBChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Value of {@link ChannelId#STATION_ENERGY_MSB}.
     *
     * @return the value
     */
    default int getSetStationEnergyMSB() {
        WriteChannel<Integer> channel = this.getStationEnergyMSBChannel();
        return channel.value().orElse(channel.getNextWriteValue().orElse(0));
    }

    /**
     * Gets the Channel for {@link ChannelId#STATION_ENERGY_LSB}.
     *
     * @return the Channel
     */
    default WriteChannel<Integer> getStationEnergyLSBChannel() {
        return this.channel(ChannelId.STATION_ENERGY_LSB);
    }

    /**
     * Gets the Channel for {@link ChannelId#STATION_ENERGY_LSB_READ}.
     *
     * @return the Channel
     */
    default Channel<Integer> getStationEnergyLSBReadChannel() {
        return this.channel(ChannelId.STATION_ENERGY_LSB_READ);
    }

    /**
     * Gets the Value of {@link ChannelId#STATION_ENERGY_LSB_READ}.
     *
     * @return the value
     */
    default int getStationEnergyLSB() {
        Channel<Integer> channel = this.getStationEnergyLSBReadChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0));
    }

    /**
     * TBD. See
     * {@link ChannelId#STATION_ENERGY_LSB}.
     *
     * @param value the next write value
     * @throws OpenemsError.OpenemsNamedException on error
     */
    default void setStationEnergyLSB(int value) throws OpenemsError.OpenemsNamedException {
        this.getStationEnergyLSBChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Value of {@link ChannelId#STATION_ENERGY_LSB}.
     *
     * @return the value
     */
    default int getSetStationEnergyLSB() {
        WriteChannel<Integer> channel = this.getStationEnergyLSBChannel();
        return channel.value().orElse(channel.getNextWriteValue().orElse(0));
    }

    /**
     * Gets the Channel for {@link ChannelId#STATION_POWER_TOTAL}.
     *
     * @return the Channel
     */
    default WriteChannel<Float> getStationPowerTotalChannel() {
        return this.channel(ChannelId.STATION_POWER_TOTAL);
    }

    /**
     * Gets the Channel for {@link ChannelId#STATION_POWER_TOTAL_READ}.
     *
     * @return the Channel
     */
    default Channel<Float> getStationPowerTotalReadChannel() {
        return this.channel(ChannelId.STATION_POWER_TOTAL_READ);
    }

    /**
     * Gets the Value of {@link ChannelId#STATION_POWER_TOTAL_READ}.
     *
     * @return the value
     */
    default float getStationPowerTotal() {
        Channel<Float> channel = this.getStationPowerTotalReadChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0.f));
    }

    /**
     * Sets the Power Limit of the Station. See
     * {@link ChannelId#STATION_POWER_TOTAL}.
     *
     * @param value the next write value
     * @throws OpenemsError.OpenemsNamedException on error
     */
    default void setStationPowerTotal(float value) throws OpenemsError.OpenemsNamedException {
        this.getStationPowerTotalChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Value of {@link ChannelId#STATION_POWER_TOTAL}.
     *
     * @return the value
     */
    default float getSetStationPowerTotal() {
        WriteChannel<Float> channel = this.getStationPowerTotalChannel();
        return channel.value().orElse(channel.getNextWriteValue().orElse(0.f));
    }

    /**
     * Gets the Channel for {@link ChannelId#STN_METER_L1_L2_VOLTAGE}.
     *
     * @return the Channel
     */
    default WriteChannel<Float> getStnMeterL1L2VoltageChannel() {
        return this.channel(ChannelId.STN_METER_L1_L2_VOLTAGE);
    }

    /**
     * Sets the Voltage between L1 and L2. See
     * {@link ChannelId#STN_METER_L1_L2_VOLTAGE}.
     *
     * @param value the next write value
     * @throws OpenemsError.OpenemsNamedException on error
     */
    default void setStnMeterL1L2Voltage(float value) throws OpenemsError.OpenemsNamedException {
        this.getStnMeterL1L2VoltageChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Value of {@link ChannelId#STN_METER_L1_L2_VOLTAGE}.
     *
     * @return the value
     */
    default float getSetStnMeterL1L2Voltage() {
        WriteChannel<Float> channel = this.getStnMeterL1L2VoltageChannel();
        return channel.value().orElse(channel.getNextWriteValue().orElse(0.f));
    }

    /**
     * Gets the Channel for {@link ChannelId#STN_METER_L2_L3_VOLTAGE}.
     *
     * @return the Channel
     */
    default WriteChannel<Float> getStnMeterL2L3VoltageChannel() {
        return this.channel(ChannelId.STN_METER_L2_L3_VOLTAGE);
    }

    /**
     * Sets the Voltage between L2 and L3. See
     * {@link ChannelId#STN_METER_L2_L3_VOLTAGE}.
     *
     * @param value the next write value
     * @throws OpenemsError.OpenemsNamedException on error
     */
    default void setStnMeterL2L3Voltage(float value) throws OpenemsError.OpenemsNamedException {
        this.getStnMeterL2L3VoltageChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Value of {@link ChannelId#STN_METER_L2_L3_VOLTAGE}.
     *
     * @return the value
     */
    default float getSetStnMeterL2L3Voltage() {
        WriteChannel<Float> channel = this.getStnMeterL2L3VoltageChannel();
        return channel.value().orElse(channel.getNextWriteValue().orElse(0.f));
    }

    /**
     * Gets the Channel for {@link ChannelId#STN_METER_L3_L1_VOLTAGE}.
     *
     * @return the Channel
     */
    default WriteChannel<Float> getStnMeterL3L1VoltageChannel() {
        return this.channel(ChannelId.STN_METER_L3_L1_VOLTAGE);
    }

    /**
     * Sets the Voltage between L3 and L1. See
     * {@link ChannelId#STN_METER_L3_L1_VOLTAGE}.
     *
     * @param value the next write value
     * @throws OpenemsError.OpenemsNamedException on error
     */
    default void setStnMeterL3L1Voltage(float value) throws OpenemsError.OpenemsNamedException {
        this.getStnMeterL3L1VoltageChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Value of {@link ChannelId#STN_METER_L2_L3_VOLTAGE}.
     *
     * @return the value
     */
    default float getSetStnMeterL3L1Voltage() {
        WriteChannel<Float> channel = this.getStnMeterL3L1VoltageChannel();
        return channel.value().orElse(channel.getNextWriteValue().orElse(0.f));
    }

    /**
     * Gets the Channel for {@link ChannelId#STN_METER_L1_N_VOLTAGE}.
     *
     * @return the Channel
     */
    default WriteChannel<Float> getStnMeterL1NVoltageChannel() {
        return this.channel(ChannelId.STN_METER_L1_N_VOLTAGE);
    }

    /**
     * Sets the Voltage between L1 and N. See
     * {@link ChannelId#STN_METER_L1_N_VOLTAGE}.
     *
     * @param value the next write value
     * @throws OpenemsError.OpenemsNamedException on error
     */
    default void setStnMeterL1NVoltage(float value) throws OpenemsError.OpenemsNamedException {
        this.getStnMeterL1NVoltageChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Value of {@link ChannelId#STN_METER_L1_N_VOLTAGE}.
     *
     * @return the value
     */
    default float getSetStnMeterL1NVoltage() {
        WriteChannel<Float> channel = this.getStnMeterL1NVoltageChannel();
        return channel.value().orElse(channel.getNextWriteValue().orElse(0.f));
    }

    /**
     * Gets the Channel for {@link ChannelId#STN_METER_L2_N_VOLTAGE}.
     *
     * @return the Channel
     */
    default WriteChannel<Float> getStnMeterL2NVoltageChannel() {
        return this.channel(ChannelId.STN_METER_L2_N_VOLTAGE);
    }

    /**
     * Sets the Voltage between L2 and N. See
     * {@link ChannelId#STN_METER_L2_N_VOLTAGE}.
     *
     * @param value the next write value
     * @throws OpenemsError.OpenemsNamedException on error
     */
    default void setStnMeterL2NVoltage(float value) throws OpenemsError.OpenemsNamedException {
        this.getStnMeterL2NVoltageChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Value of {@link ChannelId#STN_METER_L2_N_VOLTAGE}.
     *
     * @return the value
     */
    default float getSetStnMeterL2NVoltage() {
        WriteChannel<Float> channel = this.getStnMeterL2NVoltageChannel();
        return channel.value().orElse(channel.getNextWriteValue().orElse(0.f));
    }

    /**
     * Gets the Channel for {@link ChannelId#STN_METER_L3_N_VOLTAGE}.
     *
     * @return the Channel
     */
    default WriteChannel<Float> getStnMeterL3NVoltageChannel() {
        return this.channel(ChannelId.STN_METER_L3_N_VOLTAGE);
    }

    /**
     * Sets the Voltage between L3 and N. See
     * {@link ChannelId#STN_METER_L3_N_VOLTAGE}.
     *
     * @param value the next write value
     * @throws OpenemsError.OpenemsNamedException on error
     */
    default void setStnMeterL3NVoltage(float value) throws OpenemsError.OpenemsNamedException {
        this.getStnMeterL3NVoltageChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Value of {@link ChannelId#STN_METER_L3_N_VOLTAGE}.
     *
     * @return the value
     */
    default float getSetStnMeterL3NVoltage() {
        WriteChannel<Float> channel = this.getStnMeterL3NVoltageChannel();
        return channel.value().orElse(channel.getNextWriteValue().orElse(0.f));
    }

    /**
     * Gets the Channel for {@link ChannelId#REMOTE_CONTROLLER_LIFE_BIT}.
     *
     * @return the Channel
     */
    default WriteChannel<Integer> getRemoteControllerLifeBitChannel() {
        return this.channel(ChannelId.REMOTE_CONTROLLER_LIFE_BIT);
    }

    /**
     * Sets Life Bit. See
     * {@link ChannelId#REMOTE_CONTROLLER_LIFE_BIT}.
     *
     * @param value the next write value
     * @throws OpenemsError.OpenemsNamedException on error
     */
    default void setRemoteControllerLifeBit(int value) throws OpenemsError.OpenemsNamedException {
        this.getRemoteControllerLifeBitChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Value of {@link ChannelId#REMOTE_CONTROLLER_LIFE_BIT}.
     *
     * @return the value
     */
    default int getSetRemoteControllerLifeBit() {
        WriteChannel<Integer> channel = this.getRemoteControllerLifeBitChannel();
        return channel.value().orElse(channel.getNextWriteValue().orElse(0));
    }

    /**
     * Gets the Channel for {@link ChannelId#DEGRADED_MODE}.
     *
     * @return the Channel
     */
    default Channel<Integer> getDegradedModeChannel() {
        return this.channel(ChannelId.DEGRADED_MODE);
    }

    /**
     * Gets the Value of {@link ChannelId#DEGRADED_MODE}.
     *
     * @return the value
     */
    default int getDegradedMode() {
        Channel<Integer> channel = this.getDegradedModeChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0));
    }

    /**
     * Gets the Channel for {@link ChannelId#SESSION_TIME}.
     *
     * @return the Channel
     */
    default Channel<Integer> getSessionTimeChannel() {
        return this.channel(ChannelId.SESSION_TIME);
    }

    /**
     * Gets the Value of {@link ChannelId#SESSION_TIME}.
     *
     * @return the value
     */
    default int getSessionTime() {
        Channel<Integer> channel = this.getSessionTimeChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0));
    }

    /**
     * Gets the Channel for {@link ChannelId#SESSION_TIME_2}.
     *
     * @return the Channel
     */
    default Channel<Integer> getSessionTime2Channel() {
        return this.channel(ChannelId.SESSION_TIME_2);
    }

    /**
     * Gets the Value of {@link ChannelId#SESSION_TIME_2}.
     *
     * @return the value
     */
    default int getSessionTime2() {
        Channel<Integer> channel = this.getSessionTime2Channel();
        return channel.value().orElse(channel.getNextValue().orElse(0));
    }
}

