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
import io.openems.edge.bridge.modbus.api.BridgeModbusSerial;
import io.openems.edge.bridge.modbus.api.BridgeModbusTcp;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.Parity;
import io.openems.edge.bridge.modbus.api.Stopbit;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Simulator.Modbus", //
		immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class ModbusSimulator extends AbstractOpenemsComponent
		implements BridgeModbus, BridgeModbusSerial, BridgeModbusTcp, OpenemsComponent {

	// private final Logger log = LoggerFactory.getLogger(ModbusSimulator.class);

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		;
		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	@Activate
	void activate(ComponentContext context, Config config) throws IOException {
		super.activate(context, config.id(), config.alias(), config.enabled());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	public ModbusSimulator() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				BridgeModbus.ChannelId.values(), //
				ChannelId.values() //
		);
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
	public String getPortName() {
		return "PortName";
	}

	@Override
	public int getBaudrate() {
		return 9600;
	}

	@Override
	public int getDatabits() {
		return 8;
	}

	@Override
	public Stopbit getStopbits() {
		return Stopbit.ONE;
	}

	@Override
	public Parity getParity() {
		return Parity.NONE;
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
