package io.openems.edge.evse.chargepoint.abl.simulator;

import java.io.IOException;
import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;

import io.openems.edge.evse.chargepoint.abl.enums.ChargingState;

/**
 * Main ABL EVCC2/3 Modbus simulator.
 *
 * <p>
 * Combines the state machine, register map, and Modbus server to create a
 * complete hardware simulator. Supports both Modbus TCP and Modbus Serial
 * (ASCII/RTU) communication modes.
 *
 * <p>
 * Usage modes:
 * <ul>
 * <li>TCP mode: For direct connection or testing without hardware</li>
 * <li>Serial ASCII mode: For RS-485 communication with Modbus ASCII encoding
 * (as specified in ABL documentation)</li>
 * <li>Serial RTU mode: For RS-485 communication with Modbus RTU encoding</li>
 * </ul>
 */
public class AblModbusSimulator {

	/**
	 * Communication mode enumeration.
	 */
	public enum Mode {
		/** Modbus TCP mode (Ethernet). */
		TCP,
		/** Modbus Serial ASCII mode (RS-485 with ASCII encoding). */
		SERIAL_ASCII,
		/** Modbus Serial RTU mode (RS-485 with RTU encoding). */
		SERIAL_RTU
	}

	private final RegisterMap registerMap;
	private final AblStateMachine stateMachine;
	private final Timer updateTimer;
	private final Mode mode;

	// Mode-specific servers (only one will be initialized)
	private ModbusTcpServer modbusServerTcp;
	private ModbusSerialServer modbusServerSerial;

	private int previousSetpoint = -1;

	/**
	 * Create an ABL Modbus simulator in TCP mode (backward compatibility).
	 *
	 * @param ipAddress IP address to bind to (null = localhost)
	 * @param port      port number (default: 502)
	 * @param deviceId  device ID (1-16)
	 */
	public AblModbusSimulator(String ipAddress, int port, int deviceId) {
		this(Mode.TCP, ipAddress != null ? ipAddress : "127.0.0.1", String.valueOf(port), 38400, deviceId);
	}

	/**
	 * Create an ABL Modbus simulator with mode selection.
	 *
	 * @param mode     communication mode (TCP, SERIAL_ASCII, or SERIAL_RTU)
	 * @param address  IP address for TCP mode, or serial port for Serial modes
	 *                 (e.g., "127.0.0.1" or "/dev/ttyUSB0")
	 * @param port     port number for TCP mode, or unused for Serial modes
	 * @param baudRate baud rate for Serial modes (typically 38400), or unused for
	 *                 TCP
	 * @param deviceId device ID (1-16)
	 */
	public AblModbusSimulator(Mode mode, String address, String port, int baudRate, int deviceId) {
		this.mode = mode;

		// Initialize components
		this.registerMap = new RegisterMap(deviceId, 1, 2); // Firmware 1.2
		this.stateMachine = new AblStateMachine();

		// Initialize mode-specific Modbus server
		switch (mode) {
		case TCP:
			int tcpPort = Integer.parseInt(port);
			this.modbusServerTcp = new ModbusTcpServer(address, tcpPort, deviceId, this.registerMap);
			break;

		case SERIAL_ASCII:
			this.modbusServerSerial = new ModbusSerialServer(address, baudRate, deviceId, this.registerMap, true);
			break;

		case SERIAL_RTU:
			this.modbusServerSerial = new ModbusSerialServer(address, baudRate, deviceId, this.registerMap, false);
			break;
		}

		// Create update timer for periodic synchronization
		this.updateTimer = new Timer("ABL-Simulator-Update", true);
	}

	/**
	 * Start the simulator.
	 *
	 * @throws IOException if Modbus server cannot start
	 */
	public void start() throws IOException {
		// Start appropriate Modbus server based on mode
		switch (this.mode) {
		case TCP:
			this.modbusServerTcp.start();
			break;
		case SERIAL_ASCII:
		case SERIAL_RTU:
			this.modbusServerSerial.start();
			break;
		}

		// Start periodic update task (every 100ms)
		this.updateTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				AblModbusSimulator.this.update();
			}
		}, 0, 100);

		System.out.println("[ABL Simulator] Simulator started in " + this.mode + " mode");
	}

	/**
	 * Stop the simulator.
	 */
	public void stop() {
		System.out.println("[ABL Simulator] Stopping simulator...");

		// Stop update timer
		this.updateTimer.cancel();

		// Stop appropriate Modbus server
		switch (this.mode) {
		case TCP:
			if (this.modbusServerTcp != null) {
				this.modbusServerTcp.stop();
			}
			break;
		case SERIAL_ASCII:
		case SERIAL_RTU:
			if (this.modbusServerSerial != null) {
				this.modbusServerSerial.stop();
			}
			break;
		}

		// Shutdown state machine
		this.stateMachine.shutdown();

		System.out.println("[ABL Simulator] Simulator stopped");
	}

	/**
	 * Periodic update: sync between components.
	 */
	private void update() {
		try {
			// 1. Update state registers based on state machine
			this.registerMap.updateStateRegisters(//
					this.stateMachine.getCurrentState(), //
					this.stateMachine.isEvConnected(), //
					this.stateMachine.getPhaseCurrentL1(), //
					this.stateMachine.getPhaseCurrentL2(), //
					this.stateMachine.getPhaseCurrentL3());

			// 2. Sync register map TO Modbus (for reads)
			switch (this.mode) {
			case TCP:
				this.modbusServerTcp.syncFromRegisterMap();
				break;
			case SERIAL_ASCII:
			case SERIAL_RTU:
				this.modbusServerSerial.syncFromRegisterMap();
				break;
			}

			// 3. Sync Modbus TO register map (for writes)
			switch (this.mode) {
			case TCP:
				this.modbusServerTcp.syncToRegisterMap();
				break;
			case SERIAL_ASCII:
			case SERIAL_RTU:
				this.modbusServerSerial.syncToRegisterMap();
				break;
			}

			// 4. Check if setpoint has changed
			int currentSetpoint = this.registerMap.getIcmaxSetpoint();
			if (currentSetpoint != this.previousSetpoint) {
				this.stateMachine.onCurrentSetpointChanged(currentSetpoint);
				this.previousSetpoint = currentSetpoint;
			}

			// 5. Check for communication timeout
			this.stateMachine.checkCommunicationTimeout();

		} catch (Exception e) {
			System.err.println("[ABL Simulator] Error during update: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Simulate EV connection.
	 */
	public void connectEv() {
		this.stateMachine.connectEv();
		System.out.println("[ABL Simulator] EV connected");
	}

	/**
	 * Simulate EV disconnection.
	 */
	public void disconnectEv() {
		this.stateMachine.disconnectEv();
		System.out.println("[ABL Simulator] EV disconnected");
	}

	/**
	 * Inject an error state.
	 *
	 * @param errorState       error state to inject
	 * @param autoRecoverAfter duration until auto-recovery (null = no recovery)
	 */
	public void injectError(ChargingState errorState, Duration autoRecoverAfter) {
		this.stateMachine.injectError(errorState, autoRecoverAfter);
		System.out.println("[ABL Simulator] Error injected: " + errorState.getName());
	}

	/**
	 * Clear injected error.
	 */
	public void clearError() {
		this.stateMachine.clearError();
		System.out.println("[ABL Simulator] Error cleared");
	}

	/**
	 * Force state change (for testing).
	 *
	 * @param state the new state
	 */
	public void forceState(ChargingState state) {
		this.stateMachine.forceState(state);
		System.out.println("[ABL Simulator] State forced to: " + state.getName());
	}

	/**
	 * Set phase currents manually (for testing).
	 *
	 * @param l1 phase 1 current in Ampere
	 * @param l2 phase 2 current in Ampere
	 * @param l3 phase 3 current in Ampere
	 */
	public void setPhaseCurrents(int l1, int l2, int l3) {
		this.stateMachine.setPhaseCurrents(l1, l2, l3);
		System.out.println(String.format("[ABL Simulator] Phase currents set: L1=%dA, L2=%dA, L3=%dA", l1, l2, l3));
	}

	/**
	 * Get current state.
	 *
	 * @return current charging state
	 */
	public ChargingState getCurrentState() {
		return this.stateMachine.getCurrentState();
	}

	/**
	 * Check if EV is connected.
	 *
	 * @return true if EV is connected
	 */
	public boolean isEvConnected() {
		return this.stateMachine.isEvConnected();
	}

	/**
	 * Get phase current L1.
	 *
	 * @return current in Ampere
	 */
	public int getPhaseCurrentL1() {
		return this.stateMachine.getPhaseCurrentL1();
	}

	/**
	 * Get phase current L2.
	 *
	 * @return current in Ampere
	 */
	public int getPhaseCurrentL2() {
		return this.stateMachine.getPhaseCurrentL2();
	}

	/**
	 * Get phase current L3.
	 *
	 * @return current in Ampere
	 */
	public int getPhaseCurrentL3() {
		return this.stateMachine.getPhaseCurrentL3();
	}

	/**
	 * Get current setpoint from register 0x0014.
	 *
	 * @return duty cycle × 10
	 */
	public int getCurrentSetpoint() {
		return this.registerMap.getIcmaxSetpoint();
	}

	/**
	 * Get register map (for testing).
	 *
	 * @return register map
	 */
	public RegisterMap getRegisterMap() {
		return this.registerMap;
	}

	/**
	 * Get state machine (for testing).
	 *
	 * @return state machine
	 */
	public AblStateMachine getStateMachine() {
		return this.stateMachine;
	}

	/**
	 * Get the current communication mode.
	 *
	 * @return communication mode
	 */
	public Mode getMode() {
		return this.mode;
	}

	/**
	 * Main method to run simulator standalone.
	 *
	 * <p>
	 * Usage:
	 * <ul>
	 * <li>TCP mode: java AblModbusSimulator TCP [ip] [port] [deviceId]</li>
	 * <li>Serial ASCII mode: java AblModbusSimulator SERIAL_ASCII [port]
	 * [baudRate] [deviceId]</li>
	 * <li>Serial RTU mode: java AblModbusSimulator SERIAL_RTU [port] [baudRate]
	 * [deviceId]</li>
	 * </ul>
	 *
	 * @param args command line arguments
	 */
	public static void main(String[] args) {
		// Parse command line arguments
		if (args.length < 1) {
			printUsage();
			System.exit(0);
			return;
		}

		Mode mode;
		try {
			mode = Mode.valueOf(args[0].toUpperCase());
		} catch (IllegalArgumentException e) {
			System.err.println("ERROR: Unknown mode '" + args[0] + "'");
			printUsage();
			System.exit(1);
			return;
		}

		// Mode-specific defaults and parsing
		String address;
		String port;
		int baudRate;
		int deviceId;

		switch (mode) {
		case TCP:
			address = args.length > 1 ? args[1] : "127.0.0.1";
			port = args.length > 2 ? args[2] : "502";
			baudRate = 0; // Not used for TCP
			deviceId = args.length > 3 ? Integer.parseInt(args[3]) : 1;
			break;

		case SERIAL_ASCII:
		case SERIAL_RTU:
			address = args.length > 1 ? args[1] : "/dev/ttyUSB0";
			port = "0"; // Not used for Serial
			baudRate = args.length > 2 ? Integer.parseInt(args[2]) : 38400;
			deviceId = args.length > 3 ? Integer.parseInt(args[3]) : 1;
			break;

		default:
			System.err.println("ERROR: Unsupported mode");
			System.exit(1);
			return;
		}

		// Create simulator
		var simulator = new AblModbusSimulator(mode, address, port, baudRate, deviceId);

		try {
			simulator.start();

			// Print startup banner
			System.out.println("\n==============================================");
			System.out.println("    ABL EVCC2/3 Modbus Simulator");
			System.out.println("==============================================");
			System.out.println("Mode:     " + mode);

			switch (mode) {
			case TCP:
				System.out.println("Address:  " + address + ":" + port);
				break;
			case SERIAL_ASCII:
			case SERIAL_RTU:
				System.out.println("Port:     " + address);
				System.out.println("Baudrate: " + baudRate);
				System.out.println("Encoding: " + (mode == Mode.SERIAL_ASCII ? "Modbus ASCII" : "Modbus RTU"));
				break;
			}

			System.out.println("Device ID: " + deviceId);
			System.out.println("Firmware:  " + simulator.getRegisterMap().getFirmwareVersion());
			System.out.println("\nSimulator is running. Press Ctrl+C to stop.\n");

			// Add shutdown hook
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				System.out.println("\n[ABL Simulator] Caught shutdown signal");
				simulator.stop();
			}));

			// Keep running until interrupted
			Thread.currentThread().join();

		} catch (InterruptedException e) {
			System.out.println("\n[ABL Simulator] Interrupted");
			simulator.stop();
		} catch (Exception e) {
			System.err.println("\n[ABL Simulator] Fatal error: " + e.getMessage());
			e.printStackTrace();
			simulator.stop();
			System.exit(1);
		}
	}

	/**
	 * Print usage information.
	 */
	private static void printUsage() {
		System.out.println("ABL EVCC2/3 Modbus Simulator");
		System.out.println("\nUsage:");
		System.out.println("  TCP mode:");
		System.out.println("    java AblModbusSimulator TCP [ip] [port] [deviceId]");
		System.out.println("    Default: java AblModbusSimulator TCP 127.0.0.1 502 1");
		System.out.println("\n  Serial ASCII mode (as per ABL specification - default if no mode specified):");
		System.out.println("    java AblModbusSimulator SERIAL_ASCII [port] [baudRate] [deviceId]");
		System.out.println("    Default: java AblModbusSimulator SERIAL_ASCII /dev/ttyUSB0 38400 1");
		System.out.println("    Windows: java AblModbusSimulator SERIAL_ASCII COM3 38400 1");
		System.out.println("\n  Serial RTU mode:");
		System.out.println("    java AblModbusSimulator SERIAL_RTU [port] [baudRate] [deviceId]");
		System.out.println("    Default: java AblModbusSimulator SERIAL_RTU /dev/ttyUSB0 9600 1");
		System.out.println("\nExamples:");
		System.out.println("  # TCP mode on localhost");
		System.out.println("  java AblModbusSimulator TCP");
		System.out.println("\n  # Serial ASCII mode with USB-RS485 adapter (ABL spec: 38400 8E1)");
		System.out.println("  java AblModbusSimulator SERIAL_ASCII /dev/ttyUSB0 38400 1");
		System.out.println("\n  # Serial ASCII mode on Windows");
		System.out.println("  java AblModbusSimulator SERIAL_ASCII COM3 38400 1");
		System.out.println("\n  # Serial RTU mode");
		System.out.println("  java AblModbusSimulator SERIAL_RTU /dev/ttyUSB0 9600 1");
		System.out.println("\nNotes:");
		System.out.println("  - ABL EVCC2/3 specification uses Modbus ASCII at 38400 baud, 8E1");
		System.out.println("  - Use the run-simulator.sh or run-simulator.bat scripts for easier usage");
		System.out.println("  - Device ID must be between 1-16");
		System.out.println("  - For Serial modes, ensure proper permissions (Linux: dialout group)");
		System.out.println("  - RS-485 requires A-A, B-B wiring with 120Ω termination");
	}
}
