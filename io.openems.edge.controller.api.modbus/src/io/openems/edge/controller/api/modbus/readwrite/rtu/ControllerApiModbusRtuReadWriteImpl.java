package io.openems.edge.controller.api.modbus.readwrite.rtu;

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
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.modbusslave.ModbusType;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.api.modbus.CommonConfig;
import io.openems.edge.controller.api.modbus.ModbusApi;
import io.openems.edge.controller.api.modbus.readwrite.AbstractModbusReadWriteApi;
import io.openems.edge.timedata.api.Timedata;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Api.ModbusRtu.ReadWrite", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@GenerateTargetsFromReferences("Component")
public class ControllerApiModbusRtuReadWriteImpl extends AbstractModbusReadWriteApi
		implements ControllerApiModbusRtuReadWrite, ModbusApi, Controller, OpenemsComponent, ComponentJsonApi {

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference
	private Meta metaComponent;

	@Reference
	private ConfigurationAdmin cm;

	private CommonConfig.Rtu config;

	@Reference
	private ComponentManager componentManager;

	public ControllerApiModbusRtuReadWriteImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ModbusApi.ChannelId.values(), //
				AbstractModbusReadWriteApi.ChannelId.values(), //
				ControllerApiModbusRtuReadWrite.ChannelId.values() //
		);
		this.apiWorker.setLogChannel(this.getApiWorkerLogChannel());
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
		this.applyConfig(config.writeChannels());
	}

	@Modified
	private void modified(ComponentContext context, Config config) {
		this.config = CommonConfig.Rtu.from(config, this.metaComponent);
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
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(ControllerApiModbusRtuReadWrite.class, accessMode, 100) //
						.channel(0, ModbusApi.ChannelId.UNABLE_TO_START, ModbusType.UINT16) //
						.channel(1, ModbusApi.ChannelId.COMPONENT_MISSING_FAULT, ModbusType.UINT16) //
						.channel(2, ModbusApi.ChannelId.PROCESS_IMAGE_FAULT, ModbusType.UINT16) //
						.channel(3, ModbusApi.ChannelId.COMPONENT_NO_MODBUS_API_FAULT, ModbusType.UINT16) //
						.build(), //
				AbstractModbusReadWriteApi.getModbusSlaveNatureTable(accessMode));
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}
}