package io.openems.edge.bridge.modbus.api.worker;

import com.ghgande.j2mod.modbus.io.ModbusTransaction;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractModbusBridge;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.BridgeModbusTcp;
import io.openems.edge.bridge.modbus.api.LogVerbosity;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.OpenemsComponent;

public class DummyModbusBridge extends AbstractModbusBridge {

	public DummyModbusBridge(String id) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				BridgeModbus.ChannelId.values(), //
				BridgeModbusTcp.ChannelId.values() //
		);
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}
		super.activate(null, id, "", true, LogVerbosity.NONE, 1);
	}

//	public ModbusWorker getWorker() {
//		return this.worker;
//	}

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