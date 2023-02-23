package io.openems.edge.evcs.schneider.parking.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

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
    default Channel<Integer> getCpwStateChannel() {
        return this.channel(ChannelId.CPW_STATE);
    }

    /**
     * Gets the Value of {@link ChannelId#CPW_STATE}.
     *
     * @return the value
     */
    default int getCpwState() {
        Channel<Integer> channel = this.getCpwStateChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0));
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
     * Gets the Channel for {@link ChannelId#STATION_ENERGY_MSB_READ}.
     *
     * @return the Channel
     */
    default Channel<Integer> getStationEnergyMsbReadChannel() {
        return this.channel(ChannelId.STATION_ENERGY_MSB_READ);
    }

    /**
     * Gets the Value of {@link ChannelId#STATION_ENERGY_MSB_READ}.
     *
     * @return the value
     */
    default int getStationEnergyMsb() {
        Channel<Integer> channel = this.getStationEnergyMsbReadChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0));
    }

    /**
     * Gets the Channel for {@link ChannelId#STATION_ENERGY_LSB_READ}.
     *
     * @return the Channel
     */
    default Channel<Integer> getStationEnergyLsbReadChannel() {
        return this.channel(ChannelId.STATION_ENERGY_LSB_READ);
    }

    /**
     * Gets the Value of {@link ChannelId#STATION_ENERGY_LSB_READ}.
     *
     * @return the value
     */
    default int getStationEnergyLsb() {
        Channel<Integer> channel = this.getStationEnergyLsbReadChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0));
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

}

