package io.openems.edge.ess.saxpower.gridmeter;

import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.meter.api.ElectricityMeter;

public interface SaxPowerEssGridMeter extends ElectricityMeter, OpenemsComponent, ModbusComponent, ModbusSlave {

    public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        // Define any custom/vendor-specific channels here if needed in the future
        ;

        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }
    }
}