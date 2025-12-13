package io.openems.edge.evse.chargepoint.abl.simulator;

import java.io.IOException;
import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;

import io.openems.edge.evse.chargepoint.abl.enums.ChargingState;

/**
 * Main ABL EVCC2/3 Modbus TCP simulator.
 *
 * <p>
 * Combines the state machine, register map, and Modbus server to create a
 * complete hardware simulator.
 */
public class AblModbusSimulator {

	private final RegisterMap registerMap;
	private final AblStateMachine stateMachine;
	private final ModbusTcpServer modbusServer;
	private final Timer updateTimer;
	private final String ipAddress;
	private final int port;
	private int previousSetpoint = -1;

	/**
	 * Create an ABL Modbus simulator.
	 *
	 * @param ipAddress IP address to bind to (null = localhost)
	 * @param port      port number (default: 502)
	 * @param deviceId  device ID (1-16)
	 */
	public AblModbusSimulator(String ipAddress, int port, int deviceId) {
		this.ipAddress = ipAddress != null ? ipAddress : "127.0.0.1";
		this.port = port;

		// Initialize components
		this.registerMap = new RegisterMap(deviceId, 1, 2); // Firmware 1.2
		this.stateMachine = new AblStateMachine();
		this.modbusServer = new ModbusTcpServer(this.ipAddress, this.port, deviceId, this.registerMap);

		// Create update timer for periodic synchronization
		this.updateTimer = new Timer("ABL-Simulator-Update", true);
	}

	/**
	 * Start the simulator.
	 *
	 * @throws IOException if Modbus server cannot start
	 */
	public void start() throws IOException {
		// Start Modbus server
		this.modbusServer.start();

		// Start periodic update task (every 100ms)
		this.updateTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				AblModbusSimulator.this.update();
			}
		}, 0, 100);

		System.out.println("[ABL Simulator] Simulator started on " + this.ipAddress + ":" + this.port);
	}

	/**
	 * Stop the simulator.
	 */
	public void stop() {
		System.out.println("[ABL Simulator] Stopping simulator...");

		// Stop update timer
		this.updateTimer.cancel();

		// Stop Modbus server
		this.modbusServer.stop();

		// Shutdown state machine
		this.stateMachine.shutdown();

		System.out.println("[ABL Simulator] Simulator stopped");
	}

	/**
	 * Periodic update: sync between components.
	 */
	private void update() {
		try {
			// 1. Sync register map TO Modbus (for reads)
			this.registerMap.updateStateRegisters(//
					this.stateMachine.getCurrentState(), //
					this.stateMachine.isEvConnected(), //
					this.stateMachine.getPhaseCurrentL1(), //
					this.stateMachine.getPhaseCurrentL2(), //
					this.stateMachine.getPhaseCurrentL3());

			this.modbusServer.syncFromRegisterMap();

			// 2. Sync Modbus TO register map (for writes)
			this.modbusServer.syncToRegisterMap();

			// 3. Check if setpoint has changed
			int currentSetpoint = this.registerMap.getIcmaxSetpoint();
			if (currentSetpoint != this.previousSetpoint) {
				this.stateMachine.onCurrentSetpointChanged(currentSetpoint);
				this.previousSetpoint = currentSetpoint;
			}

			// 4. Check for communication timeout
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
	 * Main method to run simulator standalone.
	 *
	 * @param args command line arguments
	 */
	public static void main(String[] args) {
		String ip = args.length > 0 ? args[0] : "127.0.0.1";
		int port = args.length > 1 ? Integer.parseInt(args[1]) : 502;
		int deviceId = args.length > 2 ? Integer.parseInt(args[2]) : 1;

		AblModbusSimulator simulator = new AblModbusSimulator(ip, port, deviceId);

		try {
			simulator.start();

			System.out.println("\n==============================================");
			System.out.println("ABL EVCC2/3 Modbus Simulator");
			System.out.println("==============================================");
			System.out.println("Listening on: " + ip + ":" + port);
			System.out.println("Device ID: " + deviceId);
			System.out.println("Firmware: " + simulator.getRegisterMap().getFirmwareVersion());
			System.out.println("\nPress Ctrl+C to stop...\n");

			// Add shutdown hook
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				System.out.println("\nShutting down...");
				simulator.stop();
			}));

			// Keep running
			Thread.currentThread().join();

		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
			simulator.stop();
			System.exit(1);
		}
	}
}
