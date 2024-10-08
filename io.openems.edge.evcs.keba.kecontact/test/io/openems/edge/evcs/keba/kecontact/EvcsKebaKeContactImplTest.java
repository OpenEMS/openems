package io.openems.edge.evcs.keba.kecontact;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Phases;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.evcs.keba.kecontact.core.EvcsKebaKeContactCoreImpl;
import io.openems.edge.evcs.test.DummyEvcsPower;

public class EvcsKebaKeContactImplTest {

	private static final String COMPONENT_ID = "evcs0";
	private EvcsKebaKeContactImpl evcs;

	// Channel Addresses EVCS0
	private static ChannelAddress status = new ChannelAddress("evcs0", Evcs.ChannelId.STATUS.id());
	private static ChannelAddress chargePower = new ChannelAddress("evcs0", Evcs.ChannelId.CHARGE_POWER.id());
	private static ChannelAddress phases = new ChannelAddress("evcs0", Evcs.ChannelId.PHASES.id());
	private static ChannelAddress minimumHardwarePower = new ChannelAddress("evcs0",
			Evcs.ChannelId.MINIMUM_HARDWARE_POWER.id());
	private static ChannelAddress maximumHardwarePower = new ChannelAddress("evcs0",
			Evcs.ChannelId.MAXIMUM_HARDWARE_POWER.id());
	private static ChannelAddress fixedMaximumHwPower = new ChannelAddress("evcs0",
			Evcs.ChannelId.FIXED_MAXIMUM_HARDWARE_POWER.id());
	private static ChannelAddress chargingstationCommunicationFailed = new ChannelAddress("evcs0",
			Evcs.ChannelId.CHARGINGSTATION_COMMUNICATION_FAILED.id());

	private static ChannelAddress setChargePowerLimit = new ChannelAddress("evcs0",
			ManagedEvcs.ChannelId.SET_CHARGE_POWER_LIMIT.id());

	@Test
	public void testAdjustPowerLimitsOnPhaseSwitching() throws OpenemsException, Exception {

		this.evcs = new EvcsKebaKeContactImpl();
		new ComponentTest(this.evcs) //
				.addReference("evcsPower", new DummyEvcsPower()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("kebaKeContactCore", new EvcsKebaKeContactCoreImpl()) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setDebugMode(false) //
						.setIp("172.0.0.1") //
						.setMinHwCurrent(6000) //
						.setUseDisplay(false) //
						.setPhaseSwitchActive(true) //
						.build())
				.next(new TestCase("Preparation").inputForce(
						new ChannelAddress(COMPONENT_ID, EvcsKebaKeContact.ChannelId.X2_PHASE_SWITCH_SOURCE.id()), 4))
				.next(new TestCase("Test 1") //
						.input(status, Status.CHARGING) //
						.input(chargePower, 5000) //
						.input(setChargePowerLimit, 7000) //
						.input(fixedMaximumHwPower, 22080) //
						.input(chargingstationCommunicationFailed, false) //
						.input(phases, Phases.THREE_PHASE) //
						// Charge power itself would be read by the evcs next cycle
						.output(phases, Phases.THREE_PHASE) //
						.output(minimumHardwarePower, 18 * 230) //
						.output(maximumHardwarePower, 32 * 3 * 230) //
						.onBeforeWriteCallbacks(() -> { //
							// Assert that X2_PHASE_SWITCH_SOURCE is set to 4
							IntegerReadChannel channel = this.evcs.getX2PhaseSwitchSourceChannel();
							Integer expectedValue = 4;
							Integer actualValue = channel.value().orElse(-1);
							assertEquals("X2 Phase Switch Source should be set to 4 before applying charge power limit",
									expectedValue, actualValue);
						}).onAfterWriteCallbacks(() -> {
							// Verify if phase switch was correctly executed
							IntegerReadChannel channel = this.evcs.getX2PhaseSwitchSourceChannel();
							Integer expectedValue = 4;
							Integer actualValue = channel.value().orElse(0);
							assertEquals("X2 Phase Switch Source should remain at 4 after operations", expectedValue,
									actualValue);
						}));
		// TODO: Logic would be easier to test if the phase switch part would be in a
		// static
		// method and the sendCommand would be passed in a Consumer/Function
	}

	// @Test
	// public void testHandlingIncreasedPowerDemandWithPhaseSwitching() throws
	// OpenemsException {
	// System.out.println("Starting
	// testHandlingIncreasedPowerDemandWithPhaseSwitching");
	//
	// // Initialize the EVCS with one phase and a specific charging power
	// System.out.println("Setting phases to ONE_PHASE and charging power to
	// 3680W");
	// this.evcs._setPhases(1);
	// assertEquals("Phase should be ONE_PHASE before applying charge power limit",
	// 1, this.evcs.getPhasesAsInt());
	// this.evcs._setChargePower(3680); // Start with 3680W on one phase
	//
	// // Increase target power demand to 5000W, should trigger phase switching
	// logic
	// System.out.println("Applying charge power limit of 5000W, expecting phase
	// switch");
	// this.evcs.applyChargePowerLimit(5000);
	//
	// // Verify if phase switching to three phases occurred to meet the new power
	// // demand
	// System.out.println("Verifying if phase switching to THREE_PHASE occurred");
	// assertEquals("Should switch to three phases", Phases.THREE_PHASE,
	// this.evcs.getPhasesAsInt());
	//
	// // Expected min/max power range after accommodating the increased power
	// demand
	// Integer expectedMin = 1380; // Min power remains the same as one phase
	// Integer expectedMax = 22080; // Max power with phase switching for an EVCS
	// capable of 32A
	//
	// // Validate the adjusted min/max power limits
	// System.out.println("Validating adjusted min/max power limits after increased
	// demand");
	// assertEquals("Adjusted Min Power should be " + expectedMin, expectedMin,
	// this.evcs.getMinimumHardwarePower().get());
	// assertEquals("Adjusted Max Power should be " + expectedMax, expectedMax,
	// this.evcs.getMaximumHardwarePower().get());
	// }

	// @Test
	// public void testShouldSwitchToThreePhases() {
	// // Scenario: High power demand on single phase
	// int highPowerDemand = 5000; // Watts
	// int currentPhases = 1;
	// assertTrue("EVCS should switch to three phases for high power demand",
	// this.evcs.shouldSwitchToThreePhases(highPowerDemand, currentPhases));
	// }
	//
	// @Test
	// public void testShouldNotSwitchToThreePhases() {
	// // Scenario: Low power demand does not require three phases
	// int lowPowerDemand = 3000; // Watts
	// int currentPhases = 1;
	// assertFalse("EVCS should not switch to three phases for low power demand",
	// this.evcs.shouldSwitchToThreePhases(lowPowerDemand, currentPhases));
	// }
	//
	// @Test
	// public void testShouldSwitchToOnePhase() {
	// // Scenario: Low power demand on three phases
	// int lowPowerDemand = 2000; // Watts
	// int currentPhases = 3;
	// assertTrue("EVCS should switch to one phase for low power demand",
	// this.evcs.shouldSwitchToOnePhase(lowPowerDemand, currentPhases));
	// }
	//
	// @Test
	// public void testShouldNotSwitchToOnePhase() {
	// // Scenario: High power demand does not benefit from reducing to one phase
	// int highPowerDemand = 7000; // Watts
	// int currentPhases = 3;
	// assertFalse("EVCS should not switch to one phase for high power demand",
	// this.evcs.shouldSwitchToOnePhase(highPowerDemand, currentPhases));
	// }

}
