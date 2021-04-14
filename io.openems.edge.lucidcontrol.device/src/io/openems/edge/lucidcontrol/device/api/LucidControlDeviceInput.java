package io.openems.edge.lucidcontrol.device.api;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

public interface LucidControlDeviceInput extends OpenemsComponent {

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        INPUT_VOLTAGE(Doc.of(OpenemsType.DOUBLE).unit(Unit.VOLT)),
        PRESSURE(Doc.of(OpenemsType.DOUBLE).unit(Unit.BAR));

        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }

    }

    default Channel<Double> getVoltageChannel() {
        return this.channel(ChannelId.INPUT_VOLTAGE);
    }

    default Value<Double> getVoltage() {
        return getVoltageChannel().value();
    }

    default Channel<Double> getPressureChannel() {
        return this.channel(ChannelId.PRESSURE);
    }

    default Value<Double> getPressure() {
        return this.getPressureChannel().value();
    }

}
