package io.openems.edge.bridge.modbus.test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import com.ghgande.j2mod.modbus.io.ModbusTransaction;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractModbusBridge;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.BridgeModbusTcp;
import io.openems.edge.bridge.modbus.api.Config;
import io.openems.edge.bridge.modbus.api.LogVerbosity;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.OpenemsComponent;

public class DummyModbusBridge extends AbstractModbusBridge implements BridgeModbusTcp, BridgeModbus, OpenemsComponent {

	private final Map<String, ModbusProtocol> protocols = new HashMap<>();

	private InetAddress ipAddress = null;

	public DummyModbusBridge(String id) {
		this(id, LogVerbosity.NONE);
	}

	public DummyModbusBridge(String id, LogVerbosity logVerbosity) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				BridgeModbus.ChannelId.values(), //
				BridgeModbusTcp.ChannelId.values() //
		);
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}
		super.activate(null, new Config(id, "", true, logVerbosity, 2));
	}

	/**
	 * Sets the IP-Address.
	 * 
	 * @param ipAddress an IP-Address.
	 * @return myself
	 * @throws UnknownHostException on parse error
	 */
	public DummyModbusBridge withIpAddress(String ipAddress) throws UnknownHostException {
		this.ipAddress = InetAddress.getByName(ipAddress);
		return this;
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
		if (this.ipAddress != null) {
			return this.ipAddress;
		}
		throw new UnsupportedOperationException("Unsupported by Dummy Class");
	}

	@Override
	public ModbusTransaction getNewModbusTransaction() throws OpenemsException {
		throw new UnsupportedOperationException("getNewModbusTransaction() Unsupported by Dummy Class");
	}

	@Override
	public void closeModbusConnection() {
		throw new UnsupportedOperationException("closeModbusConnection() Unsupported by Dummy Class");
	}

}
