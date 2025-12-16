package io.openems.edge.evse.chargepoint.abl.simulator;

import java.io.IOException;

import com.ghgande.j2mod.modbus.Modbus;
import com.ghgande.j2mod.modbus.procimg.Register;
import com.ghgande.j2mod.modbus.procimg.SimpleProcessImage;
import com.ghgande.j2mod.modbus.procimg.SimpleRegister;
import com.ghgande.j2mod.modbus.slave.ModbusSlave;
import com.ghgande.j2mod.modbus.slave.ModbusSlaveFactory;
import com.ghgande.j2mod.modbus.util.SerialParameters;

/**
 * Modbus Serial (ASCII/RTU) server for ABL EVCC2/3 simulation.
 *
 * <p>
 * Implements a Modbus serial slave that responds to read/write requests via
 * RS-485 according to the ABL specification. Supports both Modbus ASCII
 * (as specified in the ABL documentation) and Modbus RTU encoding.
 *
 * <p>
 * This server is designed to work with a USB-to-RS485 adapter and a
 * Modbus gateway that converts RS-485 to Modbus TCP for OpenEMS Edge.
 *
 * <p>
 * Typical hardware setup:
 * <pre>
 * [PC/Simulator] → [USB-RS485] → [Modbus Gateway] → [OpenEMS Edge]
 *    Serial/ASCII     RS-485         Modbus TCP         TCP Client
 * </pre>
 */
public class ModbusSerialServer {

	private ModbusSlave slave;
	private final String portName;
	private final int baudRate;
	private final int unitId;
	private final RegisterMap registerMap;
	private final SimpleProcessImage processImage;
	private final boolean useAscii;
	private boolean running = false;

	// Serial parameters
	private final int dataBits;
	private final int stopBits;
	private final String parity;

	/**
	 * Create a Modbus Serial server with default serial parameters (8E1).
	 *
	 * @param portName    Serial port name (e.g., "/dev/ttyUSB0" on Linux or "COM3"
	 *                    on Windows)
	 * @param baudRate    Baud rate (typically 38400 for ABL EVCC2/3)
	 * @param unitId      Modbus unit ID (1-247, typically 1)
	 * @param registerMap Register map to use
	 * @param useAscii    true for Modbus ASCII encoding, false for RTU encoding
	 */
	public ModbusSerialServer(String portName, int baudRate, int unitId, RegisterMap registerMap, boolean useAscii) {
		this(portName, baudRate, unitId, registerMap, useAscii, 8, 1, "Even");
	}

	/**
	 * Create a Modbus Serial server with custom serial parameters.
	 *
	 * @param portName    Serial port name (e.g., "/dev/ttyUSB0" or "COM3")
	 * @param baudRate    Baud rate (9600, 19200, 38400, 57600, 115200)
	 * @param unitId      Modbus unit ID (1-247)
	 * @param registerMap Register map to use
	 * @param useAscii    true for Modbus ASCII, false for RTU
	 * @param dataBits    Data bits (7 or 8)
	 * @param stopBits    Stop bits (1 or 2)
	 * @param parity      Parity ("None", "Even", "Odd", "Mark", "Space")
	 */
	public ModbusSerialServer(String portName, int baudRate, int unitId, RegisterMap registerMap, boolean useAscii,
			int dataBits, int stopBits, String parity) {
		this.portName = portName;
		this.baudRate = baudRate;
		this.unitId = unitId;
		this.registerMap = registerMap;
		this.useAscii = useAscii;
		this.dataBits = dataBits;
		this.stopBits = stopBits;
		this.parity = parity;
		this.processImage = new SimpleProcessImage(unitId);
		this.initializeProcessImage();
	}

	/**
	 * Initialize the process image with registers from the register map.
	 *
	 * <p>
	 * Populates all registers (0x0000 to 0x00FF) with initial values from the
	 * register map. The ABL EVCC2/3 uses registers in the range 0x0001-0x0035.
	 */
	private void initializeProcessImage() {
		// Populate registers 0x0000 to 0x0100 (256 registers)
		// ABL uses: 0x0001-0x0002 (device info), 0x0003 (config),
		// 0x0014 (setpoint), 0x0033-0x0035 (status/currents)
		for (int addr = 0; addr < 256; addr++) {
			int value = this.registerMap.readRegister(addr);
			this.processImage.addRegister(new SimpleRegister(value));
		}
	}

	/**
	 * Synchronize process image FROM register map (for read operations).
	 *
	 * <p>
	 * Updates the Modbus process image with current values from the register map.
	 * This should be called periodically (e.g., every 100ms) to ensure that Modbus
	 * read requests return up-to-date state information.
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
	 * Synchronize register map FROM process image (for write operations).
	 *
	 * <p>
	 * Updates the register map with values written via Modbus. This should be
	 * called periodically (e.g., every 100ms) to ensure that Modbus write requests
	 * are reflected in the simulator's internal state.
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
	 * Start the Modbus Serial server.
	 *
	 * <p>
	 * Opens the serial port and starts listening for Modbus requests.
	 *
	 * @throws IOException if the serial port cannot be opened or configured
	 */
	public void start() throws IOException {
		if (this.running) {
			System.out.println("[ABL Modbus Simulator] Serial server already running");
			return;
		}

		try {
			// Configure serial parameters
			var params = new SerialParameters();
			params.setPortName(this.portName);
			params.setBaudRate(this.baudRate);
			params.setDatabits(this.dataBits);
			params.setStopbits(this.stopBits);
			params.setParity(this.parity);
			params.setEncoding(this.useAscii ? Modbus.SERIAL_ENCODING_ASCII : Modbus.SERIAL_ENCODING_RTU);
			params.setEcho(false);

			// RS-485 specific settings
			// These settings enable proper RS-485 half-duplex communication
			params.setRs485Mode(true); // Enable RS-485 mode
			params.setRs485RxDuringTx(false); // Disable receiver during transmit (half-duplex)
			params.setRs485TxEnableActiveHigh(true); // TX enable signal is active high
			params.setRs485EnableTermination(true); // Enable 120Ω termination resistor
			params.setRs485DelayBeforeTxMicroseconds(1000); // 1ms delay before TX (for slow devices)
			params.setRs485DelayAfterTxMicroseconds(0); // No delay after TX

			// Create and configure Modbus slave
			System.out.println("[ABL Modbus Simulator] Initializing Serial server...");
			System.out.println("  Port:     " + this.portName);
			System.out.println("  Baudrate: " + this.baudRate);
			System.out.println("  Config:   " + this.dataBits + this.parity.charAt(0) + this.stopBits);
			System.out.println("  Encoding: " + (this.useAscii ? "Modbus ASCII" : "Modbus RTU"));
			System.out.println("  Unit ID:  " + this.unitId);

			this.slave = ModbusSlaveFactory.createSerialSlave(params);
			this.slave.addProcessImage(this.unitId, this.processImage);

			// Start listening
			System.out.println("[ABL Modbus Simulator] Opening serial port " + this.portName + "...");
			this.slave.open();
			this.running = true;

			System.out.println("[ABL Modbus Simulator] Serial server started successfully");
			System.out.println("[ABL Modbus Simulator] Listening for Modbus " + (this.useAscii ? "ASCII" : "RTU")
					+ " requests on " + this.portName);
			System.out.println("[ABL Modbus Simulator] Use Ctrl+C to stop");

		} catch (Exception e) {
			var errorMsg = "Failed to start Modbus Serial server: " + e.getMessage();
			System.err.println("[ABL Modbus Simulator] ERROR: " + errorMsg);

			// Provide helpful error messages for common issues
			if (e.getMessage().contains("Permission denied") || e.getMessage().contains("Access is denied")) {
				System.err.println("\n[ABL Modbus Simulator] HINT: Serial port permission denied.");
				System.err.println("  On Linux, try:");
				System.err.println("    sudo chmod 666 " + this.portName);
				System.err.println("    or add your user to the 'dialout' group:");
				System.err.println("    sudo usermod -a -G dialout $USER");
				System.err.println("    (then log out and back in)");
				System.err.println("\n  On Windows, check if another application is using " + this.portName);

			} else if (e.getMessage().contains("No such file") || e.getMessage().contains("does not exist")) {
				System.err.println("\n[ABL Modbus Simulator] HINT: Serial port not found.");
				System.err.println("  On Linux, list available ports:");
				System.err.println("    ls -l /dev/ttyUSB* /dev/ttyACM*");
				System.err.println("\n  On Windows, check Device Manager for COM port number");

			} else if (e.getMessage().contains("Port busy") || e.getMessage().contains("already in use")) {
				System.err.println("\n[ABL Modbus Simulator] HINT: Serial port is busy.");
				System.err.println("  Another application might be using " + this.portName);
				System.err.println("  Close any terminal emulators (minicom, screen, putty, etc.)");
			}

			throw new IOException(errorMsg, e);
		}
	}

	/**
	 * Stop the Modbus Serial server.
	 *
	 * <p>
	 * Closes the serial port and stops listening for Modbus requests.
	 */
	public void stop() {
		if (!this.running) {
			return;
		}

		System.out.println("[ABL Modbus Simulator] Stopping Serial server...");
		if (this.slave != null) {
			try {
				this.slave.close();
				System.out.println("[ABL Modbus Simulator] Serial port closed");
			} catch (Exception e) {
				System.err.println("[ABL Modbus Simulator] Error closing serial port: " + e.getMessage());
			}
		}
		this.running = false;
		System.out.println("[ABL Modbus Simulator] Serial server stopped");
	}

	/**
	 * Check if the server is running.
	 *
	 * @return true if the server is running and listening for requests
	 */
	public boolean isRunning() {
		return this.running;
	}

	/**
	 * Get the process image (for direct access if needed).
	 *
	 * @return the Modbus process image
	 */
	public SimpleProcessImage getProcessImage() {
		return this.processImage;
	}

	/**
	 * Get the serial port name.
	 *
	 * @return serial port name (e.g., "/dev/ttyUSB0" or "COM3")
	 */
	public String getPortName() {
		return this.portName;
	}

	/**
	 * Get the baud rate.
	 *
	 * @return baud rate (e.g., 38400)
	 */
	public int getBaudRate() {
		return this.baudRate;
	}

	/**
	 * Get the unit ID.
	 *
	 * @return Modbus unit ID (1-247)
	 */
	public int getUnitId() {
		return this.unitId;
	}

	/**
	 * Check if using ASCII encoding.
	 *
	 * @return true if Modbus ASCII, false if Modbus RTU
	 */
	public boolean isAscii() {
		return this.useAscii;
	}

	/**
	 * Get serial configuration summary.
	 *
	 * @return string describing serial configuration (e.g., "38400 8E1")
	 */
	public String getSerialConfig() {
		return this.baudRate + " " + this.dataBits + this.parity.charAt(0) + this.stopBits;
	}
}
