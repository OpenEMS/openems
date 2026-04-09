package io.openems.edge.controller.api.modbus.readonly.rtu;

import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import com.ghgande.j2mod.modbus.Modbus;
import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.slave.ModbusSlave;
import com.ghgande.j2mod.modbus.slave.ModbusSlaveFactory;
import com.ghgande.j2mod.modbus.util.SerialParameters;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.ComponentJsonApi;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.api.modbus.AbstractModbusApi;
import io.openems.edge.controller.api.modbus.CommonConfig;
import io.openems.edge.controller.api.modbus.ModbusApi;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Api.ModbusRtu.ReadOnly", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@GenerateTargetsFromReferences("Component")
public class ControllerApiModbusRtuReadOnlyImpl extends AbstractModbusApi
		implements ControllerApiModbusRtuReadOnly, ModbusApi, Controller, OpenemsComponent, ComponentJsonApi {

	@Reference
	private Meta metaComponent;

	@Reference
	private ComponentManager componentManager;

	private CommonConfig.Rtu config;

	public ControllerApiModbusRtuReadOnlyImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ModbusApi.ChannelId.values(), //
				ControllerApiModbusRtuReadOnly.ChannelId.values() //
		);
	}

	@Reference(//
			policy = DYNAMIC, policyOption = GREEDY, cardinality = MULTIPLE, //
			target = "(&(id=${config.component_ids})(enabled=true)(!(service.pid=${config.service_pid})))")
	protected void addComponent(OpenemsComponent component) {
		super._addComponent(component);
	}

	protected void removeComponent(OpenemsComponent component) {
		super._removeComponent(component);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = CommonConfig.Rtu.from(config, this.metaComponent);
		super.activate(context, this.config, this.componentManager.getClock());
	}

	@Modified
	private void modified(ComponentContext context, Config config) {
		this.config = CommonConfig.Rtu.from(config, this.metaComponent);
		super.modified(context, this.config, this.componentManager.getClock());
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
