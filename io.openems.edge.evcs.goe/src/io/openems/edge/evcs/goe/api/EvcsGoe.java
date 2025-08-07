package io.openems.edge.evcs.goe.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.meter.api.ElectricityMeter;

public interface EvcsGoe extends Evcs, ElectricityMeter, OpenemsComponent {
    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        ERROR(Doc.of(Level.FAULT) //
                .persistencePriority(PersistencePriority.HIGH) //
                .translationKey(EvcsGoe.class, "errGeneral")), //

        GOE_STATE(Doc.of(OpenemsType.INTEGER)), //
        MAX_CURRENT(Doc.of(OpenemsType.INTEGER) //
                .accessMode(AccessMode.READ_WRITE) //
                .unit(Unit.MILLIAMPERE)), //
        FAILSAFE_CURRENT(Doc.of(OpenemsType.INTEGER) //
                .accessMode(AccessMode.READ_WRITE)//
                .unit(Unit.MILLIAMPERE) //
        );

        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }
    }

    /**
     * Gets the Channel for {@link EvcsGoe.ChannelId#ERROR}.
     *
     * @return the Channel
     */
    public default StateChannel getErrorChannel() {
        return this.channel(EvcsGoe.ChannelId.ERROR);
    }

    /**
     * Internal method to set the 'nextValue' on {@link EvcsGoe.ChannelId#ERROR}.
     *
     * @param value the next value
     */
    public default void _setError(boolean value) {
        this.getErrorChannel().setNextValue(value);
    }

    /**
     * Gets the Channel for {@link EvcsGoe.ChannelId#MAX_CURRENT}.
     *
     * @return the Channel
     */
    public default WriteChannel<Integer> getMaxCurrentChannel() {
        return this.channel(EvcsGoe.ChannelId.MAX_CURRENT);
    }

    /**
     * Internal method to set the 'nextWriteValue' on {@link EvcsGoe.ChannelId#MAX_CURRENT}.
     *
     * @param value the next value
     */
    public default void setMaxCurrent(int value) throws OpenemsError.OpenemsNamedException {
        this.getMaxCurrentChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link EvcsGoe.ChannelId#FAILSAFE_CURRENT}.
     *
     * @return the Channel
     */
    public default WriteChannel<Integer> getFailsafeCurrentChannel() {
        return this.channel(EvcsGoe.ChannelId.FAILSAFE_CURRENT);
    }

    /**
     * Internal method to set the 'nextWriteValue' on
     * {@link EvcsGoe.ChannelId#FAILSAFE_CURRENT}.
     *
     * @param value the next value
     */
    public default void setFailsafeCurrent(int value) throws OpenemsError.OpenemsNamedException {
        this.getFailsafeCurrentChannel().setNextWriteValue(value);
    }
    
}
