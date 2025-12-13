package io.openems.edge.evse.chargepoint.abl;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointAbilities;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;
import io.openems.edge.evse.api.common.ApplySetPoint;
import io.openems.edge.evse.chargepoint.abl.enums.ChargingState;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.common.types.MeterType;
import io.openems.edge.meter.api.PhaseRotation;

import static io.openems.edge.common.type.Phase.SingleOrThreePhase.THREE_PHASE;

import io.openems.common.channel.Level;

/**
 * Dummy implementation of ABL Charge Point for unit testing.
 *
 * <p>
 * This component simulates the behavior of an ABL EVCC2/3 charging station
 * without requiring actual hardware or Modbus communication.
 */
public class DummyAblChargePoint extends AbstractOpenemsComponent
		implements EvseChargePoint, ElectricityMeter, EvseChargePointAbl {

	private ChargingState currentState = ChargingState.A1;
	private boolean evConnected = false;
	private int currentSetpointMa = 0;
	private int phaseCurrentL1 = 0; // in Ampere
	private int phaseCurrentL2 = 0;
	private int phaseCurrentL3 = 0;
	private final int maxCurrentMa;
	private boolean readOnly;

	/**
	 * Create a DummyAblChargePoint with default configuration.
	 *
	 * @param id          component ID
	 * @param maxCurrentA maximum current in Ampere
	 * @param readOnly    read-only mode flag
	 */
	public DummyAblChargePoint(String id, int maxCurrentA, boolean readOnly) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				EvseChargePoint.ChannelId.values(), //
				EvseChargePointAbl.ChannelId.values() //
		);
		this.maxCurrentMa = maxCurrentA * 1000;
		this.readOnly = readOnly;

		// Note: Channels are automatically added by super() constructor
		// No need to manually add them here

		// Activate component
		super.activate(null, id, id + "_alias", true);

		// Set initial values
		this.updateChannels();
	}

	/**
	 * Convenience constructor with default values.
	 *
	 * @param id component ID
	 */
	public DummyAblChargePoint(String id) {
		this(id, 32, false);
	}

	/**
	 * Simulate EV connection.
	 */
	public void connectEv() {
		this.evConnected = true;
		if (this.currentState == ChargingState.A1) {
			this.setState(ChargingState.B1);
		}
	}

	/**
	 * Simulate EV disconnection.
	 */
	public void disconnectEv() {
		this.evConnected = false;
		this.setState(ChargingState.A1);
		this.phaseCurrentL1 = 0;
		this.phaseCurrentL2 = 0;
		this.phaseCurrentL3 = 0;
		this.updateChannels();
	}

	/**
	 * Set the charging state directly (for testing).
	 *
	 * @param state the new state
	 */
	public void setState(ChargingState state) {
		this.currentState = state;
		this.updateChannels();
	}

	/**
	 * Set phase currents directly (for testing).
	 *
	 * @param l1 phase 1 current in Ampere
	 * @param l2 phase 2 current in Ampere
	 * @param l3 phase 3 current in Ampere
	 */
	public void setPhaseCurrents(int l1, int l2, int l3) {
		this.phaseCurrentL1 = l1;
		this.phaseCurrentL2 = l2;
		this.phaseCurrentL3 = l3;
		this.updateChannels();
	}

	/**
	 * Inject an error state.
	 *
	 * @param errorState the error state to inject
	 */
	public void injectError(ChargingState errorState) {
		if (errorState.status == io.openems.edge.evse.chargepoint.abl.enums.Status.ERROR) {
			this.setState(errorState);
		}
	}

	/**
	 * Simulate the state machine progression based on current setpoint.
	 *
	 * <p>
	 * Note: This is a simplified simulation for testing purposes.
	 * The real implementation follows IEC 61851 sequence: A1 → B1 → B2 → C2.
	 * For the test dummy, we skip B2 to make tests simpler.
	 */
	private void simulateStateMachine() {
		if (!this.evConnected) {
			if (this.currentState != ChargingState.A1) {
				this.setState(ChargingState.A1);
			}
			return;
		}

		switch (this.currentState) {
		case B1:
			if (this.currentSetpointMa > 0) {
				// Simplified: Go directly to C2 (skip B2 for test simplicity)
				this.setState(ChargingState.C2);
				// Simulate charging currents proportional to setpoint
				int currentA = this.currentSetpointMa / 1000;
				this.phaseCurrentL1 = currentA;
				this.phaseCurrentL2 = currentA;
				this.phaseCurrentL3 = currentA;
			}
			break;
		case B2:
			if (this.currentSetpointMa > 0) {
				this.setState(ChargingState.C2);
				// Simulate charging currents proportional to setpoint
				int currentA = this.currentSetpointMa / 1000;
				this.phaseCurrentL1 = currentA;
				this.phaseCurrentL2 = currentA;
				this.phaseCurrentL3 = currentA;
			}
			break;
		case C2:
			if (this.currentSetpointMa == 0) {
				this.setState(ChargingState.B2);
				this.phaseCurrentL1 = 0;
				this.phaseCurrentL2 = 0;
				this.phaseCurrentL3 = 0;
			} else {
				// Update currents based on setpoint
				int currentA = this.currentSetpointMa / 1000;
				this.phaseCurrentL1 = currentA;
				this.phaseCurrentL2 = currentA;
				this.phaseCurrentL3 = currentA;
			}
			break;
		default:
			// Other states don't change automatically
			break;
		}
	}

	/**
	 * Update all channels with current values.
	 */
	private void updateChannels() {
		// Update ABL-specific channels
		this._setChargingState(this.currentState);
		this._setEvConnected(this.evConnected);
		this._setPhaseCurrentL1(this.phaseCurrentL1);
		this._setPhaseCurrentL2(this.phaseCurrentL2);
		this._setPhaseCurrentL3(this.phaseCurrentL3);

		// Update ElectricityMeter channels (convert A to mA)
		this._setCurrentL1(this.phaseCurrentL1 * 1000);
		this._setCurrentL2(this.phaseCurrentL2 * 1000);
		this._setCurrentL3(this.phaseCurrentL3 * 1000);

		// Set voltage (assumed 230V)
		this._setVoltageL1(230000); // 230V in mV
		this._setVoltageL2(230000);
		this._setVoltageL3(230000);

		// Calculate power (P = U * I)
		int powerL1 = 230 * this.phaseCurrentL1; // W
		int powerL2 = 230 * this.phaseCurrentL2;
		int powerL3 = 230 * this.phaseCurrentL3;
		this._setActivePowerL1(powerL1);
		this._setActivePowerL2(powerL2);
		this._setActivePowerL3(powerL3);
		this._setActivePower(powerL1 + powerL2 + powerL3);

		// Update ready for charging
		boolean isReady = EvseChargePointAblImpl.evaluateIsReadyForCharging(this.currentState);
		this._setIsReadyForCharging(isReady);
	}

	@Override
	public ChargePointAbilities getChargePointAbilities() {
		if (this.readOnly) {
			return null;
		}

		return ChargePointAbilities.create() //
				.setApplySetPoint(new ApplySetPoint.Ability.MilliAmpere(THREE_PHASE, 6000, this.maxCurrentMa)) //
				.setIsEvConnected(this.evConnected) //
				.setIsReadyForCharging(EvseChargePointAblImpl.evaluateIsReadyForCharging(this.currentState)) //
				.build();
	}

	@Override
	public void apply(ChargePointActions actions) {
		if (this.readOnly) {
			return;
		}

		this.currentSetpointMa = actions.getApplySetPointInMilliAmpere().value();
		this.simulateStateMachine();
		this.updateChannels();
	}

	@Override
	public PhaseRotation getPhaseRotation() {
		return PhaseRotation.L1_L2_L3;
	}

	@Override
	public boolean isReadOnly() {
		return this.readOnly;
	}

	@Override
	public MeterType getMeterType() {
		return this.readOnly ? MeterType.CONSUMPTION_METERED : MeterType.MANAGED_CONSUMPTION_METERED;
	}

	// Setters for channels (using protected methods from AbstractOpenemsComponent)
	private void _setChargingState(ChargingState state) {
		this.channel(EvseChargePointAbl.ChannelId.CHARGING_STATE).setNextValue(state);
	}

	private void _setEvConnected(boolean connected) {
		this.channel(EvseChargePointAbl.ChannelId.EV_CONNECTED).setNextValue(connected);
	}

	private void _setPhaseCurrentL1(int current) {
		this.channel(EvseChargePointAbl.ChannelId.PHASE_CURRENT_L1).setNextValue(current);
	}

	private void _setPhaseCurrentL2(int current) {
		this.channel(EvseChargePointAbl.ChannelId.PHASE_CURRENT_L2).setNextValue(current);
	}

	private void _setPhaseCurrentL3(int current) {
		this.channel(EvseChargePointAbl.ChannelId.PHASE_CURRENT_L3).setNextValue(current);
	}

	// Note: _setCurrentL1/L2/L3, _setVoltageL1/L2/L3, _setActivePowerL1/L2/L3, _setActivePower
	// are inherited from ElectricityMeter interface as public default methods.
	// No need to override them here.

	private void _setIsReadyForCharging(boolean ready) {
		this.channel(EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING).setNextValue(ready);
	}

	/**
	 * Get current state (for testing).
	 *
	 * @return current charging state
	 */
	public ChargingState getCurrentState() {
		return this.currentState;
	}

	/**
	 * Get current setpoint (for testing).
	 *
	 * @return current setpoint in mA
	 */
	public int getCurrentSetpointMa() {
		return this.currentSetpointMa;
	}

	/**
	 * Check if EV is connected (for testing).
	 *
	 * @return true if EV is connected
	 */
	public boolean isEvConnected() {
		return this.evConnected;
	}
}
