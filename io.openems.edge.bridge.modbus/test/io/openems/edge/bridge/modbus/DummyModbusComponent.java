package io.openems.edge.bridge.modbus;

import org.osgi.framework.Constants;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.ConfigUtils;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.DummyComponentContext;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.common.test.DummyConfigurationAdmin.DummyConfiguration;

public class DummyModbusComponent extends AbstractOpenemsModbusComponent implements ModbusComponent {

	public static final String DEFAULT_COMPONENT_ID = "device0";
	public static final String DEFAULT_BRIDGE_ID = "modbus0";
	public static final int DEFAULT_UNIT_ID = 1;

	public DummyModbusComponent() throws OpenemsException {
		this(DEFAULT_COMPONENT_ID, DEFAULT_BRIDGE_ID);
	}

	public DummyModbusComponent(String id, String bridgeId) throws OpenemsException {
		this(id, new DummyModbusBridge(bridgeId), DEFAULT_UNIT_ID, new io.openems.edge.common.channel.ChannelId[0]);
	}

	public DummyModbusComponent(String id, BridgeModbus bridge, int unitId,
			io.openems.edge.common.channel.ChannelId[] additionalChannelIds) throws OpenemsException {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				additionalChannelIds //
		);
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}
		this.setModbus(bridge);
		var context = new DummyComponentContext();
		context.addProperty(Constants.SERVICE_PID, Constants.SERVICE_PID);
		var cm = new DummyConfigurationAdmin();
		var dummyConfiguration = new DummyConfiguration();
		dummyConfiguration.addProperty("Modbus.target",
				ConfigUtils.generateReferenceTargetFilter(Constants.SERVICE_PID, bridge.id()));
		cm.addConfiguration(Constants.SERVICE_PID, dummyConfiguration);
		super.activate(context, id, "", true, unitId, cm, "Modbus", bridge.id());
	}

	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		return new ModbusProtocol(this);
	}

	@Override
	public ModbusProtocol getModbusProtocol() throws OpenemsException {
		return super.getModbusProtocol();
	}

}
