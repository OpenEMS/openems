package de.fenecon.femscore.modbus;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.fenecon.femscore.modbus.device.ModbusDevice;
import de.fenecon.femscore.modbus.device.WritableModbusDevice;
import de.fenecon.femscore.utils.Mutex;

/**
 * ModbusWorker handles all modbus communication on one channel like
 * /dev/ttyUSB0, /dev/ttyUSB1, eth0-ip0, eth0-ip1,...
 * 
 * @author Stefan Feilmeier
 */
public class ModbusWorker extends Thread {
	private final static Logger log = LoggerFactory.getLogger(Thread.class);

	private final List<ModbusDevice> devices = new ArrayList<ModbusDevice>();
	private final ModbusConnection modbusConnection;
	private final Mutex initQueryFinished = new Mutex(false);
	private final Mutex mainQueryFinished = new Mutex(false);

	public ModbusWorker(String name, ModbusConnection modbusConnection) {
		this.setName(name);
		this.modbusConnection = modbusConnection;
	}

	public final ModbusConnection getModbusConnection() {
		return modbusConnection;
	}

	public void waitForInit() throws InterruptedException {
		initQueryFinished.await();
	}

	public void waitForMain() throws InterruptedException {
		mainQueryFinished.await();
	}

	/**
	 * Register a new modbus device to this worker
	 * 
	 * @param device
	 */
	public synchronized void registerDevice(ModbusDevice device) {
		synchronized (devices) {
			devices.add(device);
		}
	}

	@Override
	public synchronized void run() {
		log.info("ModbusWorker {} started", getName());
		for (ModbusDevice device : devices) {
			log.info("RUN with " + device);
			try {
				device.executeInitQuery(modbusConnection);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		initQueryFinished.release();

		while (!isInterrupted()) {
			// Execute Modbus Main Queries
			for (ModbusDevice device : devices) {
				try {
					device.executeMainQuery(modbusConnection);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			mainQueryFinished.release();

			// Execute Modbus Writes
			for (ModbusDevice device : devices) {
				if (device instanceof WritableModbusDevice) {
					try {
						((WritableModbusDevice) device).executeModbusWrite(modbusConnection);
					} catch (Exception e) {
						log.error("Error while executing modbus write: {}", e.getMessage());
					}
				}
			}

			// Execute Next Modbus Queries
			for (ModbusDevice device : devices) {
				try {
					device.executeRemainingQuery(modbusConnection);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			try {
				Thread.sleep(this.modbusConnection.getCycle());
			} catch (InterruptedException e) {
				interrupt();
			}
		}
		log.info("ModbusWorker {} stopped", getName());
	}

	@Override
	public String toString() {
		return "ModbusWorker [modbusConnection=" + modbusConnection + ", devices=" + devices + "]";
	}
}