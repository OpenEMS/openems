package io.openems.edge.bridge.modbus.test;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import com.ghgande.j2mod.modbus.io.ModbusTransaction;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractModbusBridge;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.BridgeModbusTcp;
import io.openems.edge.bridge.modbus.api.LogVerbosity;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.cycle.Cycle;
import io.openems.edge.common.test.DummyCycle;

public class DummyModbusBridge extends AbstractModbusBridge implements BridgeModbusTcp, BridgeModbus, OpenemsComponent {

	private final Map<String, ModbusProtocol> protocols = new HashMap<>();

	private final Cycle cycle = new DummyCycle(1000);

	public DummyModbusBridge(String id) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				BridgeModbus.ChannelId.values(), //
				BridgeModbusTcp.ChannelId.values() //
		);
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}
		super.activate(null, id, "", true, LogVerbosity.NONE, 2);
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

	@Override
	public Cycle getCycle() {
		return this.cycle;
	}

	@Override
	public ModbusTransaction getNewModbusTransaction() throws OpenemsException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void closeModbusConnection() {
		// TODO Auto-generated method stub

	}

}
