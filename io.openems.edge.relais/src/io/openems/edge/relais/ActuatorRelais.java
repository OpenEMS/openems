package io.openems.edge.relais;

import io.openems.common.channel.Unit;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

public interface ActuatorRelais extends OpenemsComponent {
    /**
     * Is active or not
     *
     * <ul>
     * <li>Interface: ActuatorRelais
     * <li>Type: boolean
     * <li>Unit: ON_OFF
     * </ul>
     */
    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        ON_OFF(Doc.of(OpenemsType.BOOLEAN).unit(Unit.ON_OFF)); //

        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    default BooleanWriteChannel getRelaisChannel() {
        return (BooleanWriteChannel) this.channel(ChannelId.ON_OFF);
    }

    /**
     * Is active or not.
     *
     * @return
     */
    default Boolean isActive() {
        return (Boolean) this.channel(ChannelId.ON_OFF).value().get();
    }
}
