package io.openems.edge.evcs.evtec.core;

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

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evcs.EvTec.Core", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class EvTecCoreImpl extends AbstractOpenemsModbusComponent
		implements OpenemsComponent, EvTecCore, ModbusComponent {

	// private Config config;

	public EvTecCoreImpl() {
		super(OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				EvTecCore.ChannelId.values() //
		);
	}

	@Reference
	protected ConfigurationAdmin cm;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		// this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		// this.getComTimeoutEnabledChannel().setNextValue(config.comTimeoutEnabled());
		this.getComTimeoutEnabledChannel().setNextWriteValue(config.comTimeoutEnabled());
		// this.getComTimeoutValueChannel().setNextValue(config.comTimeoutValue());
		this.getComTimeoutValueChannel().setNextWriteValue(config.comTimeoutValue());
		// this.getFallbackPowerChannel().setNextValue(config.fallbackPower());
		this.getFallbackPowerChannel().setNextWriteValue(config.fallbackPower());

	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		return new ModbusProtocol(this,
				new FC3ReadRegistersTask(100, Priority.LOW,
						this.m(EvTecCore.ChannelId.CHARGER_STATE, new UnsignedWordElement(100)),
						this.m(EvTecCore.ChannelId.CHARGER_VERISON, new UnsignedWordElement(101)),
						this.m(EvTecCore.ChannelId.CHARGER_NO_OF_CONNECTORS, new UnsignedWordElement(102)),
						this.m(EvTecCore.ChannelId.CHARGER_ERROR, new UnsignedQuadruplewordElement(103))),
				new FC3ReadRegistersTask(110, Priority.LOW,
						this.m(EvTecCore.ChannelId.CHARGER_SERIAL, new StringWordElement(110, 20)),
						this.m(EvTecCore.ChannelId.CHARGER_MODEL, new StringWordElement(130, 10))),
				new FC3ReadRegistersTask(201, Priority.HIGH,
						this.m(EvTecCore.ChannelId.COM_TIMEOUT_ENABLED, new UnsignedWordElement(201)),
						this.m(EvTecCore.ChannelId.COM_TIMEOUT_VALUE, new UnsignedWordElement(202)),
						this.m(EvTecCore.ChannelId.FALLBACK_POWER, new UnsignedDoublewordElement(203))),

				new FC16WriteRegistersTask(201,
						this.m(EvTecCore.ChannelId.COM_TIMEOUT_ENABLED, new UnsignedWordElement(201)),
						this.m(EvTecCore.ChannelId.COM_TIMEOUT_VALUE, new UnsignedWordElement(202)),
						this.m(EvTecCore.ChannelId.FALLBACK_POWER, new UnsignedDoublewordElement(203))));

	}

}