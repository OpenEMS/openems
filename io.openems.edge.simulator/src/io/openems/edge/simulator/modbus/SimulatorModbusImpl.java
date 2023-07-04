package io.openems.edge.simulator.modbus;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.BridgeModbusTcp;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Simulator.Modbus", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class SimulatorModbusImpl extends AbstractOpenemsComponent
		implements SimulatorModbus, BridgeModbus, BridgeModbusTcp, OpenemsComponent {

	public SimulatorModbusImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				BridgeModbus.ChannelId.values(), //
				BridgeModbusTcp.ChannelId.values(), //
				SimulatorModbus.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws IOException {
		super.activate(context, config.id(), config.alias(), config.enabled());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public InetAddress getIpAddress() {
		try {
			return Inet4Address.getLocalHost();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void addProtocol(String sourceId, ModbusProtocol protocol) {
		// ignore
	}

	@Override
	public void removeProtocol(String sourceId) {
		// ignore
	}

}
