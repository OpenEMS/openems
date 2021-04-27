package io.openems.edge.bridge.mqtt.dummys;

import io.openems.common.types.OpenemsType;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

public interface DummyChannels extends OpenemsComponent {

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        DUMMY_ONE(Doc.of(OpenemsType.INTEGER)),

        DUMMY_TWO(Doc.of(OpenemsType.STRING)),

        DUMMY_THREE(Doc.of(OpenemsType.DOUBLE)),

        DUMMY_FOUR(Doc.of(OpenemsType.INTEGER)),

        TEMPERATURE(Doc.of(OpenemsType.INTEGER)),

        POWER(Doc.of(OpenemsType.DOUBLE));


        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }
    }

    default Channel<Integer> getDummyOne() {
        return this.channel(ChannelId.DUMMY_ONE);
    }

    default Channel<String> getDummyTwo() {
        return this.channel(ChannelId.DUMMY_TWO);
    }

    default Channel<Double> getDummyThree() {
        return this.channel(ChannelId.DUMMY_THREE);
    }

    default Channel<Integer> getDummyFour() {
        return this.channel(ChannelId.DUMMY_FOUR);
    }

    default Channel<Integer> getTemperature() {
        return this.channel(ChannelId.TEMPERATURE);
    }

    default Channel<Integer> getPower() {
        return this.channel(ChannelId.POWER);
    }
}
