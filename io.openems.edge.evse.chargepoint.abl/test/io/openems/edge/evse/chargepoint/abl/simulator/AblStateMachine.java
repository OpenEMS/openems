package io.openems.edge.evse.chargepoint.abl.simulator;

import java.time.Duration;
import java.time.Instant;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import io.openems.edge.evse.chargepoint.abl.enums.ChargingState;

/**
 * ABL EVCC2/3 state machine simulator.
 *
 * <p>
 * Simulates the state transitions and behavior of an ABL charging station based
 * on:
 * <ul>
 * <li>EV connection status
 * <li>Current setpoint (Icmax from register 0x0014)
 * <li>Error conditions
 * </ul>
 */
public class AblStateMachine {

	private ChargingState currentState = ChargingState.A1;
	private final AtomicBoolean evConnected = new AtomicBoolean(false);
	private int currentSetpointDutyCycleX10 = 0; // From register 0x0014
	private int phaseCurrentL1 = 0; // Ampere
	private int phaseCurrentL2 = 0;
	private int phaseCurrentL3 = 0;
	private ChargingState injectedErrorState = null;
	private Timer autoRecoveryTimer = null;
	private Instant lastModbusWrite = Instant.now();

	/**
	 * Handle EV connection (plug-in).
	 */
	public synchronized void connectEv() {
		this.evConnected.set(true);
		if (this.currentState == ChargingState.A1) {
			this.transitionTo(ChargingState.B1);
		}
	}

	/**
	 * Handle EV disconnection (unplug).
	 */
	public synchronized void disconnectEv() {
		this.evConnected.set(false);
		this.transitionTo(ChargingState.A1);
		this.resetCurrents();
	}

	/**
	 * Handle current setpoint change from register 0x0014.
	 *
	 * @param dutyCycleX10 duty cycle percentage × 10 (0x0050...0x03E8)
	 */
	public synchronized void onCurrentSetpointChanged(int dutyCycleX10) {
		this.currentSetpointDutyCycleX10 = dutyCycleX10;
		this.lastModbusWrite = Instant.now();

		// If in error state F4 (communication timeout), recover
		if (this.currentState == ChargingState.F4) {
			this.transitionTo(ChargingState.E2); // Recovery to setup mode
			return;
		}

		// Don't process if in error state
		if (this.currentState.status == io.openems.edge.evse.chargepoint.abl.enums.Status.ERROR) {
			return;
		}

		// State machine logic based on setpoint
		if (!this.evConnected.get()) {
			if (this.currentState != ChargingState.A1) {
				this.transitionTo(ChargingState.A1);
			}
			return;
		}

		// EV is connected
		switch (this.currentState) {
		case A1:
			// Shouldn't happen (EV connected should be in B1)
			this.transitionTo(ChargingState.B1);
			break;

		case B1:
			if (dutyCycleX10 > 0) {
				this.transitionTo(ChargingState.B2);
				// Automatically transition to C2 after short delay
				this.scheduleTransition(ChargingState.C2, 500);
			}
			break;

		case B2:
			if (dutyCycleX10 == 0) {
				// Stop charging
				this.resetCurrents();
			} else if (this.currentState == ChargingState.B2) {
				// Should already be transitioning to C2
			}
			break;

		case C2:
		case C3:
		case C4:
			if (dutyCycleX10 == 0) {
				// Stop charging
				this.transitionTo(ChargingState.B2);
				this.resetCurrents();
			} else {
				// Update charging currents based on setpoint
				this.updateChargingCurrents(dutyCycleX10);
			}
			break;

		case E0:
		case E1:
		case E2:
		case E3:
			// Setup/disabled states - no automatic transitions
			break;

		default:
			// Other states
			break;
		}
	}

	/**
	 * Inject an error state.
	 *
	 * @param errorState       the error state to inject
	 * @param autoRecoverAfter duration after which to auto-recover (null = no
	 *                         recovery)
	 */
	public synchronized void injectError(ChargingState errorState, Duration autoRecoverAfter) {
		if (errorState.status != io.openems.edge.evse.chargepoint.abl.enums.Status.ERROR) {
			return;
		}

		this.injectedErrorState = errorState;
		this.transitionTo(errorState);
		this.resetCurrents();

		// Schedule auto-recovery if requested
		if (autoRecoverAfter != null) {
			this.scheduleAutoRecovery(autoRecoverAfter);
		}
	}

	/**
	 * Clear injected error and recover.
	 */
	public synchronized void clearError() {
		if (this.currentState.status == io.openems.edge.evse.chargepoint.abl.enums.Status.ERROR) {
			this.injectedErrorState = null;
			this.transitionTo(ChargingState.E2); // Recovery to setup mode
		}
	}

	/**
	 * Force state change (for testing).
	 *
	 * @param state the new state
	 */
	public synchronized void forceState(ChargingState state) {
		this.transitionTo(state);
	}

	/**
	 * Check for communication timeout (no Modbus writes for 15 seconds).
	 */
	public synchronized void checkCommunicationTimeout() {
		if (Duration.between(this.lastModbusWrite, Instant.now()).getSeconds() > 15) {
			if (this.currentState != ChargingState.F4
					&& this.currentState.status != io.openems.edge.evse.chargepoint.abl.enums.Status.ERROR) {
				this.transitionTo(ChargingState.F4);
				this.resetCurrents();
			}
		}
	}

	/**
	 * Get current state.
	 *
	 * @return current charging state
	 */
	public ChargingState getCurrentState() {
		return this.currentState;
	}

	/**
	 * Check if EV is connected.
	 *
	 * @return true if EV is connected
	 */
	public boolean isEvConnected() {
		return this.evConnected.get();
	}

	/**
	 * Get phase current L1.
	 *
	 * @return current in Ampere
	 */
	public int getPhaseCurrentL1() {
		return this.phaseCurrentL1;
	}

	/**
	 * Get phase current L2.
	 *
	 * @return current in Ampere
	 */
	public int getPhaseCurrentL2() {
		return this.phaseCurrentL2;
	}

	/**
	 * Get phase current L3.
	 *
	 * @return current in Ampere
	 */
	public int getPhaseCurrentL3() {
		return this.phaseCurrentL3;
	}

	/**
	 * Set phase currents manually (for testing).
	 *
	 * @param l1 phase 1 current in Ampere
	 * @param l2 phase 2 current in Ampere
	 * @param l3 phase 3 current in Ampere
	 */
	public synchronized void setPhaseCurrents(int l1, int l2, int l3) {
		this.phaseCurrentL1 = Math.max(0, Math.min(80, l1));
		this.phaseCurrentL2 = Math.max(0, Math.min(80, l2));
		this.phaseCurrentL3 = Math.max(0, Math.min(80, l3));
	}

	/**
	 * Transition to a new state.
	 *
	 * @param newState the new state
	 */
	private void transitionTo(ChargingState newState) {
		if (this.currentState != newState) {
			System.out.println("[ABL Simulator] State transition: " + this.currentState.getName() + " -> "
					+ newState.getName());
			this.currentState = newState;
		}
	}

	/**
	 * Schedule a state transition after a delay.
	 *
	 * @param targetState the target state
	 * @param delayMs     delay in milliseconds
	 */
	private void scheduleTransition(ChargingState targetState, long delayMs) {
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				synchronized (AblStateMachine.this) {
					if (AblStateMachine.this.currentState != targetState) {
						AblStateMachine.this.transitionTo(targetState);
						if (targetState == ChargingState.C2) {
							// Start charging
							AblStateMachine.this.updateChargingCurrents(
									AblStateMachine.this.currentSetpointDutyCycleX10);
						}
					}
				}
			}
		}, delayMs);
	}

	/**
	 * Schedule automatic error recovery.
	 *
	 * @param duration duration after which to recover
	 */
	private void scheduleAutoRecovery(Duration duration) {
		if (this.autoRecoveryTimer != null) {
			this.autoRecoveryTimer.cancel();
		}

		this.autoRecoveryTimer = new Timer();
		this.autoRecoveryTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				AblStateMachine.this.clearError();
			}
		}, duration.toMillis());
	}

	/**
	 * Update charging currents based on setpoint.
	 *
	 * @param dutyCycleX10 duty cycle percentage × 10
	 */
	private void updateChargingCurrents(int dutyCycleX10) {
		if (dutyCycleX10 == 0) {
			this.resetCurrents();
			return;
		}

		// Convert duty cycle to current (approximate)
		// Duty cycle 0x0064 (100, 10%) ≈ 6A
		// Duty cycle 0x0214 (532, 53.2%) ≈ 32A
		// Linear approximation: I = (dutyCycleX10 - 80) * 32 / 452 + 6
		int currentA = Math.max(6, Math.min(32, (dutyCycleX10 - 80) * 32 / 452 + 6));

		// Set balanced 3-phase currents
		this.phaseCurrentL1 = currentA;
		this.phaseCurrentL2 = currentA;
		this.phaseCurrentL3 = currentA;
	}

	/**
	 * Reset all phase currents to zero.
	 */
	private void resetCurrents() {
		this.phaseCurrentL1 = 0;
		this.phaseCurrentL2 = 0;
		this.phaseCurrentL3 = 0;
	}

	/**
	 * Shutdown the state machine.
	 */
	public void shutdown() {
		if (this.autoRecoveryTimer != null) {
			this.autoRecoveryTimer.cancel();
		}
	}
}
