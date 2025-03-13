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

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.INVERT_IF_TRUE;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_2;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_3;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.FloatWordElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;

@Designate(ocd = Config.class, factory = true)
@Component(
    name = "Meter.BGETech.DS100",
    immediate = true,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class MeterBgeTechDS100Impl extends AbstractOpenemsModbusComponent implements 
    MeterBgeTechDS100, ElectricityMeter, ModbusComponent, OpenemsComponent, ModbusSlave
{
    
    private static final String MODBUS_SETTER_REFERENCE = "Modbus";
    
    @Reference
    private ConfigurationAdmin cm;
    
    private MeterType meterType = MeterType.PRODUCTION;    
    private boolean invert = false;
    
    public MeterBgeTechDS100Impl() {
        super(
            OpenemsComponent.ChannelId.values(),
            ModbusComponent.ChannelId.values(),
            ElectricityMeter.ChannelId.values(),
            MeterBgeTechDS100.ChannelId.values()
        );
    }

    @Override
    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    @Activate
    private void activate(ComponentContext context, Config config) throws OpenemsException {
        this.meterType = config.type();
        this.invert = config.invert();

        super.activate(
            context,
            config.id(),
            config.alias(),
            config.enabled(),
            config.modbusUnitId(),
            this.cm,
            MODBUS_SETTER_REFERENCE,
            config.modbus_id()
        );
    }
    
    @Override
    @Deactivate
    protected void deactivate() {
        super.deactivate();

        meterType = MeterType.PRODUCTION;
        invert = false;
    }
    
    @Override
    public MeterType getMeterType() {
        return meterType;
    }

    @Override
    protected ModbusProtocol defineModbusProtocol() {
        ModbusProtocol modbusProtocol = new ModbusProtocol(this,
            new FC3ReadRegistersTask(0x0400, Priority.HIGH,
                
                // voltage
                m(ElectricityMeter.ChannelId.VOLTAGE_L1,          new UnsignedDoublewordElement(0x0400)),
                m(ElectricityMeter.ChannelId.VOLTAGE_L2,          new UnsignedDoublewordElement(0x0402)),
                m(ElectricityMeter.ChannelId.VOLTAGE_L3,          new UnsignedDoublewordElement(0x0404)),
                m(MeterBgeTechDS100.ChannelId.VOLTAGE_L1_L2,      new UnsignedDoublewordElement(0x0406)),
                m(MeterBgeTechDS100.ChannelId.VOLTAGE_L2_L3,      new UnsignedDoublewordElement(0x0408)),
                m(MeterBgeTechDS100.ChannelId.VOLTAGE_L3_L1,      new UnsignedDoublewordElement(0x040A)),
                m(ElectricityMeter.ChannelId.VOLTAGE,             new UnsignedDoublewordElement(0x040C)),
                m(MeterBgeTechDS100.ChannelId.VOLTAGE_L_L,        new UnsignedDoublewordElement(0x040E)),

                // current
                m(ElectricityMeter.ChannelId.CURRENT_L1,          new SignedDoublewordElement(0x0410)),
                m(ElectricityMeter.ChannelId.CURRENT_L2,          new SignedDoublewordElement(0x0412)),
                m(ElectricityMeter.ChannelId.CURRENT_L3,          new SignedDoublewordElement(0x0414)),
                m(MeterBgeTechDS100.ChannelId.CURRENT_N,          new SignedDoublewordElement(0x0416)),
                m(ElectricityMeter.ChannelId.CURRENT,             new SignedDoublewordElement(0x0418)),

                // active power
                m(ElectricityMeter.ChannelId.ACTIVE_POWER_L1,     new SignedDoublewordElement(0x041A), INVERT_IF_TRUE(invert)),
                m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2,     new SignedDoublewordElement(0x041C), INVERT_IF_TRUE(invert)),
                m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3,     new SignedDoublewordElement(0x041E), INVERT_IF_TRUE(invert)),
                m(ElectricityMeter.ChannelId.ACTIVE_POWER,        new SignedDoublewordElement(0x0420), INVERT_IF_TRUE(invert)),

                // apparent power
                m(MeterBgeTechDS100.ChannelId.APPARENT_POWER_L1,  new UnsignedDoublewordElement(0x0422)),
                m(MeterBgeTechDS100.ChannelId.APPARENT_POWER_L2,  new UnsignedDoublewordElement(0x0424)),
                m(MeterBgeTechDS100.ChannelId.APPARENT_POWER_L3,  new UnsignedDoublewordElement(0x0426)),
                m(MeterBgeTechDS100.ChannelId.APPARENT_POWER,     new UnsignedDoublewordElement(0x0428)),
                
                // reactive power
                m(ElectricityMeter.ChannelId.REACTIVE_POWER_L1,   new SignedDoublewordElement(0x042A), INVERT_IF_TRUE(invert)),
                m(ElectricityMeter.ChannelId.REACTIVE_POWER_L2,   new SignedDoublewordElement(0x042C), INVERT_IF_TRUE(invert)),
                m(ElectricityMeter.ChannelId.REACTIVE_POWER_L3,   new SignedDoublewordElement(0x042E), INVERT_IF_TRUE(invert)),
                m(ElectricityMeter.ChannelId.REACTIVE_POWER,      new SignedDoublewordElement(0x0430), INVERT_IF_TRUE(invert)),
                
                // frequency
                m(MeterBgeTechDS100.ChannelId.FREQUENCY_L1,       new UnsignedWordElement(0x0432), SCALE_FACTOR_2),
                m(MeterBgeTechDS100.ChannelId.FREQUENCY_L2,       new UnsignedWordElement(0x0433), SCALE_FACTOR_2),
                m(MeterBgeTechDS100.ChannelId.FREQUENCY_L3,       new UnsignedWordElement(0x0434), SCALE_FACTOR_2),
                m(ElectricityMeter.ChannelId.FREQUENCY,           new UnsignedWordElement(0x0435), SCALE_FACTOR_2),

                // power factor
                m(MeterBgeTechDS100.ChannelId.POWER_FACTOR_L1,    new FloatWordElement(0x0436), SCALE_FACTOR_MINUS_3),
                m(MeterBgeTechDS100.ChannelId.POWER_FACTOR_L2,    new FloatWordElement(0x0437), SCALE_FACTOR_MINUS_3),
                m(MeterBgeTechDS100.ChannelId.POWER_FACTOR_L3,    new FloatWordElement(0x0438), SCALE_FACTOR_MINUS_3),
                m(MeterBgeTechDS100.ChannelId.POWER_FACTOR,       new FloatWordElement(0x0439), SCALE_FACTOR_MINUS_3)

            ),
            
            // total active and reactive power sum
            new FC3ReadRegistersTask(0x010E, Priority.LOW,
                
                // wtf ... they mixed names ... :-( ...  production is consumption and vice versa                
                m(ChannelMappings.TOTAL_FORWARD_ACTIVE_ENERGY.channelId(invert),     new SignedDoublewordElement(0x010E), SCALE_FACTOR_1),
                new DummyRegisterElement(0x0110, 0x0117),
                m(ChannelMappings.TOTAL_REVERSE_ACTIVE_ENERGY.channelId(invert),     new SignedDoublewordElement(0x0118), SCALE_FACTOR_1),
                new DummyRegisterElement(0x011A, 0x0121),                 
                m(MeterBgeTechDS100.ChannelId.ACTIVE_TOTAL_ENERGY,                   new SignedDoublewordElement(0x0122), SCALE_FACTOR_1),                
                new DummyRegisterElement(0x0124, 0x012B),
                
                // not sure, I swapped these now too ... but??
                m(ChannelMappings.TOTAL_FORWARD_REACTIVE_ENERGY.channelId(invert),   new SignedDoublewordElement(0x012C), SCALE_FACTOR_1),
                new DummyRegisterElement(0x012E, 0x0135),
                m(ChannelMappings.TOTAL_REVERSE_REACTIVE_ENERGY.channelId(invert),   new SignedDoublewordElement(0x0136), SCALE_FACTOR_1),
                new DummyRegisterElement(0x0138, 0x013F), 
                m(MeterBgeTechDS100.ChannelId.REACTIVE_TOTAL_ENERGY,                 new SignedDoublewordElement(0x0140), SCALE_FACTOR_1)
            ),
            
            // total active and reactive power L1
            new FC3ReadRegistersTask(0x0500, Priority.LOW,                
                m(MeterBgeTechDS100.ChannelId.ACTIVE_TOTAL_ENERGY_L1,                new SignedDoublewordElement(0x0500), SCALE_FACTOR_1),
                new DummyRegisterElement(0x0502, 0x0509),
                m(ChannelMappings.A_PHASE_FORWARD_ACTIVE_ENERGY.channelId(invert),   new UnsignedDoublewordElement(0x050A), SCALE_FACTOR_1),
                new DummyRegisterElement(0x050C, 0x0513),
                m(ChannelMappings.A_PHASE_REVERSE_ACTIVE_ENERGY.channelId(invert),   new UnsignedDoublewordElement(0x0514), SCALE_FACTOR_1),
                
                new DummyRegisterElement(0x0516, 0x051D),                
                m(MeterBgeTechDS100.ChannelId.REACTIVE_TOTAL_ENERGY_L1,              new SignedDoublewordElement(0x051E), SCALE_FACTOR_1),
                new DummyRegisterElement(0x0520, 0x0527),
                m(ChannelMappings.A_PHASE_FORWARD_REACTIVE_ENERGY.channelId(invert), new SignedDoublewordElement(0x0528), SCALE_FACTOR_1),
                new DummyRegisterElement(0x052A, 0x0531), 
                m(ChannelMappings.A_PHASE_REVERSE_REACTIVE_ENERGY.channelId(invert), new SignedDoublewordElement(0x0532), SCALE_FACTOR_1)                
            ),
            
            // total active and reactive power L2
            new FC3ReadRegistersTask(0x0564, Priority.LOW,                
                m(MeterBgeTechDS100.ChannelId.ACTIVE_TOTAL_ENERGY_L2,                new SignedDoublewordElement(0x0564), SCALE_FACTOR_1),
                new DummyRegisterElement(0x0566, 0x056D),
                m(ChannelMappings.B_PHASE_FORWARD_ACTIVE_ENERGY.channelId(invert),   new UnsignedDoublewordElement(0x056E), SCALE_FACTOR_1),
                new DummyRegisterElement(0x0570, 0x0577),
                m(ChannelMappings.B_PHASE_REVERSE_ACTIVE_ENERGY.channelId(invert),   new UnsignedDoublewordElement(0x0578), SCALE_FACTOR_1),
                
                new DummyRegisterElement(0x057A, 0x0581),                
                m(MeterBgeTechDS100.ChannelId.REACTIVE_TOTAL_ENERGY_L2,              new SignedDoublewordElement(0x0582), SCALE_FACTOR_1),
                new DummyRegisterElement(0x0584, 0x058B),
                m(ChannelMappings.B_PHASE_FORWARD_REACTIVE_ENERGY.channelId(invert), new SignedDoublewordElement(0x058C), SCALE_FACTOR_1),
                new DummyRegisterElement(0x058E, 0x0595), 
                m(ChannelMappings.B_PHASE_REVERSE_REACTIVE_ENERGY.channelId(invert), new SignedDoublewordElement(0x0596), SCALE_FACTOR_1)               
            ),
            
            // total active and reactive power L3
            new FC3ReadRegistersTask(0x05C8, Priority.LOW,                
                m(MeterBgeTechDS100.ChannelId.ACTIVE_TOTAL_ENERGY_L3,                new SignedDoublewordElement(0x05C8), SCALE_FACTOR_1),
                new DummyRegisterElement(0x05CA, 0x05D1),
                m(ChannelMappings.C_PHASE_FORWARD_ACTIVE_ENERGY.channelId(invert),   new UnsignedDoublewordElement(0x05D2), SCALE_FACTOR_1),
                new DummyRegisterElement(0x05D4, 0x05DB),                
                m(ChannelMappings.C_PHASE_REVERSE_ACTIVE_ENERGY.channelId(invert),   new UnsignedDoublewordElement(0x05DC), SCALE_FACTOR_1),
                
                new DummyRegisterElement(0x05DE, 0x05E5),
                m(MeterBgeTechDS100.ChannelId.REACTIVE_TOTAL_ENERGY_L3,              new SignedDoublewordElement(0x05E6), SCALE_FACTOR_1),
                new DummyRegisterElement(0x05E8, 0x05EF),
                m(ChannelMappings.C_PHASE_FORWARD_REACTIVE_ENERGY.channelId(invert), new SignedDoublewordElement(0x05F0), SCALE_FACTOR_1),
                new DummyRegisterElement(0x05F2, 0x05F9), 
                m(ChannelMappings.C_PHASE_REVERSE_REACTIVE_ENERGY.channelId(invert), new SignedDoublewordElement(0x05FA), SCALE_FACTOR_1)                
            )

        );

        return modbusProtocol;
    }

    @Override
    public String debugLog() {
        return "power:" + getActivePower().asString()
            + ", ate:" + getActiveTotalEnergy().asString()
            + ", ace:" + getActiveConsumptionEnergy().asString()
            + ", ape:" + getActiveProductionEnergy().asString();
    }

    @Override
    public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {   
        return new ModbusSlaveTable(
            OpenemsComponent.getModbusSlaveNatureTable(accessMode),
            ElectricityMeter.getModbusSlaveNatureTable(accessMode)
        );       
        
    }
	
    
    public LongReadChannel getActiveTotalEnergyChannel() {
        return this.channel(MeterBgeTechDS100.ChannelId.ACTIVE_TOTAL_ENERGY);
    }
    
    public LongReadChannel getActiveTotalEnergyL1Channel() {
        return this.channel(MeterBgeTechDS100.ChannelId.ACTIVE_TOTAL_ENERGY);
    }
    
    public LongReadChannel getActiveTotalEnergyL2Channel() {
        return this.channel(MeterBgeTechDS100.ChannelId.ACTIVE_TOTAL_ENERGY);
    }
    
    public LongReadChannel getActiveTotalEnergyL3Channel() {
        return this.channel(MeterBgeTechDS100.ChannelId.ACTIVE_TOTAL_ENERGY);
    }
    
    public Value<Long> getActiveTotalEnergy() {
        return this.getActiveTotalEnergyChannel().value();
    }
    
    public Value<Long> getActiveTotalEnergyL1() {
        return this.getActiveTotalEnergyL1Channel().value();
    }
    
    public Value<Long> getActiveTotalEnergyL2() {
        return this.getActiveTotalEnergyL2Channel().value();
    }
    
    public Value<Long> getActiveTotalEnergyL3() {
        return this.getActiveTotalEnergyL3Channel().value();
    }    

}
