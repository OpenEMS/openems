package io.openems.edge.bridge.modbus.test;

import java.util.Hashtable;

import org.osgi.framework.Constants;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.DummyComponentContext;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.DummyConfigurationAdmin.DummyConfiguration;

public abstract class DummyModbusComponent extends AbstractOpenemsModbusComponent
		implements ModbusComponent, OpenemsComponent {

	public DummyModbusComponent(String id, io.openems.edge.common.channel.ChannelId[] additionalChannels)
			throws OpenemsException {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				additionalChannels //
		);
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}
		Hashtable<String, Object> contextProperties = new Hashtable<>();
		contextProperties.put(Constants.SERVICE_PID, id);
		DummyComponentContext context = new DummyComponentContext(contextProperties);
		DummyConfigurationAdmin cm = new DummyConfigurationAdmin();
		DummyConfiguration config = cm.getOrCreateEmptyConfiguration(id);
		config.addProperty("Modbus.target", "(&(enabled=true)(!(service.pid=id0))(|(id=modbus0)))");
		this.setModbus(new DummyModbusBridge("modbus0"));

		super.activate(/* context */ context, /* id */ id, /* alias */ "", /* enabled */ true, /* unitid */ 1,
				/* ConfigurationAdmin */ cm, /* modbus reference */ "Modbus", /* Modbus-ID */ "modbus0");
	}

}
