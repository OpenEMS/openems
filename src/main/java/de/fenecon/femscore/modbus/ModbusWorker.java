package de.fenecon.femscore.modbus;

import java.util.ArrayList;
import java.util.List;
import de.fenecon.femscore.modbus.device.ModbusDevice;
import de.fenecon.femscore.modbus.device.ModbusDeviceWritable;
import net.wimpi.modbus.procimg.Register;

/**
 * ModbusWorker handles all modbus communication on one channel like /dev/ttyUSB0, 
 * /dev/ttyUSB1, eth0-ip0, eth0-ip1,... 
 * 
 * @author Stefan Feilmeier
 */
public class ModbusWorker extends Thread {

	private final List<ModbusDevice> devices = new ArrayList<ModbusDevice>();
	private final ModbusConnection modbusConnection;
	
	public ModbusWorker(ModbusConnection modbusConnection) {
		this.modbusConnection = modbusConnection;
	}
	
	public final ModbusConnection getModbusConnection() {
		return modbusConnection;
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
		while ( ! isInterrupted() ) {
			
			for(ModbusDevice device : devices) {
				if(device instanceof ModbusDeviceWritable) {
					((ModbusDeviceWritable)device).executeModbusWrite();
				}
				device.executeModbusMainQuery();
				device.executeModbusNextSmallQuery();
			}
			
			try {
				Thread.sleep( 3000 );
			} catch ( InterruptedException e ) {
				interrupt();
			}
		}
	}
	
	@Override
	public String toString() {
		return "ModbusWorker [modbusConnection=" + modbusConnection + ", devices=" + devices + "]";
	}

	public synchronized Register[] getModbusResponse(int unitid, int ref, int count) throws Exception {
		return modbusConnection.getModbusResponse(unitid, ref, count);
	}
	
	public synchronized void writeRegister(int unitid, int ref, Register reg) throws Exception {
		modbusConnection.writeRegister(unitid, ref, reg);
	}
}