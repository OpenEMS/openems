package io.openems.edge.controller.api.modbus.readwrite.rtu;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;

import com.ghgande.j2mod.modbus.Modbus;
import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.slave.ModbusSlave;
import com.ghgande.j2mod.modbus.slave.ModbusSlaveFactory;
import com.ghgande.j2mod.modbus.util.SerialParameters;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.ComponentJsonApi;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.modbusslave.ModbusType;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.api.modbus.AbstractModbusRtuApi;
import io.openems.edge.controller.api.modbus.ModbusApi;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Api.ModbusRtu.ReadWrite", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerApiModbusRtuReadWriteImpl extends AbstractModbusRtuApi
		implements ControllerApiModbusRtuReadWrite, ModbusApi, Controller, OpenemsComponent, ComponentJsonApi,
		io.openems.edge.common.modbusslave.ModbusSlave {

	@Reference
	private Meta metaComponent;

	@Reference
	private ConfigurationAdmin cm;

	private RtuConfig config;

	@Reference
	private ComponentManager componentManager;

	public ControllerApiModbusRtuReadWriteImpl() {
		super("Modbus/RTU-Api Read-Write", //
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ModbusApi.ChannelId.values(), //
				ControllerApiModbusRtuReadWrite.ChannelId.values() //
		);
		this.apiWorker.setLogChannel(this.getApiWorkerLogChannel());
	}

	@Override
	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
	protected void addComponent(OpenemsComponent component) {
		super.addComponent(component);
	}

	@Override
	protected void removeComponent(OpenemsComponent component) {
		super.removeComponent(component);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = new RtuConfig(config.id(), config.alias(), config.enabled(), this.metaComponent,
				config.component_ids(), config.apiTimeout(), config.portName(), config.baudRate(), config.databits(),
				config.stopbits(), config.parity(), config.maxConcurrentConnections());
		super.activate(context, this.cm, this.config, this.componentManager.getClock());
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsException {
		this.config = new RtuConfig(config.id(), config.alias(), config.enabled(), this.metaComponent,
				config.component_ids(), config.apiTimeout(), config.portName(), config.baudRate(), config.databits(),
				config.stopbits(), config.parity(), config.maxConcurrentConnections());
		super.modified(context, this.cm, this.config, this.componentManager.getClock());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusSlave createSlave() throws ModbusException {
		SerialParameters params = new SerialParameters();
		params.setPortName(this.config.portName());
		params.setBaudRate(this.config.baudRate());
		params.setDatabits(this.config.databits());
		params.setStopbits(this.config.stopbits().getValue());
		params.setParity(this.config.parity().getValue());
		params.setEncoding(Modbus.SERIAL_ENCODING_RTU);
		params.setEcho(false);
		return ModbusSlaveFactory.createSerialSlave(params);
	}

	@Override
	protected AccessMode getAccessMode() {
		return AccessMode.READ_WRITE;
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(ControllerApiModbusRtuReadWrite.class, accessMode, 100) //
						.channel(0, ModbusApi.ChannelId.UNABLE_TO_START, ModbusType.UINT16) //
						.channel(1, ModbusApi.ChannelId.COMPONENT_MISSING_FAULT, ModbusType.UINT16) //
						.channel(2, ModbusApi.ChannelId.PROCESS_IMAGE_FAULT, ModbusType.UINT16) //
						.channel(3, ModbusApi.ChannelId.COMPONENT_NO_MODBUS_API_FAULT, ModbusType.UINT16) //
						.channel(4, ControllerApiModbusRtuReadWrite.ChannelId.DEBUG_SET_ACTIVE_POWER_EQUALS,
								ModbusType.FLOAT32) //
						.channel(6, ControllerApiModbusRtuReadWrite.ChannelId.DEBUG_SET_ACTIVE_POWER_GREATER_OR_EQUALS,
								ModbusType.FLOAT32) //
						.channel(8, ControllerApiModbusRtuReadWrite.ChannelId.DEBUG_SET_ACTIVE_POWER_LESS_OR_EQUALS,
								ModbusType.FLOAT32) //
						.channel(10, ControllerApiModbusRtuReadWrite.ChannelId.DEBUG_SET_REACTIVE_POWER_EQUALS,
								ModbusType.FLOAT32) //
						.channel(12,
								ControllerApiModbusRtuReadWrite.ChannelId.DEBUG_SET_REACTIVE_POWER_GREATER_OR_EQUALS,
								ModbusType.FLOAT32) //
						.channel(14, ControllerApiModbusRtuReadWrite.ChannelId.DEBUG_SET_REACTIVE_POWER_LESS_OR_EQUALS,
								ModbusType.FLOAT32) //
						.build()); //
	}
}