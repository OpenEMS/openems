package io.openems.edge.evse.chargepoint.abl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import io.openems.edge.evse.api.chargepoint.Profile.ChargePointAbilities;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;
import io.openems.edge.evse.api.common.ApplySetPoint;
import io.openems.edge.evse.chargepoint.abl.enums.ChargingState;

/**
 * Unit tests for DummyAblChargePoint.
 */
public class DummyAblChargePointTest {

	private DummyAblChargePoint chargePoint;

	@Before
	public void setUp() {
		this.chargePoint = new DummyAblChargePoint("test0");
	}

	@Test
	public void testInitialState() {
		assertEquals(ChargingState.A1, this.chargePoint.getCurrentState());
		assertFalse(this.chargePoint.isEvConnected());
		assertEquals(0, this.chargePoint.getCurrentSetpointMa());
	}

	@Test
	public void testEvConnection() {
		// Initially no EV
		assertEquals(ChargingState.A1, this.chargePoint.getCurrentState());

		// Connect EV
		this.chargePoint.connectEv();
		assertTrue(this.chargePoint.isEvConnected());
		assertEquals(ChargingState.B1, this.chargePoint.getCurrentState());
	}

	@Test
	public void testEvDisconnection() {
		// Connect EV first
		this.chargePoint.connectEv();
		this.chargePoint.setState(ChargingState.C2);

		// Disconnect
		this.chargePoint.disconnectEv();
		assertFalse(this.chargePoint.isEvConnected());
		assertEquals(ChargingState.A1, this.chargePoint.getCurrentState());
	}

	@Test
	public void testNormalChargingCycle() {
		// 1. Initial state A1
		assertEquals(ChargingState.A1, this.chargePoint.getCurrentState());

		// 2. EV connects -> B1
		this.chargePoint.connectEv();
		assertEquals(ChargingState.B1, this.chargePoint.getCurrentState());

		// 3. Get abilities
		ChargePointAbilities abilities = this.chargePoint.getChargePointAbilities();
		assertNotNull(abilities);
		assertTrue(abilities.isEvConnected());

		// 4. Apply charging current -> B2 -> C2
		ChargePointActions actions = ChargePointActions.from(abilities) //
				.setApplySetPoint(new ApplySetPoint.Action.MilliAmpere(16000)) // 16A
				.build();
		this.chargePoint.apply(actions);

		// Should transition to B2 first, then C2
		// (In dummy implementation this happens immediately)
		assertEquals(ChargingState.C2, this.chargePoint.getCurrentState());
		assertEquals(16000, this.chargePoint.getCurrentSetpointMa());

		// 5. Stop charging
		actions = ChargePointActions.from(abilities) //
				.setApplySetPoint(new ApplySetPoint.Action.MilliAmpere(0)) //
				.build();
		this.chargePoint.apply(actions);
		assertEquals(ChargingState.B2, this.chargePoint.getCurrentState());

		// 6. Disconnect EV -> A1
		this.chargePoint.disconnectEv();
		assertEquals(ChargingState.A1, this.chargePoint.getCurrentState());
	}

	@Test
	public void testPhaseCurrents() {
		this.chargePoint.setPhaseCurrents(10, 12, 14);

		// Check ABL channels (in Ampere)
		assertEquals(Integer.valueOf(10),
				this.chargePoint.channel(EvseChargePointAbl.ChannelId.PHASE_CURRENT_L1).getNextValue().get());
		assertEquals(Integer.valueOf(12),
				this.chargePoint.channel(EvseChargePointAbl.ChannelId.PHASE_CURRENT_L2).getNextValue().get());
		assertEquals(Integer.valueOf(14),
				this.chargePoint.channel(EvseChargePointAbl.ChannelId.PHASE_CURRENT_L3).getNextValue().get());

		// Check ElectricityMeter channels (in mA)
		assertEquals(Integer.valueOf(10000),
				this.chargePoint.channel(io.openems.edge.meter.api.ElectricityMeter.ChannelId.CURRENT_L1).getNextValue()
						.get());
		assertEquals(Integer.valueOf(12000),
				this.chargePoint.channel(io.openems.edge.meter.api.ElectricityMeter.ChannelId.CURRENT_L2).getNextValue()
						.get());
		assertEquals(Integer.valueOf(14000),
				this.chargePoint.channel(io.openems.edge.meter.api.ElectricityMeter.ChannelId.CURRENT_L3).getNextValue()
						.get());
	}

	@Test
	public void testPowerCalculation() {
		this.chargePoint.connectEv();
		ChargePointAbilities abilities = this.chargePoint.getChargePointAbilities();

		// Set 16A charging current
		ChargePointActions actions = ChargePointActions.from(abilities) //
				.setApplySetPoint(new ApplySetPoint.Action.MilliAmpere(16000)) //
				.build();
		this.chargePoint.apply(actions);

		// Check power (P = U * I = 230V * 16A = 3680W per phase)
		assertEquals(Integer.valueOf(3680),
				this.chargePoint.channel(io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_POWER_L1).getNextValue()
						.get());
		assertEquals(Integer.valueOf(3680),
				this.chargePoint.channel(io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_POWER_L2).getNextValue()
						.get());
		assertEquals(Integer.valueOf(3680),
				this.chargePoint.channel(io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_POWER_L3).getNextValue()
						.get());

		// Total power = 3 * 3680 = 11040W
		assertEquals(Integer.valueOf(11040),
				this.chargePoint.channel(io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_POWER).getNextValue()
						.get());
	}

	@Test
	public void testErrorInjection() {
		this.chargePoint.connectEv();

		// Inject overcurrent error
		this.chargePoint.injectError(ChargingState.F9);
		assertEquals(ChargingState.F9, this.chargePoint.getCurrentState());

		// Inject welding detection error
		this.chargePoint.injectError(ChargingState.F1);
		assertEquals(ChargingState.F1, this.chargePoint.getCurrentState());
	}

	@Test
	public void testReadOnlyMode() {
		DummyAblChargePoint readOnlyChargePoint = new DummyAblChargePoint("test1", 32, true);

		assertTrue(readOnlyChargePoint.isReadOnly());

		// Abilities should be null in read-only mode
		assertEquals(null, readOnlyChargePoint.getChargePointAbilities());

		// Apply should have no effect in read-only mode
		readOnlyChargePoint.connectEv();
		ChargePointAbilities abilities = ChargePointAbilities.create() //
				.setApplySetPoint(new ApplySetPoint.Ability.MilliAmpere(
						io.openems.edge.common.type.Phase.SingleOrThreePhase.THREE_PHASE, 6000, 32000)) //
				.build();
		ChargePointActions actions = ChargePointActions.from(abilities) //
				.setApplySetPoint(new ApplySetPoint.Action.MilliAmpere(16000)) //
				.build();
		readOnlyChargePoint.apply(actions);

		// Should still be in B1 (no state change)
		assertEquals(ChargingState.B1, readOnlyChargePoint.getCurrentState());
		assertEquals(0, readOnlyChargePoint.getCurrentSetpointMa());
	}

	@Test
	public void testIsReadyForCharging() {
		// A1 - not ready
		this.chargePoint.setState(ChargingState.A1);
		assertFalse((Boolean) this.chargePoint.channel(io.openems.edge.evse.api.chargepoint.EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING)
				.getNextValue().get());

		// B1 - ready
		this.chargePoint.setState(ChargingState.B1);
		assertTrue((Boolean) this.chargePoint.channel(io.openems.edge.evse.api.chargepoint.EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING)
				.getNextValue().get());

		// C2 - charging (also ready)
		this.chargePoint.setState(ChargingState.C2);
		assertTrue((Boolean) this.chargePoint.channel(io.openems.edge.evse.api.chargepoint.EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING)
				.getNextValue().get());

		// F9 - error (not ready)
		this.chargePoint.setState(ChargingState.F9);
		assertFalse((Boolean) this.chargePoint.channel(io.openems.edge.evse.api.chargepoint.EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING)
				.getNextValue().get());
	}

	@Test
	public void testMaxCurrentLimit() {
		DummyAblChargePoint limitedChargePoint = new DummyAblChargePoint("test2", 20, false);

		ChargePointAbilities abilities = limitedChargePoint.getChargePointAbilities();
		assertNotNull(abilities);

		// Max current should be 20A = 20000mA
		assertEquals(20000, ((ApplySetPoint.Ability.MilliAmpere) abilities.applySetPoint()).max());
		assertEquals(6000, ((ApplySetPoint.Ability.MilliAmpere) abilities.applySetPoint()).min());
	}
}
