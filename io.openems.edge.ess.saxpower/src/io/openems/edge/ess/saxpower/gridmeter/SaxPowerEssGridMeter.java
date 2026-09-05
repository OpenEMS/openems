package io.openems.edge.ess.saxpower.gridmeter;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.meter.api.ElectricityMeter;

public interface SaxPowerEssGridMeter extends ElectricityMeter, OpenemsComponent, ModbusComponent, ModbusSlave {

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        //Address 40076
        GRID_POWER_SCALE_FACTOR(Doc.of(OpenemsType.INTEGER)
                .accessMode(AccessMode.READ_ONLY)
        );

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