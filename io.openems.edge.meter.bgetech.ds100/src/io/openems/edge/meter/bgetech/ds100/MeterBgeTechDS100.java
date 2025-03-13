/*
 *   OpenEMS Meter B+G E-Tech DS100 bundle
 *
 *   Written by Christian Poulter.
 *   Copyright (C) 2025 Christian Poulter <devel(at)poulter.de>
 *
 *   This program and the accompanying materials are made available under
 *   the terms of the Eclipse Public License v2.0 which accompanies this
 *   distribution, and is available at
 *
 *   https://www.eclipse.org/legal/epl-2.0
 *
 *   SPDX-License-Identifier: EPL-2.0
 *
 */

package io.openems.edge.meter.bgetech.ds100;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.meter.api.ElectricityMeter;

public interface MeterBgeTechDS100 extends ElectricityMeter, OpenemsComponent, ModbusSlave {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        // voltage
        VOLTAGE_L1_L2(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT).persistencePriority(PersistencePriority.HIGH)),
        VOLTAGE_L2_L3(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT).persistencePriority(PersistencePriority.HIGH)),
        VOLTAGE_L3_L1(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT).persistencePriority(PersistencePriority.HIGH)),
        VOLTAGE_L_L  (Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT).persistencePriority(PersistencePriority.HIGH)),

        // current
        CURRENT_N(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE).persistencePriority(PersistencePriority.HIGH)),

        // apparent power
        APPARENT_POWER_L1(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE).persistencePriority(PersistencePriority.HIGH)),
        APPARENT_POWER_L2(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE).persistencePriority(PersistencePriority.HIGH)),
        APPARENT_POWER_L3(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE).persistencePriority(PersistencePriority.HIGH)),
        APPARENT_POWER   (Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE).persistencePriority(PersistencePriority.HIGH)),

        // frequency
        FREQUENCY_L1(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIHERTZ).persistencePriority(PersistencePriority.HIGH)),
        FREQUENCY_L2(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIHERTZ).persistencePriority(PersistencePriority.HIGH)),
        FREQUENCY_L3(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIHERTZ).persistencePriority(PersistencePriority.HIGH)),
        
        // power factor
        POWER_FACTOR_L1(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE).persistencePriority(PersistencePriority.HIGH)),
        POWER_FACTOR_L2(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE).persistencePriority(PersistencePriority.HIGH)),
        POWER_FACTOR_L3(Doc.of(OpenemsType.FLOAT).unit(Unit.NONE).persistencePriority(PersistencePriority.HIGH)),
        POWER_FACTOR   (Doc.of(OpenemsType.FLOAT).unit(Unit.NONE).persistencePriority(PersistencePriority.HIGH)),

        // total active energy
        ACTIVE_TOTAL_ENERGY   (Doc.of(OpenemsType.LONG).unit(Unit.CUMULATED_WATT_HOURS).persistencePriority(PersistencePriority.LOW)),
        ACTIVE_TOTAL_ENERGY_L1(Doc.of(OpenemsType.LONG).unit(Unit.CUMULATED_WATT_HOURS).persistencePriority(PersistencePriority.LOW)),
        ACTIVE_TOTAL_ENERGY_L2(Doc.of(OpenemsType.LONG).unit(Unit.CUMULATED_WATT_HOURS).persistencePriority(PersistencePriority.LOW)),
        ACTIVE_TOTAL_ENERGY_L3(Doc.of(OpenemsType.LONG).unit(Unit.CUMULATED_WATT_HOURS).persistencePriority(PersistencePriority.LOW)),
        
        // total reactive energy
        REACTIVE_TOTAL_ENERGY   (Doc.of(OpenemsType.LONG).unit(Unit.VOLT_AMPERE_REACTIVE_HOURS).persistencePriority(PersistencePriority.LOW)),
        REACTIVE_TOTAL_ENERGY_L1(Doc.of(OpenemsType.LONG).unit(Unit.VOLT_AMPERE_REACTIVE_HOURS).persistencePriority(PersistencePriority.LOW)),
        REACTIVE_TOTAL_ENERGY_L2(Doc.of(OpenemsType.LONG).unit(Unit.VOLT_AMPERE_REACTIVE_HOURS).persistencePriority(PersistencePriority.LOW)),
        REACTIVE_TOTAL_ENERGY_L3(Doc.of(OpenemsType.LONG).unit(Unit.VOLT_AMPERE_REACTIVE_HOURS).persistencePriority(PersistencePriority.LOW)),
        
        // consumption reactive energy
        REACTIVE_CONSUMPTION_ENERGY   (Doc.of(OpenemsType.LONG).unit(Unit.VOLT_AMPERE_REACTIVE_HOURS).persistencePriority(PersistencePriority.LOW)),
        REACTIVE_CONSUMPTION_ENERGY_L1(Doc.of(OpenemsType.LONG).unit(Unit.VOLT_AMPERE_REACTIVE_HOURS).persistencePriority(PersistencePriority.LOW)),
        REACTIVE_CONSUMPTION_ENERGY_L2(Doc.of(OpenemsType.LONG).unit(Unit.VOLT_AMPERE_REACTIVE_HOURS).persistencePriority(PersistencePriority.LOW)),
        REACTIVE_CONSUMPTION_ENERGY_L3(Doc.of(OpenemsType.LONG).unit(Unit.VOLT_AMPERE_REACTIVE_HOURS).persistencePriority(PersistencePriority.LOW)),        
        
        // production reactive energy
        REACTIVE_PRODUCTION_ENERGY   (Doc.of(OpenemsType.LONG).unit(Unit.VOLT_AMPERE_REACTIVE_HOURS).persistencePriority(PersistencePriority.LOW)),
        REACTIVE_PRODUCTION_ENERGY_L1(Doc.of(OpenemsType.LONG).unit(Unit.VOLT_AMPERE_REACTIVE_HOURS).persistencePriority(PersistencePriority.LOW)),
        REACTIVE_PRODUCTION_ENERGY_L2(Doc.of(OpenemsType.LONG).unit(Unit.VOLT_AMPERE_REACTIVE_HOURS).persistencePriority(PersistencePriority.LOW)),
        REACTIVE_PRODUCTION_ENERGY_L3(Doc.of(OpenemsType.LONG).unit(Unit.VOLT_AMPERE_REACTIVE_HOURS).persistencePriority(PersistencePriority.LOW)),     
        
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
