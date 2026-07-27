package io.openems.edge.ess.saxpower;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusType;
import io.openems.edge.ess.api.SymmetricEss;

public interface SaxPower extends SymmetricEss, OpenemsComponent, ModbusComponent, ModbusSlave {

    public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        // Register 41
        ACTIVE_POWER_SET_POINT(Doc.of(OpenemsType.INTEGER) //
                .unit(Unit.WATT)),

        // Register 42
        COS_PHI_SET_POINT(Doc.of(OpenemsType.INTEGER)),

        // Register 45
        OPERATING_STATE(Doc.of(OpenemsType.INTEGER)),

        // Register 48
        METER_POWER(Doc.of(OpenemsType.INTEGER) //
                .unit(Unit.WATT));

        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }
    }

    public static ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {
        return ModbusSlaveNatureTable.of(SaxPower.class, accessMode, 100)
                .channel(0, ChannelId.ACTIVE_POWER_SET_POINT, ModbusType.UINT16)
                .channel(1, ChannelId.COS_PHI_SET_POINT, ModbusType.UINT16)
                .channel(2, ChannelId.OPERATING_STATE, ModbusType.UINT16)
                .channel(3, ChannelId.METER_POWER, ModbusType.UINT16)
                .build();
    }
}