package io.openems.edge.victron.ess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.type.Phase.SingleOrAllPhase;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.victron.battery.VictronBatteryImplTest;

/**
 * Tests for {@link VictronEssImpl}.
 */
public class VictronEssImplTest {

	private static final String ESS_ID = "ess0";
	private static final String MODBUS_ID = "modbus0";

	/**
	 * Write-channels that are mapped to the Victron hardware; none may ever get a
	 * next-write-value while the ESS is in Read-Only-Mode.
	 */
	private static final VictronEss.ChannelId[] HARDWARE_WRITE_CHANNELS = { //
			VictronEss.ChannelId.SET_ACTIVE_POWER_L1, //
			VictronEss.ChannelId.SET_ACTIVE_POWER_L2, //
			VictronEss.ChannelId.SET_ACTIVE_POWER_L3, //
			VictronEss.ChannelId.ESS_DISABLE_CHARGE_FLAG, //
			VictronEss.ChannelId.ESS_DISABLE_FEEDBACK_FLAG, //
	};

	@Test
	public void test() throws OpenemsException, Exception {
		new ComponentTest(new VictronEssImpl()) //
				.addReference("battery", VictronBatteryImplTest.createVictronBattery().sut) //
				.activate(MyConfig.create() //
						.setId(ESS_ID) //
						.setAlias("Victron ESS") //
						.setEnabled(true) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(227) //
						.setPhase(SingleOrAllPhase.ALL) //
						.setDebugMode(false) //
						.setReadOnlyMode(false) //
						.setCapacity(10000) //
						.setMaxApparentPower(5000) //
						.build()) //
				.next(new TestCase() //
						.output(SymmetricEss.ChannelId.CAPACITY, 10000)) //
				.deactivate();
	}

	@Test
	public void testChannelIdCount() {
		// Verify that all ChannelIds are defined
		var channelIds = VictronEss.ChannelId.values();
		assertNotNull(channelIds);
		// Should have many channels defined
		assertEquals(true, channelIds.length > 50);
	}

	@Test
	public void testChannelIdDoc() {
		// Verify that all ChannelIds have a doc
		for (var channelId : VictronEss.ChannelId.values()) {
			assertNotNull(channelId.doc(), "ChannelId " + channelId.name() + " should have a doc");
		}
	}

	@Test
	public void testConfigBuilder() {
		var config = MyConfig.create() //
				.setId(ESS_ID) //
				.setAlias("Victron ESS") //
				.setEnabled(true) //
				.setModbusId(MODBUS_ID) //
				.setModbusUnitId(227) //
				.setPhase(SingleOrAllPhase.ALL) //
				.setDebugMode(false) //
				.setReadOnlyMode(false) //
				.setCapacity(10000) //
				.setMaxApparentPower(5000) //
				.build();

		assertEquals(ESS_ID, config.id());
		assertEquals("Victron ESS", config.alias());
		assertEquals(true, config.enabled());
		assertEquals(MODBUS_ID, config.modbus_id());
		assertEquals(227, config.modbusUnitId());
		assertEquals(SingleOrAllPhase.ALL, config.phase());
		assertEquals(false, config.debugMode());
		assertEquals(false, config.readOnlyMode());
		assertEquals(10000, config.capacity());
		assertEquals(5000, config.maxApparentPower());
	}

	@Test
	public void testConfigSinglePhaseL1() {
		var config = MyConfig.create() //
				.setId(ESS_ID) //
				.setPhase(SingleOrAllPhase.L1) //
				.build();

		assertEquals(SingleOrAllPhase.L1, config.phase());
	}

	@Test
	public void testConfigSinglePhaseL2() {
		var config = MyConfig.create() //
				.setId(ESS_ID) //
				.setPhase(SingleOrAllPhase.L2) //
				.build();

		assertEquals(SingleOrAllPhase.L2, config.phase());
	}

	@Test
	public void testConfigSinglePhaseL3() {
		var config = MyConfig.create() //
				.setId(ESS_ID) //
				.setPhase(SingleOrAllPhase.L3) //
				.build();

		assertEquals(SingleOrAllPhase.L3, config.phase());
	}

	@Test
	public void testConfigReadOnlyMode() {
		var config = MyConfig.create() //
				.setId(ESS_ID) //
				.setReadOnlyMode(true) //
				.build();

		assertEquals(true, config.readOnlyMode());
	}

	@Test
	public void testVictronEssConstructor() {
		var victronEss = new VictronEssImpl();
		assertNotNull(victronEss);
	}

	@Test
	public void testGetPowerPrecision() {
		var victronEss = new VictronEssImpl();
		assertEquals(100, victronEss.getPowerPrecision());
	}

	@Test
	public void testCalculateAcInSetpoint_chargeWithAcOutLoad() {
		// Issue #3573: Charge request of 3kW with 5kW AC-out load
		// Battery should charge at 3kW, so AC-in must be 8kW
		var result = VictronEssImpl.calculateAcInSetpoint(-3000, 5000, 3000, 3000);
		assertEquals(-8000, result);
	}

	@Test
	public void testCalculateAcInSetpoint_chargeExceedsMaxWithAcOut() {
		// Charge request of 5kW exceeds maxChargePower of 3kW, with 2kW AC-out
		// Should clamp to -3kW charge, then subtract 2kW AC-out => -5kW
		var result = VictronEssImpl.calculateAcInSetpoint(-5000, 2000, 3000, 3000);
		assertEquals(-5000, result);
	}

	@Test
	public void testCalculateAcInSetpoint_chargeWithinLimitsNoAcOut() {
		// Charge request of 2kW, no AC-out, maxCharge 3kW
		var result = VictronEssImpl.calculateAcInSetpoint(-2000, 0, 3000, 3000);
		assertEquals(-2000, result);
	}

	@Test
	public void testCalculateAcInSetpoint_dischargeWithinLimits() {
		// Discharge request of 2kW, maxDischarge 3kW
		var result = VictronEssImpl.calculateAcInSetpoint(2000, 1000, 3000, 3000);
		assertEquals(2000, result);
	}

	@Test
	public void testCalculateAcInSetpoint_dischargeExceedsMax() {
		// Discharge request of 5kW, maxDischarge 3kW => clamped to 3kW
		var result = VictronEssImpl.calculateAcInSetpoint(5000, 0, 3000, 3000);
		assertEquals(3000, result);
	}

	@Test
	public void testCalculateAcInSetpoint_zeroPower() {
		// Zero power target, should remain zero
		var result = VictronEssImpl.calculateAcInSetpoint(0, 5000, 3000, 3000);
		assertEquals(0, result);
	}

	/**
	 * Regression test for the Read-Only-Mode bug: the symmetric
	 * {@code applyPower(int, int)} used to set the disable-charge/-discharge flags
	 * before checking Read-Only-Mode, leaking those write-values to the hardware.
	 * It must not enqueue any hardware write in Read-Only-Mode.
	 */
	@Test
	public void testApplyPowerSymmetricDoesNotWriteInReadOnlyMode() throws Exception {
		var ess = activatedReadOnlyEss(SingleOrAllPhase.ALL);

		ess.applyPower(1000, 0);

		assertNoHardwareWrites(ess);
	}

	/**
	 * Same regression on the asymmetric {@code applyPower(p1, q1, p2, q2, p3, q3)}
	 * overload.
	 */
	@Test
	public void testApplyPowerAsymmetricDoesNotWriteInReadOnlyMode() throws Exception {
		var ess = activatedReadOnlyEss(SingleOrAllPhase.ALL);

		ess.applyPower(1000, 0, 1000, 0, 1000, 0);

		assertNoHardwareWrites(ess);
	}

	/**
	 * A zero power-target still disables charge/discharge via write-flags; this
	 * must also be suppressed in Read-Only-Mode.
	 */
	@Test
	public void testApplyPowerZeroDoesNotWriteInReadOnlyMode() throws Exception {
		var ess = activatedReadOnlyEss(SingleOrAllPhase.ALL);

		ess.applyPower(0, 0);
		ess.applyPower(0, 0, 0, 0, 0, 0);

		assertNoHardwareWrites(ess);
	}

	/**
	 * Complements the Read-Only-Mode regression tests by pinning the guard's
	 * disabled branch: with Read-Only-Mode <em>off</em>, the symmetric
	 * {@code applyPower(int, int)} must fall through the Read-Only-Mode guard. Here
	 * the readiness gate is left closed, so the method stops right after the guard
	 * without needing a battery-inverter reference; the point is that the guard did
	 * not short-circuit and no hardware write leaked from the readiness gate
	 * either.
	 */
	@Test
	public void testApplyPowerSymmetricFallsThroughReadOnlyGuardWhenDisabled() throws Exception {
		var ess = activatedEss(SingleOrAllPhase.ALL, false, false);

		ess.applyPower(1000, 0);

		assertNoHardwareWrites(ess);
	}

	/**
	 * Same disabled-branch coverage on the asymmetric
	 * {@code applyPower(p1, q1, p2, q2, p3, q3)} overload.
	 */
	@Test
	public void testApplyPowerAsymmetricFallsThroughReadOnlyGuardWhenDisabled() throws Exception {
		var ess = activatedEss(SingleOrAllPhase.ALL, false, false);

		ess.applyPower(1000, 0, 1000, 0, 1000, 0);

		assertNoHardwareWrites(ess);
	}

	private static VictronEssImpl activatedReadOnlyEss(SingleOrAllPhase phase) throws Exception {
		return activatedEss(phase, true, true);
	}

	/**
	 * Activates a {@link VictronEssImpl}.
	 *
	 * @param phase         the configured phase
	 * @param readOnlyMode  whether to activate in Read-Only-Mode
	 * @param operationalOk the value to force on {@code operationalValuesOk};
	 *                      {@code true} lets {@code applyPower(...)} run past the
	 *                      readiness gate, {@code false} leaves the gate closed so
	 *                      the method returns right after the Read-Only-Mode guard
	 *                      (used to exercise that guard's disabled branch without a
	 *                      battery-inverter reference)
	 * @return the activated component
	 * @throws Exception on error
	 */
	private static VictronEssImpl activatedEss(SingleOrAllPhase phase, boolean readOnlyMode, boolean operationalOk)
			throws Exception {
		var ess = new VictronEssImpl();
		new ComponentTest(ess) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(ESS_ID) //
						.setAlias("Victron ESS") //
						.setEnabled(true) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(227) //
						.setPhase(phase) //
						.setReadOnlyMode(readOnlyMode) //
						.setCapacity(10000) //
						.setMaxApparentPower(5000) //
						.build());

		var operationalValuesOk = VictronEssImpl.class.getDeclaredField("operationalValuesOk");
		operationalValuesOk.setAccessible(true);
		operationalValuesOk.setBoolean(ess, operationalOk);

		return ess;
	}

	private static void assertNoHardwareWrites(VictronEssImpl ess) {
		for (var channelId : HARDWARE_WRITE_CHANNELS) {
			WriteChannel<?> channel = ess.channel(channelId);
			assertTrue(channel.getNextWriteValue().isEmpty(), "Read-Only-Mode leaked a write to " + channelId.name());
		}
	}

}
