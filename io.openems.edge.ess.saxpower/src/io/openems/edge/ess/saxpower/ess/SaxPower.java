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
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.api.ManagedSinglePhaseEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SinglePhaseEss;
import io.openems.edge.ess.api.SymmetricEss;

public interface SaxPower extends ManagedSinglePhaseEss, ManagedAsymmetricEss, ManagedSymmetricEss, SinglePhaseEss,
        AsymmetricEss, SymmetricEss, OpenemsComponent, ModbusComponent, ModbusSlave {

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

    default WriteChannel<Integer> getActivePowerSetPointChannel() {
        return this.channel(ChannelId.ACTIVE_POWER_SET_POINT);
    }

    default void setActivePowerSetPoint(Integer value) throws OpenemsError.OpenemsNamedException {
        this.getActivePowerSetPointChannel().setNextWriteValue(value);
    }

    @Override
    default void applyPower(int activePowerL1, int reactivePowerL1, int activePowerL2, int reactivePowerL2,
            int activePowerL3, int reactivePowerL3) throws OpenemsError.OpenemsNamedException {
        ManagedSinglePhaseEss.super.applyPower(activePowerL1, reactivePowerL1, activePowerL2, reactivePowerL2,
                activePowerL3, reactivePowerL3);
    }

    /**
     * Used for Modbus/TCP Api Controller. Provides a Modbus table for the Channels
     * of this Component.
     *
     * @param accessMode filters the Modbus-Records that should be shown
     * @return the {@link ModbusSlaveNatureTable}
     */
    static ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {
        return ModbusSlaveNatureTable.of(SaxPower.class, accessMode, 41)
                .channel(0, ChannelId.ACTIVE_POWER_SET_POINT, ModbusType.UINT16) // 41
                .channel(1, ChannelId.COS_PHI_SET_POINT, ModbusType.UINT16) // 42
                .channel(2, ChannelId.MAX_DISCHARGE_POWER, ModbusType.UINT16) // 43
                .channel(3, ChannelId.MAX_CHARGE_POWER, ModbusType.UINT16) // 44
                .build();
    }
}