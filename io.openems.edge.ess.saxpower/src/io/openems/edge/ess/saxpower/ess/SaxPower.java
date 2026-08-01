package io.openems.edge.ess.saxpower.ess;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusType;
import io.openems.edge.ess.api.ManagedSinglePhaseEss;

public interface SaxPower extends ManagedSinglePhaseEss, OpenemsComponent, ModbusComponent, ModbusSlave {

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        // Register 41
        ACTIVE_POWER_SET_POINT(Doc.of(OpenemsType.INTEGER)
                .accessMode(AccessMode.WRITE_ONLY)
                .unit(Unit.WATT)
        ),

        // Register 42
        COS_PHI_SET_POINT(Doc.of(OpenemsType.INTEGER) //
                .accessMode(AccessMode.WRITE_ONLY) //
        ),

        // Register 43
        MAX_DISCHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
                .accessMode(AccessMode.WRITE_ONLY) //
                .unit(Unit.WATT) //
        ),

        // Register 44
        MAX_CHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
                .accessMode(AccessMode.WRITE_ONLY) //
                .unit(Unit.WATT) //
        ),

        // Register 45
        OPERATING_STATE(Doc.of(OpenemsType.INTEGER)
                .accessMode(AccessMode.READ_WRITE)
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

    default WriteChannel<Integer> getOperatingStateChannel() {
        return this.channel(ChannelId.OPERATING_STATE);
    }
    default void setOperatingState(Integer value) throws OpenemsError.OpenemsNamedException {
        this.getOperatingStateChannel().setNextWriteValue(value);
    }

    static ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {
        return ModbusSlaveNatureTable.of(SaxPower.class, accessMode, 41)
                .channel(0, ChannelId.ACTIVE_POWER_SET_POINT, ModbusType.UINT16) // 41
                .channel(1, ChannelId.COS_PHI_SET_POINT, ModbusType.UINT16) // 42
                .channel(2, ChannelId.MAX_DISCHARGE_POWER, ModbusType.UINT16) // 43
                .channel(3, ChannelId.MAX_CHARGE_POWER, ModbusType.UINT16) // 44
                .channel(4, ChannelId.OPERATING_STATE, ModbusType.UINT16) // 45
                .build();
    }
}