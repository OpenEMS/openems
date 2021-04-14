package io.openems.edge.heater.chp.viessmann.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.heater.api.ChpBasic;

public interface ViessmannPowerPercentage extends ChpBasic {

        public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
            /**
             * How much Percent of the Chp Power will be used.
             *
             * <ul>
             * <li>Interface: PowerLevel
             * <li>Type: Integer
             * <li>Unit: Percent
             * </ul>
             */

            POWER_LEVEL(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT).accessMode(AccessMode.READ_WRITE)); //
            private final Doc doc;

            private ChannelId(Doc doc) {
                this.doc = doc;
            }

            public Doc doc() {
                return this.doc;
            }

        }

        default IntegerWriteChannel getPowerLevelChannel() {
            return this.channel(ChannelId.POWER_LEVEL);
        }



}
