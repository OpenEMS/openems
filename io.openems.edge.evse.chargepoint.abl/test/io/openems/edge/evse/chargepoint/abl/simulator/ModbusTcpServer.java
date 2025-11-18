package io.openems.edge.evse.chargepoint.abl.simulator;

import java.io.IOException;
import java.net.InetAddress;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.facade.ModbusTCPMaster;
import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleProcessImage;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;
import com.ghgande.j2mod.modbus.slave.ModbusSlave;
import com.ghgande.j2mod.modbus.slave.ModbusSlaveFactory;

/**
 * Modbus TCP server for ABL EVCC2/3 simulation.
 *
 * <p>
 * Implements a Modbus TCP slave that responds to read/write requests according
 * to the ABL specification.
 */
public class ModbusTcpServer {

	private ModbusSlave slave;
	private final String ipAddress;
	private final int port;
	private final int unitId;
	private final RegisterMap registerMap;
	private final SimpleProcessImage processImage;
	private boolean running = false;

	/**
	 * Create a Modbus TCP server.
	 *
	 * @param ipAddress   IP address to bind to (null = localhost)
	 * @param port        port number (default: 502)
	 * @param unitId      Modbus unit ID (default: 1)
	 * @param registerMap register map to use
	 */
	public ModbusTcpServer(String ipAddress, int port, int unitId, RegisterMap registerMap) {
		this.ipAddress = ipAddress != null ? ipAddress : "127.0.0.1";
		this.port = port;
		this.unitId = unitId;
		this.registerMap = registerMap;
		this.processImage = new SimpleProcessImage(unitId);
		this.initializeProcessImage();
	}

	/**
	 * Initialize the process image with registers from the register map.
	 */
	private void initializeProcessImage() {
		// We need to populate registers 0x0000 to 0x0100 (256 registers)
		// ABL uses: 0x0001-0x0002, 0x0003, 0x0014, 0x0033-0x0035
		for (int addr = 0; addr < 256; addr++) {
			int value = this.registerMap.readRegister(addr);
			this.processImage.addRegister(new SimpleRegister(value));
		}
	}

	/**
	 * Synchronize process image with register map (for read operations).
	 */
	public void syncFromRegisterMap() {
		for (int addr = 0; addr < 256; addr++) {
			int value = this.registerMap.readRegister(addr);
			Register reg = this.processImage.getRegister(addr);
			if (reg != null) {
				reg.setValue(value);
			}
		}
	}

	/**
	 * Synchronize register map with process image (for write operations).
	 */
	public void syncToRegisterMap() {
		for (int addr = 0; addr < 256; addr++) {
			Register reg = this.processImage.getRegister(addr);
			if (reg != null) {
				this.registerMap.writeRegister(addr, reg.getValue());
			}
		}
	}

	/**
	 * Start the Modbus TCP server.
	 *
	 * @throws IOException if server cannot start
	 */
	public void start() throws IOException {
		if (this.running) {
			return;
		}

		try {
			// Create Modbus slave
			InetAddress addr = InetAddress.getByName(this.ipAddress);
			this.slave = ModbusSlaveFactory.createTCPSlave(addr, this.port, 5, false);
			this.slave.addProcessImage(this.unitId, this.processImage);

			// Start listening
			System.out.println("[ABL Modbus Simulator] Starting server on " + this.ipAddress + ":" + this.port
					+ " (Unit ID: " + this.unitId + ")");
			this.slave.open();
			this.running = true;

		} catch (Exception e) {
			throw new IOException("Failed to start Modbus server: " + e.getMessage(), e);
		}
	}

	/**
	 * Stop the Modbus TCP server.
	 */
	public void stop() {
		if (!this.running) {
			return;
		}

		System.out.println("[ABL Modbus Simulator] Stopping server...");
		if (this.slave != null) {
			this.slave.close();
		}
		this.running = false;
	}

	/**
	 * Check if server is running.
	 *
	 * @return true if running
	 */
	public boolean isRunning() {
		return this.running;
	}

	/**
	 * Get process image (for direct access if needed).
	 *
	 * @return the process image
	 */
	public SimpleProcessImage getProcessImage() {
		return this.processImage;
	}
}
