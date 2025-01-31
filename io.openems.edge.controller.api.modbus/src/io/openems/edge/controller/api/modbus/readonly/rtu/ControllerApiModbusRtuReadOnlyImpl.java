package io.openems.edge.controller.api.modbus.readonly.rtu;

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
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.ComponentJsonApi;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.api.modbus.AbstractModbusRtuApi;
import io.openems.edge.controller.api.modbus.ModbusApi;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Api.ModbusRtu.ReadOnly", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerApiModbusRtuReadOnlyImpl extends AbstractModbusRtuApi
		implements ControllerApiModbusRtuReadOnly, ModbusApi, Controller, OpenemsComponent, ComponentJsonApi {

	@Reference
	private Meta metaComponent = null;

	@Reference
	private ConfigurationAdmin cm;

	private RtuConfig config;

	public ControllerApiModbusRtuReadOnlyImpl() {
		super("Modbus/RTU-Api Read-Only", //
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ModbusApi.ChannelId.values(), //
				ControllerApiModbusRtuReadOnly.ChannelId.values() //
		);
	}

	@Override
	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
	protected void addComponent(OpenemsComponent component) {
		super.addComponent(component);
	}

	protected void removeComponent(OpenemsComponent component) {
		super.removeComponent(component);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = new RtuConfig(config.id(), config.alias(), config.enabled(), this.metaComponent,
				config.component_ids(), 0 /* no timeout */, config.portName(), config.baudRate(), config.databits(),
				config.stopbits(), config.parity(), config.maxConcurrentConnections());
		super.activate(context, this.cm, this.config);
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsException {
		this.config = new RtuConfig(config.id(), config.alias(), config.enabled(), this.metaComponent,
				config.component_ids(), 0 /* no timeout */, config.portName(), config.baudRate(), config.databits(),
				config.stopbits(), config.parity(), config.maxConcurrentConnections());
		super.modified(context, this.cm, this.config);
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
		return AccessMode.READ_ONLY;
	}

}