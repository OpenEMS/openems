
package io.openems.edge.meter.EMH;

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
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.EMH", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class MeterEMHImpl extends AbstractOpenemsModbusComponent
		implements MeterEMH, SymmetricMeter, AsymmetricMeter, ModbusComponent, OpenemsComponent, ModbusSlave {

	private MeterType meterType = MeterType.PRODUCTION;



	@Reference
	protected ConfigurationAdmin cm;

	public MeterEMHImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				AsymmetricMeter.ChannelId.values(), //
				MeterEMH.ChannelId.values() //
		);
	}



	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		this.meterType = config.type();
		
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		//this.config = config;
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public MeterType getMeterType() {
		return this.meterType;
		//return this.config.type();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(8, Priority.HIGH, //
					m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L1, new SignedDoublewordElement(8),SIGNED_POWER_CONVERTER_AND_INVERT),	
					m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L2, new SignedDoublewordElement(10),SIGNED_POWER_CONVERTER_AND_INVERT),	
					m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3, new SignedDoublewordElement(12),SIGNED_POWER_CONVERTER_AND_INVERT),					
					m(SymmetricMeter.ChannelId.ACTIVE_POWER, new SignedDoublewordElement(14),SIGNED_POWER_CONVERTER_AND_INVERT),
					m(SymmetricMeter.ChannelId.REACTIVE_POWER, new SignedDoublewordElement(16),SIGNED_POWER_CONVERTER_AND_INVERT),
					new DummyRegisterElement(18, 19), // Reserved
					m(SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new SignedDoublewordElement(20),SIGNED_POWER_CONVERTER_AND_INVERT),
					m(SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new SignedDoublewordElement(22),SIGNED_POWER_CONVERTER_AND_INVERT)
						)
			);
	}	
	
	
	private static final ElementToChannelConverter SIGNED_POWER_CONVERTER_AND_INVERT = new ElementToChannelConverter(//
			value -> {
				if (value == null) {
					return null;
				}
				long intValue = (Long) value;
				if (intValue == -10_000) {
					return 0; // ignore '-10_000'
				}
				return intValue * 2000; // invert
			}, //
			value -> value);
	
	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricMeter.getModbusSlaveNatureTable(accessMode), //
				AsymmetricMeter.getModbusSlaveNatureTable(accessMode) //
		);
	}
}
