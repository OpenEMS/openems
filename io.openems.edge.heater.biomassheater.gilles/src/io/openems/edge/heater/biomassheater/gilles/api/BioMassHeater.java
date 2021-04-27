package io.openems.edge.heater.biomassheater.gilles.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.heater.Heater;

public interface BioMassHeater extends Heater {

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /*RW Channels_2 Byte_ Address 24576-24584*/

        BOILER_TEMPERATURE_SET_POINT(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)
                .accessMode(AccessMode.READ_WRITE)),
        BOILER_TEMPERATURE_MINIMAL_FORWARD(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)
                .accessMode(AccessMode.READ_WRITE)),
        SLIDE_IN_PERCENTAGE_VALUE_READ(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)),
        SLIDE_IN_PERCENTAGE_VALUE(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)
                .accessMode(AccessMode.READ_WRITE)),
        EXHAUST_PERFORMANCE_MIN(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)
                .accessMode(AccessMode.READ_WRITE)),
        EXHAUST_PERFORMANCE_MAX(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)
                .accessMode(AccessMode.READ_WRITE)),
        OXYGEN_PERFORMANCE_MIN(Doc.of(OpenemsType.FLOAT).unit(Unit.PERCENT)
                .accessMode(AccessMode.READ_WRITE)),
        OXYGEN_PERFORMANCE_MAX(Doc.of(OpenemsType.FLOAT).unit(Unit.PERCENT)
                .accessMode(AccessMode.READ_WRITE)),
        SLIDE_IN_MIN_READ(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)),
        SLIDE_IN_MIN(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)
                .accessMode(AccessMode.READ_WRITE)),
        SLIDE_IN_MAX_READ(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)),
        SLIDE_IN_MAX(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)
                .accessMode(AccessMode.READ_WRITE)),

        /* Read Channel 2 Byte; Address 20000-20035*/

        BOILER_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
        REWIND_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
        EXHAUST_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
        FIRE_ROOM_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
        SLIDE_IN_ACTIVE(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)),
        OXYGEN_ACTIVE(Doc.of(OpenemsType.FLOAT).unit(Unit.PERCENT)),
        VACUUM_ACTIVE(Doc.of(OpenemsType.FLOAT).unit(Unit.PASCAL)),
        PERFORMANCE_ACTIVE(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT)),
        PERFORMANCE_WM(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS)),
        PERCOLATION(Doc.of(OpenemsType.INTEGER).unit(Unit.CUBICMETER_PER_HOUR)),
        REWIND_GRID(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
        BUFFER_SENSOR_1(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
        BUFFER_SENSOR_2(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
        BUFFER_SENSOR_3(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
        BUFFER_SENSOR_4(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
        BUFFER_SENSOR_5(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
        BUFFER_SENSOR_6(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
        BUFFER_SENSOR_7(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
        BUFFER_SENSOR_8(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
        BUFFER_SENSOR_9(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
        BUFFER_SENSOR_10(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
        BUFFER_SENSOR_11(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
        BUFFER_SENSOR_12(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
        BUFFER_SENSOR_13(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
        BUFFER_SENSOR_14(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
        BUFFER_SENSOR_15(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
        BUFFER_SENSOR_16(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
        BOILER_TEMPERATURE_SET_POINT_READ_ONLY(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
        TEMPERATURE_FORWARD_MIN(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
        SLIDE_IN_PERCENTAGE_VALUE_READ_ONLY(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)),
        EXHAUST_PERFORMANCE_MIN_READ_ONLY(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
        EXHAUST_PERFORMANCE_MAX_READ_ONLY(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
        OXYGEN_PERFORMANCE_MIN_READ_ONLY(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
        OXYGEN_PERFORMANCE_MAX_READ_ONLY(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
        SLIDE_IN_MIN_READ_ONLY(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)),
        SLIDE_IN_MAX_READ_ONLY(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT)),

        /*
         * Read Channel 1 Bit Address: 10000-10025
         * FC 1 and FC2
         * */

        DISTURBANCE(Doc.of(OpenemsType.BOOLEAN)),
        WARNING(Doc.of(OpenemsType.BOOLEAN)),
        CLEARING_ACTIVE(Doc.of(OpenemsType.BOOLEAN)),
        VACUUM_CLEANING_ON(Doc.of(OpenemsType.BOOLEAN)),
        FAN_ON(Doc.of(OpenemsType.BOOLEAN)),
        FAN_PRIMARY_ON(Doc.of(OpenemsType.BOOLEAN)),
        FAN_SECONDARY_ON(Doc.of(OpenemsType.BOOLEAN)),
        STOKER_ON(Doc.of(OpenemsType.BOOLEAN)),
        ROTARY_VALVE_ON(Doc.of(OpenemsType.BOOLEAN)),
        DOSI_ON(Doc.of(OpenemsType.BOOLEAN)),
        HELIX_1_ON(Doc.of(OpenemsType.BOOLEAN)),
        HELIX_2_ON(Doc.of(OpenemsType.BOOLEAN)),
        HELIX_3_ON(Doc.of(OpenemsType.BOOLEAN)),
        CROSS_CONVEYOR(Doc.of(OpenemsType.BOOLEAN)),
        SLIDING_FLOOR_1_ON(Doc.of(OpenemsType.BOOLEAN)),
        SLIDING_FLOOR_2_ON(Doc.of(OpenemsType.BOOLEAN)),
        IGNITION_ON(Doc.of(OpenemsType.BOOLEAN)),
        LS_1(Doc.of(OpenemsType.BOOLEAN)),
        LS_2(Doc.of(OpenemsType.BOOLEAN)),
        LS_3(Doc.of(OpenemsType.BOOLEAN)),
        LS_LATERAL(Doc.of(OpenemsType.BOOLEAN)),
        LS_PUSHING_FLOOR(Doc.of(OpenemsType.BOOLEAN)),
        HELIX_ASH_1(Doc.of(OpenemsType.BOOLEAN)),
        HELIX_ASH_2(Doc.of(OpenemsType.BOOLEAN)),
        SIGNAL_CONTACT_1(Doc.of(OpenemsType.BOOLEAN)),
        SIGNAL_CONTACT_2(Doc.of(OpenemsType.BOOLEAN)),


        /*
         * Write Single Coil Task FC 5 16387 for external usage (true for activation; false for deactivation...use this primarily for controlling
         *
         * */
        EXTERNAL_CONTROL(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE));

        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }
    }

    default WriteChannel<Integer> getBoilerTemperatureSetPoint() {
        return this.channel(ChannelId.BOILER_TEMPERATURE_SET_POINT);
    }

    default WriteChannel<Integer> getBoilerTemperatureMinimalForward() {
        return this.channel(ChannelId.BOILER_TEMPERATURE_MINIMAL_FORWARD);
    }

    default WriteChannel<Integer> getSlideInPercentageValue() {
        return this.channel(ChannelId.SLIDE_IN_PERCENTAGE_VALUE);
    }

    default WriteChannel<Integer> getExhaustPerformanceMin() {
        return this.channel(ChannelId.EXHAUST_PERFORMANCE_MIN);
    }

    default WriteChannel<Integer> getExhaustPerformanceMax() {
        return this.channel(ChannelId.EXHAUST_PERFORMANCE_MAX);
    }

    default WriteChannel<Float> getOxygenPerformanceMin() {
        return this.channel(ChannelId.OXYGEN_PERFORMANCE_MIN);
    }

    default WriteChannel<Float> getOxygenPerformanceMax() {
        return this.channel(ChannelId.OXYGEN_PERFORMANCE_MAX);
    }

    default WriteChannel<Integer> getSlideInMin() {
        return this.channel(ChannelId.SLIDE_IN_MIN);
    }

    default WriteChannel<Integer> getSlideInMax() {
        return this.channel(ChannelId.SLIDE_IN_MAX);
    }

    default Channel<Integer> getBoilerTemperature() {
        return this.channel(ChannelId.BOILER_TEMPERATURE);
    }

    default Channel<Integer> getRewindTemperature() {
        return this.channel(ChannelId.REWIND_TEMPERATURE);
    }

    default Channel<Integer> getExhaustTemperature() {
        return this.channel(ChannelId.EXHAUST_TEMPERATURE);
    }

    default Channel<Integer> getFireRoomTemperature() {
        return this.channel(ChannelId.FIRE_ROOM_TEMPERATURE);
    }

    default Channel<Integer> getSlideInAcitve() {
        return this.channel(ChannelId.SLIDE_IN_ACTIVE);
    }

    default Channel<Float> getOxygenActive() {
        return this.channel(ChannelId.OXYGEN_ACTIVE);
    }

    default Channel<Float> getVacuumActive() {
        return this.channel(ChannelId.VACUUM_ACTIVE);
    }

    default Channel<Integer> getPerformanceActive() {
        return this.channel(ChannelId.PERFORMANCE_ACTIVE);
    }

    default Channel<Integer> getPerformanceWM() {
        return this.channel(ChannelId.PERFORMANCE_WM);
    }

    default Channel<Integer> getPercolation() {
        return this.channel(ChannelId.PERCOLATION);
    }

    default Channel<Integer> getRewindGrid() {
        return this.channel(ChannelId.REWIND_GRID);
    }

    default Channel<Integer> getBufferSensor_1() {
        return this.channel(ChannelId.BUFFER_SENSOR_1);
    }

    default Channel<Integer> getBufferSensor_2() {
        return this.channel(ChannelId.BUFFER_SENSOR_2);
    }

    default Channel<Integer> getBufferSensor_3() {
        return this.channel(ChannelId.BUFFER_SENSOR_3);
    }

    default Channel<Integer> getBufferSensor_4() {
        return this.channel(ChannelId.BUFFER_SENSOR_4);
    }

    default Channel<Integer> getBufferSensor_5() {
        return this.channel(ChannelId.BUFFER_SENSOR_5);
    }

    default Channel<Integer> getBufferSensor_6() {
        return this.channel(ChannelId.BUFFER_SENSOR_6);
    }

    default Channel<Integer> getBufferSensor_7() {
        return this.channel(ChannelId.BUFFER_SENSOR_7);
    }

    default Channel<Integer> getBufferSensor_8() {
        return this.channel(ChannelId.BUFFER_SENSOR_8);
    }

    default Channel<Integer> getBufferSensor_9() {
        return this.channel(ChannelId.BUFFER_SENSOR_9);
    }

    default Channel<Integer> getBufferSensor_10() {
        return this.channel(ChannelId.BUFFER_SENSOR_10);
    }

    default Channel<Integer> getBufferSensor_11() {
        return this.channel(ChannelId.BUFFER_SENSOR_11);
    }

    default Channel<Integer> getBufferSensor_12() {
        return this.channel(ChannelId.BUFFER_SENSOR_12);
    }

    default Channel<Integer> getBufferSensor_13() {
        return this.channel(ChannelId.BUFFER_SENSOR_13);
    }

    default Channel<Integer> getBufferSensor_14() {
        return this.channel(ChannelId.BUFFER_SENSOR_14);
    }

    default Channel<Integer> getBufferSensor_15() {
        return this.channel(ChannelId.BUFFER_SENSOR_15);
    }

    default Channel<Integer> getBufferSensor_16() {
        return this.channel(ChannelId.BUFFER_SENSOR_16);
    }

    default Channel<Integer> getBoilerTemperatureSetPointReadOnly() {
        return this.channel(ChannelId.BOILER_TEMPERATURE_SET_POINT_READ_ONLY);
    }

    default Channel<Integer> getTemperatureForwardMin() {
        return this.channel(ChannelId.TEMPERATURE_FORWARD_MIN);
    }

    default Channel<Integer> getSlideInPercentageValueReadOnly() {
        return this.channel(ChannelId.SLIDE_IN_PERCENTAGE_VALUE_READ_ONLY);
    }

    default Channel<Integer> getExhaustPerformanceMinReadOnly() {
        return this.channel(ChannelId.EXHAUST_PERFORMANCE_MIN_READ_ONLY);
    }

    default Channel<Integer> getExhaustPerformanceMaxReadOnly() {
        return this.channel(ChannelId.EXHAUST_PERFORMANCE_MAX_READ_ONLY);
    }

    default Channel<Integer> getOxygenPerformanceMinReadOnly() {
        return this.channel(ChannelId.OXYGEN_PERFORMANCE_MIN_READ_ONLY);
    }

    default Channel<Integer> getOxygenPerformanceMaxReadOnly() {
        return this.channel(ChannelId.OXYGEN_PERFORMANCE_MAX_READ_ONLY);
    }

    default Channel<Integer> getSlideInMinReadOnly() {
        return this.channel(ChannelId.SLIDE_IN_MIN_READ_ONLY);
    }

    default Channel<Integer> getSlideInMaxReadOnly() {
        return this.channel(ChannelId.SLIDE_IN_MAX_READ_ONLY);
    }

    default Channel<Boolean> getDisturbance() {
        return this.channel(ChannelId.DISTURBANCE);
    }

    default Channel<Boolean> getWarning() {
        return this.channel(ChannelId.WARNING);
    }

    default Channel<Boolean> getClearingActive() {
        return this.channel(ChannelId.CLEARING_ACTIVE);
    }

    default Channel<Boolean> getVacuumCleaningOn() {
        return this.channel(ChannelId.VACUUM_CLEANING_ON);
    }

    default Channel<Boolean> getFanOn() {
        return this.channel(ChannelId.FAN_ON);
    }

    default Channel<Boolean> getFanPrimaryOn() {
        return this.channel(ChannelId.FAN_PRIMARY_ON);
    }

    default Channel<Boolean> getFanSecondaryOn() {
        return this.channel(ChannelId.FAN_SECONDARY_ON);
    }

    default Channel<Boolean> getStokerOn() {
        return this.channel(ChannelId.STOKER_ON);
    }

    default Channel<Boolean> getRotaryValveOn() {
        return this.channel(ChannelId.ROTARY_VALVE_ON);
    }

    default Channel<Boolean> getDosiOn() {
        return this.channel(ChannelId.DOSI_ON);
    }

    default Channel<Boolean> getHelix_1_On() {
        return this.channel(ChannelId.HELIX_1_ON);
    }

    default Channel<Boolean> getHelix_2_On() {
        return this.channel(ChannelId.HELIX_2_ON);
    }

    default Channel<Boolean> getHelix_3_On() {
        return this.channel(ChannelId.HELIX_3_ON);
    }

    default Channel<Boolean> getCrossconveyor() {
        return this.channel(ChannelId.CROSS_CONVEYOR);
    }

    default Channel<Boolean> getSlidingFloor_1_On() {
        return this.channel(ChannelId.SLIDING_FLOOR_1_ON);
    }

    default Channel<Boolean> getSlidingFloor_2_On() {
        return this.channel(ChannelId.SLIDING_FLOOR_2_ON);
    }

    default Channel<Boolean> getIgnition_On() {
        return this.channel(ChannelId.IGNITION_ON);
    }

    default Channel<Boolean> getLS_1() {
        return this.channel(ChannelId.LS_1);
    }

    default Channel<Boolean> getLS_2() {
        return this.channel(ChannelId.LS_2);
    }

    default Channel<Boolean> getLS_3() {
        return this.channel(ChannelId.LS_3);
    }

    default Channel<Boolean> getLS_Lateral() {
        return this.channel(ChannelId.LS_LATERAL);
    }

    default Channel<Boolean> getLS_Pushing_Floor() {
        return this.channel(ChannelId.LS_PUSHING_FLOOR);
    }

    default Channel<Boolean> getHelixAsh_1() {
        return this.channel(ChannelId.HELIX_ASH_1);
    }

    default Channel<Boolean> getHelixAsh_2() {
        return this.channel(ChannelId.HELIX_ASH_2);
    }

    default Channel<Boolean> getSignalContact_1() {
        return this.channel(ChannelId.SIGNAL_CONTACT_1);
    }

    default Channel<Boolean> getSignalContact_2() {
        return this.channel(ChannelId.SIGNAL_CONTACT_2);
    }

    default WriteChannel<Boolean> getExternalControl() {
        return this.channel(ChannelId.EXTERNAL_CONTROL);
    }


}

