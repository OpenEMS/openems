package io.openems.edge.controller.api.modbus.rtu.readwrite;

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
import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.ComponentJsonApi;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.api.modbus.rtu.AbstractModbusRtuApi;
import io.openems.edge.controller.api.modbus.rtu.ModbusRtuApi;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Api.ModbusRtu.ReadWrite", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerApiModbusRtuReadWriteImpl extends AbstractModbusRtuApi
		implements ControllerApiModbusRtuReadWrite, ModbusRtuApi, Controller, OpenemsComponent, ComponentJsonApi {

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private Meta metaComponent = null;

	@Reference
	private ConfigurationAdmin cm;

	public ControllerApiModbusRtuReadWriteImpl() {
		super("Modbus/RTU-Api Read-Write", //
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ModbusRtuApi.ChannelId.values(), //
				ControllerApiModbusRtuReadWrite.ChannelId.values() //
				);
		this.apiWorker.setLogChannel(this.getApiWorkerLogChannel());
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
	protected void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		super.modified(context, this.cm,
				new ConfigRecord(config.id(), config.alias(), config.enabled(), this.metaComponent,
						config.component_ids(), config.apiTimeout(), config.port(), config.maxConcurrentConnections()));
	}
	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected AccessMode getAccessMode() {
		return AccessMode.READ_WRITE;
	}
}