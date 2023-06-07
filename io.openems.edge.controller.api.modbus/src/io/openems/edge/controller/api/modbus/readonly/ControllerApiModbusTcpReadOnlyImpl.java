package io.openems.edge.controller.api.modbus.readonly;

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

import com.ghgande.j2mod.modbus.ModbusException;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.api.modbus.AbstractModbusTcpApi;
import io.openems.edge.controller.api.modbus.ModbusTcpApi;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Api.ModbusTcp.ReadOnly", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerApiModbusTcpReadOnlyImpl extends AbstractModbusTcpApi
		implements ControllerApiModbusTcpReadOnly, ModbusTcpApi, Controller, OpenemsComponent, JsonApi {

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private Meta metaComponent = null;

	@Reference
	private ConfigurationAdmin cm;

	@Override
	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
	protected void addComponent(OpenemsComponent component) {
		super.addComponent(component);
	}

	protected void removeComponent(OpenemsComponent component) {
		super.removeComponent(component);
	}

	public ControllerApiModbusTcpReadOnlyImpl() {
		super("Modbus/TCP-Api Read-Only", //
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ModbusTcpApi.ChannelId.values(), //
				ControllerApiModbusTcpReadOnly.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws ModbusException, OpenemsException {
		super.activate(context, config.id(), config.alias(), config.enabled(), this.cm,
				new ConfigRecord(this.metaComponent, config.component_ids(), 0 /* no timeout */, config.port(),
						config.maxConcurrentConnections()));
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
}
