package io.openems.edge.consolinno.simulator.heater.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface SimulationHeaterDecentral extends OpenemsComponent {

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        NEED_HEAT_1(Doc.of(OpenemsType.BOOLEAN)),
        NEED_HEAT_ENABLE_SIGNAL_1(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        NEED_HEAT_2(Doc.of(OpenemsType.BOOLEAN)),
        NEED_HEAT_ENABLE_SIGNAL_2(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        NEED_HEAT_3(Doc.of(OpenemsType.BOOLEAN)),
        NEED_HEAT_ENABLE_SIGNAL_3(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        NEED_HEAT_4(Doc.of(OpenemsType.BOOLEAN)),
        NEED_HEAT_ENABLE_SIGNAL_4(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        NEED_HEAT_5(Doc.of(OpenemsType.BOOLEAN)),
        NEED_HEAT_ENABLE_SIGNAL_5(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        NEED_HEAT_6(Doc.of(OpenemsType.BOOLEAN)),
        NEED_HEAT_ENABLE_SIGNAL_6(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE));

        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }
    }

    default Channel<Boolean> getNeedHeatChannel_1() {
        return this.channel(ChannelId.NEED_HEAT_1);
    }

    default WriteChannel<Boolean> getNeedHeatEnableSignal_1() {
        return this.channel(ChannelId.NEED_HEAT_ENABLE_SIGNAL_1);
    }

    default Channel<Boolean> getNeedHeatChannel_2() {
        return this.channel(ChannelId.NEED_HEAT_2);
    }

    default WriteChannel<Boolean> getNeedHeatEnableSignal_2() {
        return this.channel(ChannelId.NEED_HEAT_ENABLE_SIGNAL_2);
    }

    default Channel<Boolean> getNeedHeatChannel_3() {
        return this.channel(ChannelId.NEED_HEAT_3);
    }

    default WriteChannel<Boolean> getNeedHeatEnableSignal_3() {
        return this.channel(ChannelId.NEED_HEAT_ENABLE_SIGNAL_3);
    }

    default Channel<Boolean> getNeedHeatChannel_4() {
        return this.channel(ChannelId.NEED_HEAT_4);
    }

    default WriteChannel<Boolean> getNeedHeatEnableSignal_4() {
        return this.channel(ChannelId.NEED_HEAT_ENABLE_SIGNAL_4);
    }

    default Channel<Boolean> getNeedHeatChannel_5() {
        return this.channel(ChannelId.NEED_HEAT_5);
    }

    default WriteChannel<Boolean> getNeedHeatEnableSignal_5() {
        return this.channel(ChannelId.NEED_HEAT_ENABLE_SIGNAL_5);
    }

    default Channel<Boolean> getNeedHeatChannel_6() {
        return this.channel(ChannelId.NEED_HEAT_6);
    }

    default WriteChannel<Boolean> getNeedHeatEnableSignal_6() {
        return this.channel(ChannelId.NEED_HEAT_ENABLE_SIGNAL_6);
    }

    default Channel<Boolean> getNeedHeatByNumber(int number) {
        switch (number) {
            case 1:
                return this.getNeedHeatChannel_1();
            case 2:
                return this.getNeedHeatChannel_2();
            case 3:
                return this.getNeedHeatChannel_3();
            case 4:
                return this.getNeedHeatChannel_4();
            case 5:
                return this.getNeedHeatChannel_5();
            case 6:
                return this.getNeedHeatChannel_6();
        }
        return null;
    }

    default WriteChannel<Boolean> getNeedHeatEnableByNumber(int number) {
        switch (number) {
            case 1:
                return this.getNeedHeatEnableSignal_1();
            case 2:
                return this.getNeedHeatEnableSignal_2();
            case 3:
                return this.getNeedHeatEnableSignal_3();
            case 4:
                return this.getNeedHeatEnableSignal_4();
            case 5:
                return this.getNeedHeatEnableSignal_5();
            case 6:
                return this.getNeedHeatEnableSignal_6();
        }
        return null;
    }

}
