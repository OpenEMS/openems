package io.openems.edge.bridge.modbus;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import com.ghgande.j2mod.modbus.Modbus;
import com.ghgande.j2mod.modbus.io.ModbusTCPTransaction;
import com.ghgande.j2mod.modbus.io.ModbusTransaction;
import com.ghgande.j2mod.modbus.net.TCPMasterConnection;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.controllerexecutor.EdgeEventConstants;

/**
 * Provides a service for connecting to, querying and writing to a Modbus/TCP
 * device
 */
@Designate(ocd = ConfigTcp.class, factory = true)
@Component(name = "Bridge.Modbus.Tcp", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE)
public class BridgeModbusTcp extends AbstractModbusBridge implements BridgeModbus, OpenemsComponent, EventHandler {

	// private final Logger log =
	// LoggerFactory.getLogger(BridgeModbusTcpImpl.class);

	/**
	 * The configured IP address
	 */
	private InetAddress ipAddress = null;

	@Activate
	void activate(ComponentContext context, ConfigTcp config) throws UnknownHostException {
		super.activate(context, config.service_pid(), config.id(), config.enabled());
		this.ipAddress = InetAddress.getByName(config.ip());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void closeModbusConnection() {
		if (this._connection != null) {
			this._connection.close();
		}
	}

	@Override
	public ModbusTransaction getNewModbusTransaction() throws OpenemsException {
		TCPMasterConnection connection = this.getModbusConnection();
		ModbusTCPTransaction transaction = new ModbusTCPTransaction(connection);
		transaction.setRetries(AbstractModbusBridge.DEFAULT_RETRIES);
		return transaction;
	}

	private TCPMasterConnection _connection = null;

	private synchronized TCPMasterConnection getModbusConnection() throws OpenemsException {
		if (this._connection == null) {
			/*
			 * create new connection
			 */
			TCPMasterConnection connection = new TCPMasterConnection(this.ipAddress);
			connection.setPort(Modbus.DEFAULT_PORT);
			this._connection = connection;
		}
		if (!this._connection.isConnected()) {
			try {
				this._connection.connect();
			} catch (Exception e) {
				throw new OpenemsException(
						"Connection to [" + this.ipAddress.getHostAddress() + "] failed: " + e.getMessage(), e);
			}
			this._connection.getModbusTransport().setTimeout(AbstractModbusBridge.DEFAULT_TIMEOUT);
		}
		return this._connection;
	}
}