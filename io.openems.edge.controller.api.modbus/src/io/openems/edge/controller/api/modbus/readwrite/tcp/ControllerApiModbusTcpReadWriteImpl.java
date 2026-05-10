package io.openems.edge.controller.api.modbus.readwrite.tcp;

import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
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
import com.ghgande.j2mod.modbus.slave.ModbusSlaveFactory;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.ComponentJsonApi;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.api.modbus.CommonConfig;
import io.openems.edge.controller.api.modbus.ModbusApi;
import io.openems.edge.controller.api.modbus.readwrite.AbstractModbusReadWriteApi;
import io.openems.edge.timedata.api.Timedata;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Api.ModbusTcp.ReadWrite", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@GenerateTargetsFromReferences("Component")
public class ControllerApiModbusTcpReadWriteImpl extends AbstractModbusReadWriteApi
		implements ControllerApiModbusTcpReadWrite, ModbusApi, Controller, OpenemsComponent, ComponentJsonApi {

	private volatile CommonConfig.Tcp config;

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference
	private Meta metaComponent;

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private ComponentManager componentManager;

	@Reference(//
			policy = DYNAMIC, policyOption = GREEDY, cardinality = MULTIPLE, //
			target = "(&(id=${config.component_ids})(enabled=true)(!(service.pid=${config.service_pid})))")
	protected void addComponent(OpenemsComponent component) {
		super._addComponent(component);
	}

	protected void removeComponent(OpenemsComponent component) {
		super._removeComponent(component);
	}

	public ControllerApiModbusTcpReadWriteImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ModbusApi.ChannelId.values(), //
				AbstractModbusReadWriteApi.ChannelId.values(), //
				ControllerApiModbusTcpReadWrite.ChannelId.values() //
		);
		this.apiWorker.setLogChannel(this.getApiWorkerLogChannel());
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = CommonConfig.Tcp.from(config, this.metaComponent);
		super.activate(context, this.config, this.componentManager.getClock());
		this.applyConfig(config.writeChannels());
	}

	@Modified
	private void modified(ComponentContext context, Config config) {
		this.config = CommonConfig.Tcp.from(config, this.metaComponent);
		super.modified(context, this.config, this.componentManager.getClock());
		this.applyConfig(config.writeChannels());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ConfigurationAdmin getConfigurationAdmin() {
		return this.cm;
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	protected Integer getChannelValue(String componentId, io.openems.edge.common.channel.ChannelId channelId) {
		@SuppressWarnings("deprecation")
		var channel = this._channel(getChannelNameCamel(componentId, channelId));
		if (channel == null) {
			return null;
		}
		return ((IntegerReadChannel) channel).value().get();
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(AccessMode.READ_ONLY), //
				AbstractModbusReadWriteApi.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(ControllerApiModbusTcpReadWrite.class, AccessMode.READ_ONLY, 100) //
						.build());
	}

	@Override
	protected com.ghgande.j2mod.modbus.slave.ModbusSlave createSlave() throws ModbusException {
		return ModbusSlaveFactory.createTCPSlave(//
				/* listen address */ null, //
				/* port */ this.config.port(), //
				/* poolSize */ this.config.maxConcurrentConnections(), //
				/* useRtuOverTcp */ false, //
				/* maxIdleSeconds */ MAX_IDLE_SECONDS);
	}
}
