package io.openems.edge.ess.saxpower.gridmeter;

import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.meter.api.ElectricityMeter;

public interface SaxPowerEssGridMeter extends ElectricityMeter, OpenemsComponent, ModbusComponent, ModbusSlave {

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        TEST_DUMMY(Doc.of(OpenemsType.INTEGER));

        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }
    }
}