package io.openems.edge.bhkw;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface PowerLevel extends OpenemsComponent {
        /**.
         * How much Percent
         *
         * <ul>
         * <li>Interface: PowerLevel
         * <li>Type: Integer
         * <li>Unit: Percent
         * </ul>
         */
        public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

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
