package io.openems.edge.controller.api.modbus.readonly;

import java.util.Map.Entry;
import java.util.function.Consumer;

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
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.ComponentJsonApi;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.api.common.Status;
import io.openems.edge.controller.api.common.WriteObject;
import io.openems.edge.controller.api.modbus.AbstractModbusTcpApi;
import io.openems.edge.controller.api.modbus.ModbusTcpApi;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Api.ModbusTcp.ReadOnly", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerApiModbusTcpReadOnlyImpl extends AbstractModbusTcpApi
		implements ControllerApiModbusTcpReadOnly, ModbusTcpApi, Controller, OpenemsComponent, ComponentJsonApi {

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
		super.activate(context, this.cm,
				new ConfigRecord(config.id(), config.alias(), config.enabled(),this.metaComponent, config.component_ids(), 0 /* no timeout */, config.port(),
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

	@Override
	protected Consumer<Entry<WriteChannel<?>, WriteObject>> handleWrites() {
		return entry -> { };
	}

	@Override
	protected void setOverrideStatus(Status status) {
	}

	@Override
	protected Runnable handleTimeouts() {
		return () -> { };
	}
}
