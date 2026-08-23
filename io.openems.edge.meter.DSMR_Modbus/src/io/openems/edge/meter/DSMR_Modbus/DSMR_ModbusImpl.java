package io.openems.edge.meter.DSMR_Modbus;

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

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.common.types.MeterType;

@Designate(ocd = Config.class, factory = true) 
@Component(
		name = "Meter.DSMR", 
		immediate = true, 
		configurationPolicy = ConfigurationPolicy.REQUIRE 
)
public class DSMR_ModbusImpl extends AbstractOpenemsModbusComponent 
		implements DSMR_Modbus, ElectricityMeter, OpenemsComponent, ModbusComponent { 

	@Reference
	private ConfigurationAdmin cm; 

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus); 
	}

	private Config config = null;

	public DSMR_ModbusImpl() {
		super(
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				DSMR_Modbus.ChannelId.values() //
		);
		
		ElectricityMeter.calculateSumActivePowerFromPhases(this);
		ElectricityMeter.calculateSumCurrentFromPhases(this);
		ElectricityMeter.calculateAverageVoltageFromPhases(this);
		
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException { 
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this.config = config;
	}

	@Override
	@Deactivate
	protected void deactivate() { 
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() { 
		return new ModbusProtocol(this, 
				new FC3ReadRegistersTask(18, Priority.HIGH,
						m(ElectricityMeter.ChannelId.VOLTAGE_L1 , new UnsignedDoublewordElement(18)),
						m(ElectricityMeter.ChannelId.VOLTAGE_L2 , new SignedDoublewordElement(20)),
						m(ElectricityMeter.ChannelId.VOLTAGE_L3 , new SignedDoublewordElement(22)),
						m(ElectricityMeter.ChannelId.CURRENT_L1 , new SignedDoublewordElement(24)),
						m(ElectricityMeter.ChannelId.CURRENT_L2 , new SignedDoublewordElement(26)),
						m(ElectricityMeter.ChannelId.CURRENT_L3 , new SignedDoublewordElement(28))
						),
				
				new FC3ReadRegistersTask(38, Priority.HIGH, 
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, new SignedDoublewordElement(38)),
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, new SignedDoublewordElement(40)),
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, new SignedDoublewordElement(42))
						)
				);
		
	}

	@Override
	public MeterType getMeterType() { 
		return this.config.type();
	}

	@Override
	public String debugLog() { 
		return "L:" + this.getActivePower().asString();
	}
}