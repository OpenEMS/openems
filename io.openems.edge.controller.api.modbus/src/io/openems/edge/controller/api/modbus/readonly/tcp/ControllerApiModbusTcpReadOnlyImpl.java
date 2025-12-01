package io.openems.edge.controller.api.modbus.readonly.tcp;

import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.slave.ModbusSlave;
import com.ghgande.j2mod.modbus.slave.ModbusSlaveFactory;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
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
		name = "Controller.Api.ModbusTcp.ReadOnly", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerApiModbusTcpReadOnlyImpl extends AbstractModbusApi
		implements ControllerApiModbusTcpReadOnly, ModbusApi, Controller, OpenemsComponent, ComponentJsonApi {

	@Reference
	private Meta metaComponent;

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private ComponentManager componentManager;

	private CommonConfig.Tcp config;

	@Override
	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = MULTIPLE)
	protected void addComponent(OpenemsComponent component) {
		super.addComponent(component);
	}

	@Override
	protected void removeComponent(OpenemsComponent component) {
		super.removeComponent(component);
	}

	public ControllerApiModbusTcpReadOnlyImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ModbusApi.ChannelId.values(), //
				ControllerApiModbusTcpReadOnly.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws ModbusException, OpenemsException {
		this.config = CommonConfig.Tcp.from(config, this.metaComponent);
		super.activate(context, this.cm, this.config, this.componentManager.getClock());
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		this.config = CommonConfig.Tcp.from(config, this.metaComponent);
		super.modified(context, this.cm, this.config, this.componentManager.getClock());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected AccessMode getAccessMode() {
		return AccessMode.READ_ONLY;
	}

	@Override
	protected ModbusSlave createSlave() throws ModbusException {
		return ModbusSlaveFactory.createTCPSlave(//
				/* listen address */ null, //
				/* port */ this.config.port(), //
				/* poolSize */ this.config.maxConcurrentConnections(), //
				/* useRtuOverTcp */ false, //
				/* maxIdleSeconds */ MAX_IDLE_SECONDS);
	}
}
