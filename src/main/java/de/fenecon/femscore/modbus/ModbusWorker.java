package de.fenecon.femscore.modbus;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import EDU.oswego.cs.dl.util.concurrent.Mutex;
import de.fenecon.femscore.modbus.device.ModbusDevice;
import de.fenecon.femscore.modbus.device.ModbusDeviceWritable;

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
	private final Mutex mainQueryFinished = new Mutex();

	public ModbusWorker(String name, ModbusConnection modbusConnection) {
		this.setName(name);
		this.modbusConnection = modbusConnection;
	}

	public final ModbusConnection getModbusConnection() {
		return modbusConnection;
	}

	public void waitForMainQuery() throws InterruptedException {
		synchronized (mainQueryFinished) {
			mainQueryFinished.acquire();
		}
	}

	/**
	 * Register a new modbus device to this worker
	 * 
	 * @param device
	 */
	public void registerDevice(ModbusDevice device) {
		devices.add(device);
	}

	@Override
	public void run() {
		log.info("ModbusWorker {} started", getName());
		while (!isInterrupted()) {

			// Execute Modbus Writes
			for (ModbusDevice device : devices) {
				log.info("RUN for {}", device);
				if (device instanceof ModbusDeviceWritable) {
					((ModbusDeviceWritable) device).executeModbusWrite();
				}
			}

			// Execute Modbus Main Queries
			for (ModbusDevice device : devices) {
				try {
					device.executeModbusMainQuery(modbusConnection);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			synchronized (mainQueryFinished) {
				mainQueryFinished.release();
			}

			// Execute Next Modbus Queries
			for (ModbusDevice device : devices) {
				try {
					device.executeModbusNextSmallQuery(modbusConnection);
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