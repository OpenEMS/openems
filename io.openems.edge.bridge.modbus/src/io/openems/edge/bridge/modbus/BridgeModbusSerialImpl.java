package io.openems.edge.bridge.modbus;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import com.ghgande.j2mod.modbus.Modbus;
import com.ghgande.j2mod.modbus.io.AbstractSerialTransportListener;
import com.ghgande.j2mod.modbus.io.ModbusSerialTransaction;
import com.ghgande.j2mod.modbus.io.ModbusSerialTransport;
import com.ghgande.j2mod.modbus.io.ModbusTransaction;
import com.ghgande.j2mod.modbus.msg.ModbusMessage;
import com.ghgande.j2mod.modbus.net.AbstractSerialConnection;
import com.ghgande.j2mod.modbus.net.SerialConnection;
import com.ghgande.j2mod.modbus.util.SerialParameters;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractModbusBridge;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.BridgeModbusSerial;
import io.openems.edge.bridge.modbus.api.Parity;
import io.openems.edge.bridge.modbus.api.Stopbit;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;

/**
 * Provides a service for connecting to, querying and writing to a Modbus/RTU
 * device.
 */
@Designate(ocd = ConfigSerial.class, factory = true)
@Component(//
		name = "Bridge.Modbus.Serial", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})
public class BridgeModbusSerialImpl extends AbstractModbusBridge
		implements BridgeModbus, BridgeModbusSerial, OpenemsComponent, EventHandler {

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

	/** Enable internal bus termination. */
	private boolean enableTermination;

	/**
	 * The configured delay between activating the transmitter and actually sending
	 * data in microseconds.
	 */
	private int delayBeforeTx;

	/**
	 * The configured delay between the end of transmitting data and deactivating
	 * transmitter in microseconds.
	 */
	private int delayAfterTx;

	public BridgeModbusSerialImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				BridgeModbus.ChannelId.values(), //
				BridgeModbusSerial.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, ConfigSerial config) {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.logVerbosity(),
				config.invalidateElementsAfterReadErrors());
		this.applyConfig(config);
	}

	@Modified
	private void modified(ComponentContext context, ConfigSerial config) {
		super.modified(context, config.id(), config.alias(), config.enabled(), config.logVerbosity(),
				config.invalidateElementsAfterReadErrors());
		this.applyConfig(config);
		this.closeModbusConnection();
	}

	private void applyConfig(ConfigSerial config) {
		this.portName = config.portName();
		this.baudrate = config.baudRate();
		this.databits = config.databits();
		this.stopbits = config.stopbits();
		this.parity = config.parity();
		this.enableTermination = config.enableTermination();
		this.delayBeforeTx = config.delayBeforeTx();
		this.delayAfterTx = config.delayAfterTx();
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
		var transaction = new ModbusSerialTransaction(connection);
		transaction.setRetries(AbstractModbusBridge.DEFAULT_RETRIES);
		return transaction;
	}

	private SerialConnection _connection = null;

	private synchronized SerialConnection getModbusConnection() throws OpenemsException {
		if (this._connection == null) {
			/*
			 * create new connection
			 */
			var params = new SerialParameters();
			params.setPortName(this.portName);
			params.setBaudRate(this.baudrate);
			params.setDatabits(this.databits);
			params.setStopbits(this.stopbits.getValue());
			params.setParity(this.parity.getValue());
			params.setEncoding(Modbus.SERIAL_ENCODING_RTU);
			params.setEcho(false);
			/* RS485 Settings */
			params.setRs485Mode(true);
			params.setRs485RxDuringTx(false);
			params.setRs485TxEnableActiveHigh(true);
			params.setRs485EnableTermination(this.enableTermination);
			params.setRs485DelayBeforeTxMicroseconds(this.delayBeforeTx);
			params.setRs485DelayAfterTxMicroseconds(this.delayAfterTx);
			var connection = new SerialConnection(params);
			this._connection = connection;
		}
		if (!this._connection.isOpen()) {
			try {
				this._connection.open();
			} catch (Exception e) {
				throw new OpenemsException("Connection via [" + this.portName + "] failed: " + e.getMessage());
			}

			var transport = (ModbusSerialTransport) this._connection.getModbusTransport();
			transport.setTimeout(AbstractModbusBridge.DEFAULT_TIMEOUT);

			// Sometimes read after write happens too quickly and causes read errors.
			// Add 1ms additional waiting time between write request and read response
			transport.addListener(new AbstractSerialTransportListener() {
				public void afterMessageWrite(AbstractSerialConnection port, ModbusMessage msg) {
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			});
		}
		return this._connection;
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