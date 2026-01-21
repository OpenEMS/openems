package io.openems.edge.bridge.modbus.ascii;

import java.util.concurrent.atomic.AtomicLong;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.Modbus;
import com.ghgande.j2mod.modbus.io.AbstractSerialTransportListener;
import com.ghgande.j2mod.modbus.io.ModbusSerialTransaction;
import com.ghgande.j2mod.modbus.io.ModbusSerialTransport;
import com.ghgande.j2mod.modbus.io.ModbusTransaction;
import com.ghgande.j2mod.modbus.msg.ModbusMessage;
import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.net.AbstractSerialConnection;
import com.ghgande.j2mod.modbus.net.SerialConnection;
import com.ghgande.j2mod.modbus.util.SerialParameters;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractModbusBridge;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.Config;
import io.openems.edge.bridge.modbus.api.LogVerbosity;
import io.openems.edge.bridge.modbus.api.Parity;
import io.openems.edge.bridge.modbus.api.Stopbit;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.startstop.StartStoppable;

/**
 * Provides a service for connecting to, querying and writing to a Modbus/ASCII
 * device over a serial connection.
 *
 * <p>
 * Modbus ASCII differs from Modbus RTU in these key aspects:
 * <ul>
 * <li>Frame format: ':' start delimiter, CR/LF end delimiter</li>
 * <li>Data encoding: Each byte is sent as two ASCII hex characters</li>
 * <li>Error checking: LRC (1 byte) instead of CRC-16 (2 bytes)</li>
 * <li>Timing: More tolerant of delays between characters</li>
 * </ul>
 */
@Designate(ocd = ConfigSerialAscii.class, factory = true)
@Component(//
		name = "Bridge.Modbus.Serial.Ascii", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})
public class BridgeModbusSerialAsciiImpl extends AbstractModbusBridge
		implements BridgeModbus, BridgeModbusSerialAscii, OpenemsComponent, EventHandler, StartStoppable {

	private final Logger log = LoggerFactory.getLogger(BridgeModbusSerialAsciiImpl.class);

	/** The configured Port-Name (e.g. '/dev/ttyUSB0' or 'COM3'). */
	private String portName = "";

	/** The configured Baudrate (e.g. 9600). */
	private int baudrate;

	/** The configured Databits (e.g. 8). */
	private int databits;

	/** The configured Stopbits. */
	private Stopbit stopbits;

	/** The configured parity. */
	private Parity parity;

	/** The configured log verbosity. */
	private LogVerbosity logVerbosity;

	// Health monitoring counters
	private final AtomicLong bytesSent = new AtomicLong(0);
	private final AtomicLong bytesReceived = new AtomicLong(0);
	private final AtomicLong communicationErrors = new AtomicLong(0);
	private final AtomicLong successfulTransactions = new AtomicLong(0);
	private final AtomicLong lastSuccessfulCommunication = new AtomicLong(0);

	public BridgeModbusSerialAsciiImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				BridgeModbus.ChannelId.values(), //
				BridgeModbusSerialAscii.ChannelId.values(), //
				StartStoppable.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, ConfigSerialAscii config) {
		super.activate(context, new Config(config.id(), config.alias(), config.enabled(), config.logVerbosity(),
				config.invalidateElementsAfterReadErrors()));
		this.applyConfig(config);
	}

	@Modified
	private void modified(ComponentContext context, ConfigSerialAscii config) {
		super.modified(context, new Config(config.id(), config.alias(), config.enabled(), config.logVerbosity(),
				config.invalidateElementsAfterReadErrors()));
		this.applyConfig(config);
		this.closeModbusConnection();
	}

	private void applyConfig(ConfigSerialAscii config) {
		this.portName = config.portName();
		this.baudrate = config.baudRate();
		this.databits = config.databits();
		this.stopbits = config.stopbits();
		this.parity = config.parity();
		this.logVerbosity = config.logVerbosity();
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
		if (this.isStopped()) {
			this.closeModbusConnection();
			return null;
		}

		var connection = this.getModbusConnection();
		var transaction = new ModbusSerialTransaction(connection);
		transaction.setRetries(AbstractModbusBridge.DEFAULT_RETRIES);
		return transaction;
	}

	private SerialConnection _connection = null;

	private synchronized SerialConnection getModbusConnection() throws OpenemsException {
		if (this._connection == null) {
			/*
			 * Create new connection with ASCII encoding
			 */
			var params = new SerialParameters();
			params.setPortName(this.portName);
			params.setBaudRate(this.baudrate);
			params.setDatabits(this.databits);
			params.setStopbits(this.stopbits.getValue());
			params.setParity(this.parity.getValue());
			// Key difference from RTU: Use ASCII encoding
			params.setEncoding(Modbus.SERIAL_ENCODING_ASCII);
			params.setEcho(false);
			params.disableRs485Control();
			var connection = new SerialConnection(params);
			this._connection = connection;
		}
		if (!this._connection.isOpen()) {
			try {
				this._connection.open();
			} catch (Exception e) {
				this.incrementCommunicationErrors();
				throw new OpenemsException("Connection via [" + this.portName + "] failed: " + e.getMessage());
			}

			var transport = (ModbusSerialTransport) this._connection.getModbusTransport();
			transport.setTimeout(AbstractModbusBridge.DEFAULT_TIMEOUT);

			// Add transport listener for health monitoring and debug logging
			transport.addListener(new AsciiTransportListener());
		}
		return this._connection;
	}

	/**
	 * Transport listener for monitoring communication health and raw frame logging.
	 */
	private class AsciiTransportListener extends AbstractSerialTransportListener {

		@Override
		public void beforeMessageWrite(AbstractSerialConnection port, ModbusMessage msg) {
			if (isTraceEnabled()) {
				log.info("[{}] TX >> {}", id(), formatMessage(msg));
			}
		}

		@Override
		public void afterMessageWrite(AbstractSerialConnection port, ModbusMessage msg) {
			// Estimate bytes sent: In ASCII mode, each data byte becomes 2 ASCII chars,
			// plus ':' start (1), CRLF end (2), and LRC (2 ASCII chars)
			// Approximation: (message data length * 2) + 5
			int estimatedBytes = (msg.getDataLength() + 2) * 2 + 5; // +2 for function code and unit ID
			bytesSent.addAndGet(estimatedBytes);
			updateHealthChannels();

			// Small delay after write to prevent read errors (same as RTU implementation)
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		@Override
		public void afterRequestRead(AbstractSerialConnection port, ModbusRequest req) {
			if (req != null) {
				int estimatedBytes = (req.getDataLength() + 2) * 2 + 5;
				bytesReceived.addAndGet(estimatedBytes);
				updateHealthChannels();

				if (isTraceEnabled()) {
					log.info("[{}] RX << Request: {}", id(), formatMessage(req));
				}
			}
		}

		@Override
		public void afterResponseRead(AbstractSerialConnection port, ModbusResponse res) {
			if (res != null) {
				int estimatedBytes = (res.getDataLength() + 2) * 2 + 5;
				bytesReceived.addAndGet(estimatedBytes);
				successfulTransactions.incrementAndGet();
				lastSuccessfulCommunication.set(System.currentTimeMillis());
				updateHealthChannels();

				if (isTraceEnabled()) {
					log.info("[{}] RX << Response: {}", id(), formatMessage(res));
				}
			} else {
				incrementCommunicationErrors();
				if (isTraceEnabled()) {
					log.info("[{}] RX << Response: NULL (timeout or error)", id());
				}
			}
		}

		@Override
		public void disconnected(AbstractSerialConnection port) {
			log.warn("[{}] Serial connection disconnected", id());
			incrementCommunicationErrors();
		}

		/**
		 * Formats a Modbus message for debug logging.
		 *
		 * @param msg the ModbusMessage
		 * @return formatted string representation
		 */
		private String formatMessage(ModbusMessage msg) {
			if (msg == null) {
				return "null";
			}
			var sb = new StringBuilder();
			sb.append("FC=").append(msg.getFunctionCode());
			sb.append(" Unit=").append(msg.getUnitID());
			sb.append(" Len=").append(msg.getDataLength());
			sb.append(" Hex=[").append(msg.getHexMessage()).append("]");
			return sb.toString();
		}
	}

	/**
	 * Check if trace-level logging (raw frames) is enabled.
	 *
	 * @return true if highest verbosity is configured
	 */
	private boolean isTraceEnabled() {
		return this.logVerbosity == LogVerbosity.READS_AND_WRITES_DURATION_TRACE_EVENTS;
	}

	/**
	 * Increment communication error counter and update channel.
	 */
	private void incrementCommunicationErrors() {
		this.communicationErrors.incrementAndGet();
		this.updateHealthChannels();
	}

	/**
	 * Update all health monitoring channels with current values.
	 */
	private void updateHealthChannels() {
		this._setBytesSent(this.bytesSent.get());
		this._setBytesReceived(this.bytesReceived.get());
		this._setCommunicationErrors(this.communicationErrors.get());
		this._setSuccessfulTransactions(this.successfulTransactions.get());
		var lastComm = this.lastSuccessfulCommunication.get();
		if (lastComm > 0) {
			this._setLastSuccessfulCommunication(lastComm);
		}
	}

	@Override
	public int getBaudrate() {
		return this.baudrate;
	}

	@Override
	public int getDatabits() {
		return this.databits;
	}

	@Override
	public Parity getParity() {
		return this.parity;
	}

	@Override
	public String getPortName() {
		return this.portName;
	}

	@Override
	public Stopbit getStopbits() {
		return this.stopbits;
	}
}
