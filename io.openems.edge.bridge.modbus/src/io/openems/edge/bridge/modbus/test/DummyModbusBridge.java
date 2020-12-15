package io.openems.edge.bridge.modbus.test;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.BridgeModbusTcp;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;

public class DummyModbusBridge extends AbstractOpenemsComponent
		implements BridgeModbusTcp, BridgeModbus, OpenemsComponent {

	private final Map<String, ModbusProtocol> protocols = new HashMap<>();

	public DummyModbusBridge(String id) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				BridgeModbus.ChannelId.values(), //
				BridgeModbusTcp.ChannelId.values() //
		);
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}
		super.activate(null, id, "", true);
	}

	@Override
	public void addProtocol(String sourceId, ModbusProtocol protocol) {
		this.protocols.put(sourceId, protocol);
	}

	@Override
	public void removeProtocol(String sourceId) {
		this.protocols.remove(sourceId);
	}

	@Override
	public InetAddress getIpAddress() {
		return null;
	}

}
