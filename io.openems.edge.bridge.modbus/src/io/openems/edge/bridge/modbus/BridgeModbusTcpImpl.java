package io.openems.edge.bridge.modbus;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import com.ghgande.j2mod.modbus.io.ModbusTCPTransaction;
import com.ghgande.j2mod.modbus.io.ModbusTransaction;
import com.ghgande.j2mod.modbus.net.TCPMasterConnection;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.InetAddressUtils;
import io.openems.edge.bridge.modbus.api.AbstractModbusBridge;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.BridgeModbusTcp;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;

/**
 * Provides a service for connecting to, querying and writing to a Modbus/TCP
 * device.
 */
@Designate(ocd = ConfigTcp.class, factory = true)
@Component(//
		name = "Bridge.Modbus.Tcp", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})
public class BridgeModbusTcpImpl extends AbstractModbusBridge
		implements BridgeModbus, BridgeModbusTcp, OpenemsComponent, EventHandler {

	/** The configured IP address. */
	private InetAddress ipAddress = null;
	private int port;

	public BridgeModbusTcpImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				BridgeModbus.ChannelId.values(), //
				BridgeModbusTcp.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, ConfigTcp config) throws UnknownHostException {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.logVerbosity(),
				config.invalidateElementsAfterReadErrors());
		this.setIpAddress(InetAddressUtils.parseOrNull(config.ip()));
		this.port = config.port();
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void closeModbusConnection() {
		if (this._connection != null) {
			this._connection.close();
			this._connection = null;
		}
	}

	@Override
	public ModbusTransaction getNewModbusTransaction() throws OpenemsException {
		var connection = this.getModbusConnection();
		var transaction = new ModbusTCPTransaction(connection);
		transaction.setRetries(AbstractModbusBridge.DEFAULT_RETRIES);
		return transaction;
	}

	private TCPMasterConnection _connection = null;

	private synchronized TCPMasterConnection getModbusConnection() throws OpenemsException {
		if (this._connection == null) {
			/*
			 * create new connection
			 */
			var connection = new TCPMasterConnection(this.getIpAddress());
			connection.setPort(this.port);
			this._connection = connection;
		}
		if (!this._connection.isConnected()) {
			try {
				this._connection.connect();
			} catch (Exception e) {
				throw new OpenemsException(
						"Connection to [" + this.getIpAddress().getHostAddress() + "] failed: " + e.getMessage());
			}
			this._connection.getModbusTransport().setTimeout(AbstractModbusBridge.DEFAULT_TIMEOUT);
		}
		return this._connection;
	}

	@Override
	public InetAddress getIpAddress() {
		return this.ipAddress;
	}

	public void setIpAddress(InetAddress ipAddress) {
		this.ipAddress = ipAddress;
	}
}