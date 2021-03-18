package io.openems.edge.controller.api.modbus.readonlySerial;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.slave.ModbusSlaveFactory;
import com.ghgande.j2mod.modbus.util.SerialParameters;
import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.api.modbus.AbstractModbusApi;
import io.openems.edge.controller.api.modbus.ModbusApi;
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

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Api.ModbusSerial.ReadOnly", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE)
public class ModbusSerialApiReadOnlyImpl extends AbstractModbusApi
		implements ModbusSerialApiReadOnly, ModbusApi, Controller, OpenemsComponent, JsonApi {

	private SerialParameters serialParameters;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected Meta metaComponent = null;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
	protected void addComponent(ModbusSlave component) {
		super.addComponent(component);
	}

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	protected ComponentManager cpm;

	public ModbusSerialApiReadOnlyImpl() {
		super("Modbus/Serial-Api Read-Only", //
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ModbusApi.ChannelId.values(), //
				ModbusSerialApiReadOnly.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws ModbusException, OpenemsException {
		super.activate(context, config.id(), config.alias(), config.enabled(), this.cm, this.cpm, this.metaComponent,
				config.component_ids(), 0 /* no timeout */, config.port());
		serialParameters = new SerialParameters(config.port(), config.baudRate(), config.flowControlIn().getValue(),
				config.flowControlOut().getValue(), config.databits(), config.stopbits().getValue(), config.parity().getValue(), config.echo());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	/**
	 * Creates the Modbus slave.
	 *
	 * @return the {@link ModbusSlave}
	 */
	@Override
	protected com.ghgande.j2mod.modbus.slave.ModbusSlave createModbusSlave() throws ModbusException {
		return ModbusSlaveFactory.createSerialSlave(serialParameters);
	};

	@Override
	protected AccessMode getAccessMode() {
		return AccessMode.READ_ONLY;
	}
}
