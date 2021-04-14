package io.openems.edge.controller.heatnetwork.pump.grundfos.api;


import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface PumpGrundfosControllerChannels extends OpenemsComponent {
    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        CONTROL_MODE(Doc.of(ControlModeSetting.values()).accessMode(AccessMode.READ_WRITE)),
        STOP_PUMP(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        PRESSURE_SETPOINT(Doc.of(OpenemsType.DOUBLE).unit(Unit.BAR).accessMode(AccessMode.READ_WRITE)),
        FREQUENCY_SETPOINT(Doc.of(OpenemsType.DOUBLE).unit(Unit.PERCENT).accessMode(AccessMode.READ_WRITE)),
        ONLY_READ(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE));

        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }

    }

    default WriteChannel<Integer> setControlMode() {
        return channel(ChannelId.CONTROL_MODE);
    }

    default WriteChannel<Boolean> setStopPump() {
        return channel(ChannelId.STOP_PUMP);
    }

    default WriteChannel<Double> setPressureSetpoint() {
        return channel(ChannelId.PRESSURE_SETPOINT);
    }

    default WriteChannel<Double> setFrequencySetpoint() {
        return channel(ChannelId.FREQUENCY_SETPOINT);
    }

    default WriteChannel<Boolean> setOnlyRead() {
        return channel(ChannelId.ONLY_READ);
    }
}


