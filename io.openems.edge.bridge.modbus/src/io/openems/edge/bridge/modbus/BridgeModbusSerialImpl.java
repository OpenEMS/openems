package io.openems.edge.bridge.modbus;

import java.time.Clock;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
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
import com.ghgande.j2mod.modbus.net.AbstractSerialConnection;
import com.ghgande.j2mod.modbus.net.SerialConnection;
import com.ghgande.j2mod.modbus.util.SerialParameters;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractModbusBridge;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.BridgeModbusSerial;
import io.openems.edge.bridge.modbus.api.Config;
import io.openems.edge.bridge.modbus.api.Parity;
import io.openems.edge.bridge.modbus.api.Stopbit;
import io.openems.edge.bridge.modbus.api.task.Task;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.startstop.StartStoppable;

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
		implements BridgeModbus, BridgeModbusSerial, OpenemsComponent, EventHandler, StartStoppable {

	private final Logger log = LoggerFactory.getLogger(BridgeModbusSerialImpl.class);

	private final Lock lock = new ReentrantLock();

	@Reference
	private ComponentManager componentManager;

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

	private volatile boolean activated;
	private volatile SerialConnection _connection = null;

	public BridgeModbusSerialImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				BridgeModbus.ChannelId.values(), //
				BridgeModbusSerial.ChannelId.values(), //
				StartStoppable.ChannelId.values() //
		);
	}

	@Activate
	protected void activate(ComponentContext context, ConfigSerial config) {
		super.activate(context, new Config(config.id(), config.alias(), config.enabled(), config.logVerbosity(),
				config.invalidateElementsAfterReadErrors()));
		this.applyConfig(config);
		this.activated = true;
	}

	@Modified
	private void modified(ComponentContext context, ConfigSerial config) {
		super.modified(context, new Config(config.id(), config.alias(), config.enabled(), config.logVerbosity(),
				config.invalidateElementsAfterReadErrors()));
		this.applyConfig(config);
		this.closeModbusConnection();
	}

	@Override
	public Clock getClock() {
		return this.componentManager.getClock();
	}

	private void applyConfig(ConfigSerial config) {
		this.portName = config.portName();
		this.baudrate = config.baudRate();
		this.databits = config.databits();
		this.stopbits = config.stopbits();
		this.parity = config.parity();
	}

	@Override
	@Deactivate
	protected void deactivate() {
		this.activated = false;
		super.deactivate();
	}

	@Override
	protected Task.ExecuteState executeTask(Task task) {
		if (!this.activated) {
			return Task.ExecuteState.NO_OP;
		}
		// Once upon a time, there was a story about a modbus connection that can't open
		// connections anymore after a configuration change, because
		// something in OpenEMS is still keeping the port open.
		// This happens when the serial port is closed while it's used. There is no
		// error indicator, no exception, nothing. It closes and it reports closed, but
		// in reality, it's not closed. I assume SerialPort_Posix.c from jSerialComm is
		// not handling close errors correctly.
		// By synchronizing every time something is going on with the serial port, we
		// can ensure that the port is not closed while it's used.

		final var shouldLock = task.requiresConnection();
		if (shouldLock) {
			this.lock.lock();
		}
		try {
			return task.execute(this);
		} finally {
			if (shouldLock) {
				this.lock.unlock();
			}
		}
	}

	@Override
	public void closeModbusConnection() {
		this.lock.lock();
		try {
			if (this._connection == null) {
				return;
			}

			// Warning: close() can fail silently, see comment in executeTask() method
			this._connection.close();
			this._connection = null;
		} finally {
			this.lock.unlock();
		}
	}

	@Override
	public ModbusTransaction getNewModbusTransaction() throws OpenemsException {
		if (this.isStopped() || !this.activated) {
			this.closeModbusConnection();
			return null;
		}

		var connection = this.getModbusConnection();
		var transaction = new ModbusSerialTransaction(connection);
		transaction.setRetries(AbstractModbusBridge.DEFAULT_RETRIES);
		return transaction;
	}

	protected synchronized AbstractSerialConnection getModbusConnection() throws OpenemsException {
		if (!this.activated) {
			return this._connection;
		}

		var connection = this._connection;
		if (connection == null || !connection.isOpen()) {
			this.lock.lock();
			try {
				if (this._connection == null) {
					this._connection = this.createConnection();
				}
				if (!this._connection.isOpen()) {
					this.tryOpenConnection();
				}
				connection = this._connection;
			} finally {
				this.lock.unlock();
			}
		}

		return connection;
	}

	private SerialConnection createConnection() {
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
		params.disableRs485Control();

		return new SerialConnection(params);
	}

	private void tryOpenConnection() throws OpenemsException {
		try {
			this.log.info("Open serial modbus connection to " + this.portName + " ...");
			this._connection.open();
		} catch (Exception e) {
			this.log.error("Failed to open serial modbus connection to " + this.portName, e);
			throw new OpenemsException("Connection via [" + this.portName + "] failed: " + e.getMessage());
		}

		var transport = (ModbusSerialTransport) this._connection.getModbusTransport();
		transport.setTimeout(AbstractModbusBridge.DEFAULT_TIMEOUT);

		transport.addListener(new AbstractSerialTransportListener() {
			/**
			 * Modbus requires to wait 3.5 characters between requests and a few
			 * microcontroller library's are requiring 5ms. j2mod is ensuring that by a
			 * check in ModbusSerialTransaction, but we are creating a new transaction every
			 * time, so this check does not work. Someday this should be replaced by a fix
			 * in j2mod.
			 *
			 * @param port port
			 * @param msg  msg
			 */
			@Override
			public void beforeMessageWrite(AbstractSerialConnection port, ModbusMessage msg) {
				try {
					Thread.sleep(6L);
				} catch (InterruptedException e) {
					// Empty
				}
			}

			/**
			 * Sometimes read after write happens too quickly and causes read errors. Add
			 * 1ms additional waiting time between write request and read response.
			 *
			 * <p>
			 * Notice 2026-07-01: I'm not sure if we should do that. j2mod needs exact
			 * timings on the receive side to identify the "idle time" between transfers,
			 * especially if we are slave. j2mod is already waiting in writeMessage(). Is
			 * the wait time wrongly calculated there?
			 *
			 * @param port port
			 * @param msg  msg
			 */
			@Override
			public void afterMessageWrite(AbstractSerialConnection port, ModbusMessage msg) {
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
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
