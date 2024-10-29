package io.openems.edge.levl.controller;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.DoubleReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

public interface ControllerEssBalancing extends Controller, OpenemsComponent {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        REALIZED_DISCHARGE_POWER_W(Doc.of(OpenemsType.LONG) //
                .unit(Unit.WATT) //
                .persistencePriority(PersistencePriority.HIGH)
                .text("the cumulated amount of discharge power that was realized since the last discharge request (in W)")), //
        REQUEST_TIMESTAMP(Doc.of(OpenemsType.STRING) //
                .persistencePriority(PersistencePriority.HIGH) //
                .text("the timestamp of the last levl control request")), //
        EFFICIENCY(Doc.of(OpenemsType.DOUBLE) //
                .unit(Unit.PERCENT)
                .persistencePriority(PersistencePriority.HIGH) //
                .text("efficiency of the system defined by levl")); //

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
     * Returns the IntegerReadChannel for the realized power.
     * @return the IntegerReadChannel
     */
    public default LongReadChannel getRealizedPowerWChannel() {
        return this.channel(ChannelId.REALIZED_DISCHARGE_POWER_W);
    }

    /**
     * Returns the value of the realized power.
     * @return the value of the realized power
     */
    public default Value<Long> getRealizedPowerW() {
        return this.getRealizedPowerWChannel().value();
    }

    /**
     * Sets the next value of the realized power.
     * @param value the next value
     */
    public default void _setRealizedPowerW(Long value) {
        this.getRealizedPowerWChannel().setNextValue(value);
    }

    /**
     * Returns the StringReadChannel for the last control request timestamp.
     * @return the StringReadChannel
     */
    public default StringReadChannel getLastControlRequestTimestampChannel() {
        return this.channel(ChannelId.REQUEST_TIMESTAMP);
    }

    /**
     * Returns the value of the last control request timestamp.
     * @return the value of the last control request timestamp
     */
    public default Value<String> getLastControlRequestTimestamp() {
        return this.getLastControlRequestTimestampChannel().value();
    }

    /**
     * Sets the next value of the last control request timestamp.
     * @param value the next value
     */
    public default void _setLastControlRequestTimestamp(String value) {
        this.getLastControlRequestTimestampChannel().setNextValue(value);
    }

    /**
     * Returns the DoubleReadChannel for the efficiency.
     * @return the DoubleReadChannel
     */
    public default DoubleReadChannel getEfficiencyChannel() {
        return this.channel(ChannelId.EFFICIENCY);
    }

    /**
     * Returns the value of the efficiency.
     * @return the value of the efficiency
     */
    public default Value<Double> getEfficiencyValue() {
        return this.getEfficiencyChannel().value();
    }

    /**
     * Sets the next value of the efficiency.
     * @param value the next value
     */
    public default void _setEfficiency(Double value) {
        this.getEfficiencyChannel().setNextValue(value);
    }

}